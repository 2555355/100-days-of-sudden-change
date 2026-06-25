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
    public static double GIANT_ZOMBIE_BASE_HEALTH = 200.0;

    /** 巨型僵尸最高血量 */
    public static double GIANT_ZOMBIE_MAX_HEALTH = 600.0;

    /** 巨型僵尸基础攻击力 */
    public static double GIANT_ZOMBIE_BASE_ATTACK = 8.0;

    /** 巨型僵尸最高攻击力 */
    public static double GIANT_ZOMBIE_MAX_ATTACK = 25.0;

    /** 巨型僵尸基础生成概率 (第1天) */
    public static double GIANT_ZOMBIE_BASE_CHANCE = 0.01;

    /** 巨型僵尸最高生成概率 (第100天) */
    public static double GIANT_ZOMBIE_MAX_CHANCE = 0.15;

    /** 巨型僵尸大小倍率 */
    public static float GIANT_ZOMBIE_SCALE = 2.0f;

    // ========== 阶段系统 ==========
    /** 总天数 (惊变100天) */
    public static int TOTAL_DAYS = 100;

    // ========== 方块破坏 ==========
    /** 是否允许僵尸破坏方块 */
    public static boolean ZOMBIE_CAN_BREAK_BLOCKS = true;

    /** 僵尸破坏方块间隔 (tick) */
    public static int ZOMBIE_BREAK_BLOCK_INTERVAL = 60;

    /** 僵尸破坏方块硬度上限 */
    public static float ZOMBIE_BREAK_HARDNESS_LIMIT = 5.0f;

    /** 僵尸增强AI寻路范围 */
    public static int ZOMBIE_ENHANCED_FOLLOW_RANGE = 64;

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