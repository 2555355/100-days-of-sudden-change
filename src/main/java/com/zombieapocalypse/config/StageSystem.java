package com.zombieapocalypse.config;

import net.minecraft.world.World;

/**
 * 阶段系统 - 基于世界天数计算僵尸增强阶段
 * 惊变100天：每天为一个阶段，僵尸属性随阶段线性增长
 */
public class StageSystem {

    /**
     * 获取当前阶段 (1-100)
     * 基于世界已度过天数
     */
    public static int getCurrentStage(World world) {
        if (world == null) return 1;
        long dayTime = world.getTimeOfDay() / 24000L;
        int stage = (int) dayTime + 1;
        return Math.max(1, Math.min(stage, ModConfig.TOTAL_DAYS));
    }

    /**
     * 获取阶段进度 (0.0 - 1.0)
     */
    public static double getStageProgress(World world) {
        int stage = getCurrentStage(world);
        return (double) (stage - 1) / (double) (ModConfig.TOTAL_DAYS - 1);
    }

    /**
     * 根据阶段计算僵尸血量
     * 第1天: 20, 第100天: 400, 线性增长
     */
    public static double getZombieHealth(World world) {
        double progress = getStageProgress(world);
        return ModConfig.ZOMBIE_BASE_HEALTH +
                (ModConfig.ZOMBIE_MAX_HEALTH - ModConfig.ZOMBIE_BASE_HEALTH) * progress;
    }

    /**
     * 根据阶段计算僵尸攻击力
     */
    public static double getZombieAttack(World world) {
        double progress = getStageProgress(world);
        return ModConfig.ZOMBIE_BASE_ATTACK +
                (ModConfig.ZOMBIE_MAX_ATTACK - ModConfig.ZOMBIE_BASE_ATTACK) * progress;
    }

    /**
     * 根据阶段计算僵尸移动速度
     */
    public static double getZombieSpeed(World world) {
        double progress = getStageProgress(world);
        return ModConfig.ZOMBIE_BASE_SPEED +
                (ModConfig.ZOMBIE_MAX_SPEED - ModConfig.ZOMBIE_BASE_SPEED) * progress;
    }

    /**
     * 根据阶段计算僵尸护甲值
     */
    public static double getZombieArmor(World world) {
        double progress = getStageProgress(world);
        return ModConfig.ZOMBIE_BASE_ARMOR +
                (ModConfig.ZOMBIE_MAX_ARMOR - ModConfig.ZOMBIE_BASE_ARMOR) * progress;
    }

    /**
     * 根据阶段计算巨型僵尸血量
     * 第1天: 200, 第100天: 600
     */
    public static double getGiantZombieHealth(World world) {
        double progress = getStageProgress(world);
        return ModConfig.GIANT_ZOMBIE_BASE_HEALTH +
                (ModConfig.GIANT_ZOMBIE_MAX_HEALTH - ModConfig.GIANT_ZOMBIE_BASE_HEALTH) * progress;
    }

    /**
     * 根据阶段计算巨型僵尸攻击力
     */
    public static double getGiantZombieAttack(World world) {
        double progress = getStageProgress(world);
        return ModConfig.GIANT_ZOMBIE_BASE_ATTACK +
                (ModConfig.GIANT_ZOMBIE_MAX_ATTACK - ModConfig.GIANT_ZOMBIE_BASE_ATTACK) * progress;
    }

    /**
     * 根据阶段计算巨型僵尸生成概率
     * 第1天: 2%, 第100天: 30%
     */
    public static double getGiantZombieChance(World world) {
        double progress = getStageProgress(world);
        return ModConfig.GIANT_ZOMBIE_BASE_CHANCE +
                (ModConfig.GIANT_ZOMBIE_MAX_CHANCE - ModConfig.GIANT_ZOMBIE_BASE_CHANCE) * progress;
    }

    /**
     * 获取当前天数 (从0开始)
     */
    public static int getCurrentDay(World world) {
        return (int) (world.getTimeOfDay() / 24000L);
    }

    /**
     * 是否为血月 (每10天一次)
     */
    public static boolean isBloodMoon(World world) {
        int day = getCurrentDay(world);
        return day > 0 && day % ModConfig.BLOOD_MOON_INTERVAL == 0;
    }

    /**
     * 获取僵尸智能度等级 (0-5)
     * 基于当前天数阶梯: Lv0=1-10, Lv1=11-20, Lv2=21-30, Lv3=31-50, Lv4=51-70, Lv5=71-100
     */
    public static int getIntelligenceLevel(World world) {
        int day = getCurrentDay(world);
        int level = 0;
        for (int i = ModConfig.INTELLIGENCE_DAY_THRESHOLDS.length - 1; i >= 0; i--) {
            if (day >= ModConfig.INTELLIGENCE_DAY_THRESHOLDS[i]) {
                level = i;
                break;
            }
        }
        return level;
    }

    /**
     * 获取当前智能度下的拆方块间隔
     */
    public static int getBreakInterval(World world) {
        return ModConfig.INTELLIGENCE_BREAK_INTERVALS[getIntelligenceLevel(world)];
    }

    /**
     * 获取当前智能度下的搭方块间隔
     */
    public static int getBuildInterval(World world) {
        return ModConfig.INTELLIGENCE_BUILD_INTERVALS[getIntelligenceLevel(world)];
    }

    /**
     * 获取当前智能度下的方块硬度上限
     */
    public static float getHardnessLimit(World world) {
        return ModConfig.INTELLIGENCE_HARDNESS_LIMITS[getIntelligenceLevel(world)];
    }

    /**
     * 获取当前智能度下的方块库存容量
     */
    public static int getBlockInventorySize(World world) {
        return ModConfig.INTELLIGENCE_BLOCK_INVENTORY[getIntelligenceLevel(world)];
    }

    /**
     * 获取阶段显示名称
     */
    public static String getStageDisplayName(World world) {
        int stage = getCurrentStage(world);
        return "第" + stage + "天";
    }
}