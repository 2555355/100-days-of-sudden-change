package com.zombieapocalypse.item;

import com.zombieapocalypse.ZombieApocalypseMod;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;

public class ModItems {
    /** 末日情报书 - 右键打开末日情报面板 */
    public static final Item APOCALYPSE_BOOK = Registry.register(
            Registries.ITEM,
            new Identifier(ZombieApocalypseMod.MOD_ID, "apocalypse_book"),
            new ApocalypseBookItem(new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON))
    );

    /** 独立创造物品组 - 惊变100天 */
    public static final ItemGroup APOCALYPSE_GROUP = Registry.register(
            Registries.ITEM_GROUP,
            new Identifier(ZombieApocalypseMod.MOD_ID, "apocalypse_group"),
            FabricItemGroup.builder()
                    .displayName(Text.literal("惊变100天"))
                    .icon(() -> new ItemStack(APOCALYPSE_BOOK))
                    .entries((displayContext, entries) -> {
                        entries.add(new ItemStack(APOCALYPSE_BOOK));
                    })
                    .build()
    );

    public static void registerItems() {
        ZombieApocalypseMod.LOGGER.info("注册末日情报书物品及物品组完成");
    }
}
