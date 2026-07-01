package com.zombieapocalypse.mixin;

import com.zombieapocalypse.ai.BreakBlockGoal;
import com.zombieapocalypse.ai.BuildBlockGoal;
import com.zombieapocalypse.ai.IBlockCollector;
import com.zombieapocalypse.config.ModConfig;
import com.zombieapocalypse.config.StageSystem;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Mixin to ZombieEntity - 核心僵尸行为修改
 * 1. 防白天燃烧
 * 2. 添加破坏方块AI
 * 3. 添加搭方块AI (爬高/搭桥)
 * 4. 根据阶段动态更新属性
 * 5. 允许白天生成
 * 6. 方块收集系统 (拆方块→收集材料→搭方块)
 * 7. 大幅降低小僵尸生成概率 (默认5% → 0.5%)
 */
@Mixin(ZombieEntity.class)
public abstract class ZombieEntityMixin extends HostileEntity implements IBlockCollector {

    protected ZombieEntityMixin(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
    }

    /**
     * 方块收集系统：僵尸拆方块后收集材料，搭方块时消耗
     * 库存容量随智能度阶梯增长: Lv0=2, Lv5=8
     */
    @Unique
    private final List<BlockState> blockInventory = new ArrayList<>();

    @Override
    public BlockState peekCollectedBlock() {
        return blockInventory.isEmpty() ? null : blockInventory.get(0);
    }

    @Override
    public BlockState consumeCollectedBlock() {
        return blockInventory.isEmpty() ? null : blockInventory.remove(0);
    }

    @Override
    public void addCollectedBlock(BlockState state) {
        int maxSize = StageSystem.getBlockInventorySize(this.getWorld());
        if (blockInventory.size() < maxSize) {
            blockInventory.add(state);
        }
    }

    @Override
    public boolean hasCollectedBlock() {
        return !blockInventory.isEmpty();
    }

    @Override
    public int getBlockInventorySize() {
        return blockInventory.size();
    }

    @Override
    public void clearBlockInventory() {
        blockInventory.clear();
    }

    /**
     * 禁止白天燃烧
     */
    @Inject(method = "burnsInDaylight", at = @At("HEAD"), cancellable = true)
    private void preventBurning(CallbackInfoReturnable<Boolean> cir) {
        if (ModConfig.ZOMBIE_SUN_BURN_IMMUNE) {
            cir.setReturnValue(false);
        }
    }

    /**
     * 降低小僵尸生成概率 (默认5% → 0.5%)
     * 在initialize之后，如果生成了小僵尸，有90%概率转回成年
     */
    @Inject(method = "initialize", at = @At("TAIL"))
    private void reduceBabyChance(net.minecraft.world.ServerWorldAccess world,
                                  net.minecraft.world.LocalDifficulty difficulty,
                                  net.minecraft.entity.SpawnReason spawnReason,
                                  net.minecraft.entity.EntityData entityData,
                                  net.minecraft.nbt.NbtCompound entityNbt,
                                  CallbackInfoReturnable<net.minecraft.entity.EntityData> cir) {
        ZombieEntity self = (ZombieEntity) (Object) this;
        if (self.isBaby() && self.getRandom().nextFloat() < 0.9f) {
            self.setBaby(false);
        }
    }

    /**
     * 注入AI目标 - 添加破坏方块AI和搭方块AI
     * 间隔由智能度系统动态控制
     */
    @Inject(method = "initGoals", at = @At("HEAD"))
    private void addCustomGoals(CallbackInfo ci) {
        ZombieEntity self = (ZombieEntity) (Object) this;
        this.goalSelector.add(0, new BreakBlockGoal(self));
        this.goalSelector.add(1, new BuildBlockGoal(self));
    }

