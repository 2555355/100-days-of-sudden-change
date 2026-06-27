package com.zombieapocalypse.mixin;

import com.zombieapocalypse.config.ModConfig;
import com.zombieapocalypse.config.StageSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
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
 * 末日风格UI - v2.0.0 美化版
 * 深色背景 + 血红渐变 + 智能度等级 + 血月指示器
 */
@Mixin(HandledScreen.class)
public abstract class InventoryScreenMixin {

    @Shadow protected int x;
    @Shadow protected int y;

    // 配色方案 - 末日暗红主题
    @Unique private static final int COLOR_BG_DARK    = 0xF0100A0A;
    @Unique private static final int COLOR_BG_PANEL   = 0xFF0D0808;
    @Unique private static final int COLOR_BG_CARD    = 0xCC1A1212;
    @Unique private static final int COLOR_BORDER_RED = 0xFFFF2A2A;
    @Unique private static final int COLOR_BORDER_DIM = 0x66883333;
    @Unique private static final int COLOR_ACCENT     = 0xFFFF4444;
    @Unique private static final int COLOR_TEXT       = 0xFFE0E0E0;
    @Unique private static final int COLOR_TEXT_DIM   = 0xFF999999;
    @Unique private static final int COLOR_TEXT_HI    = 0xFFFFAA00;
    @Unique private static final int COLOR_DANGER     = 0xFFFF3333;
    @Unique private static final int COLOR_BLOOD_MOON = 0xFFFF0000;

    @Unique private static final int TAB_WIDTH = 62;
    @Unique private static final int TAB_HEIGHT = 26;
    @Unique private static final int TAB_GAP = 3;
    @Unique private static final int PANEL_W = 176;
    @Unique private static final int PANEL_H = 166;

    @Unique private int currentTab = 1;
    @Unique private int hoveredTab = -1;

    @Unique
    private boolean isInventoryScreen() {
        return (Object) this instanceof InventoryScreen;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void adjustLayout(CallbackInfo ci) {
        if (!isInventoryScreen()) return;
        this.y += TAB_HEIGHT + 3;
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRenderHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!isInventoryScreen()) return;
        if (currentTab == 0) {
            MinecraftClient client = MinecraftClient.getInstance();
            int sw = client.getWindow().getScaledWidth();
            int sh = client.getWindow().getScaledHeight();
            // 暗色渐变背景
            context.fill(0, 0, sw, sh, COLOR_BG_DARK);
            context.fill(0, 0, sw, sh / 4, 0x44000000);
            renderDayPanel(context, mouseX, mouseY);
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderTail(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!isInventoryScreen()) return;
        renderTabButtons(context, mouseX, mouseY);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void handleTabClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!isInventoryScreen()) return;
        int tabY = this.y - TAB_HEIGHT - 3;
        if (mouseY >= tabY && mouseY <= tabY + TAB_HEIGHT) {
            int tab0X = this.x + 4;
            if (mouseX >= tab0X && mouseX <= tab0X + TAB_WIDTH) {
                currentTab = 0;
                cir.setReturnValue(true);
                return;
            }
            int tab1X = tab0X + TAB_WIDTH + TAB_GAP;
            if (mouseX >= tab1X && mouseX <= tab1X + TAB_WIDTH) {
                currentTab = 1;
                cir.setReturnValue(true);
                return;
            }
        }
        if (currentTab == 0) {
            if (mouseX >= this.x && mouseX <= this.x + PANEL_W &&
                    mouseY >= this.y && mouseY <= this.y + PANEL_H) {
                cir.setReturnValue(true);
            }
        }
    }

    // ===================== 标签按钮 =====================

    @Unique
    private void renderTabButtons(DrawContext context, int mouseX, int mouseY) {
        int tabY = this.y - TAB_HEIGHT - 3;
        hoveredTab = -1;
        if (mouseY >= tabY && mouseY <= tabY + TAB_HEIGHT) {
            int tab0X = this.x + 4;
            if (mouseX >= tab0X && mouseX <= tab0X + TAB_WIDTH) hoveredTab = 0;
            int tab1X = tab0X + TAB_WIDTH + TAB_GAP;
            if (mouseX >= tab1X && mouseX <= tab1X + TAB_WIDTH) hoveredTab = 1;
        }

        int tab0X = this.x + 4;
        drawTab(context, tab0X, tabY, "☠", "末日情报", currentTab == 0, hoveredTab == 0);
        int tab1X = tab0X + TAB_WIDTH + TAB_GAP;
        drawTab(context, tab1X, tabY, "⛏", "生存背包", currentTab == 1, hoveredTab == 1);
    }

