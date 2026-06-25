package com.zombieapocalypse.entity;

import com.zombieapocalypse.ai.BreakBlockGoal;
import com.zombieapocalypse.config.ModConfig;
import com.zombieapocalypse.config.StageSystem;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

/**
 * 巨型僵尸实体 - 原版僵尸的2倍大小变体
 * 血量: 200-600 (随阶段增长)
 * 攻击力: 8-25 (随阶段增长)
 */
public class GiantZombieEntity extends ZombieEntity {

    public GiantZombieEntity(EntityType<? extends GiantZombieEntity> entityType, World world) {
        super(entityType, world);
    }

    @Override
    protected void initGoals() {
        // 破坏方块AI - 最高优先级
        this.goalSelector.add(0, new BreakBlockGoal(this, ModConfig.ZOMBIE_BREAK_BLOCK_INTERVAL));

        this.goalSelector.add(1, new ZombieAttackGoal(this, 1.0, false));
        this.goalSelector.add(2, new ChaseBoatGoal(this));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 1.0));
        this.goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(6, new LookAroundGoal(this));

        // 目标选择
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, LivingEntity.class, true,
                living -> living.getType() != EntityType.VILLAGER
                        && !(living instanceof ZombieEntity)
                        && living instanceof HostileEntity == false));
    }

    public static DefaultAttributeContainer.Builder createGiantZombieAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, ModConfig.ZOMBIE_ENHANCED_FOLLOW_RANGE)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.20)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, ModConfig.GIANT_ZOMBIE_BASE_ATTACK)
                .add(EntityAttributes.GENERIC_ARMOR, 4.0)
                .add(EntityAttributes.GENERIC_MAX_HEALTH, ModConfig.GIANT_ZOMBIE_BASE_HEALTH)
                .add(EntityAttributes.ZOMBIE_SPAWN_REINFORCEMENTS, 0.05);
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getWorld().isClient) {
            updateAttributes();
        }
    }

    /**
     * 根据当前阶段更新巨型僵尸属性
     */
    private void updateAttributes() {
        World world = this.getWorld();
        if (world == null) return;

        double health = StageSystem.getGiantZombieHealth(world);
        double attack = StageSystem.getGiantZombieAttack(world);
        double speed = 0.20 + (StageSystem.getStageProgress(world) * 0.10);

        // 应用难度加成
        double difficultyMult = ModConfig.getDifficultyMultiplier(world.getDifficulty());
        health *= difficultyMult;
        attack *= difficultyMult;

        var healthAttr = this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (healthAttr != null && healthAttr.getBaseValue() != health) {
            healthAttr.setBaseValue(health);
            if (this.getHealth() > (float) health) {
                this.setHealth((float) health);
            }
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
    }

    @Override
    public boolean isFireImmune() {
        return true; // 巨型僵尸防火
    }

    @Override
    protected boolean burnsInDaylight() {
        return false; // 巨型僵尸不燃烧
    }
}