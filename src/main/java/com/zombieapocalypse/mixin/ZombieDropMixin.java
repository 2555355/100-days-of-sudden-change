package com.zombieapocalypse.mixin;

import com.zombieapocalypse.config.StageSystem;
import com.zombieapocalypse.entity.GiantZombieEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin - 僵尸掉落物增强
 * 普通僵尸和巨型僵尸额外掉落物品，随阶段增加
 */
@Mixin(LivingEntity.class)
public abstract class ZombieDropMixin {

    @Inject(method = "dropLoot", at = @At("TAIL"))
    private void addExtraDrops(DamageSource source, boolean causedByPlayer, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (!(self instanceof ZombieEntity zombie)) return;
        if (self.getWorld().isClient) return;

        World world = self.getWorld();
        double progress = StageSystem.getStageProgress(world);
        var random = world.getRandom();

        boolean isGiant = zombie instanceof GiantZombieEntity;

        if (isGiant) {
            // ===== 巨型僵尸掉落 =====
            // 腐肉 3-8
            dropItem(zombie, Items.ROTTEN_FLESH, 3 + random.nextInt(6));
            // 骨头 3-6
            dropItem(zombie, Items.BONE, 3 + random.nextInt(4));
            // 铁锭 2-5
            dropItem(zombie, Items.IRON_INGOT, 2 + random.nextInt(4));
            // 金锭 1-3
            dropItem(zombie, Items.GOLD_INGOT, 1 + random.nextInt(3));
            // 钻石 0-2 (随阶段增加概率)
            int diamondCount = random.nextDouble() < (0.3 + progress * 0.5) ? 1 + random.nextInt(2) : 0;
            if (diamondCount > 0) dropItem(zombie, Items.DIAMOND, diamondCount);
            // 绿宝石 0-3
            int emeraldCount = random.nextDouble() < (0.2 + progress * 0.4) ? 1 + random.nextInt(3) : 0;
            if (emeraldCount > 0) dropItem(zombie, Items.EMERALD, emeraldCount);
            // 附魔金苹果 0-1 (稀有)
            if (random.nextDouble() < 0.05 + progress * 0.1) {
                dropItem(zombie, Items.ENCHANTED_GOLDEN_APPLE, 1);
            }
            // 经验瓶 1-5
            dropItem(zombie, Items.EXPERIENCE_BOTTLE, 1 + random.nextInt(5));
        } else {
            // ===== 普通僵尸掉落 =====
            // 骨头 1-3
            dropItem(zombie, Items.BONE, 1 + random.nextInt(3));
            // 铁锭 0-2 (随阶段增加概率)
            int ironCount = random.nextDouble() < (0.15 + progress * 0.35) ? 1 + random.nextInt(2) : 0;
            if (ironCount > 0) dropItem(zombie, Items.IRON_INGOT, ironCount);
            // 火药 0-2
            int gunpowderCount = random.nextDouble() < (0.1 + progress * 0.3) ? 1 + random.nextInt(2) : 0;
            if (gunpowderCount > 0) dropItem(zombie, Items.GUNPOWDER, gunpowderCount);
            // 金粒 0-3
            int goldNuggetCount = random.nextDouble() < (0.1 + progress * 0.25) ? 1 + random.nextInt(3) : 0;
            if (goldNuggetCount > 0) dropItem(zombie, Items.GOLD_NUGGET, goldNuggetCount);
        }
    }

    private static void dropItem(ZombieEntity zombie, net.minecraft.item.ItemConvertible item, int count) {
        zombie.dropStack(new ItemStack(item, count));
    }
}