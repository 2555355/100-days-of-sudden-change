package com.zombieapocalypse.client;

import com.zombieapocalypse.config.ModConfig;
import com.zombieapocalypse.entity.ModEntities;
import com.zombieapocalypse.item.ModItems;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.TypedActionResult;

public class ZombieApocalypseClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // 注册巨型僵尸渲染器 - 使用原版僵尸模型，但应用2倍缩放
        EntityRendererRegistry.register(ModEntities.GIANT_ZOMBIE, (context) ->
                new GiantZombieRenderer(context, ModConfig.GIANT_ZOMBIE_SCALE));

        // 注册血月HUD提示 (血月时屏幕中央显示警告)
        BloodMoonHudOverlay.register();

        // 右键使用末日情报书时打开末日情报面板
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (stack.getItem() == ModItems.APOCALYPSE_BOOK) {
                if (world.isClient) {
                    MinecraftClient.getInstance().setScreen(new ApocalypseBookScreen());
                }
                return TypedActionResult.success(stack, world.isClient());
            }
            return TypedActionResult.pass(stack);
        });
    }
}
