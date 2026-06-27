package com.zombieapocalypse.entity;

import com.zombieapocalypse.ai.BreakBlockGoal;
import com.zombieapocalypse.ai.BuildBlockGoal;
import com.zombieapocalypse.ai.GiantZombieAttackGoal;
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
 * 攻击范围: 2.5倍 (约为普通僵尸的2.5倍距离)
 */
public class GiantZombieEntity extends ZombieEntity {

    private static final float ATTACK_RANGE_MULTIPLIER = 2.5f;

    public GiantZombieEntity(EntityType<? extends GiantZombieEntity> entityType, World world) {
        super(entityType, world);
        // 巨型僵尸可跨过两格高方块（原版默认0.6，只能跨1格）
        this.setStepHeight(2.0f);
    }

    @Override
    protected void initGoals() {
        // 破坏方块AI - 最高优先级
        this.goalSelector.add(0, new BreakBlockGoal(this));
        // 搭方块AI
        this.goalSelector.add(1, new BuildBlockGoal(this));
        // 攻击 (巨型范围)
        this.goalSelector.add(2, new GiantZombieAttackGoal(this, 1.0, false, ATTACK_RANGE_MULTIPLIER));
        this.goalSelector.add(3, new ChaseBoatGoal(this));
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
        if (!this.getWorld().isClient && this.isAlive()) {
            updateAttributes();
        }
    }

    /**
     * 根据当前阶段更新巨型僵尸属性
     * 速度额外受夜晚/血月/低血量狂暴影响
     * 攻击额外受血月影响
     * 追踪范围随智能度增长
     */
    private void updateAttributes() {
        World world = this.getWorld();
        if (world == null) return;
        if (this.isDead()) return;

        double health = StageSystem.getGiantZombieHealth(world);
        double attack = StageSystem.getGiantZombieAttack(world);
        double speed = 0.20 + (StageSystem.getStageProgress(world) * 0.10);

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

        // 动态追踪范围
        int followRange = StageSystem.getFollowRange(world);
        var followAttr = this.getAttributeInstance(EntityAttributes.GENERIC_FOLLOW_RANGE);
        if (followAttr != null && followAttr.getBaseValue() != followRange) {
            followAttr.setBaseValue(followRange);
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