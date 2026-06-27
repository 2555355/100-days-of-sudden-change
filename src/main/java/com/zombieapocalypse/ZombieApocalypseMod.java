package com.zombieapocalypse;

import com.zombieapocalypse.config.ModConfig;
import com.zombieapocalypse.config.StageSystem;
import com.zombieapocalypse.entity.GiantZombieEntity;
import com.zombieapocalypse.entity.ModEntities;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZombieApocalypseMod implements ModInitializer {
    public static final String MOD_ID = "zombieapocalypse";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final int DAYTIME_SPAWN_INTERVAL = 40;
    private int tickCounter = 0;

    @Override
    public void onInitialize() {
        LOGGER.info("=== 惊变100天 模组初始化 ===");
        LOGGER.info("僵尸末日即将来临...");

        // 注册实体
        ModEntities.registerEntities();

        // 注册巨型僵尸默认属性
        FabricDefaultAttributeRegistry.register(ModEntities.GIANT_ZOMBIE, GiantZombieEntity.createGiantZombieAttributes());

        // 注册巨型僵尸生成限制
        SpawnRestriction.register(
                ModEntities.GIANT_ZOMBIE,
                SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                (type, world, spawnReason, pos, random) -> true
        );

        // 拦截实体加载：过滤非僵尸敌对生物，替换僵尸为巨型僵尸
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            // 过滤：移除非僵尸敌对生物
            if (entity instanceof Monster && !(entity instanceof ZombieEntity)) {
                if (world.getRandom().nextDouble() > ModConfig.OTHER_HOSTILE_SPAWN_CHANCE) {
                    entity.discard();
                    return;
                }
            }

            // 僵尸替换为巨型僵尸，或应用阶段属性
            if (entity instanceof ZombieEntity && !(entity instanceof GiantZombieEntity)) {
                double giantChance = StageSystem.getGiantZombieChance(world);
                if (world.getRandom().nextDouble() < giantChance) {
                    BlockPos pos = entity.getBlockPos();
                    entity.discard();

                    GiantZombieEntity giant = new GiantZombieEntity(ModEntities.GIANT_ZOMBIE, world);
                    giant.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                            world.getRandom().nextFloat() * 360.0f, 0.0f);
                    giant.initialize(world, world.getLocalDifficulty(pos), SpawnReason.NATURAL, null, null);
                    applyStageAttributes(giant, world, true);
                    world.spawnEntity(giant);
                } else {
                    // 普通僵尸也应用阶段属性，确保满血生成
                    applyStageAttributes((LivingEntity) entity, world, false);
                }
            }
        });

        // 白天僵尸生成
        ServerTickEvents.END_WORLD_TICK.register(this::onWorldTick);

        LOGGER.info("=== 惊变100天 模组加载完成 ===");
    }

    /**
     * 白天和夜晚额外生成僵尸（增强刷新量）
     * 血月期间所有刷新倍率翻倍
     */
    private void onWorldTick(ServerWorld world) {
        if (world.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) return;
        if (!ModConfig.ZOMBIE_DAYTIME_SPAWN) return;

        tickCounter++;
        if (tickCounter < DAYTIME_SPAWN_INTERVAL) return;
        tickCounter = 0;

        double stageProgress = StageSystem.getStageProgress(world);
        int stage = StageSystem.getCurrentStage(world);
        boolean isDay = world.isDay();
        boolean isBloodMoon = StageSystem.isBloodMoon(world);

        // 血月开始时发送提示
        if (isBloodMoon && world.getTimeOfDay() % 24000L < 20) {
            for (var player : world.getPlayers()) {
                player.sendMessage(Text.literal(""), false);
                player.sendMessage(Text.literal("§c§l⚠ §4§l血月降临！§c§l ⚠"), false);
                player.sendMessage(Text.literal("§c所有怪物刷新概率和数量翻倍！"), false);
                player.sendMessage(Text.literal(""), false);
            }
        }

        double bloodMoonMult = isBloodMoon ? ModConfig.BLOOD_MOON_SPAWN_MULTIPLIER : 1.0;

        for (var player : world.getPlayers()) {
            if (player.isCreative() || player.isSpectator()) continue;

            double spawnChance;
            int spawnCount;
            if (isDay) {
                // 白天少刷：概率和数量大幅降低
                spawnChance = (0.15 + stageProgress * 0.2) * bloodMoonMult;
                spawnCount = (int) Math.max(1, (1 + stage / 25) * bloodMoonMult);
            } else {
                // 夜晚刷新：概率和数量随阶段增长，但已调低以避免过度拥挤
                spawnChance = (0.4 + stageProgress * 0.2) * bloodMoonMult;
                spawnCount = (int) ((2 + stage / 12) * bloodMoonMult);
            }

            Random random = world.getRandom();
            if (random.nextDouble() > spawnChance) continue;

            for (int i = 0; i < spawnCount; i++) {
                trySpawnZombieNearPlayer(world, player, random);
            }
        }
    }

    private void trySpawnZombieNearPlayer(ServerWorld world, PlayerEntity player, Random random) {
        double angle = random.nextDouble() * Math.PI * 2;
        double distance = 16 + random.nextDouble() * 28;
        int x = (int) (player.getX() + Math.cos(angle) * distance);
        int z = (int) (player.getZ() + Math.sin(angle) * distance);

        BlockPos pos = new BlockPos(x, world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z), z);
        if (pos.getY() < world.getBottomY() || pos.getY() > world.getTopY()) return;

        BlockPos groundPos = pos.down();
        if (!world.getBlockState(groundPos).isSolidBlock(world, groundPos)) return;
        if (!world.getBlockState(pos).isAir() || !world.getBlockState(pos.up()).isAir()) return;

        spawnZombieAt(world, pos, random);
    }

    /**
     * 在指定位置生成僵尸（根据阶段概率生成巨型僵尸）
     */
    private void spawnZombieAt(ServerWorld world, BlockPos pos, Random random) {
        double giantChance = StageSystem.getGiantZombieChance(world);

        if (random.nextDouble() < giantChance) {
            // 生成巨型僵尸
            GiantZombieEntity giant = new GiantZombieEntity(ModEntities.GIANT_ZOMBIE, world);
            giant.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                    random.nextFloat() * 360.0f, 0.0f);
            giant.initialize(world, world.getLocalDifficulty(pos), SpawnReason.NATURAL, null, null);

            applyStageAttributes(giant, world, true);
            world.spawnEntity(giant);
        } else {
            // 生成普通僵尸
            ZombieEntity zombie = new ZombieEntity(EntityType.ZOMBIE, world);
            zombie.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                    random.nextFloat() * 360.0f, 0.0f);
            zombie.initialize(world, world.getLocalDifficulty(pos), SpawnReason.NATURAL, null, null);

            applyStageAttributes(zombie, world, false);
            world.spawnEntity(zombie);
        }
    }

    /**
     * 应用阶段属性到僵尸
     */
    public static void applyStageAttributes(LivingEntity entity, ServerWorld world, boolean isGiant) {
        double health, attack, speed;
        if (isGiant) {
            health = StageSystem.getGiantZombieHealth(world);
            attack = StageSystem.getGiantZombieAttack(world);
            speed = 0.20 + (StageSystem.getStageProgress(world) * 0.10);
        } else {
            health = StageSystem.getZombieHealth(world);
            attack = StageSystem.getZombieAttack(world);
            speed = StageSystem.getZombieSpeed(world);
        }

        double difficultyMult = ModConfig.getDifficultyMultiplier(world.getDifficulty());
        health *= difficultyMult;
        attack *= difficultyMult;

        var healthAttr = entity.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(health);
            entity.setHealth((float) health);
        }

        var attackAttr = entity.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (attackAttr != null) {
            attackAttr.setBaseValue(attack);
        }

        var speedAttr = entity.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(speed);
        }
    }
}