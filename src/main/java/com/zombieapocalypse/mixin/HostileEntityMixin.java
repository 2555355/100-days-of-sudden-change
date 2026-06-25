package com.zombieapocalypse.mixin;

import com.zombieapocalypse.config.ModConfig;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.WorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 控制敌对生物生成 - 只允许僵尸类生成 + 白天生成
 */
@Mixin(HostileEntity.class)
public abstract class HostileEntityMixin {

    /**
     * 拦截所有非僵尸敌对生物的自然生成
     * 只有僵尸(及其变种)可以自然生成
     */
    @Inject(method = "canSpawnInDark", at = @At("HEAD"), cancellable = true)
    private static void restrictNonZombieSpawns(EntityType<? extends HostileEntity> type,
                                                 WorldAccess world,
                                                 SpawnReason spawnReason,
                                                 BlockPos pos,
                                                 Random random,
                                                 CallbackInfoReturnable<Boolean> cir) {
        // 只处理自然生成
        if (spawnReason != SpawnReason.NATURAL) return;

        // 允许僵尸类生成
        if (type == EntityType.ZOMBIE || type == EntityType.HUSK
                || type == EntityType.DROWNED || type == EntityType.ZOMBIE_VILLAGER) {
            return;
        }

        // 对于其他所有敌对生物，极高概率阻止生成
        if (random.nextDouble() > ModConfig.OTHER_HOSTILE_SPAWN_CHANCE) {
            cir.setReturnValue(false);
        }
    }

    /**
     * 允许僵尸白天生成 - 放宽光照条件限制
     */
    @Inject(method = "canSpawnInDark", at = @At("RETURN"), cancellable = true)
    private static void allowDaytimeZombieSpawns(EntityType<? extends HostileEntity> type,
                                                  WorldAccess world,
                                                  SpawnReason spawnReason,
                                                  BlockPos pos,
                                                  Random random,
                                                  CallbackInfoReturnable<Boolean> cir) {
        if (!ModConfig.ZOMBIE_DAYTIME_SPAWN || spawnReason != SpawnReason.NATURAL) return;

        // 只对僵尸类放宽光照条件
        if (type == EntityType.ZOMBIE || type == EntityType.HUSK
                || type == EntityType.DROWNED || type == EntityType.ZOMBIE_VILLAGER) {
            if (!cir.getReturnValue() && world.getDifficulty() != Difficulty.PEACEFUL) {
                cir.setReturnValue(true);
            }
        }
    }
}