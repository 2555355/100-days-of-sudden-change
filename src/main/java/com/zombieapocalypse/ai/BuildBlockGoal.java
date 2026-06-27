package com.zombieapocalypse.ai;

import com.zombieapocalypse.config.StageSystem;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.EnumSet;

/**
 * 僵尸搭方块AI (智能增强版)
 * 搭方块间隔随智能度阶梯递减
 */
public class BuildBlockGoal extends Goal {
    private final PathAwareEntity mob;
    private int buildCooldown;
    private BlockPos targetPlacePos;

    public BuildBlockGoal(PathAwareEntity mob) {
        this.mob = mob;
        this.buildCooldown = 0;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (buildCooldown > 0) {
            buildCooldown--;
            return false;
        }

        // 第20天后才能搭方块
        if (StageSystem.getCurrentDay(this.mob.getWorld()) < 20) return false;

        // 必须有收集到的方块材料才能搭
        if (!(this.mob instanceof IBlockCollector collector) || !collector.hasCollectedBlock()) {
            return false;
        }

        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive()) return false;

        return findPlacePosition(target);
    }

    /**
     * 寻找需要搭方块的位置
     * 优先级: 脚下垫高(直接拉近距离) > 朝玩家方向楼梯 > 前方搭桥 > 侧面搭桥
     */
    private boolean findPlacePosition(LivingEntity target) {
        World world = this.mob.getWorld();
        BlockPos mobPos = this.mob.getBlockPos();
        BlockPos targetPos = target.getBlockPos();
        Direction facing = this.mob.getHorizontalFacing();
        Direction towardPlayer = getDirectionToward(mobPos, targetPos);

        int deltaY = targetPos.getY() - mobPos.getY();
        int distToPlayer = Math.abs(targetPos.getX() - mobPos.getX()) + Math.abs(targetPos.getZ() - mobPos.getZ());
        boolean closeToPlayer = distToPlayer <= 6;

        // ===== 策略1: 玩家在上方 → 爬高(优先脚下垫高直接拉近距离) =====
        if (deltaY >= 1) {
            // 1a. 在脚下搭方块(直接垫高, 最快拉近距离)
            BlockPos feetPos = mobPos;
            if (canPlaceAt(world, feetPos)) {
                this.targetPlacePos = feetPos.toImmutable();
                return true;
            }

            // 1b. 朝玩家方向搭楼梯(优先于朝向方向, 更精准追击)
            BlockPos towardStepPos = mobPos.offset(towardPlayer);
            if (canPlaceAt(world, towardStepPos)) {
                this.targetPlacePos = towardStepPos.toImmutable();
                return true;
            }
            BlockPos towardStepUpPos = mobPos.offset(towardPlayer).up();
            if (canPlaceAt(world, towardStepUpPos)) {
                this.targetPlacePos = towardStepUpPos.toImmutable();
                return true;
            }

            // 1c. 朝向方向搭楼梯(备选)
            BlockPos stepPos = mobPos.offset(facing);
            if (canPlaceAt(world, stepPos)) {
                this.targetPlacePos = stepPos.toImmutable();
                return true;
            }
            BlockPos stepUpPos = mobPos.offset(facing).up();
            if (canPlaceAt(world, stepUpPos)) {
                this.targetPlacePos = stepUpPos.toImmutable();
                return true;
            }

            // 1d. 朝玩家方向2格搭楼梯(更远距离追击)
            if (closeToPlayer) {
                BlockPos toward2Pos = mobPos.offset(towardPlayer, 2);
                if (canPlaceAt(world, toward2Pos)) {
                    this.targetPlacePos = toward2Pos.toImmutable();
                    return true;
                }
            }
        }

        // ===== 策略2: 前方有坑/空隙 → 搭桥(朝玩家方向优先) =====
        BlockPos frontPos = mobPos.offset(towardPlayer);
        BlockPos frontBelowPos = frontPos.down();
        BlockState frontBelowState = world.getBlockState(frontBelowPos);

        if (frontBelowState.isAir() || !frontBelowState.isSolidBlock(world, frontBelowPos)) {
            if (canPlaceAt(world, frontBelowPos)) {
                BlockPos supportPos = frontBelowPos.down();
                if (world.getBlockState(supportPos).isSolidBlock(world, supportPos)) {
                    this.targetPlacePos = frontBelowPos.toImmutable();
                    return true;
                }
            }
            if (canPlaceAt(world, frontPos)) {
                this.targetPlacePos = frontPos.toImmutable();
                return true;
            }
        }

        // 朝玩家方向2格搭桥
        BlockPos front2Pos = mobPos.offset(towardPlayer, 2);
        BlockPos front2BelowPos = front2Pos.down();
        if (world.getBlockState(front2BelowPos).isAir() && canPlaceAt(world, front2BelowPos)) {
            if (world.getBlockState(front2BelowPos.down()).isSolidBlock(world, front2BelowPos.down())) {
                this.targetPlacePos = front2BelowPos.toImmutable();
                return true;
            }
        }

        // 朝向方向搭桥(备选)
        BlockPos facingPos = mobPos.offset(facing);
        BlockPos facingBelowPos = facingPos.down();
        BlockState facingBelowState = world.getBlockState(facingBelowPos);
        if (facingBelowState.isAir() || !facingBelowState.isSolidBlock(world, facingBelowPos)) {
            if (canPlaceAt(world, facingBelowPos)) {
                if (world.getBlockState(facingBelowPos.down()).isSolidBlock(world, facingBelowPos.down())) {
                    this.targetPlacePos = facingBelowPos.toImmutable();
                    return true;
                }
            }
            if (canPlaceAt(world, facingPos)) {
                this.targetPlacePos = facingPos.toImmutable();
                return true;
            }
        }

        // ===== 策略3: 侧面有坑 → 搭桥(玩家近时才扫) =====
        if (closeToPlayer) {
            for (Direction dir : Direction.values()) {
                if (dir.getAxis().isHorizontal()) {
                    BlockPos sidePos = mobPos.offset(dir);
                    BlockPos sideBelowPos = sidePos.down();
                    if (world.getBlockState(sideBelowPos).isAir() && canPlaceAt(world, sideBelowPos)) {
                        if (world.getBlockState(sideBelowPos.down()).isSolidBlock(world, sideBelowPos.down())) {
                            this.targetPlacePos = sideBelowPos.toImmutable();
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * 获取从from朝向to的水平方向
     */
    private Direction getDirectionToward(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        if (Math.abs(dx) > Math.abs(dz)) {
            return dx > 0 ? Direction.EAST : Direction.WEST;
        } else {
            return dz > 0 ? Direction.SOUTH : Direction.NORTH;
        }
    }

    /**
     * 检查是否可以在指定位置放置方块
     */
    private boolean canPlaceAt(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!state.isAir() && !state.isReplaceable()) return false;

        // 检查是否在世界上限内
        if (pos.getY() >= world.getTopY() || pos.getY() <= world.getBottomY()) return false;

        // 必须下方有支撑
        BlockPos belowPos = pos.down();
        BlockState belowState = world.getBlockState(belowPos);
        if (!belowState.isSolidBlock(world, belowPos)) return false;

        return true;
    }

    @Override
    public void start() {
        this.buildCooldown = StageSystem.getBuildInterval(this.mob.getWorld());
        if (this.targetPlacePos != null && this.mob instanceof IBlockCollector collector) {
            World world = this.mob.getWorld();
            BlockState state = world.getBlockState(this.targetPlacePos);

            if ((state.isAir() || state.isReplaceable()) && collector.hasCollectedBlock()) {
                BlockState blockToPlace = collector.consumeCollectedBlock();
                if (blockToPlace != null) {
                    world.setBlockState(this.targetPlacePos, blockToPlace);
                }
            }
            this.targetPlacePos = null;
        }
    }

    @Override
    public boolean shouldContinue() {
        return false;
    }

    @Override
    public void stop() {
        this.targetPlacePos = null;
    }
}