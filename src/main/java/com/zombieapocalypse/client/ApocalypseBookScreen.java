package com.zombieapocalypse.client;

import com.zombieapocalypse.config.ModConfig;
import com.zombieapocalypse.config.StageSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;

/**
 * 末日情报界面 - 24页单页版
 *
 * 设计原则: 每页只放1-2个小段, 内容高度严格<120px, 绝不超出裁剪区。
 * 可用内容区: 148px高, 减标题16px后约130px可用, 每页留足余量。
 *
 * 章节结构 (4大章 x 多页):
 *   第1章 概览  (P1-P2)
 *   第2章 末日历法 (P3-P6)
 *   第3章 智能与AI (P7-P10)
 *   第4章 僵尸档案 (P11-P13)
 *   第5章 战斗特性 (P14-P18)
 *   第6章 巨型僵尸 (P19-P24)
 */
public class ApocalypseBookScreen extends Screen {

    private static final int PANEL_W = 256;
    private static final int PANEL_H = 180;
    private static final int BORDER = 8;
    private static final int CONTENT_W = PANEL_W - BORDER * 2;
    private static final int CONTENT_H = PANEL_H - BORDER * 2;
    private static final int ROW_H = 10;
    private static final int TOTAL_PAGES = 24;

    private static final int COLOR_OVERLAY     = 0x80000000;
    private static final int COLOR_PANEL_BG    = 0xFFF4E3C1;
    private static final int COLOR_PANEL_BORDER= 0xFF6B4A1F;
    private static final int COLOR_PANEL_INNER = 0xFFB08A4F;
    private static final int COLOR_INK         = 0xFF2A1A0A;
    private static final int COLOR_INK_DIM     = 0xFF6B5A3F;
    private static final int COLOR_INK_HI      = 0xFF8B6914;
    private static final int COLOR_ACCENT      = 0xFF8B3A1A;
    private static final int COLOR_DANGER      = 0xFFB01818;
    private static final int COLOR_BLOOD_MOON  = 0xFFCC3030;
    private static final int COLOR_ZOMBIE      = 0xFF2A6B2A;
    private static final int COLOR_GIANT       = 0xFF2A1A6B;
    private static final int COLOR_GOLD        = 0xFF8B6914;
    private static final int COLOR_GREEN       = 0xFF2A6B2A;
    private static final int COLOR_BLUE        = 0xFF2A4A8B;
    private static final int COLOR_DIVIDER     = 0x556B4A1F;

    private static final String[] PAGE_TITLES = {
        "末日情报 - 目录",      // P1
        "概览 - 当前状态",      // P2
        "末日历法 - 进度",      // P3
        "末日历法 - 阶段",      // P4
        "末日历法 - 血月周期",  // P5
        "末日历法 - 血月效果",  // P6
        "智能与AI - 当前智能度",// P7
        "智能与AI - AI能力",    // P8
        "智能与AI - 行为说明",  // P9
        "智能度阶梯 - 表格",    // P10
        "僵尸档案 - 基础属性",  // P11
        "僵尸档案 - 属性范围",  // P12
        "僵尸档案 - 其他特性",  // P13
        "战斗特性 - 当前加成",  // P14
        "战斗特性 - 血月加成",  // P15
        "战斗特性 - 夜晚加成",  // P16
        "战斗特性 - 狂暴机制",  // P17
        "战斗特性 - 行为特性",  // P18
        "巨型僵尸 - 巨型属性",  // P19
        "巨型僵尸 - 特殊能力",  // P20
        "巨型僵尸 - 出现条件",  // P21
        "巨型僵尸 - 掉落物",    // P22
        "巨型僵尸 - 掉落说明",  // P23
        "巨型僵尸 - 应对建议"   // P24
    };
    private static final int[] PAGE_COLORS = {
        COLOR_GOLD, COLOR_GOLD,
        COLOR_GOLD, COLOR_GOLD, COLOR_BLOOD_MOON, COLOR_BLOOD_MOON,
        COLOR_ACCENT, COLOR_ACCENT, COLOR_ACCENT, COLOR_ACCENT,
        COLOR_ZOMBIE, COLOR_ZOMBIE, COLOR_ZOMBIE,
        COLOR_BLUE, COLOR_BLOOD_MOON, COLOR_GREEN, COLOR_DANGER, COLOR_INK_HI,
        COLOR_GIANT, COLOR_INK_HI, COLOR_GOLD, COLOR_GOLD, COLOR_BLUE, COLOR_DANGER
    };

    private int panelX, panelY;
    private int contentX, contentY;
    private int currentPage = 0;
    private int prevBtnX, prevBtnY, nextBtnX, nextBtnY;
    private boolean prevHovered, nextHovered;

    public ApocalypseBookScreen() {
        super(Text.literal("末日情报"));
    }

    @Override
    protected void init() {
        this.panelX = (this.width - PANEL_W) / 2;
        this.panelY = (this.height - PANEL_H) / 2;
        this.contentX = this.panelX + BORDER;
        this.contentY = this.panelY + BORDER;
        int btnY = this.panelY + PANEL_H - 18;
        this.prevBtnX = this.panelX + BORDER;
        this.nextBtnX = this.panelX + PANEL_W - BORDER - 23;
        this.prevBtnY = this.nextBtnY = btnY;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, COLOR_OVERLAY);
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        ClientPlayerEntity player = client.player;
        World world = player.getWorld();
        if (world == null) return;
        TextRenderer tr = client.textRenderer;

        this.drawPanelBackground(ctx);

