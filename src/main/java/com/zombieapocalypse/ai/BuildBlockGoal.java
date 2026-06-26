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
 * 僵尸搭方块AI
 * 僵尸会搭方块爬高或搭桥以接近玩家
 * 策略：
 * 1. 玩家在头顶上方 → 往脚下搭方块爬高
 * 2. 前方有坑/空隙 → 搭方块搭桥
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
        int distX = targetPos.getX() - mobPos.getX();
        int distZ = targetPos.getZ() - mobPos.getZ();

        // 策略1: 玩家在头顶上方 (3格以上) → 搭方块爬高
        if (targetPos.getY() > mobPos.getY() + 2) {
            // 在脚下搭方块
            BlockPos underPos = mobPos.down();
            BlockState underState = world.getBlockState(underPos);
            if (canPlaceAt(world, mobPos)) {
                this.targetPlacePos = mobPos.toImmutable();
                return true;
            }
            // 在前方一格搭方块
            Direction facing = this.mob.getHorizontalFacing();
            BlockPos frontPos = mobPos.offset(facing);
            if (canPlaceAt(world, frontPos)) {
                this.targetPlacePos = frontPos.toImmutable();
                return true;
            }
        }

        // 策略2: 前方有坑/空隙 → 搭桥
        Direction facing = this.mob.getHorizontalFacing();
        BlockPos frontPos = mobPos.offset(facing);
        BlockPos frontBelowPos = frontPos.down();
        BlockState frontBelowState = world.getBlockState(frontBelowPos);

        if (frontBelowState.isAir() || world.getBlockState(frontPos).isAir()) {
            // 前方是空的，需要搭桥
            if (canPlaceAt(world, frontBelowPos) && world.getBlockState(frontBelowPos.down()).isSolidBlock(world, frontBelowPos.down())) {
                this.targetPlacePos = frontBelowPos.toImmutable();
                return true;
            }
        }

        // 策略3: 侧面前方有坑
        for (Direction dir : Direction.values()) {
            if (dir == Direction.UP || dir == Direction.DOWN) continue;
            BlockPos sidePos = mobPos.offset(dir);
            BlockPos sideBelowPos = sidePos.down();
            if (world.getBlockState(sideBelowPos).isAir() && canPlaceAt(world, sideBelowPos)) {
                // 检查侧面下方是否有支撑
                if (world.getBlockState(sideBelowPos.down()).isSolidBlock(world, sideBelowPos.down())) {
                    this.targetPlacePos = sideBelowPos.toImmutable();
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 检查是否可以在指定位置放置方块
     */
    private boolean canPlaceAt(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (!state.isAir() && !state.isReplaceable()) return false;

        // 检查是否在世界上限内
        if (pos.getY() >= world.getTopY() || pos.getY() <= world.getBottomY()) return false;

        // 不能放在实体占据的位置
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
                // 使用收集到的方块材料，而非凭空生成
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