    @Unique
    private void drawTab(DrawContext ctx, int x, int y, String icon, String label,
                         boolean selected, boolean hovered) {
        int w = TAB_WIDTH;
        int h = TAB_HEIGHT;
        int bg, border, textCol, iconCol;

        if (selected) {
            bg = 0xEE2A0808;
            border = COLOR_BORDER_RED;
            textCol = 0xFFFF6666;
            iconCol = COLOR_ACCENT;
        } else if (hovered) {
            bg = 0xBB1A0A0A;
            border = 0xAAFF4444;
            textCol = 0xFFCCCCCC;
            iconCol = 0xFFCC8888;
        } else {
            bg = 0x88101010;
            border = COLOR_BORDER_DIM;
            textCol = COLOR_TEXT_DIM;
            iconCol = 0xFF886666;
        }

        // 背景
        ctx.fill(x, y, x + w, y + h, bg);
        // 左右边框
        ctx.fill(x, y, x + 1, y + h, border);
        ctx.fill(x + w - 1, y, x + w, y + h, border);
        // 上边框
        ctx.fill(x, y, x + w, y + 1, border);
        // 选中态：底部高亮条
        if (selected) {
            ctx.fill(x + 1, y + h - 2, x + w - 1, y + h, COLOR_ACCENT);
            // 顶部微光
            ctx.fill(x + 2, y + 1, x + w - 2, y + 2, 0x44FF6666);
        } else {
            ctx.fill(x, y + h - 1, x + w, y + h, border);
        }

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        // 图标
        int iconW = tr.getWidth(icon);
        ctx.drawTextWithShadow(tr, Text.literal(icon), x + 6, y + (h - 8) / 2, iconCol);
        // 标签文字
        int labelX = x + 6 + iconW + 4;
        ctx.drawTextWithShadow(tr, Text.literal(label), labelX, y + (h - 8) / 2, textCol);
    }

    // ===================== 天数面板 =====================

