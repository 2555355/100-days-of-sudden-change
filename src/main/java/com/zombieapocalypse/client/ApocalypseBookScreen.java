package com.zombieapocalypse.client;

import com.zombieapocalypse.config.ModConfig;
import com.zombieapocalypse.config.StageSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

/**
 * 末日情报书界面 - v6.0.0
 * 全面单页显示: 整页作为一页内容, 不再分左右双页。
 * 6页独立内容, 顶部标题, 中部内容, 底部翻页按钮。
 *
 * 页面规划:
 *   1. 封面/目录
 *   2. 末日历法
 *   3. 智能与AI
 *   4. 僵尸档案
 *   5. 战斗特性
 *   6. 巨型僵尸
 */
public class ApocalypseBookScreen extends Screen {

    // 书本背景纹理 (原版 minecraft:textures/gui/book.png)
    private static final Identifier BOOK_TEXTURE = new Identifier("minecraft", "textures/gui/book.png");
    private static final int BOOK_W = 292;
    private static final int BOOK_H = 180;

    // 书本整页可写区域 (单页布局, 使用整张书页宽度)
    private static final int PAGE_X = 20;            // 文字左边距
    private static final int PAGE_TOP = 14;          // 文字上边距
    private static final int PAGE_BOTTOM = 168;      // 文字下边距 (留出翻页按钮空间)
    private static final int PAGE_W = 252;           // 文字可用宽度 (292 - 20*2)
    private static final int PAGE_INNER_W = 252;     // 内容绘制区宽度
    private static final int ROW_H = 11;             // 行高

    // 翻页按钮位置
    private static final int BTN_W = 23;
    private static final int BTN_H = 12;
    private static final int TOTAL_PAGES = 6;

    // 墨色配色 (高对比度, 适合书页底色)
    private static final int COLOR_BG_DARK    = 0xB0000000;
    private static final int COLOR_INK        = 0xFF2A1A0A;
    private static final int COLOR_INK_DIM    = 0xFF6B5238;
    private static final int COLOR_INK_HI     = 0xFF8B2A0A;
    private static final int COLOR_ACCENT     = 0xFFA02020;
    private static final int COLOR_DANGER     = 0xFFB01818;
    private static final int COLOR_BLOOD_MOON = 0xFFCC0000;
    private static final int COLOR_ZOMBIE     = 0xFF8B3A1A;
    private static final int COLOR_GIANT      = 0xFF7A2A6B;
    private static final int COLOR_GOLD       = 0xFF8B6914;
    private static final int COLOR_GREEN      = 0xFF2A6B2A;
    private static final int COLOR_BLUE       = 0xFF1A3A6B;
    private static final int COLOR_DIVIDER    = 0x558B6B45;

    // 6页章节标题
    private static final String[] PAGE_TITLES = {
            "末日情报 · 目录", "末日历法", "智能与AI", "僵尸档案", "战斗特性", "巨型僵尸"
    };
    private static final int[] PAGE_COLORS = {
            COLOR_GOLD, COLOR_GOLD, COLOR_ACCENT, COLOR_ZOMBIE, COLOR_INK_HI, COLOR_GIANT
    };

    private int bookX, bookY;
    private int currentPage = 0;
    private int prevBtnX, prevBtnY, nextBtnX, nextBtnY;
    private boolean prevHovered, nextHovered;

    public ApocalypseBookScreen() {
        super(Text.literal("末日情报"));
    }

