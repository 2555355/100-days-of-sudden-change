package com.zombieapocalypse.ai;

import com.zombieapocalypse.config.ModConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.EnumSet;

/**
 * 僵尸破坏方块AI (增强版)
 * 僵尸会破坏阻挡其追击玩家的方块
 * 包括：木质、土质、玻璃、石质方块等
 * 更激进的破坏策略：检查周围多个方向，快速突破障碍
 * 增加卡住检测：如果僵尸长时间未移动，加大破坏范围
 */
public class BreakBlockGoal extends Goal {
    private final PathAwareEntity mob;
    private final int breakInterval;
    private int breakCooldown;
    private BlockPos targetBlock;
    private BlockPos lastMobPos;
    private int stuckTicks;

    public BreakBlockGoal(PathAwareEntity mob, int breakInterval) {
        this.mob = mob;
        this.breakInterval = breakInterval;
        this.breakCooldown = 0;
        this.lastMobPos = mob.getBlockPos();
        this.stuckTicks = 0;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (!ModConfig.ZOMBIE_CAN_BREAK_BLOCKS) return false;

        // 卡住检测：如果僵尸长时间未移动，减少冷却时间
        BlockPos currentPos = this.mob.getBlockPos();
        if (currentPos.equals(this.lastMobPos)) {
            stuckTicks++;
        } else {
            stuckTicks = 0;
            this.lastMobPos = currentPos;
        }

        if (breakCooldown > 0) {
            // 卡住时加速冷却
            breakCooldown -= (stuckTicks > 40) ? 3 : 1;
            return false;
        }

        LivingEntity target = this.mob.getTarget();
        if (target == null || !target.isAlive()) return false;

        return findBlockToBreak(target);
    }

    private boolean findBlockToBreak(LivingEntity target) {
        World world = this.mob.getWorld();
        BlockPos mobPos = this.mob.getBlockPos();
        BlockPos targetPos = target.getBlockPos();
        Direction facing = this.mob.getHorizontalFacing();

        // 计算朝玩家方向
        Direction towardPlayer = getDirectionToward(mobPos, targetPos);

        // 检查所有阻挡路径的方块
        // 前方 (多个高度)
        for (int y = 0; y <= 2; y++) {
            BlockPos checkPos = mobPos.offset(facing).up(y);
            if (checkAndSetBlock(world, checkPos)) return true;
        }

        // 前方2格 (多个高度)
        for (int y = 0; y <= 2; y++) {
            BlockPos checkPos = mobPos.offset(facing, 2).up(y);
            if (checkAndSetBlock(world, checkPos)) return true;
        }

        // 朝玩家方向检查（可能与facing不同）
        if (towardPlayer != facing) {
            for (int y = 0; y <= 2; y++) {
                BlockPos checkPos = mobPos.offset(towardPlayer).up(y);
                if (checkAndSetBlock(world, checkPos)) return true;
            }
            for (int y = 0; y <= 2; y++) {
                BlockPos checkPos = mobPos.offset(towardPlayer, 2).up(y);
                if (checkAndSetBlock(world, checkPos)) return true;
            }
        }

        // 头顶
        BlockPos headPos = mobPos.up(1);
        if (checkAndSetBlock(world, headPos)) return true;
        BlockPos headPos2 = mobPos.up(2);
        if (checkAndSetBlock(world, headPos2)) return true;

        // 脚下
        BlockPos feetPos = mobPos;
        if (checkAndSetBlock(world, feetPos)) return true;

        // 左右侧
        Direction left = facing.rotateYCounterclockwise();
        Direction right = facing.rotateYClockwise();
        for (int y = 0; y <= 2; y++) {
            if (checkAndSetBlock(world, mobPos.offset(left).up(y))) return true;
            if (checkAndSetBlock(world, mobPos.offset(right).up(y))) return true;
        }

        // 卡住时扩大搜索范围 - 检查更远的方块
        if (stuckTicks > 40) {
            for (int y = 0; y <= 2; y++) {
                BlockPos checkPos = mobPos.offset(facing, 3).up(y);
                if (checkAndSetBlock(world, checkPos)) return true;
            }
            // 检查所有水平方向
            for (Direction dir : Direction.values()) {
                if (dir.getAxis().isHorizontal()) {
                    for (int y = 0; y <= 2; y++) {
                        BlockPos checkPos = mobPos.offset(dir, 2).up(y);
                        if (checkAndSetBlock(world, checkPos)) return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * 获取从from朝向to的方向
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

    private boolean checkAndSetBlock(World world, BlockPos pos) {
        if (pos.equals(this.mob.getBlockPos())) return false; // 不破坏自己站的方块
        BlockState state = world.getBlockState(pos);
        if (canBreakBlock(state, world, pos)) {
            this.targetBlock = pos.toImmutable();
            return true;
        }
        return false;
    }

    private boolean canBreakBlock(BlockState state, World world, BlockPos pos) {
        if (state.isAir()) return false;
        if (state.getHardness(world, pos) < 0) return false;
        if (state.getHardness(world, pos) > ModConfig.ZOMBIE_BREAK_HARDNESS_LIMIT) return false;

        // 不可破坏的方块
        if (state.isOf(Blocks.BEDROCK)) return false;
        if (state.isOf(Blocks.BARRIER)) return false;
        if (state.isOf(Blocks.COMMAND_BLOCK)) return false;
        if (state.isOf(Blocks.STRUCTURE_BLOCK)) return false;
        if (state.isOf(Blocks.OBSIDIAN)) return false;
        if (state.isOf(Blocks.CRYING_OBSIDIAN)) return false;
        if (state.isOf(Blocks.END_PORTAL_FRAME)) return false;
        if (state.isOf(Blocks.REINFORCED_DEEPSLATE)) return false;
        if (state.isOf(Blocks.NETHERITE_BLOCK)) return false;
        if (state.isOf(Blocks.ANCIENT_DEBRIS)) return false;

        return true;
    }

    @Override
    public void start() {
        this.breakCooldown = this.breakInterval;
        if (this.targetBlock != null) {
            World world = this.mob.getWorld();
            BlockState state = world.getBlockState(this.targetBlock);

            if (canBreakBlock(state, world, this.targetBlock)) {
                // 存储方块类型到僵尸库存，供搭方块时使用
                if (this.mob instanceof IBlockCollector collector) {
                    collector.addCollectedBlock(state);
                }
                world.breakBlock(this.targetBlock, true, this.mob);
            }
            this.targetBlock = null;
        }
    }

    @Override
    public boolean shouldContinue() {
        return false;
    }

    @Override
    public void stop() {
        this.targetBlock = null;
    }
}