package com.zombieapocalypse.mixin;

import com.zombieapocalypse.config.ModConfig;
import com.zombieapocalypse.config.StageSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 生存背包界面 Mixin
 * 在背包上方添加两个分类标签:
 * - 天数: 显示当前天数和阶段信息
 * - 背包: 显示正常生存背包
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {

    // 使用 intermediary 名称避免与 InventoryScreen 的 mouseX/mouseY 冲突
    @Shadow(remap = false)
    private int field_2776; // HandledScreen.x
    @Shadow(remap = false)
    private int field_2800; // HandledScreen.y

    @Unique
    private static final int TAB_HEIGHT = 28;
    @Unique
    private static final int TAB_WIDTH = 60;
    @Unique
    private int currentTab = 1; // 0=天数, 1=背包 (默认背包)

    /**
     * 移动背包Y坐标，为标签栏腾出空间
     */
    @Inject(method = "init", at = @At("TAIL"))
    private void adjustLayout(CallbackInfo ci) {
        this.field_2800 += TAB_HEIGHT;
    }

    /**
     * 在背包上方渲染标签栏
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void renderTabs(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        int x = this.field_2776;
        int y = this.field_2800;
        int tabY = y - TAB_HEIGHT;

        // 绘制标签栏背景
        context.fill(x, tabY, x + 176, tabY + TAB_HEIGHT, 0xC0101010);
        context.fill(x, tabY + TAB_HEIGHT - 1, x + 176, tabY + TAB_HEIGHT, 0xFF555555);

        // 绘制 "天数" 标签
        int tab0X = x + 8;
        int tab0Y = tabY + 3;
        int tab0Color = currentTab == 0 ? 0xFF3A3A3A : 0xFF252525;
        int tab0TextColor = currentTab == 0 ? 0xFFFFFFAA : 0xFFAAAAAA;
        context.fill(tab0X, tab0Y, tab0X + TAB_WIDTH, tab0Y + 23, tab0Color);
        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer,
                Text.literal("天数"), tab0X + TAB_WIDTH / 2, tab0Y + 7, tab0TextColor);

        // 绘制 "背包" 标签
        int tab1X = tab0X + TAB_WIDTH + 4;
        int tab1Y = tab0Y;
        int tab1Color = currentTab == 1 ? 0xFF3A3A3A : 0xFF252525;
        int tab1TextColor = currentTab == 1 ? 0xFFFFFFAA : 0xFFAAAAAA;
        context.fill(tab1X, tab1Y, tab1X + TAB_WIDTH, tab1Y + 23, tab1Color);
        context.drawCenteredTextWithShadow(MinecraftClient.getInstance().textRenderer,
                Text.literal("背包"), tab1X + TAB_WIDTH / 2, tab1Y + 7, tab1TextColor);

        // 如果当前是"天数"标签，渲染天数信息面板
        if (currentTab == 0) {
            renderDayInfo(context, x, y);
        }
    }

    /**
     * 渲染天数信息面板
     */
    @Unique
    private void renderDayInfo(DrawContext context, int x, int y) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        World world = client.player.getWorld();
        if (world == null) return;

        int currentDay = StageSystem.getCurrentStage(world);
        double progress = StageSystem.getStageProgress(world);

        int centerX = x + 88;
        int textY = y + 15;

        var renderer = client.textRenderer;

        // 半透明遮罩
        context.fill(x, y, x + 176, y + 166, 0xDD000000);

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
                Text.literal("§c僵尸属性:"), x + 22, textY, 0xFFFFFF);
        textY += 13;
        context.drawTextWithShadow(renderer,
                Text.literal(String.format("§7血量: §c%.0f  §7攻击: §c%.1f",
                        zombieHealth, zombieAttack)),
                x + 22, textY, 0xAAAAAA);
        textY += 12;
        context.drawTextWithShadow(renderer,
                Text.literal(String.format("§7巨型僵尸: §5%.0f血 §7攻击§5%.1f",
                        giantHealth, giantAttack)),
                x + 22, textY, 0xAAAAAA);
        textY += 12;
        context.drawTextWithShadow(renderer,
                Text.literal(String.format("§7巨型概率: §d%.1f%%", giantChance * 100)),
                x + 22, textY, 0xAAAAAA);
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

    /**
     * 处理标签点击
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void handleTabClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        int x = this.field_2776;
        int y = this.field_2800;
        int tabY = y - TAB_HEIGHT;

        // 检查是否点击了标签区域
        if (mouseY >= tabY && mouseY <= tabY + TAB_HEIGHT) {
            int tab0X = x + 8;
            // "天数" 标签
            if (mouseX >= tab0X && mouseX <= tab0X + TAB_WIDTH) {
                currentTab = 0;
                cir.setReturnValue(true);
                return;
            }
            // "背包" 标签
            int tab1X = tab0X + TAB_WIDTH + 4;
            if (mouseX >= tab1X && mouseX <= tab1X + TAB_WIDTH) {
                currentTab = 1;
                cir.setReturnValue(true);
                return;
            }
        }

        // 在"天数"标签时，阻止背包区域的点击
        if (currentTab == 0) {
            if (mouseX >= x && mouseX <= x + 176 &&
                    mouseY >= y && mouseY <= y + 166) {
                cir.setReturnValue(true);
            }
        }
    }
}