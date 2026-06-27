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

        // 第20天后才能破坏方块
        if (StageSystem.getCurrentDay(this.mob.getWorld()) < 20) return false;

        BlockPos currentPos = this.mob.getBlockPos();
        if (currentPos.equals(this.lastMobPos)) {
            stuckTicks++;
        } else {
            stuckTicks = 0;
            this.lastMobPos = currentPos;
        }

        if (breakCooldown > 0) {
            // 卡住时加速冷却恢复, 更快突破
            breakCooldown -= (stuckTicks > 25) ? 4 : 1;
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

        // 计算与玩家的水平距离, 距离近时更积极破坏
        int distToPlayer = Math.abs(targetPos.getX() - mobPos.getX()) + Math.abs(targetPos.getZ() - mobPos.getZ());
        boolean closeToPlayer = distToPlayer <= 5;

        // 第一优先级：直接阻挡前进的方块(前方1格, 玩家方向1格) - 最关键
        if (breakDirectObstacle(world, mobPos, facing, towardPlayer)) return true;

        // 第二优先级：扫描门、玻璃、活板门 (快速突破, 1-2格内)
        if (scanPriorityBlocks(world, mobPos, facing, towardPlayer)) return true;

        // 第三优先级：前方1-2格常规方块(脚/头/顶高度)
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

        // 第四优先级：头顶阻挡(玩家在上方时破坏头顶够到玩家)
        int deltaY = targetPos.getY() - mobPos.getY();
        if (deltaY >= 1) {
            // 破坏头顶1-3格, 让僵尸能向上挖到玩家
            for (int y = 1; y <= 3; y++) {
                if (checkAndSetBlock(world, mobPos.up(y))) return true;
            }
        } else {
            if (checkAndSetBlock(world, mobPos.up(1))) return true;
            if (checkAndSetBlock(world, mobPos.up(2))) return true;
        }

        // 第五优先级：侧面(玩家近时才扫, 避免偏离追击)
        if (closeToPlayer) {
            Direction left = facing.rotateYCounterclockwise();
            Direction right = facing.rotateYClockwise();
            for (int y = 0; y <= 2; y++) {
                if (checkAndSetBlock(world, mobPos.offset(left).up(y))) return true;
                if (checkAndSetBlock(world, mobPos.offset(right).up(y))) return true;
            }
        }

        // 第六优先级：卡住时扩大扫描(前方3格 + 周围2格 + 头顶更高)
        if (stuckTicks > 25) {
            for (int y = 0; y <= 3; y++) {
                if (checkAndSetBlock(world, mobPos.offset(facing, 3).up(y))) return true;
            }
            for (Direction dir : Direction.values()) {
                if (dir.getAxis().isHorizontal()) {
                    for (int y = 0; y <= 3; y++) {
                        if (checkAndSetBlock(world, mobPos.offset(dir, 2).up(y))) return true;
                    }
                }
            }
            // 卡住时也尝试破坏脚下(玩家在下方时)
            if (deltaY < 0) {
                if (checkAndSetBlock(world, mobPos.down())) return true;
            }
        }
        return false;
    }

    /**
     * 破坏直接阻挡前进的方块(前方1格脚部/头部, 玩家方向1格脚部/头部)
     * 这是让僵尸能贴近玩家最关键的一步
     */
    private boolean breakDirectObstacle(World world, BlockPos mobPos, Direction facing, Direction towardPlayer) {
        // 前方1格的脚和头(阻挡行走)
        if (checkAndSetBlock(world, mobPos.offset(facing))) return true;
        if (checkAndSetBlock(world, mobPos.offset(facing).up(1))) return true;
        // 朝玩家方向1格的脚和头
        if (towardPlayer != facing) {
            if (checkAndSetBlock(world, mobPos.offset(towardPlayer))) return true;
            if (checkAndSetBlock(world, mobPos.offset(towardPlayer).up(1))) return true;
        }
        return false;
    }

    /**
     * 优先扫描门、玻璃、活板门等可快速突破的方块(1-2格内)
     */
    private boolean scanPriorityBlocks(World world, BlockPos mobPos, Direction facing, Direction towardPlayer) {
        for (int dist = 1; dist <= 2; dist++) {
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