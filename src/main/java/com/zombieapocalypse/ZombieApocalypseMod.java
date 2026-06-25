package com.zombieapocalypse;

import com.zombieapocalypse.config.ModConfig;
import com.zombieapocalypse.config.StageSystem;
import com.zombieapocalypse.entity.ModEntities;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZombieApocalypseMod implements ModInitializer {
    public static final String MOD_ID = "zombieapocalypse";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final int DAYTIME_SPAWN_INTERVAL = 100; // 每5秒尝试一次白天生成
    private int tickCounter = 0;

    @Override
    public void onInitialize() {
        LOGGER.info("=== 惊变100天 模组初始化 ===");
        LOGGER.info("僵尸末日即将来临...");

        // 注册实体
        ModEntities.registerEntities();

        // 注册巨型僵尸生成限制
        SpawnRestriction.register(
                ModEntities.GIANT_ZOMBIE,
                SpawnRestriction.Location.ON_GROUND,
                Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                (type, world, spawnReason, pos, random) -> true
        );

        // 白天僵尸生成 - 周期性在世界中生成僵尸
        ServerTickEvents.END_WORLD_TICK.register(this::onWorldTick);

        LOGGER.info("=== 惊变100天 模组加载完成 ===");
    }

    /**
     * 每个世界 tick 时调用
     * 白天时额外生成僵尸，确保玩家始终面临威胁
     */
    private void onWorldTick(ServerWorld world) {
        if (world.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) return;
        if (!ModConfig.ZOMBIE_DAYTIME_SPAWN) return;

        // 检查是否为白天
        if (!world.isDay()) return;

        tickCounter++;
        if (tickCounter < DAYTIME_SPAWN_INTERVAL) return;
        tickCounter = 0;

        // 获取当前阶段
        int stage = StageSystem.getCurrentStage(world);
        double stageProgress = StageSystem.getStageProgress(world);

        // 每个玩家尝试生成僵尸
        var players = world.getPlayers();
        for (var player : players) {
            if (player.isCreative() || player.isSpectator()) continue;

            // 阶段越高，生成概率越大 (基础20% → 最高70%)
            double spawnChance = 0.2 + (stageProgress * 0.5);
            // 白天生成倍率
            spawnChance *= ModConfig.ZOMBIE_DAYTIME_SPAWN_MULTIPLIER;

            Random random = world.getRandom();
            if (random.nextDouble() > spawnChance) continue;

            // 在玩家周围随机位置生成僵尸
            int spawnCount = 1 + (stage / 20); // 阶段越高，一次生成越多
            for (int i = 0; i < spawnCount; i++) {
                trySpawnZombieNearPlayer(world, player, random);
            }
        }
    }

    /**
     * 在玩家附近尝试生成一个僵尸
     */
    private void trySpawnZombieNearPlayer(ServerWorld world, net.minecraft.entity.player.PlayerEntity player, Random random) {
        // 在玩家周围 24-48 格范围内随机位置
        double angle = random.nextDouble() * Math.PI * 2;
        double distance = 24 + random.nextDouble() * 24;
        int x = (int) (player.getX() + Math.cos(angle) * distance);
        int z = (int) (player.getZ() + Math.sin(angle) * distance);

        // 找到合适的 Y 坐标
        BlockPos pos = new BlockPos(x, world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z), z);

        // 确保位置有效
        if (pos.getY() < world.getBottomY() || pos.getY() > world.getTopY()) return;

        // 检查位置是否适合生成
        BlockPos spawnPos = pos.down();
        if (!world.getBlockState(spawnPos).isSolidBlock(world, spawnPos)) return;
        if (!world.getBlockState(pos).isAir() || !world.getBlockState(pos.up()).isAir()) return;

        // 创建僵尸
        ZombieEntity zombie = new ZombieEntity(EntityType.ZOMBIE, world);
        zombie.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
                random.nextFloat() * 360.0f, 0.0f);

        // 初始化
        zombie.initialize(world, world.getLocalDifficulty(pos), SpawnReason.NATURAL, null, null);

        // 设置属性为当前阶段值
        double health = StageSystem.getZombieHealth(world);
        double attack = StageSystem.getZombieAttack(world);
        double difficultyMult = ModConfig.getDifficultyMultiplier(world.getDifficulty());
        health *= difficultyMult;
        attack *= difficultyMult;

        var healthAttr = zombie.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.setBaseValue(health);
            zombie.setHealth((float) health);
        }

        var attackAttr = zombie.getAttributeInstance(net.minecraft.entity.attribute.EntityAttributes.GENERIC_ATTACK_DAMAGE);
        if (attackAttr != null) {
            attackAttr.setBaseValue(attack);
        }

        world.spawnEntity(zombie);
    }
}