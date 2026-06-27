package com.zombieapocalypse.client;

import com.zombieapocalypse.config.ModConfig;
import com.zombieapocalypse.config.StageSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.world.World;

/**
 * 末日情报书界面 - v3.0.0
 * 3个子页面: 基础信息 / 僵尸详情 / 巨型僵尸详情
 * 内容超出可视区域时支持鼠标滚轮滚动（直接 override mouseScrolled，无需 Mixin）
 * 面板尺寸自适应窗口（取窗口大小的较小比例，居中显示）
 */
public class ApocalypseBookScreen extends Screen {

    // 配色方案
    private static final int COLOR_BG_DARK    = 0xF0100A0A;
    private static final int COLOR_BG_PANEL   = 0xFF0D0808;
    private static final int COLOR_BG_CARD    = 0xCC1A1212;
    private static final int COLOR_BG_SUBTAB  = 0x88151010;
    private static final int COLOR_BG_SUBTAB_SEL = 0xEE2A0808;
    private static final int COLOR_BORDER_RED = 0xFFFF2A2A;
    private static final int COLOR_BORDER_DIM = 0x66883333;
    private static final int COLOR_ACCENT     = 0xFFFF4444;
    private static final int COLOR_TEXT       = 0xFFE0E0E0;
    private static final int COLOR_TEXT_DIM   = 0xFF999999;
    private static final int COLOR_TEXT_HI    = 0xFFFFAA00;
    private static final int COLOR_DANGER     = 0xFFFF3333;
    private static final int COLOR_BLOOD_MOON = 0xFFFF0000;
    private static final int COLOR_ZOMBIE     = 0xFFFF6644;
    private static final int COLOR_GIANT      = 0xFFFF66FF;

    // 面板尺寸（比原版背包大，给内容更多空间）
    private static final int PANEL_W = 200;
    private static final int PANEL_H = 220;
    private static final int SUBTAB_W = 56;
    private static final int SUBTAB_H = 14;
    private static final int SCROLL_BAR_W = 3;
    private static final int SCROLL_STEP = 12;

    private int currentSubTab = 0;   // 0=基础, 1=僵尸, 2=巨型
    private int hoveredSubTab = -1;
    // 每个子页面独立的滚动偏移
    private int scrollBasic = 0;
    private int scrollZombie = 0;
    private int scrollGiant = 0;

    // 面板左上角坐标（居中）
    private int panelX;
    private int panelY;

    public ApocalypseBookScreen() {
        super(Text.literal("末日情报"));
    }