    @Override
    protected void init() {
        bookX = (this.width - BOOK_W) / 2;
        bookY = (this.height - BOOK_H) / 2;
        prevBtnX = bookX + 18;
        nextBtnX = bookX + BOOK_W - 18 - BTN_W;
        prevBtnY = nextBtnY = bookY + BOOK_H - BTN_H - 3;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 半透明背景
        context.fill(0, 0, this.width, this.height, COLOR_BG_DARK);

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        World world = client.player.getWorld();
        if (world == null) return;

        TextRenderer tr = client.textRenderer;

        // 书本背景纹理 (9参数避免256回绕)
        context.drawTexture(BOOK_TEXTURE, bookX, bookY, 0, 0, BOOK_W, BOOK_H, BOOK_W, BOOK_H);

        // 整页内容区 (用scissor裁剪到书页可写区域)
        int contentX = bookX + PAGE_X;
        int contentY = bookY + PAGE_TOP;
        int contentW = PAGE_INNER_W;
        int contentH = PAGE_BOTTOM - PAGE_TOP;

        context.enableScissor(bookX, bookY, bookX + BOOK_W, bookY + BOOK_H);
        renderPage(context, tr, world, contentX, contentY, contentW, contentH, currentPage);
        context.disableScissor();

        // 翻页按钮
        prevHovered = mouseX >= prevBtnX && mouseX <= prevBtnX + BTN_W
                && mouseY >= prevBtnY && mouseY <= prevBtnY + BTN_H;
        nextHovered = mouseX >= nextBtnX && mouseX <= nextBtnX + BTN_W
                && mouseY >= nextBtnY && mouseY <= nextBtnY + BTN_H;
        drawPageButton(context, tr, prevBtnX, prevBtnY, "◀", currentPage > 0, prevHovered);
        drawPageButton(context, tr, nextBtnX, nextBtnY, "▶", currentPage < TOTAL_PAGES - 1, nextHovered);

        // 页码
        String pageInfo = (currentPage + 1) + " / " + TOTAL_PAGES;
        context.drawCenteredTextWithShadow(tr, Text.literal(pageInfo),
                bookX + BOOK_W / 2, bookY + BOOK_H - 13, COLOR_INK_DIM);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (prevHovered && currentPage > 0) { currentPage--; return true; }
        if (nextHovered && currentPage < TOTAL_PAGES - 1) { currentPage++; return true; }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() { return false; }

    // ===================== 内容分发 =====================

    private void renderPage(DrawContext ctx, TextRenderer tr, World world,
                            int x, int y, int w, int h, int page) {
        // 顶部标题
        drawTitle(ctx, tr, x, y, w, PAGE_TITLES[page], PAGE_COLORS[page]);

        // 内容从标题下方开始
        int contentY = y + 16;

        switch (page) {
            case 0 -> renderCoverPage(ctx, tr, world, x, contentY, w);
            case 1 -> renderCalendarPage(ctx, tr, world, x, contentY, w);
            case 2 -> renderIntelligencePage(ctx, tr, world, x, contentY, w);
            case 3 -> renderZombieStatsPage(ctx, tr, world, x, contentY, w);
            case 4 -> renderZombieBehaviorPage(ctx, tr, world, x, contentY, w);
            case 5 -> renderGiantPage(ctx, tr, world, x, contentY, w);
        }
    }

    private void drawTitle(DrawContext ctx, TextRenderer tr, int x, int y, int w,
                           String title, int color) {
        ctx.drawCenteredTextWithShadow(tr, Text.literal(title), x + w / 2, y, color);
        ctx.fill(x + 60, y + 11, x + w - 60, y + 12, color);
    }

    // ===================== 第1页: 封面/目录 =====================

    private void renderCoverPage(DrawContext ctx, TextRenderer tr, World world, int x, int y, int w) {
        int currentDay = StageSystem.getCurrentStage(world);
        boolean isBloodMoon = StageSystem.isBloodMoon(world);
        int intelLevel = StageSystem.getIntelligenceLevel(world);

        int cy = y;
        drawSectionTitle(ctx, tr, x, cy, w, "当前状态", COLOR_GOLD); cy += 13;
        drawRow(ctx, tr, x, cy, w, "天数", currentDay + " / " + ModConfig.TOTAL_DAYS, COLOR_INK_HI); cy += ROW_H;
        String bmText = isBloodMoon ? "进行中" : "~" + StageSystem.getDaysToNextBloodMoon(world) + "天后";
        drawRow(ctx, tr, x, cy, w, "血月", bmText, isBloodMoon ? COLOR_BLOOD_MOON : COLOR_INK); cy += ROW_H;
        String[] intelNames = {"迟钝", "普通", "机敏", "狡猾", "凶残", "嗜血"};
        drawRow(ctx, tr, x, cy, w, "智能度", "Lv" + intelLevel + " " + intelNames[intelLevel],
                intelLevel >= 4 ? COLOR_DANGER : COLOR_INK_HI); cy += ROW_H;
        drawRow(ctx, tr, x, cy, w, "阶段", getStageTip(currentDay), COLOR_INK_HI); cy += ROW_H + 4;

        drawSectionTitle(ctx, tr, x, cy, w, "目录索引", COLOR_ACCENT); cy += 13;
        String[][] toc = {
                {"P2", "末日历法"},
                {"P3", "智能与AI"},
                {"P4", "僵尸档案"},
                {"P5", "战斗特性"},
                {"P6", "巨型僵尸"}
        };
        for (String[] e : toc) {
            ctx.drawTextWithShadow(tr, Text.literal(e[0] + "    " + e[1]), x, cy, COLOR_INK); cy += ROW_H;
        }
    }

    // ===================== 第2页: 末日历法 =====================

    private void renderCalendarPage(DrawContext ctx, TextRenderer tr, World world, int x, int y, int w) {
        int currentDay = StageSystem.getCurrentStage(world);
        double progress = StageSystem.getStageProgress(world);
        boolean isBloodMoon = StageSystem.isBloodMoon(world);

        int cy = y;
        drawSectionTitle(ctx, tr, x, cy, w, "末日历法", COLOR_GOLD); cy += 13;
        drawRow(ctx, tr, x, cy, w, "当前天数", currentDay + " / " + ModConfig.TOTAL_DAYS, COLOR_INK_HI); cy += ROW_H;
        drawRow(ctx, tr, x, cy, w, "阶段", getStageTip(currentDay), COLOR_INK_HI); cy += ROW_H + 2;

        // 进度条
        int barX = x;
        int barW = w;
        ctx.fill(barX, cy, barX + barW, cy + 1, 0xFF8B6B45);
        int filledW = (int) (barW * progress);
        if (filledW > 0) ctx.fill(barX, cy, barX + filledW, cy + 1, getProgressColor(progress));
        ctx.drawTextWithShadow(tr, Text.literal((int)(progress * 100) + "%"),
                x + w / 2 - 5, cy + 2, COLOR_INK_DIM);
        cy += 12;

        drawSectionTitle(ctx, tr, x, cy, w, "血月周期", COLOR_BLOOD_MOON); cy += 13;
        int daysToBM = StageSystem.getDaysToNextBloodMoon(world);
        String bmText = isBloodMoon ? "今日血月" : "~" + daysToBM + "天后";
        drawRow(ctx, tr, x, cy, w, "下次血月", bmText, isBloodMoon ? COLOR_BLOOD_MOON : COLOR_INK); cy += ROW_H;
        drawRow(ctx, tr, x, cy, w, "刷新倍率", isBloodMoon ? "x2.0" : "x1.0",
                isBloodMoon ? COLOR_BLOOD_MOON : COLOR_INK_DIM); cy += ROW_H + 4;

        drawSectionTitle(ctx, tr, x, cy, w, "生存提示", COLOR_BLUE); cy += 13;
        String[][] tips = getSurvivalTips(currentDay, isBloodMoon);
        for (String[] t : tips) {
            ctx.drawTextWithShadow(tr, Text.literal("• " + t[0]), x, cy,
                    Integer.parseUnsignedInt(t[1], 16));
            cy += ROW_H;
        }
    }

    // ===================== 第3页: 智能与AI =====================

    private void renderIntelligencePage(DrawContext ctx, TextRenderer tr, World world, int x, int y, int w) {
        int intelLevel = StageSystem.getIntelligenceLevel(world);
        int breakInt = StageSystem.getBreakInterval(world);
        int buildInt = StageSystem.getBuildInterval(world);
        float hardLimit = StageSystem.getHardnessLimit(world);
        int invSize = StageSystem.getBlockInventorySize(world);
        double reinChance = StageSystem.getReinforcementChance(world);
        int followRange = StageSystem.getFollowRange(world);

        int cy = y;
        drawSectionTitle(ctx, tr, x, cy, w, "当前智能度", COLOR_ACCENT); cy += 13;
        String[] intelNames = {"迟钝", "普通", "机敏", "狡猾", "凶残", "嗜血"};
        int[] intelColors = {COLOR_GREEN, COLOR_GOLD, 0xFF8B6914, COLOR_DANGER, COLOR_BLOOD_MOON, COLOR_BLOOD_MOON};
        drawRow(ctx, tr, x, cy, w, "等级", "Lv" + intelLevel, intelColors[intelLevel]); cy += ROW_H;
        drawRow(ctx, tr, x, cy, w, "命名", intelNames[intelLevel], intelColors[intelLevel]); cy += ROW_H;
        drawRow(ctx, tr, x, cy, w, "追踪范围", followRange + "格", COLOR_INK); cy += ROW_H + 4;

        drawSectionTitle(ctx, tr, x, cy, w, "AI能力", COLOR_INK_HI); cy += 13;
        drawRow(ctx, tr, x, cy, w, "拆方块间隔", String.format("%.1fs", breakInt / 20.0), COLOR_INK); cy += ROW_H;
        drawRow(ctx, tr, x, cy, w, "搭方块间隔", String.format("%.1fs", buildInt / 20.0), COLOR_INK); cy += ROW_H;
        drawRow(ctx, tr, x, cy, w, "硬度上限", String.format("%.0f", hardLimit), COLOR_INK); cy += ROW_H;
        drawRow(ctx, tr, x, cy, w, "库存容量", invSize + "格", COLOR_INK); cy += ROW_H;
        String reinStr = reinChance > 0 ? String.format("%.0f%%", reinChance * 100) : "未解锁";
        drawRow(ctx, tr, x, cy, w, "呼叫增援", reinStr,
                reinChance > 0 ? COLOR_DANGER : COLOR_INK_DIM); cy += ROW_H + 4;

        drawSectionTitle(ctx, tr, x, cy, w, "智能度阶梯", COLOR_GOLD); cy += 13;
        String[][] tiers = {
                {"Lv0 1-10天  迟钝", "FF2A6B2A"},
                {"Lv1 11-20天 普通", "FF8B6914"},
                {"Lv2 21-30天 机敏", "FF8B6914"},
                {"Lv3 31-50天 狡猾", "FFB01818"}
        };
        for (String[] t : tiers) {
            ctx.drawTextWithShadow(tr, Text.literal(t[0]), x, cy,
                    Integer.parseUnsignedInt(t[1], 16));
            cy += ROW_H;
        }
    }

    // ===================== 第4页: 僵尸档案 =====================

    private void renderZombieStatsPage(DrawContext ctx, TextRenderer tr, World world, int x, int y, int w) {
        double health = StageSystem.getZombieHealth(world);
        double attack = StageSystem.getZombieAttack(world);
        double armor = StageSystem.getZombieArmor(world);
        double speed = StageSystem.getZombieSpeed(world);
        double atkMult = StageSystem.getAttackMultiplier(world);
        double spdMult = StageSystem.getSpeedMultiplier(world, 1.0f);

        int cy = y;
        drawSectionTitle(ctx, tr, x, cy, w, "基础属性", COLOR_ZOMBIE); cy += 13;
        drawRowExtra(ctx, tr, x, cy, w, "血量", String.format("%.0f", health),
                String.format("(x%.1f)", health / 20), COLOR_DANGER); cy += ROW_H;
        drawRowExtra(ctx, tr, x, cy, w, "攻击", String.format("%.1f", attack),
                atkMult > 1 ? String.format("x%.1f", atkMult) : "", COLOR_INK_HI); cy += ROW_H;
        drawRow(ctx, tr, x, cy, w, "护甲", String.format("%.0f", armor), COLOR_INK); cy += ROW_H;
        drawRowExtra(ctx, tr, x, cy, w, "速度", String.format("%.2f", speed),
                spdMult > 1 ? String.format("x%.2f", spdMult) : "", COLOR_INK); cy += ROW_H + 4;

        drawSectionTitle(ctx, tr, x, cy, w, "属性范围 (第1天 → 第100天)", COLOR_GOLD); cy += 13;
        drawRow(ctx, tr, x, cy, w, "血量", "20 → 400", COLOR_INK); cy += ROW_H;
        drawRow(ctx, tr, x, cy, w, "攻击", "3 → 15", COLOR_INK); cy += ROW_H;
        drawRow(ctx, tr, x, cy, w, "护甲", "2 → 12", COLOR_INK); cy += ROW_H;
        drawRow(ctx, tr, x, cy, w, "速度", "0.23 → 0.35", COLOR_INK); cy += ROW_H + 4;

        drawSectionTitle(ctx, tr, x, cy, w, "血月加成", COLOR_BLOOD_MOON); cy += 13;
        drawRow(ctx, tr, x, cy, w, "攻击倍率", "+20%", COLOR_BLOOD_MOON); cy += ROW_H;
        drawRow(ctx, tr, x, cy, w, "速度倍率", "+30%", COLOR_BLOOD_MOON);
    }

    // ===================== 第5页: 战斗特性 =====================

    private void renderZombieBehaviorPage(DrawContext ctx, TextRenderer tr, World world, int x, int y, int w) {
        boolean isBloodMoon = StageSystem.isBloodMoon(world);
        boolean isNight = !world.isDay();

        int cy = y;
        drawSectionTitle(ctx, tr, x, cy, w, "当前加成", COLOR_BLUE); cy += 13;
        drawRow(ctx, tr, x, cy, w, "夜晚速度", isNight ? "+15% 激活" : "未激活",
                isNight ? COLOR_GREEN : COLOR_INK_DIM); cy += ROW_H;
        drawRow(ctx, tr, x, cy, w, "血月速度", isBloodMoon ? "+30% 激活" : "未激活",
                isBloodMoon ? COLOR_BLOOD_MOON : COLOR_INK_DIM); cy += ROW_H;
        drawRow(ctx, tr, x, cy, w, "血月攻击", isBloodMoon ? "+20% 激活" : "未激活",
                isBloodMoon ? COLOR_BLOOD_MOON : COLOR_INK_DIM); cy += ROW_H;
        drawRow(ctx, tr, x, cy, w, "低血狂暴", "<30% +50%速", COLOR_DANGER); cy += ROW_H + 4;

        drawSectionTitle(ctx, tr, x, cy, w, "行为特性", COLOR_INK_HI); cy += 13;
        String[] behaviors = {
                "• 防白天燃烧",
                "• 第20天后开始拆搭方块",
                "• 优先破坏门窗玻璃",
                "• 卡住时加速突破",
                "• 朝玩家方向搭楼梯",
                "• 空中搭天桥追击"
        };
        for (String b : behaviors) {
            ctx.drawTextWithShadow(tr, Text.literal(b), x, cy, COLOR_INK); cy += ROW_H;
        }
        cy += 4;

        drawSectionTitle(ctx, tr, x, cy, w, "威胁警告", COLOR_DANGER); cy += 13;
        ctx.drawTextWithShadow(tr, Text.literal("• 高墙无法完全阻挡"), x, cy, COLOR_DANGER); cy += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("• 备足武器与药水"), x, cy, COLOR_DANGER);
    }

