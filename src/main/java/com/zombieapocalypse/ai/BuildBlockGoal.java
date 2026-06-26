package com.zombieapocalypse.ai;

import com.zombieapocalypse.config.ModConfig;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.EnumSet;

/**
 * 僵尸搭方块AI (增强版)
 * 策略：
 * 1. 玩家在头顶上方 → 往脚下搭方块爬高（支持楼梯式搭建）
 * 2. 前方有坑/空隙 → 搭方块搭桥
 * 3. 玩家在侧面高处 → 搭楼梯接近
 *
 * 材料来源：需要先破坏方块收集材料，不能凭空生成
 */
public class BuildBlockGoal extends Goal {
    private final PathAwareEntity mob;
    private final int buildInterval;
    private int buildCooldown;
    private BlockPos targetPlacePos;

    public BuildBlockGoal(PathAwareEntity mob, int buildInterval) {
        this.mob = mob;
        this.buildInterval = buildInterval;
        this.buildCooldown = 0;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (buildCooldown > 0) {
            buildCooldown--;
            return false;
        }

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
     */
    private boolean findPlacePosition(LivingEntity target) {
        World world = this.mob.getWorld();
        BlockPos mobPos = this.mob.getBlockPos();
        BlockPos targetPos = target.getBlockPos();
        Direction facing = this.mob.getHorizontalFacing();

        int deltaY = targetPos.getY() - mobPos.getY();

        // ===== 策略1: 玩家在上方 → 爬高 =====
        if (deltaY >= 1) {
            // 1a. 在脚下搭方块（直接垫高）
            BlockPos feetPos = mobPos;
            if (canPlaceAt(world, feetPos)) {
                this.targetPlacePos = feetPos.toImmutable();
                return true;
            }

            // 1b. 在前方搭方块 → 形成楼梯（斜向上）
            BlockPos stepPos = mobPos.offset(facing);
            if (canPlaceAt(world, stepPos)) {
                this.targetPlacePos = stepPos.toImmutable();
                return true;
            }

            // 1c. 在前方上方搭方块 → 楼梯第二级
            BlockPos stepUpPos = mobPos.offset(facing).up();
            if (canPlaceAt(world, stepUpPos)) {
                this.targetPlacePos = stepUpPos.toImmutable();
                return true;
            }

            // 1d. 朝玩家水平方向搭楼梯
            Direction towardPlayer = getDirectionToward(mobPos, targetPos);
            if (towardPlayer != facing) {
                BlockPos altStepPos = mobPos.offset(towardPlayer);
                if (canPlaceAt(world, altStepPos)) {
                    this.targetPlacePos = altStepPos.toImmutable();
                    return true;
                }
                BlockPos altStepUpPos = mobPos.offset(towardPlayer).up();
                if (canPlaceAt(world, altStepUpPos)) {
                    this.targetPlacePos = altStepUpPos.toImmutable();
                    return true;
                }
            }
        }

        // ===== 策略2: 前方有坑/空隙 → 搭桥 =====
        BlockPos frontPos = mobPos.offset(facing);
        BlockPos frontBelowPos = frontPos.down();
        BlockState frontBelowState = world.getBlockState(frontBelowPos);

        if (frontBelowState.isAir() || !frontBelowState.isSolidBlock(world, frontBelowPos)) {
            if (canPlaceAt(world, frontBelowPos)) {
                // 检查下方是否有支撑
                BlockPos supportPos = frontBelowPos.down();
                if (world.getBlockState(supportPos).isSolidBlock(world, supportPos)) {
                    this.targetPlacePos = frontBelowPos.toImmutable();
                    return true;
                }
            }
            // 如果前下方有支撑但上方是空的，在脚下放方块辅助
            if (canPlaceAt(world, frontPos)) {
                this.targetPlacePos = frontPos.toImmutable();
                return true;
            }
        }

        // 前方2格搭桥
        BlockPos front2Pos = mobPos.offset(facing, 2);
        BlockPos front2BelowPos = front2Pos.down();
        if (world.getBlockState(front2BelowPos).isAir() && canPlaceAt(world, front2BelowPos)) {
            if (world.getBlockState(front2BelowPos.down()).isSolidBlock(world, front2BelowPos.down())) {
                this.targetPlacePos = front2BelowPos.toImmutable();
                return true;
            }
        }

        // ===== 策略3: 侧面有坑 → 搭桥 =====
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
        this.buildCooldown = this.buildInterval;
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