        ctx.enableScissor(this.contentX, this.contentY,
                          this.contentX + CONTENT_W, this.contentY + CONTENT_H);
        this.renderPage(ctx, tr, world, this.contentX, this.contentY, CONTENT_W, this.currentPage);
        ctx.disableScissor();

        this.prevHovered = this.inBounds(mouseX, mouseY, this.prevBtnX, this.prevBtnY, 23, 12);
        this.nextHovered = this.inBounds(mouseX, mouseY, this.nextBtnX, this.nextBtnY, 23, 12);
        this.drawPageButton(ctx, tr, this.prevBtnX, this.prevBtnY, "<",
                this.currentPage > 0, this.prevHovered);
        this.drawPageButton(ctx, tr, this.nextBtnX, this.nextBtnY, ">",
                this.currentPage < TOTAL_PAGES - 1, this.nextHovered);

        String pageInfo = (this.currentPage + 1) + " / " + TOTAL_PAGES;
        ctx.drawCenteredTextWithShadow(tr, Text.literal(pageInfo),
                this.panelX + PANEL_W / 2, this.prevBtnY + 2, COLOR_INK_DIM);
    }

    private void drawPanelBackground(DrawContext ctx) {
        int x0 = this.panelX, y0 = this.panelY;
        int x1 = x0 + PANEL_W, y1 = y0 + PANEL_H;
        ctx.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, COLOR_PANEL_BORDER);
        ctx.fill(x0, y0, x1, y1, COLOR_PANEL_BG);
        ctx.fill(x0 + 2, y0 + 2, x1 - 2, y1 - 2, COLOR_PANEL_INNER);
        ctx.fill(x0 + 3, y0 + 3, x1 - 3, y1 - 3, COLOR_PANEL_BG);
    }

    private void renderPage(DrawContext ctx, TextRenderer tr, World world,
                            int x, int y, int w, int page) {
        this.drawTitle(ctx, tr, x, y, w, PAGE_TITLES[page], PAGE_COLORS[page]);
        int bodyY = y + 16;
        switch (page) {
            case 0 -> this.renderP1Cover(ctx, tr, world, x, bodyY, w);
            case 1 -> this.renderP2Status(ctx, tr, world, x, bodyY, w);
            case 2 -> this.renderP3Progress(ctx, tr, world, x, bodyY, w);
            case 3 -> this.renderP4Stage(ctx, tr, world, x, bodyY, w);
            case 4 -> this.renderP5BloodMoonCycle(ctx, tr, world, x, bodyY, w);
            case 5 -> this.renderP6BloodMoonEffect(ctx, tr, world, x, bodyY, w);
            case 6 -> this.renderP7IntelCurrent(ctx, tr, world, x, bodyY, w);
            case 7 -> this.renderP8IntelAbility(ctx, tr, world, x, bodyY, w);
            case 8 -> this.renderP9IntelBehavior(ctx, tr, world, x, bodyY, w);
            case 9 -> this.renderP10IntelTier(ctx, tr, world, x, bodyY, w);
            case 10 -> this.renderP11ZombieBasic(ctx, tr, world, x, bodyY, w);
            case 11 -> this.renderP12ZombieRange(ctx, tr, world, x, bodyY, w);
            case 12 -> this.renderP13ZombieOther(ctx, tr, world, x, bodyY, w);
            case 13 -> this.renderP14BuffCurrent(ctx, tr, world, x, bodyY, w);
            case 14 -> this.renderP15BuffBloodMoon(ctx, tr, world, x, bodyY, w);
            case 15 -> this.renderP16BuffNight(ctx, tr, world, x, bodyY, w);
            case 16 -> this.renderP17Rage(ctx, tr, world, x, bodyY, w);
            case 17 -> this.renderP18Behavior(ctx, tr, world, x, bodyY, w);
            case 18 -> this.renderP19GiantStats(ctx, tr, world, x, bodyY, w);
            case 19 -> this.renderP20GiantAbility(ctx, tr, world, x, bodyY, w);
            case 20 -> this.renderP21GiantCondition(ctx, tr, world, x, bodyY, w);
            case 21 -> this.renderP22GiantDrops(ctx, tr, world, x, bodyY, w);
            case 22 -> this.renderP23GiantDropNote(ctx, tr, world, x, bodyY, w);
            case 23 -> this.renderP24GiantAdvice(ctx, tr, world, x, bodyY, w);
        }
    }

    private void drawTitle(DrawContext ctx, TextRenderer tr, int x, int y, int w,
                           String title, int color) {
        ctx.drawCenteredTextWithShadow(tr, Text.literal(title), x + w / 2, y, color);
        int lineW = (int) (w * 0.6);
        int lx = x + (w - lineW) / 2;
        ctx.fill(lx, y + 12, lx + lineW, y + 13, color);
    }

    private void drawSectionTitle(DrawContext ctx, TextRenderer tr, int x, int y, int w,
                                  String title, int color) {
        ctx.drawText(tr, Text.literal(title), x, y, color, false);
        ctx.fill(x, y + 10, x + w, y + 11, COLOR_DIVIDER);
    }

    private void drawRow(DrawContext ctx, TextRenderer tr, int x, int y, int w,
                         String label, String value, int valueColor) {
        ctx.drawText(tr, Text.literal(label), x, y, COLOR_INK_DIM, false);
        ctx.drawText(tr, Text.literal(value),
                x + w - tr.getWidth(value), y, valueColor, false);
    }

    private void drawRowExtra(DrawContext ctx, TextRenderer tr, int x, int y, int w,
                              String label, String value, String extra, int valueColor) {
        ctx.drawText(tr, Text.literal(label), x, y, COLOR_INK_DIM, false);
        if (extra == null || extra.isEmpty()) {
            ctx.drawText(tr, Text.literal(value),
                    x + w - tr.getWidth(value), y, valueColor, false);
        } else {
            int extraW = tr.getWidth(extra);
            int valueW = tr.getWidth(value);
            int extraX = x + w - extraW;
            int valX = extraX - 3 - valueW;
            ctx.drawText(tr, Text.literal(value), valX, y, valueColor, false);
            ctx.drawText(tr, Text.literal(extra), extraX, y, COLOR_INK_DIM, false);
        }
    }

    private void drawBullet(DrawContext ctx, TextRenderer tr, int x, int y, String text, int color) {
        ctx.drawText(tr, Text.literal("* " + text), x, y, color, false);
    }

    private void drawPageButton(DrawContext ctx, TextRenderer tr, int x, int y,
                                String arrow, boolean enabled, boolean hovered) {
        int bg, border, col;
        if (!enabled) {
            bg = 0x22000000; border = 0x44222222; col = 0xFF6B5A3F;
        } else if (hovered) {
            bg = 0xCC8B3A1A; border = 0xFFCC3030; col = 0xFFFFFFFF;
        } else {
            bg = 0x886B4A1F; border = 0xFF8B6914; col = 0xFF2A1A0A;
        }
        ctx.fill(x, y, x + 23, y + 12, bg);
        ctx.fill(x, y, x + 23, y + 1, border);
        ctx.fill(x, y + 11, x + 23, y + 12, border);
        ctx.fill(x, y, x + 1, y + 12, border);
        ctx.fill(x + 22, y, x + 23, y + 12, border);
        ctx.drawCenteredTextWithShadow(tr, Text.literal(arrow), x + 11, y + 2, col);
    }

    private boolean inBounds(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private int getProgressColor(double progress) {
        if (progress < 0.25) return COLOR_GREEN;
        if (progress < 0.5) return COLOR_GOLD;
        if (progress < 0.75) return COLOR_ACCENT;
        return COLOR_DANGER;
    }

    private String getStageTip(int day) {
        if (day <= 10) return "初期";
        if (day <= 30) return "发展期";
        if (day <= 50) return "中期";
        if (day <= 70) return "后期";
        return "最终期";
    }

    private String[] getSurvivalTips(int day, boolean isBloodMoon) {
        if (isBloodMoon) return new String[]{"血月刷新翻倍", "加固门窗死守", "警惕巨型僵尸"};
        if (day <= 10) return new String[]{"收集木头建庇护所", "制作石制装备", "留意血月来临"};
        if (day <= 30) return new String[]{"升级铁制装备", "准备弓箭远程", "注意僵尸变强"};
        if (day <= 50) return new String[]{"携带钻石装备", "加固墙体防御", "远离巨型僵尸"};
        return new String[]{"末日降临谨慎行动", "备足药水防御", "高墙护城河最佳"};
    }

    // ===================== P1: 目录 =====================

    private void renderP1Cover(DrawContext ctx, TextRenderer tr, World world, int x, int y, int w) {
        int cy = y;
        this.drawSectionTitle(ctx, tr, x, cy, w, "惊变100天 生存手册", COLOR_GOLD);
        cy += 13;
        this.drawBullet(ctx, tr, x, cy, "记录世界末日进程情报", COLOR_INK); cy += ROW_H;
        this.drawBullet(ctx, tr, x, cy, "共24页, 翻页查看详情", COLOR_INK_DIM); cy += ROW_H + 4;

        this.drawSectionTitle(ctx, tr, x, cy, w, "目录索引", COLOR_ACCENT);
        cy += 13;
        String[][] toc = {
            {"P2", "概览 - 当前状态"},
            {"P3-P6", "末日历法 (4页)"},
            {"P7-P10", "智能与AI (4页)"},
            {"P11-P13", "僵尸档案 (3页)"},
            {"P14-P18", "战斗特性 (5页)"},
            {"P19-P24", "巨型僵尸 (6页)"}
        };
        for (String[] e : toc) {
            ctx.drawText(tr, Text.literal(e[0] + "    " + e[1]), x, cy, COLOR_INK, false);
            cy += ROW_H;
        }
    }

    // ===================== P2: 当前状态 =====================

    private void renderP2Status(DrawContext ctx, TextRenderer tr, World world, int x, int y, int w) {
        int currentDay = StageSystem.getCurrentStage(world);
        boolean isBloodMoon = StageSystem.isBloodMoon(world);
        int intelLevel = StageSystem.getIntelligenceLevel(world);
        String[] intelNames = {"迟钝", "普通", "机敏", "狡猾", "凶残", "嗜血"};

        int cy = y;
        this.drawSectionTitle(ctx, tr, x, cy, w, "当前世界状态", COLOR_GOLD);
        this.drawRow(ctx, tr, x, cy += 13, w, "天数",
                currentDay + " / " + ModConfig.TOTAL_DAYS, COLOR_INK_HI);
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "阶段",
                this.getStageTip(currentDay), COLOR_INK_HI);
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "智能度",
                "Lv" + intelLevel + " " + intelNames[intelLevel],
                intelLevel >= 4 ? COLOR_DANGER : COLOR_INK_HI);
        String bmText = isBloodMoon ? "进行中"
                : "~" + StageSystem.getDaysToNextBloodMoon(world) + "天后";
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "血月", bmText,
                isBloodMoon ? COLOR_BLOOD_MOON : COLOR_INK);
        cy += ROW_H + 4;

        this.drawSectionTitle(ctx, tr, x, cy, w, "提示", COLOR_BLUE);
        this.drawBullet(ctx, tr, x, cy += 13, "翻页查看各章节详情", COLOR_INK_DIM);
    }

    // ===================== P3: 末日进度 =====================

    private void renderP3Progress(DrawContext ctx, TextRenderer tr, World world, int x, int y, int w) {
        int currentDay = StageSystem.getCurrentStage(world);
        double progress = StageSystem.getStageProgress(world);

        int cy = y;
        this.drawSectionTitle(ctx, tr, x, cy, w, "末日进度", COLOR_GOLD);
        this.drawRow(ctx, tr, x, cy += 13, w, "当前天数",
                currentDay + " / " + ModConfig.TOTAL_DAYS, COLOR_INK_HI);
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "总天数",
                String.valueOf(ModConfig.TOTAL_DAYS), COLOR_INK);
        cy += ROW_H + 4;

        this.drawSectionTitle(ctx, tr, x, cy, w, "进度条", COLOR_ACCENT);
        int barX = x, barW = w;
        ctx.fill(barX, cy += 13, barX + barW, cy + 2, COLOR_INK_DIM);
        int filledW = (int) (barW * progress);
        if (filledW > 0) {
            ctx.fill(barX, cy, barX + filledW, cy + 2, this.getProgressColor(progress));
        }
        ctx.drawCenteredTextWithShadow(tr, Text.literal((int) (progress * 100) + "%"),
                x + w / 2, cy + 4, COLOR_INK);
        cy += 12;
        this.drawBullet(ctx, tr, x, cy, "颜色随进度变红 = 越危险", COLOR_INK_DIM);
    }

    // ===================== P4: 阶段 =====================

    private void renderP4Stage(DrawContext ctx, TextRenderer tr, World world, int x, int y, int w) {
        int currentDay = StageSystem.getCurrentStage(world);
        int cy = y;
        this.drawSectionTitle(ctx, tr, x, cy, w, "阶段划分", COLOR_GOLD);
        cy += 13;
        String[][] stages = {
            {"1-10天", "初期", "FF2A6B2A"},
            {"11-30天", "发展期", "FF8B6914"},
            {"31-50天", "中期", "FF8B6914"},
            {"51-70天", "后期", "FFB01818"},
            {"71-100天", "最终期", "FFCC3030"}
        };
        for (String[] s : stages) {
            ctx.drawText(tr, Text.literal(s[0] + "    " + s[1]), x, cy,
                    Integer.parseUnsignedInt(s[2], 16), false);
            cy += ROW_H;
        }
        cy += 4;
        this.drawSectionTitle(ctx, tr, x, cy, w, "当前阶段", COLOR_ACCENT);
        ctx.drawText(tr, Text.literal("-> " + this.getStageTip(currentDay)),
                x, cy += 13, COLOR_INK_HI, false);
    }

    // ===================== P5: 血月周期 =====================

    private void renderP5BloodMoonCycle(DrawContext ctx, TextRenderer tr, World world, int x, int y, int w) {
        boolean isBloodMoon = StageSystem.isBloodMoon(world);
        int daysToBM = StageSystem.getDaysToNextBloodMoon(world);
        int cy = y;
        this.drawSectionTitle(ctx, tr, x, cy, w, "血月周期", COLOR_BLOOD_MOON);
        String bmText = isBloodMoon ? "今日血月" : "~" + daysToBM + "天后";
        this.drawRow(ctx, tr, x, cy += 13, w, "下次血月", bmText,
                isBloodMoon ? COLOR_BLOOD_MOON : COLOR_INK);
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "当前状态",
                isBloodMoon ? "进行中" : "未激活",
                isBloodMoon ? COLOR_BLOOD_MOON : COLOR_INK_DIM);
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "刷新倍率",
                isBloodMoon ? "x2.0" : "x1.0",
                isBloodMoon ? COLOR_BLOOD_MOON : COLOR_INK_DIM);
        cy += ROW_H + 4;

        this.drawSectionTitle(ctx, tr, x, cy, w, "说明", COLOR_BLUE);
        this.drawBullet(ctx, tr, x, cy += 13, "血月为随机刷新", COLOR_INK);
        this.drawBullet(ctx, tr, x, cy += ROW_H, "请提前做好防御准备", COLOR_INK_DIM);
    }

    // ===================== P6: 血月效果 =====================

    private void renderP6BloodMoonEffect(DrawContext ctx, TextRenderer tr, World world, int x, int y, int w) {
        int cy = y;
        this.drawSectionTitle(ctx, tr, x, cy, w, "血月效果", COLOR_BLOOD_MOON);
        cy += 13;
        this.drawBullet(ctx, tr, x, cy, "僵尸刷新翻倍", COLOR_BLOOD_MOON); cy += ROW_H;
        this.drawBullet(ctx, tr, x, cy, "僵尸速度 +30%", COLOR_BLOOD_MOON); cy += ROW_H;
        this.drawBullet(ctx, tr, x, cy, "僵尸攻击 +20%", COLOR_BLOOD_MOON); cy += ROW_H;
        this.drawBullet(ctx, tr, x, cy, "巨型僵尸概率提升", COLOR_GIANT); cy += ROW_H + 4;

        this.drawSectionTitle(ctx, tr, x, cy, w, "生存提示", COLOR_BLUE);
        int currentDay = StageSystem.getCurrentStage(world);
        boolean isBloodMoon = StageSystem.isBloodMoon(world);
        cy += 13;
        for (String t : this.getSurvivalTips(currentDay, isBloodMoon)) {
            this.drawBullet(ctx, tr, x, cy, t, COLOR_INK);
            cy += ROW_H;
        }
    }

    // ===================== P7: 当前智能度 =====================

    private void renderP7IntelCurrent(DrawContext ctx, TextRenderer tr, World world, int x, int y, int w) {
        int intelLevel = StageSystem.getIntelligenceLevel(world);
        int followRange = StageSystem.getFollowRange(world);
        String[] intelNames = {"迟钝", "普通", "机敏", "狡猾", "凶残", "嗜血"};
        int[] intelColors = {COLOR_GREEN, COLOR_GOLD, COLOR_GOLD, COLOR_DANGER, COLOR_BLOOD_MOON, COLOR_BLOOD_MOON};

        int cy = y;
        this.drawSectionTitle(ctx, tr, x, cy, w, "当前智能度", COLOR_ACCENT);
        this.drawRow(ctx, tr, x, cy += 13, w, "等级", "Lv" + intelLevel, intelColors[intelLevel]);
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "命名", intelNames[intelLevel], intelColors[intelLevel]);
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "追踪范围", followRange + "格", COLOR_INK);
        cy += ROW_H + 4;

        this.drawSectionTitle(ctx, tr, x, cy, w, "说明", COLOR_BLUE);
        this.drawBullet(ctx, tr, x, cy += 13, "智能度随天数提升", COLOR_INK);
        this.drawBullet(ctx, tr, x, cy += ROW_H, "高智能度更危险", COLOR_DANGER);
    }

    // ===================== P8: AI能力 =====================

    private void renderP8IntelAbility(DrawContext ctx, TextRenderer tr, World world, int x, int y, int w) {
        int breakInt = StageSystem.getBreakInterval(world);
        int buildInt = StageSystem.getBuildInterval(world);
        float hardLimit = StageSystem.getHardnessLimit(world);
        int invSize = StageSystem.getBlockInventorySize(world);
        double reinChance = StageSystem.getReinforcementChance(world);

        int cy = y;
        this.drawSectionTitle(ctx, tr, x, cy, w, "AI能力参数", COLOR_INK_HI);
        this.drawRow(ctx, tr, x, cy += 13, w, "拆方块间隔", String.format("%.1fs", breakInt / 20.0), COLOR_INK);
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "搭方块间隔", String.format("%.1fs", buildInt / 20.0), COLOR_INK);
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "硬度上限", String.format("%.0f", hardLimit), COLOR_INK);
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "库存容量", invSize + "格", COLOR_INK);
        String reinStr = reinChance > 0 ? String.format("%.0f%%", reinChance * 100) : "未解锁";
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "呼叫增援", reinStr,
                reinChance > 0 ? COLOR_DANGER : COLOR_INK_DIM);
    }

    // ===================== P9: 行为说明 =====================

    private void renderP9IntelBehavior(DrawContext ctx, TextRenderer tr, World world, int x, int y, int w) {
        int cy = y;
        this.drawSectionTitle(ctx, tr, x, cy, w, "AI行为说明", COLOR_GOLD);
        cy += 13;
        this.drawBullet(ctx, tr, x, cy, "第20天后开始拆搭方块", COLOR_INK); cy += ROW_H;
        this.drawBullet(ctx, tr, x, cy, "优先破坏门窗玻璃", COLOR_INK); cy += ROW_H;
        this.drawBullet(ctx, tr, x, cy, "朝玩家方向搭楼梯追击", COLOR_INK); cy += ROW_H;
        this.drawBullet(ctx, tr, x, cy, "空中搭天桥追击", COLOR_INK); cy += ROW_H;
        this.drawBullet(ctx, tr, x, cy, "卡住时加速突破", COLOR_DANGER); cy += ROW_H;
        this.drawBullet(ctx, tr, x, cy, "玩家在上方时塔式堆叠", COLOR_DANGER); cy += ROW_H;
        this.drawBullet(ctx, tr, x, cy, "玩家在下方时破坏头顶", COLOR_DANGER);
    }

    // ===================== P10: 智能度阶梯 =====================

    private void renderP10IntelTier(DrawContext ctx, TextRenderer tr, World world, int x, int y, int w) {
        int intelLevel = StageSystem.getIntelligenceLevel(world);
        int cy = y;
        this.drawSectionTitle(ctx, tr, x, cy, w, "智能度阶梯 (6级)", COLOR_ACCENT);
        cy += 13;
        String[][] tiers = {
            {"Lv0  1-10天   迟钝", "FF2A6B2A"},
            {"Lv1  11-20天  普通", "FF8B6914"},
            {"Lv2  21-30天  机敏", "FF8B6914"},
            {"Lv3  31-50天  狡猾", "FFB01818"},
            {"Lv4  51-70天  凶残", "FFCC3030"},
            {"Lv5  71-100天 嗜血", "FFCC3030"}
        };
        for (String[] t : tiers) {
            ctx.drawText(tr, Text.literal(t[0]), x, cy,
                    Integer.parseUnsignedInt(t[1], 16), false);
            cy += ROW_H;
        }
        cy += 4;
        String[] intelNames = {"迟钝", "普通", "机敏", "狡猾", "凶残", "嗜血"};
        int[] intelColors = {COLOR_GREEN, COLOR_GOLD, COLOR_GOLD, COLOR_DANGER, COLOR_BLOOD_MOON, COLOR_BLOOD_MOON};
        ctx.drawText(tr, Text.literal("-> 当前 Lv" + intelLevel + " " + intelNames[intelLevel]),
                x, cy, intelColors[intelLevel], false);
    }

    // ===================== P11: 僵尸基础属性 =====================

    private void renderP11ZombieBasic(DrawContext ctx, TextRenderer tr, World world, int x, int y, int w) {
        double health = StageSystem.getZombieHealth(world);
        double attack = StageSystem.getZombieAttack(world);
        double armor = StageSystem.getZombieArmor(world);
        double speed = StageSystem.getZombieSpeed(world);
        double atkMult = StageSystem.getAttackMultiplier(world);
        double spdMult = StageSystem.getSpeedMultiplier(world, 1.0f);

        int cy = y;
        this.drawSectionTitle(ctx, tr, x, cy, w, "基础属性 (当前)", COLOR_ZOMBIE);
        this.drawRowExtra(ctx, tr, x, cy += 13, w, "血量",
                String.format("%.0f", health), String.format("(x%.1f)", health / 20.0), COLOR_DANGER);
        this.drawRowExtra(ctx, tr, x, cy += ROW_H, w, "攻击",
                String.format("%.1f", attack), atkMult > 1 ? String.format("x%.1f", atkMult) : "", COLOR_INK_HI);
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "护甲", String.format("%.0f", armor), COLOR_INK);
        this.drawRowExtra(ctx, tr, x, cy += ROW_H, w, "速度",
                String.format("%.2f", speed), spdMult > 1 ? String.format("x%.2f", spdMult) : "", COLOR_INK);
    }

    // ===================== P12: 属性范围 =====================

    private void renderP12ZombieRange(DrawContext ctx, TextRenderer tr, World world, int x, int y, int w) {
        int cy = y;
        this.drawSectionTitle(ctx, tr, x, cy, w, "属性范围 (第1天 -> 第100天)", COLOR_GOLD);
        this.drawRow(ctx, tr, x, cy += 13, w, "血量", "20 -> 400", COLOR_INK);
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "攻击", "3 -> 15", COLOR_INK);
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "护甲", "2 -> 12", COLOR_INK);
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "速度", "0.23 -> 0.35", COLOR_INK);
        cy += ROW_H + 4;

        this.drawSectionTitle(ctx, tr, x, cy, w, "说明", COLOR_BLUE);
        this.drawBullet(ctx, tr, x, cy += 13, "属性随天数线性增长", COLOR_INK);
        this.drawBullet(ctx, tr, x, cy += ROW_H, "血月时额外提升", COLOR_BLOOD_MOON);
    }

    // ===================== P13: 其他特性 =====================

    private void renderP13ZombieOther(DrawContext ctx, TextRenderer tr, World world, int x, int y, int w) {
        int cy = y;
        this.drawSectionTitle(ctx, tr, x, cy, w, "其他特性", COLOR_BLUE);
        cy += 13;
        this.drawBullet(ctx, tr, x, cy, "防白天燃烧", COLOR_INK); cy += ROW_H;
        this.drawBullet(ctx, tr, x, cy, "白天也可生成 (倍率0.6)", COLOR_INK); cy += ROW_H;
        this.drawBullet(ctx, tr, x, cy, "低血量(<30%)狂暴 +50%速", COLOR_DANGER); cy += ROW_H + 4;

        this.drawSectionTitle(ctx, tr, x, cy, w, "应对建议", COLOR_GOLD);
        this.drawBullet(ctx, tr, x, cy += 13, "白天仍需警惕", COLOR_INK_DIM);
        this.drawBullet(ctx, tr, x, cy += ROW_H, "保持满血避免狂暴", COLOR_INK_DIM);
    }

    // ===================== P14: 当前加成 =====================

    private void renderP14BuffCurrent(DrawContext ctx, TextRenderer tr, World world, int x, int y, int w) {
        boolean isBloodMoon = StageSystem.isBloodMoon(world);
        boolean isNight = !world.isDay();
        int cy = y;
        this.drawSectionTitle(ctx, tr, x, cy, w, "当前加成状态", COLOR_BLUE);
        this.drawRow(ctx, tr, x, cy += 13, w, "夜晚速度",
                isNight ? "+15% 激活" : "未激活",
                isNight ? COLOR_GREEN : COLOR_INK_DIM);
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "血月速度",
                isBloodMoon ? "+30% 激活" : "未激活",
                isBloodMoon ? COLOR_BLOOD_MOON : COLOR_INK_DIM);
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "血月攻击",
                isBloodMoon ? "+20% 激活" : "未激活",
                isBloodMoon ? COLOR_BLOOD_MOON : COLOR_INK_DIM);
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "低血狂暴", "<30% +50%速", COLOR_DANGER);
    }

    // ===================== P15: 血月加成 =====================

    private void renderP15BuffBloodMoon(DrawContext ctx, TextRenderer tr, World world, int x, int y, int w) {
        int cy = y;
        this.drawSectionTitle(ctx, tr, x, cy, w, "血月加成详情", COLOR_BLOOD_MOON);
        this.drawRow(ctx, tr, x, cy += 13, w, "攻击倍率", "+20%", COLOR_BLOOD_MOON);
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "速度倍率", "+30%", COLOR_BLOOD_MOON);
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "刷新倍率", "x2.0", COLOR_BLOOD_MOON);
        cy += ROW_H + 4;

        this.drawSectionTitle(ctx, tr, x, cy, w, "血月应对", COLOR_DANGER);
        this.drawBullet(ctx, tr, x, cy += 13, "加固门窗死守", COLOR_DANGER);
        this.drawBullet(ctx, tr, x, cy += ROW_H, "准备充足武器药水", COLOR_DANGER);
    }

    // ===================== P16: 夜晚加成 =====================

    private void renderP16BuffNight(DrawContext ctx, TextRenderer tr, World world, int x, int y, int w) {
        boolean isNight = !world.isDay();
        int cy = y;
        this.drawSectionTitle(ctx, tr, x, cy, w, "夜晚加成详情", COLOR_GREEN);
        this.drawRow(ctx, tr, x, cy += 13, w, "速度倍率", "+15%", COLOR_GREEN);
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "生成密度", "提高", COLOR_GREEN);
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "当前状态",
                isNight ? "夜晚已激活" : "白天未激活",
                isNight ? COLOR_GREEN : COLOR_INK_DIM);
        cy += ROW_H + 4;

        this.drawSectionTitle(ctx, tr, x, cy, w, "说明", COLOR_BLUE);
        this.drawBullet(ctx, tr, x, cy += 13, "夜晚僵尸更活跃", COLOR_INK);
        this.drawBullet(ctx, tr, x, cy += ROW_H, "建议夜晚待在室内", COLOR_INK_DIM);
    }

    // ===================== P17: 狂暴机制 =====================

    private void renderP17Rage(DrawContext ctx, TextRenderer tr, World world, int x, int y, int w) {
        int cy = y;
        this.drawSectionTitle(ctx, tr, x, cy, w, "狂暴机制", COLOR_DANGER);
        cy += 13;
        this.drawBullet(ctx, tr, x, cy, "血量低于30%时触发", COLOR_INK); cy += ROW_H;
        this.drawBullet(ctx, tr, x, cy, "速度额外 +50%", COLOR_DANGER); cy += ROW_H;
        this.drawBullet(ctx, tr, x, cy, "无视冷却加速突破", COLOR_DANGER); cy += ROW_H;
        this.drawBullet(ctx, tr, x, cy, "高智能度更易触发", COLOR_BLOOD_MOON); cy += ROW_H + 4;

        this.drawSectionTitle(ctx, tr, x, cy, w, "狂暴应对", COLOR_GOLD);
        this.drawBullet(ctx, tr, x, cy += 13, "保持满血避免触发", COLOR_INK);
        this.drawBullet(ctx, tr, x, cy += ROW_H, "远程武器优先击杀", COLOR_INK);
    }

    // ===================== P18: 行为特性 =====================

    private void renderP18Behavior(DrawContext ctx, TextRenderer tr, World world, int x, int y, int w) {
        int cy = y;
        this.drawSectionTitle(ctx, tr, x, cy, w, "行为特性", COLOR_INK_HI);
        cy += 13;
        this.drawBullet(ctx, tr, x, cy, "防白天燃烧", COLOR_INK); cy += ROW_H;
        this.drawBullet(ctx, tr, x, cy, "第20天后开始拆搭方块", COLOR_INK); cy += ROW_H;
        this.drawBullet(ctx, tr, x, cy, "优先破坏门窗玻璃", COLOR_INK); cy += ROW_H;
        this.drawBullet(ctx, tr, x, cy, "卡住时加速突破", COLOR_INK); cy += ROW_H;
        this.drawBullet(ctx, tr, x, cy, "朝玩家方向搭楼梯", COLOR_INK); cy += ROW_H;
        this.drawBullet(ctx, tr, x, cy, "空中搭天桥追击", COLOR_INK); cy += ROW_H;
        this.drawBullet(ctx, tr, x, cy, "高墙无法完全阻挡", COLOR_DANGER);
    }

    // ===================== P19: 巨型属性 =====================

    private void renderP19GiantStats(DrawContext ctx, TextRenderer tr, World world, int x, int y, int w) {
        double health = StageSystem.getGiantZombieHealth(world);
        double attack = StageSystem.getGiantZombieAttack(world);
        double chance = StageSystem.getGiantZombieChance(world);
        int followRange = StageSystem.getFollowRange(world);
        double atkMult = StageSystem.getAttackMultiplier(world);

        int cy = y;
        this.drawSectionTitle(ctx, tr, x, cy, w, "巨型属性 (2倍大小)", COLOR_GIANT);
        this.drawRowExtra(ctx, tr, x, cy += 13, w, "血量",
                String.format("%.0f", health), String.format("(x%.1f)", health / 400.0), COLOR_GIANT);
        this.drawRowExtra(ctx, tr, x, cy += ROW_H, w, "攻击",
                String.format("%.1f", attack), atkMult > 1 ? String.format("x%.1f", atkMult) : "", COLOR_GIANT);
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "生成概率", String.format("%.1f%%", chance * 100), COLOR_GIANT);
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "追踪范围", followRange + "格", COLOR_INK);
    }

    // ===================== P20: 特殊能力 =====================

    private void renderP20GiantAbility(DrawContext ctx, TextRenderer tr, World world, int x, int y, int w) {
        int cy = y;
        this.drawSectionTitle(ctx, tr, x, cy, w, "特殊能力", COLOR_INK_HI);
        cy += 13;
        this.drawBullet(ctx, tr, x, cy, "2.5倍攻击距离", COLOR_GIANT); cy += ROW_H;
        this.drawBullet(ctx, tr, x, cy, "可跨越2格高墙", COLOR_GIANT); cy += ROW_H;
        this.drawBullet(ctx, tr, x, cy, "防火免疫", COLOR_GREEN); cy += ROW_H;
        this.drawBullet(ctx, tr, x, cy, "血月时属性增强", COLOR_BLOOD_MOON); cy += ROW_H + 4;

        this.drawSectionTitle(ctx, tr, x, cy, w, "威胁说明", COLOR_DANGER);
        this.drawBullet(ctx, tr, x, cy += 13, "近战极难对抗", COLOR_DANGER);
        this.drawBullet(ctx, tr, x, cy += ROW_H, "务必保持距离", COLOR_DANGER);
    }

    // ===================== P21: 出现条件 =====================

    private void renderP21GiantCondition(DrawContext ctx, TextRenderer tr, World world, int x, int y, int w) {
        int cy = y;
        this.drawSectionTitle(ctx, tr, x, cy, w, "出现条件", COLOR_GOLD);
        this.drawRow(ctx, tr, x, cy += 13, w, "起始天数", "第40天", COLOR_INK_HI);
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "血月加成", "概率+属性提升", COLOR_BLOOD_MOON);
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "威胁等级", "极高", COLOR_DANGER);
        cy += ROW_H + 4;

        this.drawSectionTitle(ctx, tr, x, cy, w, "注意", COLOR_DANGER);
        this.drawBullet(ctx, tr, x, cy += 13, "第40天后随时可能出现", COLOR_DANGER);
        this.drawBullet(ctx, tr, x, cy += ROW_H, "血月时更需警惕", COLOR_BLOOD_MOON);
    }

    // ===================== P22: 掉落物 =====================

    private void renderP22GiantDrops(DrawContext ctx, TextRenderer tr, World world, int x, int y, int w) {
        int cy = y;
        this.drawSectionTitle(ctx, tr, x, cy, w, "掉落物一览", COLOR_GOLD);
        cy += 13;
        this.drawBullet(ctx, tr, x, cy, "腐肉 3-8", COLOR_INK); cy += ROW_H;
        this.drawBullet(ctx, tr, x, cy, "骨头 3-6", COLOR_INK); cy += ROW_H;
        this.drawBullet(ctx, tr, x, cy, "铁锭 2-5", COLOR_INK); cy += ROW_H;
        this.drawBullet(ctx, tr, x, cy, "金锭 1-3", COLOR_INK); cy += ROW_H;
        this.drawBullet(ctx, tr, x, cy, "钻石 0-2", COLOR_INK_HI); cy += ROW_H;
        this.drawBullet(ctx, tr, x, cy, "绿宝石 0-3", COLOR_INK_HI); cy += ROW_H;
        this.drawBullet(ctx, tr, x, cy, "附魔金苹果 0-1", COLOR_GIANT); cy += ROW_H;
        this.drawBullet(ctx, tr, x, cy, "经验瓶 1-5", COLOR_GREEN);
    }

    // ===================== P23: 掉落说明 =====================

    private void renderP23GiantDropNote(DrawContext ctx, TextRenderer tr, World world, int x, int y, int w) {
        int cy = y;
        this.drawSectionTitle(ctx, tr, x, cy, w, "掉落说明", COLOR_BLUE);
        cy += 13;
        this.drawBullet(ctx, tr, x, cy, "击杀后掉落物散落地面", COLOR_INK); cy += ROW_H;
        this.drawBullet(ctx, tr, x, cy, "附魔金苹果概率较低", COLOR_GIANT); cy += ROW_H;
        this.drawBullet(ctx, tr, x, cy, "经验瓶可获大量经验", COLOR_GREEN); cy += ROW_H;
        this.drawBullet(ctx, tr, x, cy, "掉落数量随天数增加", COLOR_INK_DIM); cy += ROW_H + 4;

        this.drawSectionTitle(ctx, tr, x, cy, w, "拾取建议", COLOR_GOLD);
        this.drawBullet(ctx, tr, x, cy += 13, "击杀后迅速拾取", COLOR_INK_DIM);
        this.drawBullet(ctx, tr, x, cy += ROW_H, "注意周围其他僵尸", COLOR_DANGER);
    }

    // ===================== P24: 应对建议 =====================

    private void renderP24GiantAdvice(DrawContext ctx, TextRenderer tr, World world, int x, int y, int w) {
        int cy = y;
        this.drawSectionTitle(ctx, tr, x, cy, w, "应对建议", COLOR_DANGER);
        cy += 13;
        this.drawBullet(ctx, tr, x, cy, "保持距离用弓箭攻击", COLOR_INK); cy += ROW_H;
        this.drawBullet(ctx, tr, x, cy, "利用地形卡位击杀", COLOR_INK); cy += ROW_H;
        this.drawBullet(ctx, tr, x, cy, "准备附魔钻石装备", COLOR_DANGER); cy += ROW_H;
        this.drawBullet(ctx, tr, x, cy, "血月时尽量避免交战", COLOR_BLOOD_MOON); cy += ROW_H;
        this.drawBullet(ctx, tr, x, cy, "多人协作更易击杀", COLOR_INK); cy += ROW_H + 4;

        this.drawSectionTitle(ctx, tr, x, cy, w, "结语", COLOR_GOLD);
        ctx.drawText(tr, Text.literal("祝君好运, 活过100天!"), x, cy += 13, COLOR_GOLD, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.prevHovered && this.currentPage > 0) {
            this.currentPage--;
            return true;
        }
        if (this.nextHovered && this.currentPage < TOTAL_PAGES - 1) {
            this.currentPage++;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
