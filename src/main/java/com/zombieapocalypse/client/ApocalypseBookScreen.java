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
 * 末日情报书界面 - v4.0.0
 * 极简书页风格: 移除卡片背景, 用分隔线+墨色文字直接书写, 保持书页质感。
 * 三个章节翻页切换: 基础情报 / 僵尸档案 / 巨型僵尸
 */
public class ApocalypseBookScreen extends Screen {

    private static final Identifier BOOK_TEXTURE = new Identifier("minecraft", "textures/gui/book.png");
    private static final int BOOK_W = 292;
    private static final int BOOK_H = 180;

    // 书页可写区域(原版book.png测量)
    private static final int PAGE_LEFT_X = 20;
    private static final int PAGE_RIGHT_X = 149;
    private static final int PAGE_TOP = 16;
    private static final int PAGE_BOTTOM = 152;   // 上方留翻页按钮空间
    private static final int PAGE_W = 124;
    private static final int PAGE_HEADER_Y = 18;   // 章节标题行
    private static final int PAGE_BODY_Y = 30;     // 正文起始

    // 墨色配色(贴合书页, 高可读性)
    private static final int COLOR_BG_DARK    = 0xB0000000;  // 半透明黑(能看到游戏画面)
    private static final int COLOR_OVERLAY    = 0x00000000;  // 不再暗化书页
    private static final int COLOR_INK        = 0xFF2A1A0A;  // 主文字(深墨)
    private static final int COLOR_INK_DIM    = 0xFF5A4530;  // 次要文字(浅墨, 加深)
    private static final int COLOR_INK_HI     = 0xFF8B2A0A;  // 强调(暗红墨)
    private static final int COLOR_TITLE      = 0xFF6B1010;  // 标题(深红墨)
    private static final int COLOR_ACCENT     = 0xFFA02020;  // 强调红
    private static final int COLOR_DANGER     = 0xFFB01818;  // 危险
    private static final int COLOR_BLOOD_MOON = 0xFFCC0000;  // 血月
    private static final int COLOR_ZOMBIE     = 0xFF8B3A1A;  // 僵尸(棕红墨)
    private static final int COLOR_GIANT      = 0xFF7A2A6B;  // 巨型(紫红墨)
    private static final int COLOR_GOLD       = 0xFF8B6914;  // 金(暗金墨)
    private static final int COLOR_GREEN      = 0xFF2A6B2A;  // 绿(安全)
    private static final int COLOR_BLUE       = 0xFF1A3A6B;  // 蓝(信息)
    private static final int COLOR_DIVIDER    = 0xFF8B6B45;  // 分隔线(棕)

    private static final String[] CHAPTER_TITLES = {"基础情报", "僵尸档案", "巨型僵尸"};
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
        // 半透明背景(能看到游戏画面, 不过度遮挡)
        context.fill(0, 0, this.width, this.height, COLOR_BG_DARK);

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        World world = client.player.getWorld();
        if (world == null) return;

        TextRenderer tr = client.textRenderer;

        // 书本背景纹理(9参数版本: 指定纹理宽高 292x180, 避免256回绕错位)
        context.drawTexture(BOOK_TEXTURE, bookX, bookY, 0, 0, BOOK_W, BOOK_H, BOOK_W, BOOK_H);

        int leftX = bookX + PAGE_LEFT_X;
        int rightX = bookX + PAGE_RIGHT_X;
        int bodyTop = bookY + PAGE_BODY_Y;

        boolean isBloodMoon = StageSystem.isBloodMoon(world);

        // 章节标题(左右页)
        drawChapterHeader(context, tr, leftX, bookY + PAGE_HEADER_Y, PAGE_W, currentSpread);
        drawChapterHeader(context, tr, rightX, bookY + PAGE_HEADER_Y, PAGE_W, currentSpread);

        // 血月横幅(左页标题下, 紧凑显示)
        int leftBodyTop = bodyTop;
        if (isBloodMoon) {
            int bmY = bookY + PAGE_HEADER_Y + 11;
            context.drawCenteredTextWithShadow(tr, Text.literal("✦ 血月进行中 ✦"),
                    leftX + PAGE_W / 2, bmY, COLOR_BLOOD_MOON);
            leftBodyTop = bmY + 11;
        }