    @Unique
    private void renderDayPanel(DrawContext ctx, int mouseX, int mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        World world = client.player.getWorld();
        if (world == null) return;

        TextRenderer tr = client.textRenderer;
        int px = this.x;
        int py = this.y;
        int pw = PANEL_W;
        int ph = PANEL_H;
        int centerX = px + pw / 2;

        int currentDay = StageSystem.getCurrentStage(world);
        double progress = StageSystem.getStageProgress(world);
        boolean isBloodMoon = StageSystem.isBloodMoon(world);
        int intelLevel = StageSystem.getIntelligenceLevel(world);

        // ===== 面板背景 =====
        ctx.fill(px, py, px + pw, py + ph, COLOR_BG_PANEL);
        // 渐变顶部条
        for (int i = 0; i < 4; i++) {
            int alpha = 0xFF - i * 0x30;
            ctx.fill(px, py + i, px + pw, py + i + 1, (alpha << 24) | 0x2A0808);
        }
        // 边框
        drawBorder(ctx, px, py, pw, ph, COLOR_BORDER_RED, 1);
        // 四角装饰
        drawCornerDecor(ctx, px, py, pw, ph);

        // ===== 标题横幅 =====
        int titleY = py + 9;
        int titleH = 18;
        ctx.fill(px + 6, titleY, px + pw - 6, titleY + titleH, 0x993A0505);
        ctx.fill(px + 6, titleY, px + pw - 6, titleY + 1, COLOR_ACCENT);
        ctx.fill(px + 6, titleY + titleH - 1, px + pw - 6, titleY + titleH, COLOR_ACCENT);
        // 血月时标题变鲜红
        int titleColor = isBloodMoon ? COLOR_BLOOD_MOON : 0xFFFF5555;
        ctx.drawCenteredTextWithShadow(tr, Text.literal("☠ 惊变100天 ☠"), centerX, titleY + 5, titleColor);

        // 血月警告条
        if (isBloodMoon) {
            int warnY = titleY + titleH + 2;
            ctx.fill(px + 6, warnY, px + pw - 6, warnY + 12, 0xCC660000);
            ctx.fill(px + 6, warnY, px + pw - 6, warnY + 1, COLOR_BLOOD_MOON);
            ctx.fill(px + 6, warnY + 11, px + pw - 6, warnY + 12, COLOR_BLOOD_MOON);
            ctx.drawCenteredTextWithShadow(tr, Text.literal("● 血月进行中 ●"), centerX, warnY + 2, COLOR_BLOOD_MOON);
        }

        // ===== 天数卡片 =====
        int cardY = isBloodMoon ? titleY + titleH + 18 : titleY + titleH + 4;
        int cardW = pw - 12;
        int cardX = px + 6;
        int cardH = 44;

        drawCard(ctx, cardX, cardY, cardW, cardH);
        // 天数大字
        String dayStr = String.valueOf(currentDay);
        ctx.drawTextWithShadow(tr, Text.literal("§f生存第"), cardX + 8, cardY + 7, COLOR_TEXT_DIM);
        int dayNumW = tr.getWidth(dayStr);
        ctx.drawTextWithShadow(tr, Text.literal("§e§l" + dayStr), cardX + 34, cardY + 5, COLOR_TEXT_HI);
        ctx.drawTextWithShadow(tr, Text.literal("§f天 §7/ " + ModConfig.TOTAL_DAYS), cardX + 34 + dayNumW + 2, cardY + 7, COLOR_TEXT_DIM);

        // 进度条
        int barY = cardY + 22;
        int barH = 12;
        int barX = cardX + 8;
        int barW = cardW - 16;
        ctx.fill(barX, barY, barX + barW, barY + barH, 0xFF050505);
        ctx.fill(barX, barY, barX + barW, barY + 1, 0x44FF3333);
        ctx.fill(barX, barY + barH - 1, barX + barW, barY + barH, 0x44FF3333);
        int filledW = (int) (barW * progress);
        // 渐变进度条
        int barColor = getProgressColor(progress);
        if (filledW > 0) {
            ctx.fill(barX + 1, barY + 1, barX + filledW, barY + barH - 1, barColor);
            // 高光
            ctx.fill(barX + 1, barY + 1, barX + filledW, barY + 2, 0x66FFFFFF);
        }
        ctx.drawCenteredTextWithShadow(tr, Text.literal(String.format("%.0f%%", progress * 100)),
                barX + barW / 2, barY + 2, COLOR_TEXT);

        // ===== 智能度卡片 =====
        cardY += cardH + 4;
        int intelCardH = 22;
        drawCard(ctx, cardX, cardY, cardW, intelCardH);
        ctx.drawTextWithShadow(tr, Text.literal("§7僵尸智能度"), cardX + 8, cardY + 7, COLOR_TEXT_DIM);
        String[] intelNames = {"迟钝", "普通", "机敏", "狡猾", "凶残", "嗜血"};
        String[] intelColors = {"§a", "§e", "§6", "§c", "§4", "§4§l"};
        String intelText = intelColors[intelLevel] + "Lv" + intelLevel + " " + intelNames[intelLevel];
        ctx.drawTextWithShadow(tr, Text.literal(intelText), cardX + cardW - 8 - tr.getWidth(intelText.replace("§", "").replace("l", "")),
                cardY + 7, COLOR_TEXT);

        // ===== 僵尸属性卡片 =====
        cardY += intelCardH + 4;
        int attrCardH = 48;
        drawCard(ctx, cardX, cardY, cardW, attrCardH);

        double zombieHealth = StageSystem.getZombieHealth(world);
        double zombieAttack = StageSystem.getZombieAttack(world);
        double zombieArmor = StageSystem.getZombieArmor(world);
        double zombieSpeed = StageSystem.getZombieSpeed(world);
        double giantHealth = StageSystem.getGiantZombieHealth(world);
        double giantAttack = StageSystem.getGiantZombieAttack(world);
        double giantChance = StageSystem.getGiantZombieChance(world);

        ctx.drawTextWithShadow(tr, Text.literal("§c⚔ 僵尸属性"), cardX + 8, cardY + 5, COLOR_ACCENT);

        int rowY = cardY + 18;
        int colW = (cardW - 16) / 2;
        drawAttrRow(ctx, tr, cardX + 8, rowY, colW, "血量", String.format("%.0f", zombieHealth), COLOR_DANGER);
        drawAttrRow(ctx, tr, cardX + 8 + colW, rowY, colW, "攻击", String.format("%.1f", zombieAttack), COLOR_TEXT_HI);
        rowY += 12;
        drawAttrRow(ctx, tr, cardX + 8, rowY, colW, "护甲", String.format("%.0f", zombieArmor), COLOR_TEXT);
        drawAttrRow(ctx, tr, cardX + 8 + colW, rowY, colW, "速度", String.format("%.2f", zombieSpeed), COLOR_TEXT);
        rowY += 12;
        drawAttrRow(ctx, tr, cardX + 8, rowY, colW, "巨型血量", String.format("%.0f", giantHealth), 0xFFFF44FF);
        String giantStr = String.format("%.1f%%", giantChance * 100);
        drawAttrRow(ctx, tr, cardX + 8 + colW, rowY, colW, "巨型概率", giantStr, 0xFFFF44FF);

        // ===== 底部提示 =====
        cardY += attrCardH + 4;
        int tipH = ph - (cardY - py) - 4;
        if (tipH > 8) {
            drawCard(ctx, cardX, cardY, cardW, tipH);
            String tip = getStageTip(currentDay);
            ctx.drawCenteredTextWithShadow(tr, Text.literal(tip), centerX, cardY + (tipH - 8) / 2, COLOR_TEXT);
        }
    }

