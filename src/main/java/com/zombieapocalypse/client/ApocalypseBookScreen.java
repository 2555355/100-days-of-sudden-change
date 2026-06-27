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
 * 末日情报书界面 - v3.3.0
 * 原版成书纹理双页布局, 内容精确贴合书页可写区域。
 * 三个章节翻页切换: 基础情报 / 僵尸档案 / 巨型僵尸
 */
public class ApocalypseBookScreen extends Screen {

    private static final Identifier BOOK_TEXTURE = new Identifier("minecraft", "textures/gui/book.png");
    private static final int BOOK_W = 292;
    private static final int BOOK_H = 180;

    // 书页可写区域(基于原版book.png纹理测量)
    // 左页: x=20~143, 右页: x=149~272 (书脊在146~148)
    // 上下: y=16~158 (顶部留标题, 底部留翻页按钮)
    private static final int PAGE_LEFT_X = 20;
    private static final int PAGE_RIGHT_X = 149;
    private static final int PAGE_TOP = 16;
    private static final int PAGE_BOTTOM = 158;
    private static final int PAGE_W = 124;   // 143-20+1
    private static final int PAGE_CONTENT_TOP = 22;  // 标题下方内容起始

    // 配色(暗黑典籍风)
    private static final int COLOR_BG_DARK    = 0xF0100A0A;
    private static final int COLOR_BG_CARD    = 0x88120808;
    private static final int COLOR_BG_CARD_ALT= 0x550C0404;
    private static final int COLOR_BORDER_DIM = 0x55883333;
    private static final int COLOR_ACCENT     = 0xFFFF4444;
    private static final int COLOR_TEXT       = 0xFFF0E0D0;
    private static final int COLOR_TEXT_DIM   = 0xFF998877;
    private static final int COLOR_TEXT_HI    = 0xFFFFCC44;
    private static final int COLOR_DANGER     = 0xFFFF5544;
    private static final int COLOR_BLOOD_MOON = 0xFFFF2200;
    private static final int COLOR_ZOMBIE     = 0xFFFF7755;
    private static final int COLOR_GIANT      = 0xFFFF77FF;
    private static final int COLOR_GOLD       = 0xFFFFD700;
    private static final int COLOR_INK        = 0xFF2A1A0A;  // 书页墨色

    private static final String[] CHAPTER_TITLES = {"基础情报", "僵尸档案", "巨型僵尸"};
    private static final String[] CHAPTER_ICONS = {"☠", "⚔", "✦"};
    private static final int[] CHAPTER_COLORS = {COLOR_GOLD, COLOR_ZOMBIE, COLOR_GIANT};

    private static final int ROW_H = 11;
    private static final int BTN_W = 23, BTN_H = 13;

    private int bookX, bookY;
    private int currentSpread = 0;
    private final int maxSpread = 2;
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
        prevBtnY = nextBtnY = bookY + BOOK_H - BTN_H - 4;
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

        // 书本背景纹理
        context.drawTexture(BOOK_TEXTURE, bookX, bookY, 0, 0, BOOK_W, BOOK_H);

        // 书页轻微暗化(保持书页质感, 让文字可读)
        context.fill(bookX + 16, bookY + PAGE_TOP, bookX + BOOK_W - 16, bookY + PAGE_BOTTOM, 0x33050005);

        // 左右页内容坐标(转换为屏幕绝对坐标)
        int leftX = bookX + PAGE_LEFT_X;
        int rightX = bookX + PAGE_RIGHT_X;
        int contentTop = bookY + PAGE_CONTENT_TOP;

        boolean isBloodMoon = StageSystem.isBloodMoon(world);

        // 章节标题(简洁文字+下划线, 不用背景条避免压住书页)
        drawChapterTitle(context, tr, leftX, bookY + PAGE_TOP + 2, PAGE_W, currentSpread);
        drawChapterTitle(context, tr, rightX, bookY + PAGE_TOP + 2, PAGE_W, currentSpread);

        // 血月横幅(若有, 显示在左页标题下)
        int bodyTop = contentTop;
        if (isBloodMoon) {
            int bmY = bodyTop;
            context.fill(leftX, bmY, leftX + PAGE_W, bmY + 12, 0xAA660000);
            context.fill(leftX, bmY, leftX + PAGE_W, bmY + 1, COLOR_BLOOD_MOON);
            context.fill(leftX, bmY + 11, leftX + PAGE_W, bmY + 12, COLOR_BLOOD_MOON);
            context.drawCenteredTextWithShadow(tr, Text.literal("● 血月进行中 ●"),
                    leftX + PAGE_W / 2, bmY + 2, COLOR_BLOOD_MOON);
            bodyTop += 14;
        }

