package com.zombieapocalypse.mixin;

import com.zombieapocalypse.config.ModConfig;
import com.zombieapocalypse.config.StageSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * HandledScreen Mixin - 仅为生存背包界面添加分类标签
 * 使用创造模式风格的标签按钮和背景
 */
@Mixin(HandledScreen.class)
public abstract class InventoryScreenMixin {

    @Shadow
    protected int x;
    @Shadow
    protected int y;

    @Unique
    private static final Identifier TAB_TEXTURE = new Identifier("textures/gui/container/creative_inventory/tabs.png");
    @Unique
    private static final Identifier CREATIVE_BG = new Identifier("textures/gui/container/creative_inventory.png");
    @Unique
    private static final int TAB_WIDTH = 28;
    @Unique
    private static final int TAB_HEIGHT = 28;
    @Unique
    private int currentTab = 1; // 0=天数, 1=背包

    @Unique
    private boolean isInventoryScreen() {
        return (Object) this instanceof InventoryScreen;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void adjustLayout(CallbackInfo ci) {
        if (!isInventoryScreen()) return;
        this.y += TAB_HEIGHT;
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void renderTabs(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!isInventoryScreen()) return;
        int tabY = this.y - TAB_HEIGHT;

        // 绘制 "天数" 标签 (创造模式风格)
        int tab0X = this.x + 8;
        drawCreativeTab(context, tab0X, tabY, currentTab == 0, "天数");

        // 绘制 "背包" 标签
        int tab1X = tab0X + TAB_WIDTH + 2;
        drawCreativeTab(context, tab1X, tabY, currentTab == 1, "背包");

        // 如果当前是"天数"标签，渲染天数信息面板
        if (currentTab == 0) {
            renderDayInfo(context);
        }
    }

    /**
     * 绘制创造模式风格的标签按钮
     */
    @Unique
    private void drawCreativeTab(DrawContext context, int tabX, int tabY, boolean selected, String label) {
        // 绘制标签背景 (使用创造模式标签纹理)
        int v = selected ? 32 : 0;
        context.drawTexture(TAB_TEXTURE, tabX, tabY, 0, v, TAB_WIDTH, 32);

        // 绘制标签文字
        int textColor = selected ? 0xFFFFFF : 0xA0A0A0;
        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer,
                Text.literal(label), tabX + TAB_WIDTH / 2, tabY + 8, textColor);
    }

