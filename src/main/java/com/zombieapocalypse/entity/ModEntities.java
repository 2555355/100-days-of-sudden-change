package com.zombieapocalypse.entity;

import com.zombieapocalypse.ZombieApocalypseMod;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {
    public static final EntityType<GiantZombieEntity> GIANT_ZOMBIE = Registry.register(
            Registries.ENTITY_TYPE,
            new Identifier(ZombieApocalypseMod.MOD_ID, "giant_zombie"),
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, GiantZombieEntity::new)
                    .dimensions(EntityDimensions.fixed(1.2f, 3.9f)) // 原版僵尸的2倍 (0.6x1.95)
                    .trackRangeBlocks(64)
                    .forceTrackedVelocityUpdates(true)
                    .trackedUpdateRate(3)
                    .build()
    );

    public static void registerEntities() {
        ZombieApocalypseMod.LOGGER.info("注册巨型僵尸实体完成");
    }
}