package com.zombieapocalypse.mixin;

import com.zombieapocalypse.config.ModConfig;
import com.zombieapocalypse.config.StageSystem;
import com.zombieapocalypse.entity.GiantZombieEntity;
import com.zombieapocalypse.entity.ModEntities;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.SpawnHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 控制生物生成:
 * 1. 大幅减少非僵尸敌对生物生成
 * 2. 僵尸有概率被替换为巨型僵尸
 */
@Mixin(SpawnHelper.class)
public abstract class MobSpawnMixin {

    /**
     * 拦截生物生成，控制生成类型
     * 如果非僵尸敌对生物，极高概率取消生成
     * 如果僵尸，根据阶段概率替换为巨型僵尸
     */
    @Inject(method = "spawn", at = @At("HEAD"), cancellable = true)
    private static void onEntitySpawn(ServerWorld world, EntityType<?> entityType,
                                       SpawnReason spawnReason, BlockPos pos,
                                       Random random, CallbackInfoReturnable<Boolean> cir) {
        // 只处理自然生成
        if (spawnReason != SpawnReason.NATURAL) return;

        // 非僵尸的敌对生物：大幅降低生成概率
        if (entityType != EntityType.ZOMBIE && entityType != EntityType.HUSK
                && entityType != EntityType.DROWNED && entityType != EntityType.ZOMBIE_VILLAGER) {

            // 检查是否为敌对生物
            if (isHostileMob(entityType)) {
                // 以极低概率(2%)允许生成
                if (random.nextDouble() > ModConfig.OTHER_HOSTILE_SPAWN_CHANCE) {
                    cir.setReturnValue(false);
                    return;
                }
            }
        }

        // 僵尸类：有概率替换为巨型僵尸
        if (entityType == EntityType.ZOMBIE || entityType == EntityType.HUSK
                || entityType == EntityType.DROWNED || entityType == EntityType.ZOMBIE_VILLAGER) {

            double giantChance = StageSystem.getGiantZombieChance(world);
            if (random.nextDouble() < giantChance) {
                // 取消原僵尸生成，生成巨型僵尸
                cir.setReturnValue(false);

                GiantZombieEntity giantZombie = new GiantZombieEntity(ModEntities.GIANT_ZOMBIE, world);
                giantZombie.refreshPositionAndAngles(
                        pos.getX() + 0.5,
                        pos.getY(),
                        pos.getZ() + 0.5,
                        random.nextFloat() * 360.0f,
                        0.0f
                );

                // 初始化巨型僵尸属性
                giantZombie.initialize(world, world.getLocalDifficulty(pos),
                        SpawnReason.NATURAL, null, null);

                world.spawnEntity(giantZombie);
                return;
            }
        }
    }

    /**
     * 判断是否为需要限制的敌对生物
     */
    private static boolean isHostileMob(EntityType<?> type) {
        return type == EntityType.SKELETON
                || type == EntityType.CREEPER
                || type == EntityType.SPIDER
                || type == EntityType.CAVE_SPIDER
                || type == EntityType.ENDERMAN
                || type == EntityType.WITCH
                || type == EntityType.SLIME
                || type == EntityType.PHANTOM
                || type == EntityType.SILVERFISH
                || type == EntityType.ENDERMITE
                || type == EntityType.STRAY
                || type == EntityType.WITHER_SKELETON
                || type == EntityType.BLAZE
                || type == EntityType.GHAST
                || type == EntityType.MAGMA_CUBE
                || type == EntityType.PIGLIN
                || type == EntityType.ZOMBIFIED_PIGLIN
                || type == EntityType.HOGLIN
                || type == EntityType.ZOGLIN
                || type == EntityType.PILLAGER
                || type == EntityType.VINDICATOR
                || type == EntityType.EVOKER
                || type == EntityType.VEX
                || type == EntityType.RAVAGER
                || type == EntityType.GUARDIAN
                || type == EntityType.ELDER_GUARDIAN
                || type == EntityType.SHULKER;
    }
}