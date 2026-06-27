package com.zombieapocalypse.item;

import com.zombieapocalypse.ZombieApocalypseMod;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

public class ModItems {
    /** 末日情报书 - 右键打开末日情报面板 */
    public static final Item APOCALYPSE_BOOK = Registry.register(
            Registries.ITEM,
            new Identifier(ZombieApocalypseMod.MOD_ID, "apocalypse_book"),
            new ApocalypseBookItem(new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON))
    );

    public static void registerItems() {
        ZombieApocalypseMod.LOGGER.info("注册末日情报书物品完成");
    }
}
