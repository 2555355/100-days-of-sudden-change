package com.zombieapocalypse.ai;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.entity.mob.ZombieEntity;

/**
 * 巨型僵尸攻击目标 - 扩大攻击范围
 * 默认僵尸攻击距离 ≈ 1.5格，巨型僵尸需要更大范围
 */
public class GiantZombieAttackGoal extends ZombieAttackGoal {
    private final float rangeMultiplier;

    public GiantZombieAttackGoal(ZombieEntity zombie, double speed, boolean pauseWhenMobIdle, float rangeMultiplier) {
        super(zombie, speed, pauseWhenMobIdle);
        this.rangeMultiplier = rangeMultiplier;
    }

    @Override
    protected double getSquaredMaxAttackDistance(LivingEntity entity) {
        float baseDist = this.mob.getWidth() * 2.0f * this.rangeMultiplier;
        return (double)(baseDist * baseDist + entity.getWidth());
    }
}