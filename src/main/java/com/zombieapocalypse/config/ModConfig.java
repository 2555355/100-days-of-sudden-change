package com.zombieapocalypse.config;

import net.minecraft.world.Difficulty;

/**
 * 模组配置
 * 控制僵尸末日模组的各项参数
 */
public class ModConfig {
    // ========== 生成控制 ==========
    /** 非僵尸敌对生物生成概率倍率 (0.0 = 几乎不生成, 1.0 = 正常) */
    public static double OTHER_HOSTILE_SPAWN_CHANCE = 0.02;

    /** 是否允许僵尸白天生成 */
    public static boolean ZOMBIE_DAYTIME_SPAWN = true;

    /** 僵尸白天生成概率倍率 (相对于夜晚) */
    public static double ZOMBIE_DAYTIME_SPAWN_MULTIPLIER = 0.6;

    /** 僵尸全局刷新倍率 (作用于刷新概率和数量, 0.5 = 在原有基础上再减半) */
    public static double ZOMBIE_SPAWN_MULTIPLIER = 0.5;

    // ========== 僵尸属性 ==========
    /** 是否防白天燃烧 */
    public static boolean ZOMBIE_SUN_BURN_IMMUNE = true;

    /** 僵尸基础血量 */
    public static double ZOMBIE_BASE_HEALTH = 20.0;

    /** 僵尸最高血量 (第100天) */
    public static double ZOMBIE_MAX_HEALTH = 400.0;

    /** 僵尸基础攻击力 */
    public static double ZOMBIE_BASE_ATTACK = 3.0;

    /** 僵尸最高攻击力 */
    public static double ZOMBIE_MAX_ATTACK = 15.0;

    /** 僵尸基础移动速度 */
    public static double ZOMBIE_BASE_SPEED = 0.23;

    /** 僵尸最高移动速度 */
    public static double ZOMBIE_MAX_SPEED = 0.35;

    /** 僵尸基础护甲值 */
    public static double ZOMBIE_BASE_ARMOR = 2.0;

    /** 僵尸最高护甲值 */
    public static double ZOMBIE_MAX_ARMOR = 12.0;

    // ========== 巨型僵尸 ==========
    /** 巨型僵尸基础血量 */
    public static double GIANT_ZOMBIE_BASE_HEALTH = 400.0;

    /** 巨型僵尸最高血量 */
    public static double GIANT_ZOMBIE_MAX_HEALTH = 600.0;

    /** 巨型僵尸基础攻击力 */
    public static double GIANT_ZOMBIE_BASE_ATTACK = 8.0;

    /** 巨型僵尸最高攻击力 */
    public static double GIANT_ZOMBIE_MAX_ATTACK = 25.0;

    /** 巨型僵尸基础生成概率 (第40天起) */
    public static double GIANT_ZOMBIE_BASE_CHANCE = 0.05;

    /** 巨型僵尸最高生成概率 (第100天) */
    public static double GIANT_ZOMBIE_MAX_CHANCE = 0.30;

    /** 巨型僵尸大小倍率 */
    public static float GIANT_ZOMBIE_SCALE = 2.0f;

    // ========== 阶段系统 ==========
    /** 总天数 (惊变100天) */
    public static int TOTAL_DAYS = 100;

    // ========== 方块破坏 ==========
    /** 是否允许僵尸破坏方块 */
    public static boolean ZOMBIE_CAN_BREAK_BLOCKS = true;

    /** 僵尸破坏方块间隔 (tick) */
    public static int ZOMBIE_BREAK_BLOCK_INTERVAL = 30;

    /** 僵尸破坏方块硬度上限 */
    public static float ZOMBIE_BREAK_HARDNESS_LIMIT = 10.0f;

    /** 僵尸搭方块间隔 (tick) */
    public static int ZOMBIE_BUILD_BLOCK_INTERVAL = 40;

    /** 僵尸增强AI寻路范围 */
    public static int ZOMBIE_ENHANCED_FOLLOW_RANGE = 64;

    // ========== 血月系统 ==========
    /** 血月间隔天数 */
    public static int BLOOD_MOON_INTERVAL = 10;
    /** 血月期间刷新倍率 */
    public static double BLOOD_MOON_SPAWN_MULTIPLIER = 2.0;

    // ========== 僵尸智能度阶梯 ==========
    /** 各智力等级起始天数 [1, 11, 21, 31, 51, 71] */
    public static int[] INTELLIGENCE_DAY_THRESHOLDS = {1, 11, 21, 31, 51, 71};
    /** 各智力等级拆方块间隔 (tick) */
    public static int[] INTELLIGENCE_BREAK_INTERVALS = {30, 20, 14, 9, 5, 3};
    /** 各智力等级搭方块间隔 (tick) */
    public static int[] INTELLIGENCE_BUILD_INTERVALS = {24, 16, 10, 6, 4, 2};
    /** 各智力等级可破坏方块硬度上限 */
    public static float[] INTELLIGENCE_HARDNESS_LIMITS = {10, 15, 25, 35, 50, 100};
    /** 各智力等级方块库存容量 */
    public static int[] INTELLIGENCE_BLOCK_INVENTORY = {4, 6, 8, 12, 16, 24};

    // ========== AI增强 ==========
    /** 低血量狂暴阈值 (血量低于此比例时触发) */
    public static float ENRAGE_HEALTH_THRESHOLD = 0.3f;
    /** 低血量狂暴速度倍率 */
    public static double ENRAGE_SPEED_MULTIPLIER = 1.5;
    /** 夜晚速度加成倍率 */
    public static double NIGHT_SPEED_BONUS = 1.15;
    /** 血月速度加成倍率 */
    public static double BLOOD_MOON_SPEED_BONUS = 1.3;
    /** 血月攻击加成倍率 */
    public static double BLOOD_MOON_ATTACK_BONUS = 1.2;
    /** 各智力等级追踪范围 */
    public static int[] INTELLIGENCE_FOLLOW_RANGE = {48, 64, 80, 96, 128, 160};
    /** 各智力等级呼叫增援概率 */
    public static double[] INTELLIGENCE_REINFORCEMENT_CHANCE = {0.0, 0.0, 0.05, 0.10, 0.15, 0.25};

    // ========== 难度加成 ==========
    /** 简单难度属性倍率 */
    public static double EASY_MULTIPLIER = 0.7;
    /** 普通难度属性倍率 */
    public static double NORMAL_MULTIPLIER = 1.0;
    /** 困难难度属性倍率 */
    public static double HARD_MULTIPLIER = 1.5;

    /**
     * 获取难度倍率
     */
    public static double getDifficultyMultiplier(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY, PEACEFUL -> EASY_MULTIPLIER;
            case NORMAL -> NORMAL_MULTIPLIER;
            case HARD -> HARD_MULTIPLIER;
        };
    }
}