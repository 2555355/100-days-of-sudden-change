package com.zombieapocalypse.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * 末日情报书 - 右键打开末日情报面板
 * 实际打开 Screen 的逻辑在客户端 ZombieApocalypseClient 中通过 UseItemCallback 处理
 * （避免物品类直接依赖客户端类，保证专用服务器不崩溃）
 */
public class ApocalypseBookItem extends Item {

    public ApocalypseBookItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        // 客户端打开 Screen 由 UseItemCallback 处理；服务端仅返回成功以同步动作
        return TypedActionResult.success(stack, world.isClient());
    }
}