    // ===================== 辅助绘制方法 =====================

    @Unique
    private int getProgressColor(double progress) {
        if (progress < 0.25) return 0xFF33AA33;
        if (progress < 0.50) return 0xFFCCAA22;
        if (progress < 0.75) return 0xFFCC6622;
        return 0xFFFF2222;
    }

    @Unique
    private String getStageTip(int day) {
        if (day <= 10) return "§a初期 - 收集资源，建立基地";
        if (day <= 30) return "§e发展 - 加固防御，准备迎战";
        if (day <= 50) return "§6中期 - 僵尸越来越强！";
        if (day <= 70) return "§c后期 - 巨型僵尸频繁出没！";
        return "§4最终 - 末日降临，拼死生存！";
    }

    @Unique
    private void drawAttrRow(DrawContext ctx, TextRenderer tr, int x, int y, int w,
                             String label, String value, int valueColor) {
        ctx.drawTextWithShadow(tr, Text.literal("§7" + label), x, y, COLOR_TEXT_DIM);
        String valStr = "§f" + value;
        int valW = tr.getWidth(valStr.replace("§f", ""));
        ctx.drawTextWithShadow(tr, Text.literal(valStr), x + w - valW - 2, y, valueColor);
    }

    @Unique
    private void drawCard(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, COLOR_BG_CARD);
        // 顶部高光
        ctx.fill(x, y, x + w, y + 1, 0x33FF4444);
        // 底部暗线
        ctx.fill(x, y + h - 1, x + w, y + h, 0x44110000);
        // 左边框微光
        ctx.fill(x, y, x + 1, y + h, 0x22FF3333);
    }

    @Unique
    private void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color, int thickness) {
        ctx.fill(x, y, x + w, y + thickness, color);
        ctx.fill(x, y + h - thickness, x + w, y + h, color);
        ctx.fill(x, y, x + thickness, y + h, color);
        ctx.fill(x + w - thickness, y, x + w, y + h, color);
    }

    @Unique
    private void drawCornerDecor(DrawContext ctx, int x, int y, int w, int h) {
        int s = 6;
        // 左上
        ctx.fill(x, y, x + s, y + 1, COLOR_ACCENT);
        ctx.fill(x, y, x + 1, y + s, COLOR_ACCENT);
        // 右上
        ctx.fill(x + w - s, y, x + w, y + 1, COLOR_ACCENT);
        ctx.fill(x + w - 1, y, x + w, y + s, COLOR_ACCENT);
        // 左下
        ctx.fill(x, y + h - 1, x + s, y + h, COLOR_ACCENT);
        ctx.fill(x, y + h - s, x + 1, y + h, COLOR_ACCENT);
        // 右下
        ctx.fill(x + w - s, y + h - 1, x + w, y + h, COLOR_ACCENT);
        ctx.fill(x + w - 1, y + h - s, x + w, y + h, COLOR_ACCENT);
    }
}
