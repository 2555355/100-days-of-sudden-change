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
 * 僵尸搭方块AI (强力追击版)
 * 核心目标: 通过搭方块让僵尸尽可能接近玩家。
 *
 * 策略优先级:
 *   1. 玩家在上方 → 脚下垫高塔式堆叠(可连续搭多格, 无需下方支撑)
 *   2. 玩家在水平方向 → 朝玩家方向搭天桥(空中搭方块)
 *   3. 前方/侧面有坑 → 搭桥跨越
 *
 * 关键改进:
 *   - canPlaceAt 放宽: 脚下位置无需下方支撑(僵尸自身踩着)
 *   - 相邻实体方块即可放置(空气也能搭, 不必下面有支撑)
 *   - 朝玩家方向(towardPlayer)绝对优先
 *   - 卡住时立即触发(无需等冷却)
 */
public class BuildBlockGoal extends Goal {
    private final PathAwareEntity mob;
    private int buildCooldown;
    private BlockPos targetPlacePos;
    private BlockPos lastMobPos;
    private int stuckTicks;

    public BuildBlockGoal(PathAwareEntity mob) {
        this.mob = mob;
        this.buildCooldown = 0;
        this.lastMobPos = mob.getBlockPos();
        this.stuckTicks = 0;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        // 第20天后才能搭方块
        if (StageSystem.getCurrentDay(this.mob.getWorld()) < 20) return false;

        // 卡住检测
        BlockPos currentPos = this.mob.getBlockPos();
        if (currentPos.equals(this.lastMobPos)) {
            stuckTicks++;
        } else {
            stuckTicks = 0;
            this.lastMobPos = currentPos;
        }

        // 卡住时无视冷却立即尝试搭(更激进)
        if (stuckTicks > 15) {
            LivingEntity t = this.mob.getTarget();
            if (t != null && t.isAlive()) {
                if (findPlacePosition(t)) return true;
            }
        }

        if (buildCooldown > 0) {
            buildCooldown -= (stuckTicks > 15) ? 3 : 1;
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
     * 优先级: 脚下垫高(玩家在上方) > 朝玩家方向搭天桥 > 前方搭桥 > 侧面
     */
    private boolean findPlacePosition(LivingEntity target) {
        World world = this.mob.getWorld();
        BlockPos mobPos = this.mob.getBlockPos();
        BlockPos targetPos = target.getBlockPos();
        Direction facing = this.mob.getHorizontalFacing();
        Direction towardPlayer = getDirectionToward(mobPos, targetPos);

        int deltaY = targetPos.getY() - mobPos.getY();
        int horizontalDist = Math.max(Math.abs(targetPos.getX() - mobPos.getX()),
                                       Math.abs(targetPos.getZ() - mobPos.getZ()));

        // ===== 策略1: 玩家在上方 → 脚下垫高塔式堆叠 =====
        // 只要玩家比僵尸高, 就持续在脚下搭方块把自己垫上去
        if (deltaY >= 1) {
            // 1a. 直接在脚下搭(无需下方支撑, 僵尸自己踩着)
            BlockPos feetPos = mobPos;
            if (canPlaceAtFeet(world, feetPos)) {
                this.targetPlacePos = feetPos.toImmutable();
                return true;
            }

            // 1b. 朝玩家方向1格搭楼梯(脚下位置)
            BlockPos towardStepPos = mobPos.offset(towardPlayer);
            if (canPlaceAtFeet(world, towardStepPos)) {
                this.targetPlacePos = towardStepPos.toImmutable();
                return true;
            }

            // 1c. 朝向方向1格搭楼梯
            BlockPos facingStepPos = mobPos.offset(facing);
            if (canPlaceAtFeet(world, facingStepPos)) {
                this.targetPlacePos = facingStepPos.toImmutable();
                return true;
            }

            // 1d. 朝玩家方向2格搭(更远追击)
            if (horizontalDist >= 2) {
                BlockPos toward2Pos = mobPos.offset(towardPlayer, 2);
                if (canPlaceAtFeet(world, toward2Pos)) {
                    this.targetPlacePos = toward2Pos.toImmutable();
                    return true;
                }
            }

            // 1e. 朝玩家方向1格上方(楼梯第二级)
            BlockPos towardUpPos = mobPos.offset(towardPlayer).up();
            if (canPlaceAt(world, towardUpPos)) {
                this.targetPlacePos = towardUpPos.toImmutable();
                return true;
            }
        }

        // ===== 策略2: 玩家在水平方向(同高或略低) → 朝玩家方向搭天桥 =====
        if (deltaY <= 0) {
            // 2a. 朝玩家方向1格脚下(空中搭天桥)
            BlockPos towardFeetPos = mobPos.offset(towardPlayer);
            if (canPlaceAtFeet(world, towardFeetPos)) {
                this.targetPlacePos = towardFeetPos.toImmutable();
                return true;
            }

            // 2b. 朝玩家方向2格脚下(远距离搭桥)
            if (horizontalDist >= 2) {
                BlockPos toward2FeetPos = mobPos.offset(towardPlayer, 2);
                if (canPlaceAtFeet(world, toward2FeetPos)) {
                    this.targetPlacePos = toward2FeetPos.toImmutable();
                    return true;
                }
            }

            // 2c. 朝玩家方向3格脚下(更远搭桥, 玩家距离>3时)
            if (horizontalDist >= 3) {
                BlockPos toward3FeetPos = mobPos.offset(towardPlayer, 3);
                if (canPlaceAtFeet(world, toward3FeetPos)) {
                    this.targetPlacePos = toward3FeetPos.toImmutable();
                    return true;
                }
            }
        }

        // ===== 策略3: 前方有坑 → 搭桥(朝玩家方向优先) =====
        BlockPos frontPos = mobPos.offset(towardPlayer);
        BlockPos frontBelowPos = frontPos.down();
        BlockState frontBelowState = world.getBlockState(frontBelowPos);

        if (frontBelowState.isAir() || !frontBelowState.isSolidBlock(world, frontBelowPos)) {
            if (canPlaceAt(world, frontBelowPos)) {
                this.targetPlacePos = frontBelowPos.toImmutable();
                return true;
            }
            // 前方位置直接搭(空中)
            if (canPlaceAtFeet(world, frontPos)) {
                this.targetPlacePos = frontPos.toImmutable();
                return true;
            }
        }

        // 朝玩家方向2格下方搭桥
        BlockPos front2BelowPos = mobPos.offset(towardPlayer, 2).down();
        if (world.getBlockState(front2BelowPos).isAir() && canPlaceAt(world, front2BelowPos)) {
            this.targetPlacePos = front2BelowPos.toImmutable();
            return true;
        }

        // ===== 策略4: 朝向方向搭桥(备选) =====
        BlockPos facingPos = mobPos.offset(facing);
        BlockPos facingBelowPos = facingPos.down();
        if (canPlaceAt(world, facingBelowPos)) {
            this.targetPlacePos = facingBelowPos.toImmutable();
            return true;
        }
        if (canPlaceAtFeet(world, facingPos)) {
            this.targetPlacePos = facingPos.toImmutable();
            return true;
        }

        // ===== 策略5: 侧面搭桥(最后手段) =====
        for (Direction dir : Direction.values()) {
            if (dir.getAxis().isHorizontal()) {
                BlockPos sidePos = mobPos.offset(dir);
                BlockPos sideBelowPos = sidePos.down();
                if (world.getBlockState(sideBelowPos).isAir() && canPlaceAt(world, sideBelowPos)) {
                    this.targetPlacePos = sideBelowPos.toImmutable();
                    return true;
                }
                if (canPlaceAtFeet(world, sidePos)) {
                    this.targetPlacePos = sidePos.toImmutable();
                    return true;
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
     * 检查是否可以在指定位置放置方块(标准: 需要下方支撑)
     */
    private boolean canPlaceAt(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!state.isAir() && !state.isReplaceable()) return false;

        if (pos.getY() >= world.getTopY() || pos.getY() <= world.getBottomY()) return false;

        BlockPos belowPos = pos.down();
        BlockState belowState = world.getBlockState(belowPos);
        if (!belowState.isSolidBlock(world, belowPos)) return false;

        return true;
    }

    /**
     * 脚下垫高专用: 无需下方支撑(僵尸自身踩着或相邻有实体方块即可)
     * 用于塔式堆叠和空中搭天桥
     */
    private boolean canPlaceAtFeet(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!state.isAir() && !state.isReplaceable()) return false;

        if (pos.getY() >= world.getTopY() || pos.getY() <= world.getBottomY()) return false;

        // 检查是否为僵尸自身位置或相邻(僵尸能踩着搭)
        BlockPos mobPos = this.mob.getBlockPos();
        if (pos.equals(mobPos)) return true;  // 脚下直接搭

        // 相邻有实体方块即可(包括僵尸脚下、侧面、下方)
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.offset(dir);
            if (neighbor.equals(mobPos)) return true;  // 紧邻僵尸
            BlockState neighborState = world.getBlockState(neighbor);
            if (neighborState.isSolidBlock(world, neighbor)) return true;
        }

        return false;
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
