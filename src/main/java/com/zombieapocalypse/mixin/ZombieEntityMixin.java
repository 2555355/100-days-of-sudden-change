package com.zombieapocalypse.mixin;

import com.zombieapocalypse.ai.BreakBlockGoal;
import com.zombieapocalypse.ai.BuildBlockGoal;
import com.zombieapocalypse.ai.IBlockCollector;
import com.zombieapocalypse.config.ModConfig;
import com.zombieapocalypse.config.StageSystem;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ZombieEntity;
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
     * 降低小僵尸生成概率 (默认5% → 2%)
     * 在initialize之后，如果生成了小僵尸，有60%概率转回成年
     */
    @Inject(method = "initialize", at = @At("TAIL"))
    private void reduceBabyChance(net.minecraft.world.ServerWorldAccess world,
                                  net.minecraft.world.LocalDifficulty difficulty,
                                  net.minecraft.entity.SpawnReason spawnReason,
                                  net.minecraft.entity.EntityData entityData,
                                  net.minecraft.nbt.NbtCompound entityNbt,
                                  CallbackInfoReturnable<net.minecraft.entity.EntityData> cir) {
        ZombieEntity self = (ZombieEntity) (Object) this;
        if (self.isBaby() && self.getRandom().nextFloat() < 0.6f) {
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
     * 每tick更新僵尸属性 (基于阶段)
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (this.getWorld().isClient) return;
        updateZombieAttributes();
    }

    /**
     * 根据阶段更新僵尸属性
     * 血量: 20-400, 攻击: 3-15, 速度: 0.23-0.35, 护甲: 2-12
     */
    @Unique
    private void updateZombieAttributes() {
        World world = this.getWorld();
        if (world == null) return;
        // 死了就不更新，避免复活僵尸
        if (this.isDead()) return;

        double health = StageSystem.getZombieHealth(world);
        double attack = StageSystem.getZombieAttack(world);
        double speed = StageSystem.getZombieSpeed(world);
        double armor = StageSystem.getZombieArmor(world);

        double difficultyMult = ModConfig.getDifficultyMultiplier(world.getDifficulty());
        health *= difficultyMult;
        attack *= difficultyMult;

        var healthAttr = this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (healthAttr != null && healthAttr.getBaseValue() != health) {
            // 按比例恢复血量，避免僵尸长期残血
            double oldMax = healthAttr.getBaseValue();
            float healthRatio = oldMax > 0 ? this.getHealth() / (float) oldMax : 1.0f;
            healthAttr.setBaseValue(health);
            this.setHealth(Math.min((float) health, (float) health * healthRatio));
        }

        var attackAttr = this.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (attackAttr != null && attackAttr.getBaseValue() != attack) {
            attackAttr.setBaseValue(attack);
        }

        var speedAttr = this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speedAttr != null) {
            double currentSpeed = speedAttr.getBaseValue();
            if (Math.abs(currentSpeed - speed) > 0.001) {
                speedAttr.setBaseValue(speed);
            }
        }

        var armorAttr = this.getAttributeInstance(EntityAttributes.GENERIC_ARMOR);
        if (armorAttr != null && armorAttr.getBaseValue() != armor) {
            armorAttr.setBaseValue(armor);
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