    /**
     * 渲染天数信息面板 (美化版)
     */
    @Unique
    private void renderDayInfo(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        World world = client.player.getWorld();
        if (world == null) return;

        int currentDay = StageSystem.getCurrentStage(world);
        double progress = StageSystem.getStageProgress(world);
        int centerX = this.x + 88;
        var renderer = client.textRenderer;

        // 绘制创造模式背景
        context.drawTexture(CREATIVE_BG, this.x, this.y, 0, 0, 176, 83);
        context.drawTexture(CREATIVE_BG, this.x, this.y + 83, 0, 83, 176, 83);

        // ===== 标题区域 =====
        int titleY = this.y + 8;
        // 标题背景条
        int barX = this.x + 10;
        int barW = 156;
        context.fill(barX, titleY, barX + barW, titleY + 16, 0x44FF2222);
        context.drawCenteredTextWithShadow(renderer,
                Text.literal("§c§l☠ 惊变100天 ☠"), centerX, titleY + 3, 0xFF6666);

        // ===== 天数卡片 =====
        int cardY = titleY + 22;
        int cardH = 36;
        drawCard(context, barX, cardY, barW, cardH);
        context.drawTextWithShadow(renderer,
                Text.literal("§f第 " + currentDay + " / " + ModConfig.TOTAL_DAYS + " 天"),
                barX + 10, cardY + 10, 0xFFEE44);

        // 进度条
        int progY = cardY + 22;
        int progH = 8;
        context.fill(barX + 10, progY, barX + barW - 10, progY + progH, 0xFF222222);
        int filledW = (int) ((barW - 20) * progress);
        int barColor = progress < 0.3 ? 0xFF55CC55 : progress < 0.6 ? 0xFFCCCC44 :
                progress < 0.8 ? 0xFFFF9944 : 0xFFFF4444;
        context.fill(barX + 10, progY, barX + 10 + filledW, progY + progH, barColor);
        context.drawCenteredTextWithShadow(renderer,
                Text.literal(String.format("%.0f%%", progress * 100)),
                centerX, progY - 1, 0xFFFFFF);

        // ===== 僵尸属性卡片 =====
        cardY = cardY + cardH + 6;
        cardH = 56;
        drawCard(context, barX, cardY, barW, cardH);

        double zombieHealth = StageSystem.getZombieHealth(world);
        double zombieAttack = StageSystem.getZombieAttack(world);
        double giantHealth = StageSystem.getGiantZombieHealth(world);
        double giantAttack = StageSystem.getGiantZombieAttack(world);
        double giantChance = StageSystem.getGiantZombieChance(world);

        context.drawTextWithShadow(renderer,
                Text.literal("§c§l僵尸属性"), barX + 10, cardY + 6, 0xFF4444);
        int rowY = cardY + 20;
        context.drawTextWithShadow(renderer,
                Text.literal(String.format("§7血量: §c%.0f    §7攻击: §c%.1f    §7护甲: §c%.0f",
                        zombieHealth, zombieAttack, ModConfig.ZOMBIE_BASE_ARMOR +
                                (ModConfig.ZOMBIE_MAX_ARMOR - ModConfig.ZOMBIE_BASE_ARMOR) * progress)),
                barX + 10, rowY, 0xAAAAAA);
        rowY += 13;
        context.drawTextWithShadow(renderer,
                Text.literal(String.format("§5巨型: §5%.0f血  §5%.1f攻  §d%.1f%%概率",
                        giantHealth, giantAttack, giantChance * 100)),
                barX + 10, rowY, 0xAAAAAA);
        rowY += 13;
        context.drawTextWithShadow(renderer,
                Text.literal(String.format("§e速度: §e%.2f",
                        StageSystem.getZombieSpeed(world))),
                barX + 10, rowY, 0xAAAAAA);

        // ===== 阶段提示卡片 =====
        cardY = cardY + cardH + 6;
        cardH = 28;
        drawCard(context, barX, cardY, barW, cardH);

        String tip;
        String tipColor;
        if (currentDay <= 10) {
            tip = "初期阶段 - 僵尸较弱，收集资源";
            tipColor = "§a";
        } else if (currentDay <= 30) {
            tip = "发展阶段 - 僵尸变强，建造防御";
            tipColor = "§e";
        } else if (currentDay <= 50) {
            tip = "中期阶段 - 僵尸很强，注意防守";
            tipColor = "§6";
        } else if (currentDay <= 70) {
            tip = "后期阶段 - 巨型僵尸频繁出现";
            tipColor = "§c";
        } else {
            tip = "最终阶段 - 生存下去！";
            tipColor = "§4";
        }
        context.drawCenteredTextWithShadow(renderer,
                Text.literal(tipColor + "§l" + tip), centerX, cardY + 9, 0xFFFFFF);
    }

    /**
     * 绘制卡片背景
     */
    @Unique
    private void drawCard(DrawContext context, int x, int y, int w, int h) {
        context.fill(x, y, x + w, y + h, 0x88222222);
        context.fill(x, y, x + w, y + 1, 0xFF555555);
        context.fill(x, y + h - 1, x + w, y + h, 0xFF555555);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void handleTabClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!isInventoryScreen()) return;
        int tabY = this.y - TAB_HEIGHT;

        if (mouseY >= tabY && mouseY <= tabY + 32) {
            int tab0X = this.x + 8;
            if (mouseX >= tab0X && mouseX <= tab0X + TAB_WIDTH) {
                currentTab = 0;
                cir.setReturnValue(true);
                return;
            }
            int tab1X = tab0X + TAB_WIDTH + 2;
            if (mouseX >= tab1X && mouseX <= tab1X + TAB_WIDTH) {
                currentTab = 1;
                cir.setReturnValue(true);
                return;
            }
        }

        if (currentTab == 0) {
            if (mouseX >= this.x && mouseX <= this.x + 176 &&
                    mouseY >= this.y && mouseY <= this.y + 166) {
                cir.setReturnValue(true);
            }
        }
    }
}