        // 用 scissor 裁剪到书页可写区域, 防止内容超出
        int clipTop = bookY + PAGE_TOP;
        int clipBottom = bookY + PAGE_BOTTOM;
        context.enableScissor(bookX + 14, clipTop, bookX + BOOK_W - 14, clipBottom);

        // 章节内容
        switch (currentSpread) {
            case 0 -> renderBasicSpread(context, tr, world, leftX, rightX, leftBodyTop, bodyTop, PAGE_W);
            case 1 -> renderZombieSpread(context, tr, world, leftX, rightX, bodyTop, PAGE_W);
            case 2 -> renderGiantSpread(context, tr, world, leftX, rightX, bodyTop, PAGE_W);
        }

        context.disableScissor();

        // 翻页按钮
        prevHovered = mouseX >= prevBtnX && mouseX <= prevBtnX + BTN_W && mouseY >= prevBtnY && mouseY <= prevBtnY + BTN_H;
        nextHovered = mouseX >= nextBtnX && mouseX <= nextBtnX + BTN_W && mouseY >= nextBtnY && mouseY <= nextBtnY + BTN_H;
        drawPageButton(context, tr, prevBtnX, prevBtnY, "◀", currentSpread > 0, prevHovered);
        drawPageButton(context, tr, nextBtnX, nextBtnY, "▶", currentSpread < maxSpread, nextHovered);

        // 页码
        String pageInfo = (currentSpread + 1) + " / " + (maxSpread + 1);
        context.drawCenteredTextWithShadow(tr, Text.literal(pageInfo), bookX + BOOK_W / 2,
                bookY + BOOK_H - 14, COLOR_INK_DIM);
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
     * 章节标题: 居中文字 + 双下划线(墨色, 贴合书页)
     */
    private void drawChapterHeader(DrawContext ctx, TextRenderer tr, int x, int y, int w, int chapter) {
        int color = CHAPTER_COLORS[chapter];
        String title = CHAPTER_TITLES[chapter];
        ctx.drawCenteredTextWithShadow(tr, Text.literal(title), x + w / 2, y, color);
        // 双下划线(主线用章节色, 副线用淡棕)
        ctx.fill(x + 16, y + 10, x + w - 16, y + 11, color);
        ctx.fill(x + 24, y + 12, x + w - 24, y + 13, 0x558B6B45);
    }

    /**
     * 节标题: 带前置符号, 下划线分隔
     */
    private void drawSectionTitle(DrawContext ctx, TextRenderer tr, int x, int y, int w, String title, int color) {
        ctx.drawTextWithShadow(tr, Text.literal("§ " + title), x, y, color);
        ctx.fill(x, y + 10, x + w, y + 11, 0x558B6B45);
    }

    /**
     * 数据行: 标签左对齐 + 数值右对齐(墨色)
     */
    private void drawRow(DrawContext ctx, TextRenderer tr, int x, int y, int w,
                         String label, String value, int valueColor) {
        ctx.drawTextWithShadow(tr, Text.literal(label), x, y, COLOR_INK_DIM);
        ctx.drawTextWithShadow(tr, Text.literal(value), x + w - tr.getWidth(value) - 1, y, valueColor);
    }