    @Override
    protected void init() {
        // 居中放置面板
        panelX = (this.width - PANEL_W) / 2;
        panelY = (this.height - PANEL_H) / 2;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 全屏深色背景
        context.fill(0, 0, this.width, this.height, COLOR_BG_DARK);
        context.fill(0, 0, this.width, this.height / 4, 0x44000000);

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        World world = client.player.getWorld();
        if (world == null) return;

        TextRenderer tr = client.textRenderer;
        int px = panelX;
        int py = panelY;
        int pw = PANEL_W;
        int ph = PANEL_H;
        int centerX = px + pw / 2;

        boolean isBloodMoon = StageSystem.isBloodMoon(world);

        // 面板背景
        context.fill(px, py, px + pw, py + ph, COLOR_BG_PANEL);
        for (int i = 0; i < 4; i++) {
            int alpha = 0xFF - i * 0x30;
            context.fill(px, py + i, px + pw, py + i + 1, (alpha << 24) | 0x2A0808);
        }
        drawBorder(context, px, py, pw, ph, COLOR_BORDER_RED, 1);
        drawCornerDecor(context, px, py, pw, ph);

        // 标题横幅
        int titleY = py + 5;
        int titleH = 15;
        context.fill(px + 6, titleY, px + pw - 6, titleY + titleH, 0x993A0505);
        context.fill(px + 6, titleY, px + pw - 6, titleY + 1, COLOR_ACCENT);
        context.fill(px + 6, titleY + titleH - 1, px + pw - 6, titleY + titleH, COLOR_ACCENT);
        int titleColor = isBloodMoon ? COLOR_BLOOD_MOON : 0xFFFF5555;
        context.drawCenteredTextWithShadow(tr, Text.literal("☠ 惊变100天 ☠"), centerX, titleY + 3, titleColor);

        // 血月警告
        if (isBloodMoon) {
            int warnY = titleY + titleH + 1;
            context.fill(px + 6, warnY, px + pw - 6, warnY + 11, 0xCC660000);
            context.fill(px + 6, warnY, px + pw - 6, warnY + 1, COLOR_BLOOD_MOON);
            context.fill(px + 6, warnY + 10, px + pw - 6, warnY + 11, COLOR_BLOOD_MOON);
            context.drawCenteredTextWithShadow(tr, Text.literal("● 血月进行中 ●"), centerX, warnY + 1, COLOR_BLOOD_MOON);
        }

        // 子标签栏
        int subTabY = py + 24;
        renderSubTabs(context, tr, px, subTabY, mouseX, mouseY);

        // 内容区域
        int contentTop = subTabY + SUBTAB_H + 3;
        int contentBottom = py + ph - 3;
        int visibleH = contentBottom - contentTop;
        int cardW = pw - 12 - SCROLL_BAR_W - 2;
        int cardX = px + 6;

        // 测量内容高度
        int measuredHeight = measurePage(currentSubTab);
        int maxScroll = Math.max(0, measuredHeight - visibleH);
        int scroll = getCurrentScroll();
        if (scroll > maxScroll) {
            scroll = maxScroll;
            setCurrentScroll(scroll);
        }

        // 启用裁剪绘制内容
        context.enableScissor(cardX - 1, contentTop, cardX + cardW + 1, contentBottom);
        int canvasY = contentTop - scroll;
        switch (currentSubTab) {
            case 0 -> renderBasicPage(context, tr, world, cardX, canvasY, cardW);
            case 1 -> renderZombiePage(context, tr, world, cardX, canvasY, cardW);
            case 2 -> renderGiantPage(context, tr, world, cardX, canvasY, cardW);
        }
        context.disableScissor();

        // 滚动条
        drawScrollBar(context, px + pw - 6, contentTop, contentBottom, scroll, maxScroll);

        // 底部提示
        String hint = "滚轮滚动 | ESC 关闭";
        int hintW = tr.getWidth(hint);
        context.drawTextWithShadow(tr, Text.literal(hint), px + pw - 6 - hintW, py + ph - 11, COLOR_TEXT_DIM);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        // amount > 0 向上滚（内容下移），< 0 向下滚（内容上移）
        if (amount == 0) return false;
        int maxScroll = getMaxScroll();
        if (maxScroll <= 0) return false;
        int delta = amount > 0 ? -SCROLL_STEP : SCROLL_STEP;
        int next = Math.max(0, Math.min(maxScroll, getCurrentScroll() + delta));
        setCurrentScroll(next);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 子标签点击
        int subTabY = panelY + 24;
        if (mouseY >= subTabY && mouseY <= subTabY + SUBTAB_H) {
            int sx = panelX + 6;
            for (int i = 0; i < 3; i++) {
                if (mouseX >= sx && mouseX <= sx + SUBTAB_W) {
                    if (currentSubTab != i) {
                        currentSubTab = i;
                        setCurrentScroll(0);
                    }
                    return true;
                }
                sx += SUBTAB_W + 2;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // ===================== 滚动状态 =====================

    private int getCurrentScroll() {
        return switch (currentSubTab) {
            case 0 -> scrollBasic;
            case 1 -> scrollZombie;
            default -> scrollGiant;
        };
    }

    private void setCurrentScroll(int v) {
        switch (currentSubTab) {
            case 0 -> scrollBasic = v;
            case 1 -> scrollZombie = v;
            default -> scrollGiant = v;
        }
    }

    private int getMaxScroll() {
        int measuredHeight = measurePage(currentSubTab);
        int visibleH = (panelY + PANEL_H - 3) - (panelY + 24 + SUBTAB_H + 3);
        return Math.max(0, measuredHeight - visibleH);
    }

    // ===================== 内容高度测量 =====================

    private int measurePage(int sub) {
        return switch (sub) {
            case 0 -> measureBasic();
            case 1 -> measureZombie();
            default -> measureGiant();
        };
    }

    private int measureBasic() {
        int h = 34 + 2;     // 天数卡片
        h += 20 + 2;        // 智能度卡片
        h += 4 + 11 * 5 + 4; // AI能力卡片（标题+5行）
        h += 4 + 11 * 4 + 4; // 生存提示卡片（标题+4行）
        return h;
    }

    private int measureZombie() {
        int h = 58 + 2;     // 属性卡片
        h += 46 + 2;        // 战斗能力卡片
        h += 4 + 11 * 3 + 4; // 加成状态卡片（标题+3行）
        return h;
    }

    private int measureGiant() {
        int h = 70 + 2;     // 属性卡片
        h += 4 + 11 * 5 + 4; // 掉落物卡片（标题+5行）
        return h;
    }

    private void drawScrollBar(DrawContext ctx, int x, int top, int bottom, int scroll, int maxScroll) {
        ctx.fill(x, top, x + SCROLL_BAR_W, bottom, 0x55220A0A);
        if (maxScroll <= 0) return;
        int trackH = bottom - top;
        int thumbH = Math.max(12, trackH * trackH / (trackH + maxScroll));
        int thumbY = top + (int) ((long) (trackH - thumbH) * scroll / maxScroll);
        ctx.fill(x, thumbY, x + SCROLL_BAR_W, thumbY + thumbH, COLOR_ACCENT);
        ctx.fill(x, thumbY, x + SCROLL_BAR_W, thumbY + 1, 0xFFFFAAAA);
    }

    // ===================== 子标签 =====================

    private void renderSubTabs(DrawContext ctx, TextRenderer tr, int px, int subTabY, int mouseX, int mouseY) {
        hoveredSubTab = -1;
        if (mouseY >= subTabY && mouseY <= subTabY + SUBTAB_H) {
            int sx = px + 6;
            for (int i = 0; i < 3; i++) {
                if (mouseX >= sx && mouseX <= sx + SUBTAB_W) {
                    hoveredSubTab = i;
                    break;
                }
                sx += SUBTAB_W + 2;
            }
        }

        String[] labels = {"基础", "僵尸", "巨型"};
        int[] colors = {COLOR_TEXT_HI, COLOR_ZOMBIE, COLOR_GIANT};
        int sx = px + 6;
        for (int i = 0; i < 3; i++) {
            boolean sel = currentSubTab == i;
            boolean hov = hoveredSubTab == i;
            int bg = sel ? COLOR_BG_SUBTAB_SEL : (hov ? 0xBB1A0A0A : COLOR_BG_SUBTAB);
            int border = sel ? colors[i] : COLOR_BORDER_DIM;
            int textCol = sel ? colors[i] : COLOR_TEXT_DIM;

            ctx.fill(sx, subTabY, sx + SUBTAB_W, subTabY + SUBTAB_H, bg);
            ctx.fill(sx, subTabY, sx + SUBTAB_W, subTabY + 1, border);
            ctx.fill(sx, subTabY + SUBTAB_H - 1, sx + SUBTAB_W, subTabY + SUBTAB_H, border);
            ctx.fill(sx, subTabY, sx + 1, subTabY + SUBTAB_H, border);
            ctx.fill(sx + SUBTAB_W - 1, subTabY, sx + SUBTAB_W, subTabY + SUBTAB_H, border);
            if (sel) {
                ctx.fill(sx + 1, subTabY + SUBTAB_H - 2, sx + SUBTAB_W - 1, subTabY + SUBTAB_H, colors[i]);
            }
            ctx.drawCenteredTextWithShadow(tr, Text.literal(labels[i]),
                    sx + SUBTAB_W / 2, subTabY + 3, textCol);
            sx += SUBTAB_W + 2;
        }
    }

    // ===================== 基础信息页 =====================

    private void renderBasicPage(DrawContext ctx, TextRenderer tr, World world,
                                 int cardX, int canvasY, int cardW) {
        int currentDay = StageSystem.getCurrentStage(world);
        double progress = StageSystem.getStageProgress(world);
        boolean isBloodMoon = StageSystem.isBloodMoon(world);
        int intelLevel = StageSystem.getIntelligenceLevel(world);

        int cardY = canvasY;

        // 天数卡片
        int dayCardH = 34;
        drawCard(ctx, cardX, cardY, cardW, dayCardH);
        String dayStr = String.valueOf(currentDay);
        int dayTextY = cardY + 5;
        ctx.drawTextWithShadow(tr, Text.literal("第"), cardX + 6, dayTextY, COLOR_TEXT_DIM);
        ctx.drawTextWithShadow(tr, Text.literal(dayStr), cardX + 18, dayTextY, COLOR_TEXT_HI);
        ctx.drawTextWithShadow(tr, Text.literal("天 /" + ModConfig.TOTAL_DAYS),
                cardX + 18 + tr.getWidth(dayStr) + 2, dayTextY, COLOR_TEXT_DIM);
        // 血月倒计时
        int currentDayRaw = StageSystem.getCurrentDay(world);
        int nextBM = ((currentDayRaw / ModConfig.BLOOD_MOON_INTERVAL) + 1) * ModConfig.BLOOD_MOON_INTERVAL;
        int daysToBM = nextBM - currentDayRaw;
        String bmText = isBloodMoon ? "● 血月今日" : "下次血月: " + daysToBM + "天后";
        int bmColor = isBloodMoon ? COLOR_BLOOD_MOON : COLOR_TEXT_DIM;
        ctx.drawTextWithShadow(tr, Text.literal(bmText), cardX + 6, cardY + 16, bmColor);
        // 进度条
        int barY = cardY + 26;
        int barH = 5;
        int barX = cardX + 6;
        int barW = cardW - 12;
        ctx.fill(barX, barY, barX + barW, barY + barH, 0xFF050505);
        int filledW = (int) (barW * progress);
        if (filledW > 0) {
            ctx.fill(barX, barY, barX + filledW, barY + barH, getProgressColor(progress));
            ctx.fill(barX, barY, barX + filledW, barY + 1, 0x66FFFFFF);
        }

        // 智能度卡片
        cardY += dayCardH + 2;
        int intelCardH = 20;
        drawCard(ctx, cardX, cardY, cardW, intelCardH);
        ctx.drawTextWithShadow(tr, Text.literal("智能度"), cardX + 6, cardY + 6, COLOR_TEXT_DIM);
        String[] intelNames = {"迟钝", "普通", "机敏", "狡猾", "凶残", "嗜血"};
        int[] intelColors = {0xFF33AA33, 0xFFFFAA00, 0xFFCC6622, 0xFFFF3333, 0xFFFF0000, 0xFFFF0000};
        String intelText = "Lv" + intelLevel + " " + intelNames[intelLevel];
        int intelW = tr.getWidth(intelText);
        ctx.drawTextWithShadow(tr, Text.literal(intelText), cardX + cardW - 6 - intelW, cardY + 6, intelColors[intelLevel]);

        // AI能力卡片
        cardY += intelCardH + 2;
        int aiCardH = 4 + 11 * 5 + 4;
        drawCard(ctx, cardX, cardY, cardW, aiCardH);
        ctx.drawTextWithShadow(tr, Text.literal("⚙ AI能力"), cardX + 6, cardY + 4, COLOR_TEXT_HI);

        int aiY = cardY + 16;
        int breakInt = StageSystem.getBreakInterval(world);
        int buildInt = StageSystem.getBuildInterval(world);
        float hardLimit = StageSystem.getHardnessLimit(world);
        double reinChance = StageSystem.getReinforcementChance(world);
        int invSize = StageSystem.getBlockInventorySize(world);

        ctx.drawTextWithShadow(tr, Text.literal(
                String.format("拆: %.1fs   搭: %.1fs", breakInt / 20.0, buildInt / 20.0)),
                cardX + 6, aiY, COLOR_TEXT);
        aiY += 11;
        ctx.drawTextWithShadow(tr, Text.literal(
                String.format("硬度上限: %.0f   库存: %d", hardLimit, invSize)),
                cardX + 6, aiY, COLOR_TEXT);
        aiY += 11;
        String reinStr = reinChance > 0
                ? String.format("增援概率: %.0f%%", reinChance * 100)
                : "增援: 未解锁";
        int reinColor = reinChance > 0 ? COLOR_DANGER : COLOR_TEXT_DIM;
        ctx.drawTextWithShadow(tr, Text.literal(reinStr), cardX + 6, aiY, reinColor);
        aiY += 11;
        ctx.drawTextWithShadow(tr, Text.literal("夜速+15%  血月+30%速 +20%攻"),
                cardX + 6, aiY, COLOR_TEXT);
        aiY += 11;
        ctx.drawTextWithShadow(tr, Text.literal(getStageTip(currentDay)),
                cardX + 6, aiY, COLOR_TEXT_HI);

        // 生存提示卡片
        cardY += aiCardH + 2;
        int tipCardH = 4 + 11 * 4 + 4;
        drawCard(ctx, cardX, cardY, cardW, tipCardH);
        ctx.drawTextWithShadow(tr, Text.literal("★ 生存提示"), cardX + 6, cardY + 4, 0xFF66DDFF);

        int tipY = cardY + 16;
        String[][] tipsData = getSurvivalTips(currentDay, isBloodMoon);
        for (String[] tip : tipsData) {
            ctx.drawTextWithShadow(tr, Text.literal(tip[0]), cardX + 6, tipY, Integer.parseUnsignedInt(tip[1], 16));
            tipY += 11;
        }
    }

    private String[][] getSurvivalTips(int day, boolean isBloodMoon) {
        if (isBloodMoon) {
            return new String[][]{
                    {"血月期间怪物刷新翻倍！", "FFFF3333"},
                    {"加固门窗，准备死守！", "FFFF3333"},
                    {"巨型僵尸频繁出没，注意远程", "FFFF6644"},
                    {"保留火把与高墙防御", "FFFFAA00"}
            };
        }
        if (day <= 10) {
            return new String[][]{
                    {"收集木头与食物，建立庇护所", "FF33AA33"},
                    {"制作石制武器与护甲", "FF33AA33"},
                    {"夜晚尽量待在室内", "FF999999"},
                    {"留意第10天的血月！", "FFFFAA00"}
            };
        }
        if (day <= 30) {
            return new String[][]{
                    {"加固防御，使用铁制装备", "FFFFAA00"},
                    {"准备弓箭应对远程威胁", "FFFFAA00"},
                    {"僵尸开始变强，注意血量", "FFCC6622"},
                    {"第20/30天有血月", "FFFF3333"}
            };
        }
        if (day <= 50) {
            return new String[][]{
                    {"携带钻石装备出门", "FFCC6622"},
                    {"僵尸可破坏方块，加固墙体", "FFFF3333"},
                    {"巨型僵尸出现，保持距离", "FFFF3333"},
                    {"第40/50天血月极其危险", "FFFF3333"}
            };
        }
        return new String[][]{
                {"末日降临，谨慎行动", "FFFF3333"},
                {"巨型僵尸群出没，备足药水", "FFFF3333"},
                {"高墙+护城河是最佳防御", "FFFF3333"},
                {"第60/70/80/90/100天均为血月", "FFFF3333"}
        };
    }

    // ===================== 僵尸详情页 =====================

    private void renderZombiePage(DrawContext ctx, TextRenderer tr, World world,
                                  int cardX, int canvasY, int cardW) {
        double health = StageSystem.getZombieHealth(world);
        double attack = StageSystem.getZombieAttack(world);
        double armor = StageSystem.getZombieArmor(world);
        double speed = StageSystem.getZombieSpeed(world);
        int followRange = StageSystem.getFollowRange(world);
        double reinChance = StageSystem.getReinforcementChance(world);
        int breakInt = StageSystem.getBreakInterval(world);
        int buildInt = StageSystem.getBuildInterval(world);
        float hardLimit = StageSystem.getHardnessLimit(world);
        int invSize = StageSystem.getBlockInventorySize(world);
        boolean isBloodMoon = StageSystem.isBloodMoon(world);
        boolean isNight = !world.isDay();

        int cardY = canvasY;

        // 属性卡片
        int attrCardH = 58;
        drawCard(ctx, cardX, cardY, cardW, attrCardH);
        ctx.drawTextWithShadow(tr, Text.literal("⚔ 普通僵尸属性"), cardX + 6, cardY + 4, COLOR_ZOMBIE);

        int rowY = cardY + 16;
        drawDetailRow(ctx, tr, cardX + 6, rowY, cardW - 12, "血量", String.format("%.0f / 20", health),
                String.format("(%.1fx)", health / 20), COLOR_DANGER);
        rowY += 11;
        double atkMult = StageSystem.getAttackMultiplier(world);
        drawDetailRow(ctx, tr, cardX + 6, rowY, cardW - 12, "攻击", String.format("%.1f", attack),
                atkMult > 1 ? String.format("(x%.1f)", atkMult) : "", COLOR_TEXT_HI);
        rowY += 11;
        drawDetailRow(ctx, tr, cardX + 6, rowY, cardW - 12, "护甲", String.format("%.0f", armor), "", COLOR_TEXT);
        rowY += 11;
        double spdMult = StageSystem.getSpeedMultiplier(world, 1.0f);
        String spdExtra = spdMult > 1 ? String.format("(x%.2f)", spdMult) : "";
        drawDetailRow(ctx, tr, cardX + 6, rowY, cardW - 12, "速度", String.format("%.2f", speed), spdExtra, COLOR_TEXT);
        rowY += 11;
        drawDetailRow(ctx, tr, cardX + 6, rowY, cardW - 12, "追踪范围", followRange + " 格", "", COLOR_TEXT);

        // 战斗能力卡片
        cardY += attrCardH + 2;
        int combatCardH = 46;
        drawCard(ctx, cardX, cardY, cardW, combatCardH);
        ctx.drawTextWithShadow(tr, Text.literal("⚙ 战斗能力"), cardX + 6, cardY + 4, COLOR_TEXT_HI);

        rowY = cardY + 16;
        drawDetailRow(ctx, tr, cardX + 6, rowY, cardW - 12, "拆方块", String.format("%.1fs", breakInt / 20.0), "", COLOR_TEXT);
        rowY += 11;
        drawDetailRow(ctx, tr, cardX + 6, rowY, cardW - 12, "搭方块", String.format("%.1fs", buildInt / 20.0), "", COLOR_TEXT);
        rowY += 11;
        drawDetailRow(ctx, tr, cardX + 6, rowY, cardW - 12, "硬度上限", String.format("%.0f", hardLimit), "", COLOR_TEXT);
        rowY += 11;
        String reinStr = reinChance > 0 ? String.format("%.0f%%", reinChance * 100) : "未解锁";
        int reinColor = reinChance > 0 ? COLOR_DANGER : COLOR_TEXT_DIM;
        drawDetailRow(ctx, tr, cardX + 6, rowY, cardW - 12, "呼叫增援", reinStr, "", reinColor);

        // 加成状态卡片
        cardY += combatCardH + 2;
        int buffCardH = 4 + 11 * 3 + 4;
        drawCard(ctx, cardX, cardY, cardW, buffCardH);
        ctx.drawTextWithShadow(tr, Text.literal("★ 当前加成"), cardX + 6, cardY + 4, 0xFF66DDFF);

        rowY = cardY + 16;
        String nightStr = isNight ? "激活 +15%速" : "未激活";
        int nightColor = isNight ? 0xFF33AA33 : COLOR_TEXT_DIM;
        drawDetailRow(ctx, tr, cardX + 6, rowY, cardW - 12, "夜晚加成", nightStr, "", nightColor);
        rowY += 11;
        String bmStr = isBloodMoon ? "激活 +30%速 +20%攻" : "未激活";
        int bmColor = isBloodMoon ? COLOR_BLOOD_MOON : COLOR_TEXT_DIM;
        drawDetailRow(ctx, tr, cardX + 6, rowY, cardW - 12, "血月加成", bmStr, "", bmColor);
        rowY += 11;
        drawDetailRow(ctx, tr, cardX + 6, rowY, cardW - 12, "库存容量", invSize + " 格", "", COLOR_TEXT);
    }

    // ===================== 巨型僵尸详情页 =====================

    private void renderGiantPage(DrawContext ctx, TextRenderer tr, World world,
                                 int cardX, int canvasY, int cardW) {
        double health = StageSystem.getGiantZombieHealth(world);
        double attack = StageSystem.getGiantZombieAttack(world);
        double chance = StageSystem.getGiantZombieChance(world);
        int followRange = StageSystem.getFollowRange(world);

        int cardY = canvasY;

        // 属性卡片
        int attrCardH = 70;
        drawCard(ctx, cardX, cardY, cardW, attrCardH);
        int titleY = cardY + 4;
        ctx.drawTextWithShadow(tr, Text.literal("▾ 巨型僵尸属性"), cardX + 6, titleY, COLOR_GIANT);
        String scaleText = "(2倍缩放)";
        ctx.drawTextWithShadow(tr, Text.literal(scaleText),
                cardX + cardW - 6 - tr.getWidth(scaleText), titleY, COLOR_TEXT_DIM);

        int rowY = cardY + 16;
        double atkMult = StageSystem.getAttackMultiplier(world);
        drawDetailRow(ctx, tr, cardX + 6, rowY, cardW - 12, "血量", String.format("%.0f / 200", health),
                String.format("(%.1fx)", health / 200), COLOR_GIANT);
        rowY += 11;
        drawDetailRow(ctx, tr, cardX + 6, rowY, cardW - 12, "攻击", String.format("%.1f", attack),
                atkMult > 1 ? String.format("(x%.1f)", atkMult) : "", COLOR_GIANT);
        rowY += 11;
        drawDetailRow(ctx, tr, cardX + 6, rowY, cardW - 12, "生成概率", String.format("%.1f%%", chance * 100), "", COLOR_GIANT);
        rowY += 11;
        drawDetailRow(ctx, tr, cardX + 6, rowY, cardW - 12, "追踪范围", followRange + " 格", "", COLOR_GIANT);
        rowY += 11;
        drawDetailRow(ctx, tr, cardX + 6, rowY, cardW - 12, "攻击范围", "2.5x (约4.6格)", "", COLOR_GIANT);
        rowY += 11;
        drawDetailRow(ctx, tr, cardX + 6, rowY, cardW - 12, "防火", "免疫", "", COLOR_TEXT);

        // 掉落物卡片
        cardY += attrCardH + 2;
        int dropCardH = 4 + 11 * 5 + 4;
        drawCard(ctx, cardX, cardY, cardW, dropCardH);
        ctx.drawTextWithShadow(tr, Text.literal("◈ 掉落物"), cardX + 6, cardY + 4, 0xFFFFAA00);

        rowY = cardY + 16;
        ctx.drawTextWithShadow(tr, Text.literal("腐肉 3-8   骨头 3-6"), cardX + 6, rowY, COLOR_TEXT);
        rowY += 11;
        ctx.drawTextWithShadow(tr, Text.literal("铁锭 2-5   金锭 1-3"), cardX + 6, rowY, COLOR_TEXT);
        rowY += 11;
        ctx.drawTextWithShadow(tr, Text.literal("钻石 0-2   绿宝石 0-3"), cardX + 6, rowY, COLOR_TEXT);
        rowY += 11;
        ctx.drawTextWithShadow(tr, Text.literal("附魔金苹果 0-1 (稀有)"), cardX + 6, rowY, 0xFFFF66FF);
        rowY += 11;
        ctx.drawTextWithShadow(tr, Text.literal("经验瓶 1-5"), cardX + 6, rowY, 0xFF33AA33);
    }

    // ===================== 辅助绘制方法 =====================

    private void drawDetailRow(DrawContext ctx, TextRenderer tr, int x, int y, int w,
                               String label, String value, String extra, int valueColor) {
        ctx.drawTextWithShadow(tr, Text.literal(label), x, y, COLOR_TEXT_DIM);

        int valW = tr.getWidth(value);
        int extraW = extra.isEmpty() ? 0 : tr.getWidth(extra);
        int rightPad = 2;
        int gap = 4;

        if (extra.isEmpty()) {
            ctx.drawTextWithShadow(tr, Text.literal(value), x + w - valW - rightPad, y, valueColor);
        } else {
            int extraX = x + w - extraW - rightPad;
            int valX = extraX - gap - valW;
            ctx.drawTextWithShadow(tr, Text.literal(value), valX, y, valueColor);
            ctx.drawTextWithShadow(tr, Text.literal(extra), extraX, y, COLOR_TEXT_DIM);
        }
    }

    private int getProgressColor(double progress) {
        if (progress < 0.25) return 0xFF33AA33;
        if (progress < 0.50) return 0xFFCCAA22;
        if (progress < 0.75) return 0xFFCC6622;
        return 0xFFFF2222;
    }

    private String getStageTip(int day) {
        if (day <= 10) return "初期 - 收集资源，建立基地";
        if (day <= 30) return "发展 - 加固防御，准备迎战";
        if (day <= 50) return "中期 - 僵尸越来越强！";
        if (day <= 70) return "后期 - 巨型僵尸频繁出没！";
        return "最终 - 末日降临，拼死生存！";
    }

    private void drawCard(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, COLOR_BG_CARD);
        ctx.fill(x, y, x + w, y + 1, 0x33FF4444);
        ctx.fill(x, y + h - 1, x + w, y + h, 0x44110000);
        ctx.fill(x, y, x + 1, y + h, 0x22FF3333);
    }

    private void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color, int thickness) {
        ctx.fill(x, y, x + w, y + thickness, color);
        ctx.fill(x, y + h - thickness, x + w, y + h, color);
        ctx.fill(x, y, x + thickness, y + h, color);
        ctx.fill(x + w - thickness, y, x + w, y + h, color);
    }

    private void drawCornerDecor(DrawContext ctx, int x, int y, int w, int h) {
        int s = 6;
        ctx.fill(x, y, x + s, y + 1, COLOR_ACCENT);
        ctx.fill(x, y, x + 1, y + s, COLOR_ACCENT);
        ctx.fill(x + w - s, y, x + w, y + 1, COLOR_ACCENT);
        ctx.fill(x + w - 1, y, x + w, y + s, COLOR_ACCENT);
        ctx.fill(x, y + h - 1, x + s, y + h, COLOR_ACCENT);
        ctx.fill(x, y + h - s, x + 1, y + h, COLOR_ACCENT);
        ctx.fill(x + w - s, y + h - 1, x + w, y + h, COLOR_ACCENT);
        ctx.fill(x + w - 1, y + h - s, x + w, y + h, COLOR_ACCENT);
    }
}
