package com.zombieapocalypse.ai;

import com.zombieapocalypse.config.ModConfig;
import com.zombieapocalypse.config.StageSystem;
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
 * 僵尸破坏方块AI (智能增强版)
 * 僵尸会破坏阻挡其追击玩家的方块，智能度随天数阶梯增长
 * Lv0: 30tick间隔, 硬度10  |  Lv3: 9tick间隔, 硬度35
 * Lv1: 20tick间隔, 硬度15  |  Lv4: 5tick间隔, 硬度50
 * Lv2: 14tick间隔, 硬度25  |  Lv5: 3tick间隔, 硬度100
 */
public class BreakBlockGoal extends Goal {
    private final PathAwareEntity mob;
    private int breakCooldown;
    private BlockPos targetBlock;
    private BlockPos lastMobPos;
    private int stuckTicks;

    public BreakBlockGoal(PathAwareEntity mob) {
        this.mob = mob;
        this.breakCooldown = 0;
        this.lastMobPos = mob.getBlockPos();
        this.stuckTicks = 0;
        this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
    }

    @Override
    public boolean canStart() {
        if (!ModConfig.ZOMBIE_CAN_BREAK_BLOCKS) return false;

        BlockPos currentPos = this.mob.getBlockPos();
        if (currentPos.equals(this.lastMobPos)) {
            stuckTicks++;
        } else {
            stuckTicks = 0;
            this.lastMobPos = currentPos;
        }

        if (breakCooldown > 0) {
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
        Direction towardPlayer = getDirectionToward(mobPos, targetPos);

        // 第一优先级：扫描门、玻璃、活板门 (更智能的突破)
        if (scanPriorityBlocks(world, mobPos, facing, towardPlayer)) return true;

        // 第二优先级：常规方块
        for (int y = 0; y <= 2; y++) {
            if (checkAndSetBlock(world, mobPos.offset(facing).up(y))) return true;
        }
        for (int y = 0; y <= 2; y++) {
            if (checkAndSetBlock(world, mobPos.offset(facing, 2).up(y))) return true;
        }
        if (towardPlayer != facing) {
            for (int y = 0; y <= 2; y++) {
                if (checkAndSetBlock(world, mobPos.offset(towardPlayer).up(y))) return true;
            }
            for (int y = 0; y <= 2; y++) {
                if (checkAndSetBlock(world, mobPos.offset(towardPlayer, 2).up(y))) return true;
            }
        }
        if (checkAndSetBlock(world, mobPos.up(1))) return true;
        if (checkAndSetBlock(world, mobPos.up(2))) return true;
        if (checkAndSetBlock(world, mobPos)) return true;
        Direction left = facing.rotateYCounterclockwise();
        Direction right = facing.rotateYClockwise();
        for (int y = 0; y <= 2; y++) {
            if (checkAndSetBlock(world, mobPos.offset(left).up(y))) return true;
            if (checkAndSetBlock(world, mobPos.offset(right).up(y))) return true;
        }
        if (stuckTicks > 40) {
            for (int y = 0; y <= 2; y++) {
                if (checkAndSetBlock(world, mobPos.offset(facing, 3).up(y))) return true;
            }
            for (Direction dir : Direction.values()) {
                if (dir.getAxis().isHorizontal()) {
                    for (int y = 0; y <= 2; y++) {
                        if (checkAndSetBlock(world, mobPos.offset(dir, 2).up(y))) return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 优先扫描门、玻璃、活板门等可快速突破的方块
     */
    private boolean scanPriorityBlocks(World world, BlockPos mobPos, Direction facing, Direction towardPlayer) {
        // 检查朝向方向1-3格内的门/玻璃
        for (int dist = 1; dist <= 3; dist++) {
            for (int y = 0; y <= 2; y++) {
                BlockPos pos = mobPos.offset(facing, dist).up(y);
                if (isPriorityBlockAt(world, pos)) {
                    this.targetBlock = pos.toImmutable();
                    return true;
                }
            }
            if (towardPlayer != facing) {
                for (int y = 0; y <= 2; y++) {
                    BlockPos pos = mobPos.offset(towardPlayer, dist).up(y);
                    if (isPriorityBlockAt(world, pos)) {
                        this.targetBlock = pos.toImmutable();
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isPriorityBlockAt(World world, BlockPos pos) {
        if (pos.equals(this.mob.getBlockPos())) return false;
        BlockState state = world.getBlockState(pos);
        if (state.isAir()) return false;
        return isPriorityBlock(state);
    }

    private Direction getDirectionToward(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        return Math.abs(dx) > Math.abs(dz)
                ? (dx > 0 ? Direction.EAST : Direction.WEST)
                : (dz > 0 ? Direction.SOUTH : Direction.NORTH);
    }

    private boolean checkAndSetBlock(World world, BlockPos pos) {
        if (pos.equals(this.mob.getBlockPos())) return false;
        BlockState state = world.getBlockState(pos);
        if (canBreakBlock(state, world, pos)) {
            this.targetBlock = pos.toImmutable();
            return true;
        }
        return false;
    }

    /**
     * 是否为优先破坏方块 (门、玻璃、活板门)
     */
    private boolean isPriorityBlock(BlockState state) {
        return state.isIn(net.minecraft.registry.tag.BlockTags.DOORS)
                || state.isIn(net.minecraft.registry.tag.BlockTags.WOODEN_TRAPDOORS)
                || state.getBlock() instanceof net.minecraft.block.GlassBlock
                || state.getBlock() instanceof net.minecraft.block.StainedGlassBlock;
    }

    /**
     * 检查方块是否可破坏 (使用动态硬度上限)
     */
    private boolean canBreakBlock(BlockState state, World world, BlockPos pos) {
        if (state.isAir()) return false;
        float hardness = state.getHardness(world, pos);
        if (hardness < 0) return false;
        float limit = StageSystem.getHardnessLimit(world);
        if (hardness > limit) return false;

        if (state.isOf(Blocks.BEDROCK)) return false;
        if (state.isOf(Blocks.BARRIER)) return false;
        if (state.isOf(Blocks.COMMAND_BLOCK)) return false;
        if (state.isOf(Blocks.STRUCTURE_BLOCK)) return false;
        if (state.isOf(Blocks.END_PORTAL_FRAME)) return false;
        if (state.isOf(Blocks.REINFORCED_DEEPSLATE)) return false;

        return true;
    }

    @Override
    public void start() {
        this.breakCooldown = StageSystem.getBreakInterval(this.mob.getWorld());
        if (this.targetBlock != null) {
            World world = this.mob.getWorld();
            BlockState state = world.getBlockState(this.targetBlock);
            if (canBreakBlock(state, world, this.targetBlock)) {
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