        // 章节内容
        switch (currentSpread) {
            case 0 -> renderBasicSpread(context, tr, world, leftX, rightX, bodyTop, PAGE_W);
            case 1 -> renderZombieSpread(context, tr, world, leftX, rightX, bodyTop, PAGE_W);
            case 2 -> renderGiantSpread(context, tr, world, leftX, rightX, bodyTop, PAGE_W);
        }

        // 翻页按钮
        prevHovered = mouseX >= prevBtnX && mouseX <= prevBtnX + BTN_W && mouseY >= prevBtnY && mouseY <= prevBtnY + BTN_H;
        nextHovered = mouseX >= nextBtnX && mouseX <= nextBtnX + BTN_W && mouseY >= nextBtnY && mouseY <= nextBtnY + BTN_H;
        drawPageButton(context, tr, prevBtnX, prevBtnY, "◀", currentSpread > 0, prevHovered);
        drawPageButton(context, tr, nextBtnX, nextBtnY, "▶", currentSpread < maxSpread, nextHovered);

        // 页码
        String pageInfo = (currentSpread + 1) + " / " + (maxSpread + 1);
        context.drawCenteredTextWithShadow(tr, Text.literal(pageInfo), bookX + BOOK_W / 2,
                bookY + BOOK_H - 14, COLOR_TEXT_DIM);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (prevHovered && currentSpread > 0) { currentSpread--; return true; }
        if (nextHovered && currentSpread < maxSpread) { currentSpread++; return true; }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() { return false; }

    // ===================== 标题与装饰 =====================

    /**
     * 章节标题: 图标+标题居中 + 双下划线装饰(贴合书页, 无背景条)
     */
    private void drawChapterTitle(DrawContext ctx, TextRenderer tr, int x, int y, int w, int chapter) {
        int color = CHAPTER_COLORS[chapter];
        String icon = CHAPTER_ICONS[chapter];
        String title = CHAPTER_TITLES[chapter];
        int iconW = tr.getWidth(icon);
        int titleW = tr.getWidth(title);
        int totalW = iconW + 3 + titleW;
        int startX = x + (w - totalW) / 2;
        ctx.drawTextWithShadow(tr, Text.literal(icon), startX, y, color);
        ctx.drawTextWithShadow(tr, Text.literal(title), startX + iconW + 3, y, color);
        // 双下划线装饰
        ctx.fill(x + 8, y + 10, x + w - 8, y + 11, color);
        ctx.fill(x + 12, y + 12, x + w - 12, y + 13, 0x66884444);
    }

    /**
     * 卡片标题: 竖条+文字(更明显)
     */
    private void drawCardTitle(DrawContext ctx, TextRenderer tr, int x, int y, String title, int color) {
        ctx.fill(x, y, x + 2, y + 9, color);
        ctx.drawTextWithShadow(tr, Text.literal(title), x + 4, y + 1, color);
    }

    /**
     * 数据行斑马纹背景
     */
    private void drawZebraRow(DrawContext ctx, int x, int y, int w, boolean alt) {
        if (alt) ctx.fill(x, y, x + w, y + ROW_H, COLOR_BG_CARD_ALT);
    }

    private void drawPageButton(DrawContext ctx, TextRenderer tr, int x, int y, String arrow,
                                boolean enabled, boolean hovered) {
        int bg = enabled ? (hovered ? 0xCC3A0808 : 0x881A0A0A) : 0x33000000;
        int border = enabled ? (hovered ? COLOR_ACCENT : COLOR_BORDER_DIM) : 0x44222222;
        int col = enabled ? (hovered ? 0xFFFF8888 : COLOR_TEXT_DIM) : 0xFF555555;
        ctx.fill(x, y, x + BTN_W, y + BTN_H, bg);
        ctx.fill(x, y, x + BTN_W, y + 1, border);
        ctx.fill(x, y + BTN_H - 1, x + BTN_W, y + BTN_H, border);
        ctx.fill(x, y, x + 1, y + BTN_H, border);
        ctx.fill(x + BTN_W - 1, y, x + BTN_W, y + BTN_H, border);
        if (enabled && hovered) ctx.fill(x + 1, y + 1, x + BTN_W - 1, y + 2, 0x66FF8888);
        ctx.drawCenteredTextWithShadow(tr, Text.literal(arrow), x + BTN_W / 2, y + 2, col);
    }