    /**
     * 每tick更新僵尸属性 (基于阶段 + 夜晚/血月/低血量加成)
     *
     * 性能优化 (v3.0.0): 属性更新改为每10tick一次, 避免每tick重复调用7+个StageSystem方法。
     *                    速度/狂暴等连续性需求由 onDamaged + 玩家攻击时即时刷新覆盖。
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (this.getWorld().isClient) return;
        // 每10tick更新一次属性, 大幅减少 StageSystem 调用开销
        if ((this.age & 0b1111) != 0) return;
        updateZombieAttributes();
    }

    /**
     * 根据阶段更新僵尸属性
     * 血量: 20-400, 攻击: 3-15, 速度: 0.23-0.35, 护甲: 2-12
     * 速度额外受夜晚/血月/低血量狂暴影响
     * 攻击额外受血月影响
     * 追踪范围随智能度增长
     */
    @Unique
    private void updateZombieAttributes() {
        World world = this.getWorld();
        if (world == null) return;
        if (this.isDead()) return;

        // 一次性计算阶段进度, 避免多次调用 getStageProgress
        double progress = StageSystem.getStageProgress(world);
        double health = ModConfig.ZOMBIE_BASE_HEALTH +
                (ModConfig.ZOMBIE_MAX_HEALTH - ModConfig.ZOMBIE_BASE_HEALTH) * progress;
        double attack = ModConfig.ZOMBIE_BASE_ATTACK +
                (ModConfig.ZOMBIE_MAX_ATTACK - ModConfig.ZOMBIE_BASE_ATTACK) * progress;
        double speed = ModConfig.ZOMBIE_BASE_SPEED +
                (ModConfig.ZOMBIE_MAX_SPEED - ModConfig.ZOMBIE_BASE_SPEED) * progress;
        double armor = ModConfig.ZOMBIE_BASE_ARMOR +
                (ModConfig.ZOMBIE_MAX_ARMOR - ModConfig.ZOMBIE_BASE_ARMOR) * progress;

        double difficultyMult = ModConfig.getDifficultyMultiplier(world.getDifficulty());
        health *= difficultyMult;
        attack *= difficultyMult;

        var healthAttr = this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (healthAttr != null && healthAttr.getBaseValue() != health) {
            double oldMax = healthAttr.getBaseValue();
            float healthRatio = oldMax > 0 ? this.getHealth() / (float) oldMax : 1.0f;
            healthAttr.setBaseValue(health);
            this.setHealth(Math.min((float) health, (float) health * healthRatio));
        }

        // 攻击力: 基础 * 血月倍率
        double finalAttack = attack * StageSystem.getAttackMultiplier(world);
        var attackAttr = this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (attackAttr != null && attackAttr.getBaseValue() != finalAttack) {
            attackAttr.setBaseValue(finalAttack);
        }

        // 速度: 基础 * 夜晚/血月/低血量倍率
        float healthRatio = healthAttr != null && healthAttr.getBaseValue() > 0
                ? this.getHealth() / (float) healthAttr.getBaseValue() : 1.0f;
        double finalSpeed = speed * StageSystem.getSpeedMultiplier(world, healthRatio);
        var speedAttr = this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speedAttr != null) {
            double currentSpeed = speedAttr.getBaseValue();
            if (Math.abs(currentSpeed - finalSpeed) > 0.001) {
                speedAttr.setBaseValue(finalSpeed);
            }
        }

        var armorAttr = this.getAttributeInstance(EntityAttributes.GENERIC_ARMOR);
        if (armorAttr != null && armorAttr.getBaseValue() != armor) {
            armorAttr.setBaseValue(armor);
        }

        // 动态追踪范围 (随智能度增长)
        int followRange = StageSystem.getFollowRange(world);
        var followAttr = this.getAttributeInstance(EntityAttributes.GENERIC_FOLLOW_RANGE);
        if (followAttr != null && followAttr.getBaseValue() != followRange) {
            followAttr.setBaseValue(followRange);
        }
    }

    /**
     * 受到伤害时呼叫增援 (高智能度触发)
     * 在附近生成1-2只僵尸
     */
    @Inject(method = "damage", at = @At("TAIL"))
    private void onDamaged(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (this.getWorld().isClient) return;
        if (!cir.getReturnValue()) return;
        if (this.isDead()) return;

        World world = this.getWorld();
        double reinforceChance = StageSystem.getReinforcementChance(world);
        if (world.getRandom().nextDouble() >= reinforceChance) return;

        // 在附近生成增援僵尸
        if (world instanceof ServerWorld serverWorld) {
            int count = 1 + world.getRandom().nextInt(2);
            for (int i = 0; i < count; i++) {
                double angle = world.getRandom().nextDouble() * Math.PI * 2;
                double distance = 8 + world.getRandom().nextDouble() * 12;
                int x = (int) (this.getX() + Math.cos(angle) * distance);
                int z = (int) (this.getZ() + Math.sin(angle) * distance);
                BlockPos pos = new BlockPos(x, this.getBlockY() + world.getRandom().nextInt(3) - 1, z);

                if (pos.getY() < world.getBottomY() || pos.getY() > world.getTopY()) continue;
                if (!world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) continue;
                if (!world.getBlockState(pos).isAir() || !world.getBlockState(pos.up()).isAir()) continue;

                ZombieEntity reinforcement = new ZombieEntity(EntityType.ZOMBIE, serverWorld);
                reinforcement.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                        world.getRandom().nextFloat() * 360.0f, 0.0f);
                reinforcement.initialize(serverWorld, serverWorld.getLocalDifficulty(pos),
                        SpawnReason.REINFORCEMENT, null, null);
                serverWorld.spawnEntity(reinforcement);
            }
        }
    }

    /**
     * 增强寻路范围 - 让僵尸从更远的地方追踪玩家
     */
    @Inject(method = "createZombieAttributes", at = @At("RETURN"), cancellable = true)
    private static void modifyBaseAttributes(CallbackInfoReturnable<net.minecraft.entity.attribute.DefaultAttributeContainer.Builder> cir) {
        cir.setReturnValue(cir.getReturnValue()
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, ModConfig.ZOMBIE_ENHANCED_FOLLOW_RANGE));
    }
}