    // ===================== 第6页: 巨型僵尸 =====================

    private void renderGiantPage(DrawContext ctx, TextRenderer tr, World world, int x, int y, int w) {
        double health = StageSystem.getGiantZombieHealth(world);
        double attack = StageSystem.getGiantZombieAttack(world);
        double chance = StageSystem.getGiantZombieChance(world);
        int followRange = StageSystem.getFollowRange(world);
        double atkMult = StageSystem.getAttackMultiplier(world);

        int cy = y;
        drawSectionTitle(ctx, tr, x, cy, w, "巨型属性 (2倍大小 · 第40天起)", COLOR_GIANT); cy += 13;
        drawRowExtra(ctx, tr, x, cy, w, "血量", String.format("%.0f", health),
                String.format("(x%.1f)", health / 400), COLOR_GIANT); cy += ROW_H;
        drawRowExtra(ctx, tr, x, cy, w, "攻击", String.format("%.1f", attack),
                atkMult > 1 ? String.format("x%.1f", atkMult) : "", COLOR_GIANT); cy += ROW_H;
        drawRow(ctx, tr, x, cy, w, "生成概率", String.format("%.1f%%", chance * 100), COLOR_GIANT); cy += ROW_H;
        drawRow(ctx, tr, x, cy, w, "追踪范围", followRange + "格", COLOR_INK); cy += ROW_H + 4;

        drawSectionTitle(ctx, tr, x, cy, w, "特殊能力", COLOR_INK_HI); cy += 13;
        String[] abilities = {
                "• 2.5倍攻击距离",
                "• 可跨越2格高墙",
                "• 防火免疫",
                "• 血月时属性增强"
        };
        String[] abilityColors = {"FF2A1A0A", "FF2A1A0A", "FF2A6B2A", "FF2A1A0A"};
        for (int i = 0; i < abilities.length; i++) {
            ctx.drawTextWithShadow(tr, Text.literal(abilities[i]), x, cy,
                    Integer.parseUnsignedInt(abilityColors[i], 16));
            cy += ROW_H;
        }
        cy += 4;

        drawSectionTitle(ctx, tr, x, cy, w, "掉落物", COLOR_GOLD); cy += 13;
        ctx.drawTextWithShadow(tr, Text.literal("• 腐肉 3-8  ·  骨头 3-6"), x, cy, COLOR_INK); cy += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("• 铁锭 2-5  ·  金锭 1-3"), x, cy, COLOR_INK); cy += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("• 钻石 0-2  ·  绿宝石 0-3"), x, cy, COLOR_INK_HI); cy += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("• 附魔金苹果 0-1"), x, cy, COLOR_GIANT); cy += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("• 经验瓶 1-5"), x, cy, COLOR_GREEN);
    }

    // ===================== 辅助方法 =====================

    private void drawSectionTitle(DrawContext ctx, TextRenderer tr, int x, int y, int w,
                                  String title, int color) {
        ctx.drawTextWithShadow(tr, Text.literal("§ " + title), x, y, color);
        ctx.fill(x, y + 10, x + w, y + 11, COLOR_DIVIDER);
    }

    private void drawRow(DrawContext ctx, TextRenderer tr, int x, int y, int w,
                         String label, String value, int valueColor) {
        ctx.drawTextWithShadow(tr, Text.literal(label), x, y, COLOR_INK_DIM);
        ctx.drawTextWithShadow(tr, Text.literal(value),
                x + w - tr.getWidth(value), y, valueColor);
    }

    private void drawRowExtra(DrawContext ctx, TextRenderer tr, int x, int y, int w,
                              String label, String value, String extra, int valueColor) {
        ctx.drawTextWithShadow(tr, Text.literal(label), x, y, COLOR_INK_DIM);
        int extraW = extra.isEmpty() ? 0 : tr.getWidth(extra);
        int valueW = tr.getWidth(value);
        if (extra.isEmpty()) {
            ctx.drawTextWithShadow(tr, Text.literal(value),
                    x + w - valueW, y, valueColor);
        } else {
            int extraX = x + w - extraW;
            int valX = extraX - 3 - valueW;
            ctx.drawTextWithShadow(tr, Text.literal(value), valX, y, valueColor);
            ctx.drawTextWithShadow(tr, Text.literal(extra), extraX, y, COLOR_INK_DIM);
        }
    }

    private void drawPageButton(DrawContext ctx, TextRenderer tr, int x, int y, String arrow,
                                boolean enabled, boolean hovered) {
        int bg = enabled ? (hovered ? 0xCC3A0808 : 0x661A0808) : 0x22000000;
        int border = enabled ? (hovered ? COLOR_ACCENT : 0x886B3A1A) : 0x44222222;
        int col = enabled ? (hovered ? COLOR_DANGER : COLOR_INK) : 0xFF888888;
        ctx.fill(x, y, x + BTN_W, y + BTN_H, bg);
        ctx.fill(x, y, x + BTN_W, y + 1, border);
        ctx.fill(x, y + BTN_H - 1, x + BTN_W, y + BTN_H, border);
        ctx.fill(x, y, x + 1, y + BTN_H, border);
        ctx.fill(x + BTN_W - 1, y, x + BTN_W, y + BTN_H, border);
        ctx.drawCenteredTextWithShadow(tr, Text.literal(arrow), x + BTN_W / 2, y + 2, col);
    }

    private int getProgressColor(double progress) {
        if (progress < 0.25) return COLOR_GREEN;
        if (progress < 0.50) return COLOR_GOLD;
        if (progress < 0.75) return 0xFF8B6914;
        return COLOR_DANGER;
    }

    private String getStageTip(int day) {
        if (day <= 10) return "初期";
        if (day <= 30) return "发展期";
        if (day <= 50) return "中期";
        if (day <= 70) return "后期";
        return "最终期";
    }

    private String[][] getSurvivalTips(int day, boolean isBloodMoon) {
        if (isBloodMoon) {
            return new String[][]{
                    {"血月刷新翻倍", "FFB01818"},
                    {"加固门窗死守", "FFB01818"},
                    {"警惕巨型僵尸", "FF8B3A1A"}
            };
        }
        if (day <= 10) {
            return new String[][]{
                    {"收集木头建庇护所", "FF2A6B2A"},
                    {"制作石制装备", "FF2A6B2A"},
                    {"留意血月来临", "FF8B6914"}
            };
        }
        if (day <= 30) {
            return new String[][]{
                    {"升级铁制装备", "FF8B6914"},
                    {"准备弓箭远程", "FF8B6914"},
                    {"注意僵尸变强", "FF8B3A1A"}
            };
        }
        if (day <= 50) {
            return new String[][]{
                    {"携带钻石装备", "FF8B3A1A"},
                    {"加固墙体防御", "FFB01818"},
                    {"远离巨型僵尸", "FFB01818"}
            };
        }
        return new String[][]{
                {"末日降临谨慎行动", "FFB01818"},
                {"备足药水防御", "FFB01818"},
                {"高墙护城河最佳", "FFB01818"}
        };
    }
}
