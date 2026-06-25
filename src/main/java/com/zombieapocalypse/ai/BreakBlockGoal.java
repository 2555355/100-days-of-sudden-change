package com.zombieapocalypse.ai;

import com.zombieapocalypse.config.ModConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.EnumSet;

/**
 * 僵尸破坏方块AI
 * 僵尸会破坏阻挡其追击玩家的方块
 * 包括：木质、土质、玻璃、石质方块等
 */
public class BreakBlockGoal extends Goal {
    private final PathAwareEntity mob;
    private final int breakInterval;
    private int breakCooldown;
    private BlockPos targetBlock;

    public BreakBlockGoal(PathAwareEntity mob, int breakInterval) {
        this.mob = mob;
        this.breakInterval = breakInterval;
        this.breakCooldown = 0;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (!ModConfig.ZOMBIE_CAN_BREAK_BLOCKS) return false;
        if (breakCooldown > 0) {
            breakCooldown--;
            return false;
        }

        // 只有有攻击目标时才破坏方块
        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive()) return false;

        // 检测前方是否有阻挡方块
        return findBlockToBreak(target);
    }

    private boolean findBlockToBreak(LivingEntity target) {
        World world = this.mob.getWorld();
        BlockPos mobPos = this.mob.getBlockPos();

        // 获取僵尸面向的方向
        Direction facing = this.mob.getHorizontalFacing();

        // 检查前方、上方、下方的方块
        BlockPos[] checkPositions = {
                mobPos.up(),                   // 头部高度
                mobPos.up(2),                  // 头顶上方
                mobPos.offset(facing),         // 前方
                mobPos.up().offset(facing),    // 前方头部高度
                mobPos.down(),                 // 脚下
        };

        BlockPos targetPos = target.getBlockPos();
        for (BlockPos checkPos : checkPositions) {
            BlockState state = world.getBlockState(checkPos);
            if (canBreakBlock(state, world, checkPos)) {
                // 检查是否阻挡了到目标的路径
                if (isBlockingPath(checkPos, targetPos)) {
                    this.targetBlock = checkPos.toImmutable();
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isBlockingPath(BlockPos blockPos, BlockPos targetPos) {
        BlockPos mobPos = this.mob.getBlockPos();
        // 方块在僵尸和目标之间
        return (blockPos.getY() >= mobPos.getY() - 1 && blockPos.getY() <= mobPos.getY() + 2);
    }

    private boolean canBreakBlock(BlockState state, World world, BlockPos pos) {
        if (state.isAir()) return false;
        if (state.getHardness(world, pos) < 0) return false; // 不可破坏
        if (state.getHardness(world, pos) > ModConfig.ZOMBIE_BREAK_HARDNESS_LIMIT) return false;

        // 不可破坏的方块类型
        if (state.isOf(Blocks.BEDROCK)) return false;
        if (state.isOf(Blocks.BARRIER)) return false;
        if (state.isOf(Blocks.COMMAND_BLOCK)) return false;
        if (state.isOf(Blocks.STRUCTURE_BLOCK)) return false;
        if (state.isOf(Blocks.OBSIDIAN)) return false;
        if (state.isOf(Blocks.CRYING_OBSIDIAN)) return false;
        if (state.isOf(Blocks.END_PORTAL_FRAME)) return false;
        if (state.isOf(Blocks.REINFORCED_DEEPSLATE)) return false;

        return true;
    }

    @Override
    public void start() {
        this.breakCooldown = this.breakInterval;
        if (this.targetBlock != null) {
            World world = this.mob.getWorld();
            BlockState state = world.getBlockState(this.targetBlock);

            if (canBreakBlock(state, world, this.targetBlock)) {
                // 直接破坏方块 (在生存模式下产生掉落物)
                world.breakBlock(this.targetBlock, true, this.mob);
            }
            this.targetBlock = null;
        }
    }

    @Override
    public boolean shouldContinue() {
        return false; // 每次执行一次破坏
    }

    @Override
    public void stop() {
        this.targetBlock = null;
    }
}