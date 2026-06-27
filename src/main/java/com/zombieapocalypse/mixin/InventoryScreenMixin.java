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
        int titleY = py + 6;
        int titleH = 16;
        ctx.fill(px + 6, titleY, px + pw - 6, titleY + titleH, 0x993A0505);
        ctx.fill(px + 6, titleY, px + pw - 6, titleY + 1, COLOR_ACCENT);
        ctx.fill(px + 6, titleY + titleH - 1, px + pw - 6, titleY + titleH, COLOR_ACCENT);
        int titleColor = isBloodMoon ? COLOR_BLOOD_MOON : 0xFFFF5555;
        ctx.drawCenteredTextWithShadow(tr, Text.literal("☠ 惊变100天 ☠"), centerX, titleY + 4, titleColor);

        // 血月警告条
        int bloodMoonOffset = 0;
        if (isBloodMoon) {
            int warnY = titleY + titleH + 1;
            bloodMoonOffset = 13;
            ctx.fill(px + 6, warnY, px + pw - 6, warnY + 11, 0xCC660000);
            ctx.fill(px + 6, warnY, px + pw - 6, warnY + 1, COLOR_BLOOD_MOON);
            ctx.fill(px + 6, warnY + 10, px + pw - 6, warnY + 11, COLOR_BLOOD_MOON);
            ctx.drawCenteredTextWithShadow(tr, Text.literal("● 血月进行中 ●"), centerX, warnY + 1, COLOR_BLOOD_MOON);
        }

        int cardW = pw - 12;
        int cardX = px + 6;
        int cardY = titleY + titleH + bloodMoonOffset + 2;

        // ===== 天数 + 血月倒计时 (紧凑) =====
        int dayCardH = 36;
        drawCard(ctx, cardX, cardY, cardW, dayCardH);
        String dayStr = String.valueOf(currentDay);
        ctx.drawTextWithShadow(tr, Text.literal("§f第"), cardX + 6, cardY + 5, COLOR_TEXT_DIM);
        ctx.drawTextWithShadow(tr, Text.literal("§e§l" + dayStr), cardX + 18, cardY + 4, COLOR_TEXT_HI);
        ctx.drawTextWithShadow(tr, Text.literal("§f天 §7/" + ModConfig.TOTAL_DAYS), cardX + 18 + tr.getWidth(dayStr) + 2, cardY + 5, COLOR_TEXT_DIM);
        // 血月倒计时
        int currentDayRaw = StageSystem.getCurrentDay(world);
        int nextBloodMoon = ((currentDayRaw / ModConfig.BLOOD_MOON_INTERVAL) + 1) * ModConfig.BLOOD_MOON_INTERVAL;
        int daysToBloodMoon = nextBloodMoon - currentDayRaw;
        String bmText = isBloodMoon ? "§4● 血月今日" : "§7下次血月: §c" + daysToBloodMoon + "天后";
        ctx.drawTextWithShadow(tr, Text.literal(bmText), cardX + 6, cardY + 18, COLOR_TEXT_DIM);

        // 进度条
        int barY = cardY + 28;
        int barH = 6;
        int barX = cardX + 6;
        int barW = cardW - 12;
        ctx.fill(barX, barY, barX + barW, barY + barH, 0xFF050505);
        int filledW = (int) (barW * progress);
        int barColor = getProgressColor(progress);
        if (filledW > 0) {
            ctx.fill(barX, barY, barX + filledW, barY + barH, barColor);
            ctx.fill(barX, barY, barX + filledW, barY + 1, 0x66FFFFFF);
        }

        // ===== 智能度卡片 =====
        cardY += dayCardH + 2;
        int intelCardH = 20;
        drawCard(ctx, cardX, cardY, cardW, intelCardH);
        ctx.drawTextWithShadow(tr, Text.literal("§7智能度"), cardX + 6, cardY + 6, COLOR_TEXT_DIM);
        String[] intelNames = {"迟钝", "普通", "机敏", "狡猾", "凶残", "嗜血"};
        String[] intelColors = {"§a", "§e", "§6", "§c", "§4", "§4§l"};
        String intelText = intelColors[intelLevel] + "Lv" + intelLevel + " " + intelNames[intelLevel];
        int intelTextW = tr.getWidth(intelText.replaceAll("§[0-9a-fklmnor]", ""));
        ctx.drawTextWithShadow(tr, Text.literal(intelText), cardX + cardW - 6 - intelTextW, cardY + 6, COLOR_TEXT);

        // ===== 僵尸属性卡片 =====
        cardY += intelCardH + 2;
        int attrCardH = 40;
        drawCard(ctx, cardX, cardY, cardW, attrCardH);

        double zombieHealth = StageSystem.getZombieHealth(world);
        double zombieAttack = StageSystem.getZombieAttack(world);
        double zombieArmor = StageSystem.getZombieArmor(world);
        double zombieSpeed = StageSystem.getZombieSpeed(world);
        double giantHealth = StageSystem.getGiantZombieHealth(world);
        double giantAttack = StageSystem.getGiantZombieAttack(world);
        double giantChance = StageSystem.getGiantZombieChance(world);

        ctx.drawTextWithShadow(tr, Text.literal("§c⚔ 僵尸属性"), cardX + 6, cardY + 4, COLOR_ACCENT);

        int rowY = cardY + 15;
        int colW = (cardW - 12) / 2;
        drawAttrRow(ctx, tr, cardX + 6, rowY, colW, "血量", String.format("%.0f", zombieHealth), COLOR_DANGER);
        drawAttrRow(ctx, tr, cardX + 6 + colW, rowY, colW, "攻击", String.format("%.1f", zombieAttack), COLOR_TEXT_HI);
        rowY += 11;
        drawAttrRow(ctx, tr, cardX + 6, rowY, colW, "护甲", String.format("%.0f", zombieArmor), COLOR_TEXT);
        drawAttrRow(ctx, tr, cardX + 6 + colW, rowY, colW, "速度", String.format("%.2f", zombieSpeed), COLOR_TEXT);

        // ===== 巨型僵尸卡片 =====
        cardY += attrCardH + 2;
        int giantCardH = 26;
        drawCard(ctx, cardX, cardY, cardW, giantCardH);
        ctx.drawTextWithShadow(tr, Text.literal("§5▾ 巨型僵尸"), cardX + 6, cardY + 4, 0xFFFF66FF);
        rowY = cardY + 15;
        drawAttrRow(ctx, tr, cardX + 6, rowY, colW, "血量", String.format("%.0f", giantHealth), 0xFFFF88FF);
        drawAttrRow(ctx, tr, cardX + 6 + colW, rowY, colW, "攻击", String.format("%.1f", giantAttack), 0xFFFF88FF);
        rowY += 11;
        drawAttrRow(ctx, tr, cardX + 6, rowY, colW, "概率", String.format("%.1f%%", giantChance * 100), 0xFFFF88FF);
        int followRange = StageSystem.getFollowRange(world);
        drawAttrRow(ctx, tr, cardX + 6 + colW, rowY, colW, "追踪", followRange + "格", 0xFFFF88FF);

        // ===== AI能力卡片 =====
        cardY += giantCardH + 2;
        int aiCardH = ph - (cardY - py) - 4;
        if (aiCardH > 10) {
            drawCard(ctx, cardX, cardY, cardW, aiCardH);
            ctx.drawTextWithShadow(tr, Text.literal("§e⚙ AI能力"), cardX + 6, cardY + 4, COLOR_TEXT_HI);

            int aiY = cardY + 15;
            int breakInterval = StageSystem.getBreakInterval(world);
            int buildInterval = StageSystem.getBuildInterval(world);
            float hardnessLimit = StageSystem.getHardnessLimit(world);
            double reinforceChance = StageSystem.getReinforcementChance(world);
            int invSize = StageSystem.getBlockInventorySize(world);

            // 拆/搭方块速度
            String breakStr = String.format("§7拆: §c%.1fs  §7搭: §c%.1fs", breakInterval / 20.0, buildInterval / 20.0);
            ctx.drawTextWithShadow(tr, Text.literal(breakStr), cardX + 6, aiY, COLOR_TEXT);
            aiY += 11;
            // 硬度上限 + 库存
            String hardStr = String.format("§7硬度上限: §c%.0f  §7库存: §c%d", hardnessLimit, invSize);
            ctx.drawTextWithShadow(tr, Text.literal(hardStr), cardX + 6, aiY, COLOR_TEXT);
            aiY += 11;
            // 增援概率
            String reinStr = reinforceChance > 0
                    ? String.format("§7增援概率: §c%.0f%%", reinforceChance * 100)
                    : "§7增援: §8未解锁";
            ctx.drawTextWithShadow(tr, Text.literal(reinStr), cardX + 6, aiY, COLOR_TEXT);
            aiY += 11;
            // 夜晚/血月加成
            String buffStr = "§7夜速§a+15% §7血月§c+30%速§c+20%攻";
            ctx.drawTextWithShadow(tr, Text.literal(buffStr), cardX + 6, aiY, COLOR_TEXT);
            aiY += 11;
            // 阶段提示
            if (aiY < cardY + aiCardH - 2) {
                String tip = getStageTip(currentDay);
                ctx.drawTextWithShadow(tr, Text.literal(tip), cardX + 6, aiY, COLOR_TEXT);
            }
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
