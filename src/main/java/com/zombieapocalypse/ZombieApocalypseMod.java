package com.zombieapocalypse;

import com.zombieapocalypse.entity.ModEntities;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.world.Heightmap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZombieApocalypseMod implements ModInitializer {
    public static final String MOD_ID = "zombieapocalypse";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

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

        // 记录服务器启动
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // 全局阶段追踪可以在这里处理
        });

        LOGGER.info("=== 惊变100天 模组加载完成 ===");
    }
}