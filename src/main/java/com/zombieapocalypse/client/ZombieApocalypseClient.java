package com.zombieapocalypse.client;

import com.zombieapocalypse.config.ModConfig;
import com.zombieapocalypse.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ZombieEntityRenderer;
import net.minecraft.entity.mob.ZombieEntity;

public class ZombieApocalypseClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // 注册巨型僵尸渲染器 - 使用原版僵尸模型，但应用2倍缩放
        EntityRendererRegistry.register(ModEntities.GIANT_ZOMBIE, (context) ->
                new GiantZombieRenderer(context, ModConfig.GIANT_ZOMBIE_SCALE));
    }
}