    /**
     * 数据行: 标签 + 数值 + 倍率(三列)
     */
    private void drawRowExtra(DrawContext ctx, TextRenderer tr, int x, int y, int w,
                              String label, String value, String extra, int valueColor) {
        ctx.drawTextWithShadow(tr, Text.literal(label), x, y, COLOR_INK_DIM);
        int extraW = extra.isEmpty() ? 0 : tr.getWidth(extra);
        int valueW = tr.getWidth(value);
        if (extra.isEmpty()) {
            ctx.drawTextWithShadow(tr, Text.literal(value), x + w - valueW - 1, y, valueColor);
        } else {
            int extraX = x + w - extraW - 1;
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

    // ===================== 第1展开页: 基础情报 =====================

    private void renderBasicSpread(DrawContext ctx, TextRenderer tr, World world,
                                   int leftX, int rightX, int leftTop, int rightTop, int pageW) {
        int currentDay = StageSystem.getCurrentStage(world);
        double progress = StageSystem.getStageProgress(world);
        boolean isBloodMoon = StageSystem.isBloodMoon(world);
        int intelLevel = StageSystem.getIntelligenceLevel(world);

        // ===== 左页: 末日历法 + 进度 + 智能度(合并) =====
        int y = leftTop;
        drawSectionTitle(ctx, tr, leftX, y, pageW, "末日历法", COLOR_GOLD);
        y += 14;
        drawRow(ctx, tr, leftX, y, pageW, "天数", currentDay + " / " + ModConfig.TOTAL_DAYS, COLOR_INK_HI);
        y += ROW_H;
        int daysToBM = StageSystem.getDaysToNextBloodMoon(world);
        String bmText = isBloodMoon ? "今日血月" : "~" + daysToBM + "天后";
        drawRow(ctx, tr, leftX, y, pageW, "下次血月", bmText,
                isBloodMoon ? COLOR_BLOOD_MOON : COLOR_INK);
        y += ROW_H + 2;
        // 进度条
        int barX = leftX + 2;
        int barW = pageW - 4;
        ctx.fill(barX, y, barX + barW, y + 1, 0xFF8B6B45);
        int filledW = (int) (barW * progress);
        if (filledW > 0) ctx.fill(barX, y, barX + filledW, y + 1, getProgressColor(progress));
        ctx.drawTextWithShadow(tr, Text.literal((int)(progress * 100) + "%"),
                leftX + pageW / 2 - 6, y + 2, COLOR_INK_DIM);
        y += 14;
        // 智能度(合并到同页, 减少section)
        String[] intelNames = {"迟钝", "普通", "机敏", "狡猾", "凶残", "嗜血"};
        int[] intelColors = {COLOR_GREEN, COLOR_GOLD, 0xFF8B6914, COLOR_DANGER, COLOR_BLOOD_MOON, COLOR_BLOOD_MOON};
        drawRow(ctx, tr, leftX, y, pageW, "智能度", "Lv" + intelLevel + " " + intelNames[intelLevel], intelColors[intelLevel]);
        y += ROW_H;
        drawRow(ctx, tr, leftX, y, pageW, "阶段", getStageTip(currentDay), COLOR_INK_HI);
        y += ROW_H + 4;

        // 生存提示(左页下半)
        drawSectionTitle(ctx, tr, leftX, y, pageW, "生存提示", COLOR_BLUE);
        y += 14;
        String[][] tips = getSurvivalTips(currentDay, isBloodMoon);
        for (String[] t : tips) {
            ctx.drawTextWithShadow(tr, Text.literal("• " + t[0]), leftX, y, Integer.parseUnsignedInt(t[1], 16));
            y += ROW_H;
        }

        // ===== 右页: AI能力 =====
        int ry = rightTop;
        int breakInt = StageSystem.getBreakInterval(world);
        int buildInt = StageSystem.getBuildInterval(world);
        float hardLimit = StageSystem.getHardnessLimit(world);
        double reinChance = StageSystem.getReinforcementChance(world);
        int invSize = StageSystem.getBlockInventorySize(world);

        drawSectionTitle(ctx, tr, rightX, ry, pageW, "AI能力", COLOR_INK_HI);
        ry += 14;
        drawRow(ctx, tr, rightX, ry, pageW, "拆方块", String.format("%.1fs", breakInt / 20.0), COLOR_INK);
        ry += ROW_H;
        drawRow(ctx, tr, rightX, ry, pageW, "搭方块", String.format("%.1fs", buildInt / 20.0), COLOR_INK);
        ry += ROW_H;
        drawRow(ctx, tr, rightX, ry, pageW, "硬度上限", String.format("%.0f", hardLimit), COLOR_INK);
        ry += ROW_H;
        drawRow(ctx, tr, rightX, ry, pageW, "库存容量", invSize + "格", COLOR_INK);
        ry += ROW_H;
        String reinStr = reinChance > 0 ? String.format("%.0f%%", reinChance * 100) : "未解锁";
        drawRow(ctx, tr, rightX, ry, pageW, "呼叫增援", reinStr,
                reinChance > 0 ? COLOR_DANGER : COLOR_INK_DIM);
        ry += ROW_H + 4;

        // 阶段总览(右页下半)
        drawSectionTitle(ctx, tr, rightX, ry, pageW, "阶段总览", COLOR_GOLD);
        ry += 14;
        ctx.drawTextWithShadow(tr, Text.literal("• 第20天后拆搭方块"), rightX, ry, COLOR_INK); ry += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("• 第40天后巨型僵尸"), rightX, ry, COLOR_INK); ry += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("• 血月随机刷新"), rightX, ry, COLOR_INK);
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
        drawSectionTitle(ctx, tr, leftX, y, pageW, "基础属性", COLOR_ZOMBIE);
        y += 14;
        double atkMult = StageSystem.getAttackMultiplier(world);
        double spdMult = StageSystem.getSpeedMultiplier(world, 1.0f);
        drawRowExtra(ctx, tr, leftX, y, pageW, "血量", String.format("%.0f", health),
                String.format("(x%.1f)", health / 20), COLOR_DANGER); y += ROW_H;
        drawRowExtra(ctx, tr, leftX, y, pageW, "攻击", String.format("%.1f", attack),
                atkMult > 1 ? String.format("x%.1f", atkMult) : "", COLOR_INK_HI); y += ROW_H;
        drawRow(ctx, tr, leftX, y, pageW, "护甲", String.format("%.0f", armor), COLOR_INK); y += ROW_H;
        drawRowExtra(ctx, tr, leftX, y, pageW, "速度", String.format("%.2f", speed),
                spdMult > 1 ? String.format("x%.2f", spdMult) : "", COLOR_INK); y += ROW_H;
        drawRow(ctx, tr, leftX, y, pageW, "追踪范围", followRange + "格", COLOR_INK); y += ROW_H + 4;

        // 战斗能力
        drawSectionTitle(ctx, tr, leftX, y, pageW, "战斗能力", COLOR_INK_HI);
        y += 14;
        drawRow(ctx, tr, leftX, y, pageW, "拆方块", String.format("%.1fs", breakInt / 20.0), COLOR_INK); y += ROW_H;
        drawRow(ctx, tr, leftX, y, pageW, "搭方块", String.format("%.1fs", buildInt / 20.0), COLOR_INK); y += ROW_H;
        drawRow(ctx, tr, leftX, y, pageW, "硬度上限", String.format("%.0f", hardLimit), COLOR_INK); y += ROW_H;
        String reinStr = reinChance > 0 ? String.format("%.0f%%", reinChance * 100) : "未解锁";
        drawRow(ctx, tr, leftX, y, pageW, "呼叫增援", reinStr, reinChance > 0 ? COLOR_DANGER : COLOR_INK_DIM);

        // 右页: 当前加成
        int ry = top;
        drawSectionTitle(ctx, tr, rightX, ry, pageW, "当前加成", COLOR_BLUE);
        ry += 14;
        drawRow(ctx, tr, rightX, ry, pageW, "夜晚速度", isNight ? "+15% 激活" : "未激活",
                isNight ? COLOR_GREEN : COLOR_INK_DIM); ry += ROW_H;
        drawRow(ctx, tr, rightX, ry, pageW, "血月速度", isBloodMoon ? "+30% 激活" : "未激活",
                isBloodMoon ? COLOR_BLOOD_MOON : COLOR_INK_DIM); ry += ROW_H;
        drawRow(ctx, tr, rightX, ry, pageW, "血月攻击", isBloodMoon ? "+20% 激活" : "未激活",
                isBloodMoon ? COLOR_BLOOD_MOON : COLOR_INK_DIM); ry += ROW_H;
        drawRow(ctx, tr, rightX, ry, pageW, "低血狂暴", "<30% +50%速", COLOR_DANGER); ry += ROW_H + 4;

        // 行为特性
        drawSectionTitle(ctx, tr, rightX, ry, pageW, "行为特性", COLOR_INK_HI);
        ry += 14;
        ctx.drawTextWithShadow(tr, Text.literal("• 防白天燃烧"), rightX, ry, COLOR_INK); ry += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("• 第20天后拆搭方块"), rightX, ry, COLOR_INK); ry += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("• 优先破坏门窗玻璃"), rightX, ry, COLOR_INK); ry += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("• 卡住时加速突破"), rightX, ry, COLOR_INK);
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
        drawSectionTitle(ctx, tr, leftX, y, pageW, "巨型属性", COLOR_GIANT);
        y += 14;
        ctx.drawTextWithShadow(tr, Text.literal("(2倍大小 · 第40天起)"), leftX, y, COLOR_INK_DIM);
        y += ROW_H;
        double atkMult = StageSystem.getAttackMultiplier(world);
        drawRowExtra(ctx, tr, leftX, y, pageW, "血量", String.format("%.0f", health),
                String.format("(x%.1f)", health / 400), COLOR_GIANT); y += ROW_H;
        drawRowExtra(ctx, tr, leftX, y, pageW, "攻击", String.format("%.1f", attack),
                atkMult > 1 ? String.format("x%.1f", atkMult) : "", COLOR_GIANT); y += ROW_H;
        drawRow(ctx, tr, leftX, y, pageW, "生成概率", String.format("%.1f%%", chance * 100), COLOR_GIANT); y += ROW_H;
        drawRow(ctx, tr, leftX, y, pageW, "追踪范围", followRange + "格", COLOR_INK); y += ROW_H;
        drawRow(ctx, tr, leftX, y, pageW, "攻击范围", "2.5倍", COLOR_INK); y += ROW_H;
        drawRow(ctx, tr, leftX, y, pageW, "跨越高度", "2格", COLOR_INK); y += ROW_H;
        drawRow(ctx, tr, leftX, y, pageW, "防火", "免疫", COLOR_GREEN); y += ROW_H + 4;

        // 特性
        drawSectionTitle(ctx, tr, leftX, y, pageW, "特殊能力", COLOR_INK_HI);
        y += 14;
        ctx.drawTextWithShadow(tr, Text.literal("• 2.5倍攻击距离"), leftX, y, COLOR_INK); y += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("• 可跨越2格高墙"), leftX, y, COLOR_INK); y += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("• 血月时属性增强"), leftX, y, COLOR_INK);

        // 右页: 掉落物
        int ry = top;
        drawSectionTitle(ctx, tr, rightX, ry, pageW, "掉落物", COLOR_GOLD);
        ry += 14;
        ctx.drawTextWithShadow(tr, Text.literal("腐肉    3-8"), rightX, ry, COLOR_INK); ry += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("骨头    3-6"), rightX, ry, COLOR_INK); ry += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("铁锭    2-5"), rightX, ry, COLOR_INK); ry += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("金锭    1-3"), rightX, ry, COLOR_INK); ry += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("钻石    0-2"), rightX, ry, COLOR_INK_HI); ry += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("绿宝石  0-3"), rightX, ry, COLOR_INK_HI); ry += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("附魔金苹果 0-1"), rightX, ry, COLOR_GIANT); ry += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("经验瓶  1-5"), rightX, ry, COLOR_GREEN); ry += ROW_H + 4;

        // 警告
        drawSectionTitle(ctx, tr, rightX, ry, pageW, "威胁警告", COLOR_DANGER);
        ry += 14;
        ctx.drawTextWithShadow(tr, Text.literal("• 保持远程攻击"), rightX, ry, COLOR_DANGER); ry += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("• 准备高墙防御"), rightX, ry, COLOR_DANGER); ry += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("• 备足药水箭矢"), rightX, ry, COLOR_DANGER);
    }

    // ===================== 辅助 =====================

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
