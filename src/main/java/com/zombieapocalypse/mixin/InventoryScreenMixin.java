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
     * 渲染天数信息面板 (创造模式背包背景风格)
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
        int textY = this.y + 15;
        var renderer = client.textRenderer;

        // 绘制创造模式风格的背景
        // 背景上半部分
        context.drawTexture(CREATIVE_BG, this.x, this.y, 0, 0, 176, 83);
        // 背景下半部分
        context.drawTexture(CREATIVE_BG, this.x, this.y + 83, 0, 83, 176, 83);

        // 标题
        context.drawCenteredTextWithShadow(renderer,
                Text.literal("§6§l惊变100天"), centerX, textY, 0xFFFFFF);
        textY += 22;

        // 当前天数
        context.drawCenteredTextWithShadow(renderer,
                Text.literal("§e当前: 第 " + currentDay + " / " + ModConfig.TOTAL_DAYS + " 天"),
                centerX, textY, 0xFFFFFF);
        textY += 16;

        // 进度条
        int barWidth = 140;
        int barX = centerX - barWidth / 2;
        int barY = textY;
        context.fill(barX, barY, barX + barWidth, barY + 12, 0xFF333333);
        int filledWidth = (int) (barWidth * progress);
        int barColor = progress < 0.3 ? 0xFF55FF55 : progress < 0.6 ? 0xFFFFFF55 :
                progress < 0.8 ? 0xFFFFAA00 : 0xFFFF5555;
        context.fill(barX, barY, barX + filledWidth, barY + 12, barColor);
        context.drawCenteredTextWithShadow(renderer,
                Text.literal(String.format("%.1f%%", progress * 100)),
                centerX, barY + 2, 0xFFFFFF);
        textY += 20;

        // 分隔线
        context.fill(barX, textY, barX + barWidth, textY + 1, 0xFF555555);
        textY += 10;

        // 僵尸属性
        double zombieHealth = StageSystem.getZombieHealth(world);
        double zombieAttack = StageSystem.getZombieAttack(world);
        double giantHealth = StageSystem.getGiantZombieHealth(world);
        double giantAttack = StageSystem.getGiantZombieAttack(world);
        double giantChance = StageSystem.getGiantZombieChance(world);

        context.drawTextWithShadow(renderer,
                Text.literal("§c僵尸属性:"), this.x + 22, textY, 0xFFFFFF);
        textY += 13;
        context.drawTextWithShadow(renderer,
                Text.literal(String.format("§7血量: §c%.0f  §7攻击: §c%.1f",
                        zombieHealth, zombieAttack)),
                this.x + 22, textY, 0xAAAAAA);
        textY += 12;
        context.drawTextWithShadow(renderer,
                Text.literal(String.format("§7巨型僵尸: §5%.0f血 §7攻击§5%.1f",
                        giantHealth, giantAttack)),
                this.x + 22, textY, 0xAAAAAA);
        textY += 12;
        context.drawTextWithShadow(renderer,
                Text.literal(String.format("§7巨型概率: §d%.1f%%", giantChance * 100)),
                this.x + 22, textY, 0xAAAAAA);
        textY += 18;

        // 阶段提示
        context.fill(barX, textY, barX + barWidth, textY + 1, 0xFF555555);
        textY += 10;

        String tip;
        if (currentDay <= 10) {
            tip = "§a初期阶段 - 僵尸较弱，尽快收集资源";
        } else if (currentDay <= 30) {
            tip = "§e发展阶段 - 僵尸开始变强，建造防御";
        } else if (currentDay <= 50) {
            tip = "§6中期阶段 - 僵尸很强，注意防守";
        } else if (currentDay <= 70) {
            tip = "§c后期阶段 - 巨型僵尸频繁出现";
        } else {
            tip = "§4最终阶段 - 生存下去！";
        }
        context.drawCenteredTextWithShadow(renderer, Text.literal(tip), centerX, textY, 0xFFFFFF);
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