    // ===================== 第1展开页: 基础情报 =====================

    private void renderBasicSpread(DrawContext ctx, TextRenderer tr, World world,
                                   int leftX, int rightX, int top, int pageW) {
        int currentDay = StageSystem.getCurrentStage(world);
        double progress = StageSystem.getStageProgress(world);
        boolean isBloodMoon = StageSystem.isBloodMoon(world);
        int intelLevel = StageSystem.getIntelligenceLevel(world);

        // ===== 左页 =====
        int y = top;
        // 天数卡片(修正进度条位置: 放在卡片内)
        int dayCardH = 38;
        drawCard(ctx, leftX, y, pageW, dayCardH);
        drawCardTitle(ctx, tr, leftX + 3, y + 3, "末日历法", COLOR_GOLD);
        String dayStr = String.valueOf(currentDay);
        int dayY = y + 16;
        ctx.drawTextWithShadow(tr, Text.literal("第"), leftX + 5, dayY, COLOR_TEXT_DIM);
        ctx.drawTextWithShadow(tr, Text.literal(dayStr), leftX + 15, dayY, COLOR_TEXT_HI);
        ctx.drawTextWithShadow(tr, Text.literal("天 /" + ModConfig.TOTAL_DAYS),
                leftX + 15 + tr.getWidth(dayStr) + 2, dayY, COLOR_TEXT_DIM);
        // 血月倒计时
        int daysToBM = StageSystem.getDaysToNextBloodMoon(world);
        String bmText = isBloodMoon ? "● 血月今日" : "下次血月 ~" + daysToBM + "天";
        ctx.drawTextWithShadow(tr, Text.literal(bmText), leftX + 5, dayY + 11,
                isBloodMoon ? COLOR_BLOOD_MOON : COLOR_TEXT_DIM);
        // 进度条(确保在卡片内: dayY+22 = y+38, 卡片到y+38, 刚好)
        int barY = dayY + 22;
        int barX = leftX + 5;
        int barW = pageW - 10;
        ctx.fill(barX - 1, barY - 1, barX + barW + 1, barY + 5, 0xFF2A1A0A);
        ctx.fill(barX, barY, barX + barW, barY + 4, 0xFF0A0505);
        int filledW = (int) (barW * progress);
        if (filledW > 0) {
            ctx.fill(barX, barY, barX + filledW, barY + 4, getProgressColor(progress));
            ctx.fill(barX, barY, barX + filledW, barY + 1, 0x88FFFFFF);
        }

        y += dayCardH + 3;
        // 智能度卡片
        drawCard(ctx, leftX, y, pageW, 22);
        drawCardTitle(ctx, tr, leftX + 3, y + 3, "智能度", COLOR_ACCENT);
        String[] intelNames = {"迟钝", "普通", "机敏", "狡猾", "凶残", "嗜血"};
        int[] intelColors = {0xFF33AA33, 0xFFFFAA00, 0xFFCC6622, 0xFFFF3333, 0xFFFF0000, 0xFFFF0000};
        String intelText = "Lv" + intelLevel + " " + intelNames[intelLevel];
        ctx.drawTextWithShadow(tr, Text.literal(intelText),
                leftX + pageW - 5 - tr.getWidth(intelText), y + 7, intelColors[intelLevel]);

        y += 25;
        // 阶段卡片
        drawCard(ctx, leftX, y, pageW, 22);
        drawCardTitle(ctx, tr, leftX + 3, y + 3, "当前阶段", COLOR_TEXT_HI);
        String tip = getStageTip(currentDay);
        ctx.drawTextWithShadow(tr, Text.literal(tip),
                leftX + pageW - 5 - tr.getWidth(tip), y + 7, COLOR_TEXT_HI);

        // ===== 右页 =====
        int ry = top;
        int breakInt = StageSystem.getBreakInterval(world);
        int buildInt = StageSystem.getBuildInterval(world);
        float hardLimit = StageSystem.getHardnessLimit(world);
        double reinChance = StageSystem.getReinforcementChance(world);
        int invSize = StageSystem.getBlockInventorySize(world);

        int aiCardH = 4 + ROW_H * 4 + 8;
        drawCard(ctx, rightX, ry, pageW, aiCardH);
        drawCardTitle(ctx, tr, rightX + 3, ry + 3, "⚙ AI能力", COLOR_TEXT_HI);
        int aiY = ry + 16;
        drawZebraRow(ctx, rightX + 2, aiY, pageW - 4, true);
        ctx.drawTextWithShadow(tr, Text.literal(String.format("拆 %.1fs / 搭 %.1fs", breakInt / 20.0, buildInt / 20.0)),
                rightX + 5, aiY + 1, COLOR_TEXT); aiY += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal(String.format("硬度 %.0f / 库存 %d", hardLimit, invSize)),
                rightX + 5, aiY + 1, COLOR_TEXT); aiY += ROW_H;
        drawZebraRow(ctx, rightX + 2, aiY, pageW - 4, true);
        String reinStr = reinChance > 0 ? String.format("增援 %.0f%%", reinChance * 100) : "增援 未解锁";
        ctx.drawTextWithShadow(tr, Text.literal(reinStr), rightX + 5, aiY + 1,
                reinChance > 0 ? COLOR_DANGER : COLOR_TEXT_DIM); aiY += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("夜+15%速 / 血月+30%速"), rightX + 5, aiY + 1, COLOR_TEXT);

        ry += aiCardH + 3;
        String[][] tips = getSurvivalTips(currentDay, isBloodMoon);
        int tipCardH = 4 + ROW_H * tips.length + 8;
        drawCard(ctx, rightX, ry, pageW, tipCardH);
        drawCardTitle(ctx, tr, rightX + 3, ry + 3, "★ 生存提示", 0xFF66DDFF);
        int tipY = ry + 16;
        for (int i = 0; i < tips.length; i++) {
            drawZebraRow(ctx, rightX + 2, tipY, pageW - 4, i % 2 == 1);
            ctx.drawTextWithShadow(tr, Text.literal(tips[i][0]), rightX + 5, tipY + 1,
                    Integer.parseUnsignedInt(tips[i][1], 16));
            tipY += ROW_H;
        }
    }

    // ===================== 第2展开页: 僵尸档案 =====================

    private void renderZombieSpread(DrawContext ctx, TextRenderer tr, World world,
                                    int leftX, int rightX, int top, int pageW) {
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

        // 左页: 属性
        int y = top;
        int attrCardH = 4 + ROW_H * 5 + 8;
        drawCard(ctx, leftX, y, pageW, attrCardH);
        drawCardTitle(ctx, tr, leftX + 3, y + 3, "⚔ 属性", COLOR_ZOMBIE);
        int ry = y + 16;
        double atkMult = StageSystem.getAttackMultiplier(world);
        double spdMult = StageSystem.getSpeedMultiplier(world, 1.0f);
        drawZebraRow(ctx, leftX + 2, ry, pageW - 4, true);
        drawDetailRow(ctx, tr, leftX + 5, ry + 1, pageW - 10, "血量", String.format("%.0f/20", health),
                String.format("(%.1fx)", health / 20), COLOR_DANGER); ry += ROW_H;
        drawDetailRow(ctx, tr, leftX + 5, ry + 1, pageW - 10, "攻击", String.format("%.1f", attack),
                atkMult > 1 ? String.format("x%.1f", atkMult) : "", COLOR_TEXT_HI); ry += ROW_H;
        drawZebraRow(ctx, leftX + 2, ry, pageW - 4, true);
        drawDetailRow(ctx, tr, leftX + 5, ry + 1, pageW - 10, "护甲", String.format("%.0f", armor), "", COLOR_TEXT); ry += ROW_H;
        drawDetailRow(ctx, tr, leftX + 5, ry + 1, pageW - 10, "速度", String.format("%.2f", speed),
                spdMult > 1 ? String.format("x%.2f", spdMult) : "", COLOR_TEXT); ry += ROW_H;
        drawZebraRow(ctx, leftX + 2, ry, pageW - 4, true);
        drawDetailRow(ctx, tr, leftX + 5, ry + 1, pageW - 10, "追踪", followRange + "格", "", COLOR_TEXT);

        // 右页: 战斗能力 + 加成
        int rry = top;
        int combatCardH = 4 + ROW_H * 4 + 8;
        drawCard(ctx, rightX, rry, pageW, combatCardH);
        drawCardTitle(ctx, tr, rightX + 3, rry + 3, "⚙ 战斗能力", COLOR_TEXT_HI);
        int cy = rry + 16;
        drawZebraRow(ctx, rightX + 2, cy, pageW - 4, true);
        drawDetailRow(ctx, tr, rightX + 5, cy + 1, pageW - 10, "拆方块", String.format("%.1fs", breakInt / 20.0), "", COLOR_TEXT); cy += ROW_H;
        drawDetailRow(ctx, tr, rightX + 5, cy + 1, pageW - 10, "搭方块", String.format("%.1fs", buildInt / 20.0), "", COLOR_TEXT); cy += ROW_H;
        drawZebraRow(ctx, rightX + 2, cy, pageW - 4, true);
        drawDetailRow(ctx, tr, rightX + 5, cy + 1, pageW - 10, "硬度上限", String.format("%.0f", hardLimit), "", COLOR_TEXT); cy += ROW_H;
        String reinStr = reinChance > 0 ? String.format("%.0f%%", reinChance * 100) : "未解锁";
        drawDetailRow(ctx, tr, rightX + 5, cy + 1, pageW - 10, "呼叫增援", reinStr, "",
                reinChance > 0 ? COLOR_DANGER : COLOR_TEXT_DIM);

        rry += combatCardH + 3;
        int buffCardH = 4 + ROW_H * 3 + 8;
        drawCard(ctx, rightX, rry, pageW, buffCardH);
        drawCardTitle(ctx, tr, rightX + 3, rry + 3, "★ 当前加成", 0xFF66DDFF);
        int by = rry + 16;
        drawZebraRow(ctx, rightX + 2, by, pageW - 4, true);
        drawDetailRow(ctx, tr, rightX + 5, by + 1, pageW - 10, "夜晚", isNight ? "激活 +15%速" : "未激活", "",
                isNight ? 0xFF33AA33 : COLOR_TEXT_DIM); by += ROW_H;
        drawDetailRow(ctx, tr, rightX + 5, by + 1, pageW - 10, "血月", isBloodMoon ? "激活 +30%速" : "未激活", "",
                isBloodMoon ? COLOR_BLOOD_MOON : COLOR_TEXT_DIM); by += ROW_H;
        drawZebraRow(ctx, rightX + 2, by, pageW - 4, true);
        drawDetailRow(ctx, tr, rightX + 5, by + 1, pageW - 10, "库存", invSize + "格", "", COLOR_TEXT);
    }

    // ===================== 第3展开页: 巨型僵尸 =====================

    private void renderGiantSpread(DrawContext ctx, TextRenderer tr, World world,
                                   int leftX, int rightX, int top, int pageW) {
        double health = StageSystem.getGiantZombieHealth(world);
        double attack = StageSystem.getGiantZombieAttack(world);
        double chance = StageSystem.getGiantZombieChance(world);
        int followRange = StageSystem.getFollowRange(world);

        // 左页: 属性
        int y = top;
        int attrCardH = 4 + ROW_H * 6 + 8;
        drawCard(ctx, leftX, y, pageW, attrCardH);
        drawCardTitle(ctx, tr, leftX + 3, y + 3, "✦ 巨型属性 (2x)", COLOR_GIANT);
        int ry = y + 16;
        double atkMult = StageSystem.getAttackMultiplier(world);
        drawZebraRow(ctx, leftX + 2, ry, pageW - 4, true);
        drawDetailRow(ctx, tr, leftX + 5, ry + 1, pageW - 10, "血量", String.format("%.0f/400", health),
                String.format("(%.1fx)", health / 400), COLOR_GIANT); ry += ROW_H;
        drawDetailRow(ctx, tr, leftX + 5, ry + 1, pageW - 10, "攻击", String.format("%.1f", attack),
                atkMult > 1 ? String.format("x%.1f", atkMult) : "", COLOR_GIANT); ry += ROW_H;
        drawZebraRow(ctx, leftX + 2, ry, pageW - 4, true);
        drawDetailRow(ctx, tr, leftX + 5, ry + 1, pageW - 10, "生成概率", String.format("%.1f%%", chance * 100), "", COLOR_GIANT); ry += ROW_H;
        drawDetailRow(ctx, tr, leftX + 5, ry + 1, pageW - 10, "追踪", followRange + "格", "", COLOR_GIANT); ry += ROW_H;
        drawZebraRow(ctx, leftX + 2, ry, pageW - 4, true);
        drawDetailRow(ctx, tr, leftX + 5, ry + 1, pageW - 10, "攻击范围", "2.5x", "", COLOR_GIANT); ry += ROW_H;
        drawDetailRow(ctx, tr, leftX + 5, ry + 1, pageW - 10, "防火", "免疫", "", COLOR_TEXT);

        // 右页: 掉落物 + 特性
        int rry = top;
        int dropCardH = 4 + ROW_H * 5 + 8;
        drawCard(ctx, rightX, rry, pageW, dropCardH);
        drawCardTitle(ctx, tr, rightX + 3, rry + 3, "◈ 掉落物", COLOR_GOLD);
        int dy = rry + 16;
        drawZebraRow(ctx, rightX + 2, dy, pageW - 4, true);
        ctx.drawTextWithShadow(tr, Text.literal("腐肉 3-8  骨头 3-6"), rightX + 5, dy + 1, COLOR_TEXT); dy += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("铁锭 2-5  金锭 1-3"), rightX + 5, dy + 1, COLOR_TEXT); dy += ROW_H;
        drawZebraRow(ctx, rightX + 2, dy, pageW - 4, true);
        ctx.drawTextWithShadow(tr, Text.literal("钻石 0-2  绿宝石 0-3"), rightX + 5, dy + 1, COLOR_TEXT); dy += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("附魔金苹果 0-1"), rightX + 5, dy + 1, 0xFFFF66FF); dy += ROW_H;
        drawZebraRow(ctx, rightX + 2, dy, pageW - 4, true);
        ctx.drawTextWithShadow(tr, Text.literal("经验瓶 1-5"), rightX + 5, dy + 1, 0xFF33AA33);

        rry += dropCardH + 3;
        int featCardH = 4 + ROW_H * 2 + 8;
        drawCard(ctx, rightX, rry, pageW, featCardH);
        drawCardTitle(ctx, tr, rightX + 3, rry + 3, "☠ 特性", COLOR_GIANT);
        int fy = rry + 16;
        drawZebraRow(ctx, rightX + 2, fy, pageW - 4, true);
        ctx.drawTextWithShadow(tr, Text.literal("可跨越2格高方块"), rightX + 5, fy + 1, COLOR_TEXT); fy += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("2.5倍攻击范围"), rightX + 5, fy + 1, COLOR_TEXT);
    }

    // ===================== 辅助绘制 =====================

    private void drawDetailRow(DrawContext ctx, TextRenderer tr, int x, int y, int w,
                               String label, String value, String extra, int valueColor) {
        ctx.drawTextWithShadow(tr, Text.literal(label), x, y, COLOR_TEXT_DIM);
        int valW = tr.getWidth(value);
        int extraW = extra.isEmpty() ? 0 : tr.getWidth(extra);
        int rightPad = 2;
        int gap = 3;
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
        if (day <= 10) return "初期-收集建基地";
        if (day <= 30) return "发展-加固防御";
        if (day <= 50) return "中期-僵尸变强";
        if (day <= 70) return "后期-巨型出没";
        return "最终-拼死生存";
    }

    private String[][] getSurvivalTips(int day, boolean isBloodMoon) {
        if (isBloodMoon) {
            return new String[][]{
                    {"血月刷新翻倍!", "FFFF3333"},
                    {"加固门窗死守!", "FFFF3333"},
                    {"注意巨型僵尸", "FFFF6644"}
            };
        }
        if (day <= 10) {
            return new String[][]{
                    {"收集木头建庇护所", "FF33AA33"},
                    {"制作石制装备", "FF33AA33"},
                    {"留意第10天血月", "FFFFAA00"}
            };
        }
        if (day <= 30) {
            return new String[][]{
                    {"用铁制装备加固", "FFFFAA00"},
                    {"准备弓箭远程", "FFFFAA00"},
                    {"僵尸变强注意血量", "FFCC6622"}
            };
        }
        if (day <= 50) {
            return new String[][]{
                    {"携带钻石装备", "FFCC6622"},
                    {"加固墙体防破坏", "FFFF3333"},
                    {"巨型僵尸保持距离", "FFFF3333"}
            };
        }
        return new String[][]{
                {"末日降临谨慎行动", "FFFF3333"},
                {"备足药水防御", "FFFF3333"},
                {"高墙护城河最佳", "FFFF3333"}
        };
    }

    private void drawCard(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, COLOR_BG_CARD);
        ctx.fill(x, y, x + w, y + 1, 0x33FF4444);
        ctx.fill(x, y + h - 1, x + w, y + h, 0x55110000);
        ctx.fill(x, y, x + 1, y + h, 0x22FF3333);
    }
}
