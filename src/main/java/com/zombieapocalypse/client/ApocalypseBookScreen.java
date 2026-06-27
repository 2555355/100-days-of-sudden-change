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
 * 末日情报书界面 - v5.0.0
 * 单页显示: 左页为章节封面, 右页为内容。6页结构, 内容宽松不拥挤。
 *
 * 页面规划:
 *   1. 封面/目录
 *   2. 基础情报 - 末日历法
 *   3. 基础情报 - 智能度与AI
 *   4. 僵尸档案 - 属性与战斗
 *   5. 僵尸档案 - 加成与特性
 *   6. 巨型僵尸
 */
public class ApocalypseBookScreen extends Screen {

    private static final Identifier BOOK_TEXTURE = new Identifier("minecraft", "textures/gui/book.png");
    private static final int BOOK_W = 292;
    private static final int BOOK_H = 180;

    // 书页可写区域(原版book.png测量)
    private static final int PAGE_LEFT_X = 20;
    private static final int PAGE_RIGHT_X = 149;
    private static final int PAGE_TOP = 16;
    private static final int PAGE_BOTTOM = 152;
    private static final int PAGE_W = 124;
    private static final int PAGE_HEADER_Y = 18;
    private static final int PAGE_BODY_Y = 30;

    // 墨色配色(贴合书页, 高可读性)
    private static final int COLOR_BG_DARK    = 0xB0000000;
    private static final int COLOR_INK        = 0xFF2A1A0A;
    private static final int COLOR_INK_DIM    = 0xFF5A4530;
    private static final int COLOR_INK_HI     = 0xFF8B2A0A;
    private static final int COLOR_ACCENT     = 0xFFA02020;
    private static final int COLOR_DANGER     = 0xFFB01818;
    private static final int COLOR_BLOOD_MOON = 0xFFCC0000;
    private static final int COLOR_ZOMBIE     = 0xFF8B3A1A;
    private static final int COLOR_GIANT      = 0xFF7A2A6B;
    private static final int COLOR_GOLD       = 0xFF8B6914;
    private static final int COLOR_GREEN      = 0xFF2A6B2A;
    private static final int COLOR_BLUE       = 0xFF1A3A6B;

    // 6页章节信息(左页封面用)
    private static final String[] PAGE_TITLES = {
            "末日情报", "末日历法", "智能与AI", "僵尸档案", "战斗特性", "巨型僵尸"
    };
    private static final String[] PAGE_ICONS = {
            "☠", "☀", "✦", "⚔", "✧", "✦"
    };
    private static final int[] PAGE_COLORS = {
            COLOR_GOLD, COLOR_GOLD, COLOR_ACCENT, COLOR_ZOMBIE, COLOR_INK_HI, COLOR_GIANT
    };

    private static final int ROW_H = 11;
    private static final int BTN_W = 23, BTN_H = 13;
    private static final int TOTAL_PAGES = 6;

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
        prevBtnY = nextBtnY = bookY + BOOK_H - BTN_H - 4;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 半透明背景(能看到游戏画面)
        context.fill(0, 0, this.width, this.height, COLOR_BG_DARK);

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        World world = client.player.getWorld();
        if (world == null) return;

        TextRenderer tr = client.textRenderer;

        // 书本背景纹理(9参数: 指定纹理宽高292x180, 避免256回绕)
        context.drawTexture(BOOK_TEXTURE, bookX, bookY, 0, 0, BOOK_W, BOOK_H, BOOK_W, BOOK_H);

        int leftX = bookX + PAGE_LEFT_X;
        int rightX = bookX + PAGE_RIGHT_X;
        int bodyTop = bookY + PAGE_BODY_Y;

        // 左页: 章节封面
        drawChapterCover(context, tr, leftX, bookY + PAGE_HEADER_Y, PAGE_W, currentPage);

        // 右页: 内容(用scissor裁剪防止超出)
        context.enableScissor(bookX + PAGE_RIGHT_X - 2, bookY + PAGE_TOP,
                bookX + PAGE_RIGHT_X + PAGE_W + 2, bookY + PAGE_BOTTOM);
        renderPageContent(context, tr, world, rightX, bodyTop, PAGE_W, currentPage);
        context.disableScissor();

        // 翻页按钮
        prevHovered = mouseX >= prevBtnX && mouseX <= prevBtnX + BTN_W && mouseY >= prevBtnY && mouseY <= prevBtnY + BTN_H;
        nextHovered = mouseX >= nextBtnX && mouseX <= nextBtnX + BTN_W && mouseY >= nextBtnY && mouseY <= nextBtnY + BTN_H;
        drawPageButton(context, tr, prevBtnX, prevBtnY, "◀", currentPage > 0, prevHovered);
        drawPageButton(context, tr, nextBtnX, nextBtnY, "▶", currentPage < TOTAL_PAGES - 1, nextHovered);

        // 页码
        String pageInfo = (currentPage + 1) + " / " + TOTAL_PAGES;
        context.drawCenteredTextWithShadow(tr, Text.literal(pageInfo), bookX + BOOK_W / 2,
                bookY + BOOK_H - 14, COLOR_INK_DIM);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (prevHovered && currentPage > 0) { currentPage--; return true; }
        if (nextHovered && currentPage < TOTAL_PAGES - 1) { currentPage++; return true; }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() { return false; }

    // ===================== 左页: 章节封面 =====================

    /**
     * 左页作为章节封面: 大图标 + 章节标题 + 装饰线 + 章节描述
     */
    private void drawChapterCover(DrawContext ctx, TextRenderer tr, int x, int y, int w, int page) {
        int color = PAGE_COLORS[page];
        String icon = PAGE_ICONS[page];
        String title = PAGE_TITLES[page];
        int centerX = x + w / 2;

        // 大图标(居中, 放大显示用文字模拟)
        ctx.drawCenteredTextWithShadow(tr, Text.literal(icon), centerX, y + 8, color);
        // 主标题
        ctx.drawCenteredTextWithShadow(tr, Text.literal(title), centerX, y + 22, color);
        // 装饰双下划线
        ctx.fill(x + 20, y + 34, x + w - 20, y + 35, color);
        ctx.fill(x + 30, y + 36, x + w - 30, y + 37, 0x558B6B45);

        // 章节描述(封面下半)
        int descY = y + 46;
        String[] desc = getPageDescription(page);
        for (String line : desc) {
            ctx.drawTextWithShadow(tr, Text.literal(line), x + 6, descY, COLOR_INK_DIM);
            descY += ROW_H;
        }
    }

    private String[] getPageDescription(int page) {
        return switch (page) {
            case 0 -> new String[]{
                    "惊变100天生存手册",
                    "",
                    "记录当前世界末日进程,",
                    "包含僵尸属性、AI能力、",
                    "巨型僵尸情报等。",
                    "",
                    "翻页查看详细情报。"
            };
            case 1 -> new String[]{
                    "记录当前天数与血月周期",
                    "",
                    "• 100天生存挑战",
                    "• 血月随机刷新",
                    "• 阶段进度追踪"
            };
            case 2 -> new String[]{
                    "僵尸智能度与AI能力",
                    "",
                    "• 6级智能度系统",
                    "• 拆搭方块能力",
                    "• 呼叫增援机制"
            };
            case 3 -> new String[]{
                    "普通僵尸属性档案",
                    "",
                    "• 血量/攻击/护甲/速度",
                    "• 随阶段线性增长",
                    "• 血月额外加成"
            };
            case 4 -> new String[]{
                    "僵尸战斗特性与加成",
                    "",
                    "• 夜晚/血月加成",
                    "• 低血量狂暴",
                    "• 破坏方块行为"
            };
            case 5 -> new String[]{
                    "巨型僵尸档案",
                    "",
                    "• 2倍大小, 第40天起",
                    "• 2.5倍攻击范围",
                    "• 丰富掉落物"
            };
            default -> new String[]{""};
        };
    }

    // ===================== 右页: 内容分发 =====================

    private void renderPageContent(DrawContext ctx, TextRenderer tr, World world,
                                   int x, int y, int w, int page) {
        // 页面标题
        drawPageHeader(ctx, tr, x, y - 12, w, PAGE_TITLES[page], PAGE_COLORS[page]);

        switch (page) {
            case 0 -> renderCoverPage(ctx, tr, world, x, y, w);
            case 1 -> renderCalendarPage(ctx, tr, world, x, y, w);
            case 2 -> renderIntelligencePage(ctx, tr, world, x, y, w);
            case 3 -> renderZombieStatsPage(ctx, tr, world, x, y, w);
            case 4 -> renderZombieBehaviorPage(ctx, tr, world, x, y, w);
            case 5 -> renderGiantPage(ctx, tr, world, x, y, w);
        }
    }

    /**
     * 右页顶部小标题(区别于左页大封面)
     */
    private void drawPageHeader(DrawContext ctx, TextRenderer tr, int x, int y, int w, String title, int color) {
        ctx.drawCenteredTextWithShadow(tr, Text.literal(title), x + w / 2, y, color);
        ctx.fill(x + 30, y + 10, x + w - 30, y + 11, 0x558B6B45);
    }

    // ===================== 第1页: 封面/目录 =====================

    private void renderCoverPage(DrawContext ctx, TextRenderer tr, World world, int x, int y, int w) {
        int currentDay = StageSystem.getCurrentStage(world);
        boolean isBloodMoon = StageSystem.isBloodMoon(world);
        int intelLevel = StageSystem.getIntelligenceLevel(world);

        int cy = y;
        drawSectionTitle(ctx, tr, x, cy, w, "当前状态", COLOR_GOLD); cy += 14;
        drawRow(ctx, tr, x, cy, w, "天数", currentDay + " / " + ModConfig.TOTAL_DAYS, COLOR_INK_HI); cy += ROW_H;
        String bmText = isBloodMoon ? "进行中" : "~" + StageSystem.getDaysToNextBloodMoon(world) + "天后";
        drawRow(ctx, tr, x, cy, w, "血月", bmText, isBloodMoon ? COLOR_BLOOD_MOON : COLOR_INK); cy += ROW_H;
        String[] intelNames = {"迟钝", "普通", "机敏", "狡猾", "凶残", "嗜血"};
        drawRow(ctx, tr, x, cy, w, "智能度", "Lv" + intelLevel, intelLevel >= 4 ? COLOR_DANGER : COLOR_INK_HI); cy += ROW_H;
        drawRow(ctx, tr, x, cy, w, "阶段", getStageTip(currentDay), COLOR_INK_HI); cy += ROW_H + 6;

        drawSectionTitle(ctx, tr, x, cy, w, "目录", COLOR_ACCENT); cy += 14;
        ctx.drawTextWithShadow(tr, Text.literal("P2  末日历法"), x, cy, COLOR_INK_DIM); cy += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("P3  智能与AI"), x, cy, COLOR_INK_DIM); cy += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("P4  僵尸档案"), x, cy, COLOR_INK_DIM); cy += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("P5  战斗特性"), x, cy, COLOR_INK_DIM); cy += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("P6  巨型僵尸"), x, cy, COLOR_INK_DIM);
    }

    // ===================== 第2页: 末日历法 =====================

    private void renderCalendarPage(DrawContext ctx, TextRenderer tr, World world, int x, int y, int w) {
        int currentDay = StageSystem.getCurrentStage(world);
        double progress = StageSystem.getStageProgress(world);
        boolean isBloodMoon = StageSystem.isBloodMoon(world);

        int cy = y;
        drawSectionTitle(ctx, tr, x, cy, w, "末日历法", COLOR_GOLD); cy += 14;
        drawRow(ctx, tr, x, cy, w, "当前天数", currentDay + " / " + ModConfig.TOTAL_DAYS, COLOR_INK_HI); cy += ROW_H;
        drawRow(ctx, tr, x, cy, w, "总天数", String.valueOf(ModConfig.TOTAL_DAYS), COLOR_INK); cy += ROW_H + 4;

        // 进度条
        int barX = x + 2;
        int barW = w - 4;
        ctx.fill(barX, cy, barX + barW, cy + 1, 0xFF8B6B45);
        int filledW = (int) (barW * progress);
        if (filledW > 0) ctx.fill(barX, cy, barX + filledW, cy + 1, getProgressColor(progress));
        ctx.drawTextWithShadow(tr, Text.literal((int)(progress * 100) + "%"), x + w / 2 - 6, cy + 2, COLOR_INK_DIM);
        cy += 14;

        drawRow(ctx, tr, x, cy, w, "阶段", getStageTip(currentDay), COLOR_INK_HI); cy += ROW_H + 6;

        // 血月
        drawSectionTitle(ctx, tr, x, cy, w, "血月周期", COLOR_BLOOD_MOON); cy += 14;
        int daysToBM = StageSystem.getDaysToNextBloodMoon(world);
        String bmText = isBloodMoon ? "今日血月" : "~" + daysToBM + "天后";
        drawRow(ctx, tr, x, cy, w, "下次血月", bmText, isBloodMoon ? COLOR_BLOOD_MOON : COLOR_INK); cy += ROW_H;
        drawRow(ctx, tr, x, cy, w, "刷新倍率", isBloodMoon ? "x2.0" : "x1.0", isBloodMoon ? COLOR_BLOOD_MOON : COLOR_INK_DIM); cy += ROW_H + 6;

        // 生存提示
        drawSectionTitle(ctx, tr, x, cy, w, "生存提示", COLOR_BLUE); cy += 14;
        String[][] tips = getSurvivalTips(currentDay, isBloodMoon);
        for (String[] t : tips) {
            ctx.drawTextWithShadow(tr, Text.literal("• " + t[0]), x, cy, Integer.parseUnsignedInt(t[1], 16));
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
        drawSectionTitle(ctx, tr, x, cy, w, "智能度", COLOR_ACCENT); cy += 14;
        String[] intelNames = {"迟钝", "普通", "机敏", "狡猾", "凶残", "嗜血"};
        int[] intelColors = {COLOR_GREEN, COLOR_GOLD, 0xFF8B6914, COLOR_DANGER, COLOR_BLOOD_MOON, COLOR_BLOOD_MOON};
        drawRow(ctx, tr, x, cy, w, "等级", "Lv" + intelLevel, intelColors[intelLevel]); cy += ROW_H;
        drawRow(ctx, tr, x, cy, w, "命名", intelNames[intelLevel], intelColors[intelLevel]); cy += ROW_H;
        drawRow(ctx, tr, x, cy, w, "追踪范围", followRange + "格", COLOR_INK); cy += ROW_H + 6;

        drawSectionTitle(ctx, tr, x, cy, w, "AI能力", COLOR_INK_HI); cy += 14;
        drawRow(ctx, tr, x, cy, w, "拆方块间隔", String.format("%.1fs", breakInt / 20.0), COLOR_INK); cy += ROW_H;
        drawRow(ctx, tr, x, cy, w, "搭方块间隔", String.format("%.1fs", buildInt / 20.0), COLOR_INK); cy += ROW_H;
        drawRow(ctx, tr, x, cy, w, "硬度上限", String.format("%.0f", hardLimit), COLOR_INK); cy += ROW_H;
        drawRow(ctx, tr, x, cy, w, "库存容量", invSize + "格", COLOR_INK); cy += ROW_H;
        String reinStr = reinChance > 0 ? String.format("%.0f%%", reinChance * 100) : "未解锁";
        drawRow(ctx, tr, x, cy, w, "呼叫增援", reinStr, reinChance > 0 ? COLOR_DANGER : COLOR_INK_DIM); cy += ROW_H + 6;

        drawSectionTitle(ctx, tr, x, cy, w, "智能度阶梯", COLOR_GOLD); cy += 14;
        ctx.drawTextWithShadow(tr, Text.literal("Lv0 1-10天 迟钝"), x, cy, COLOR_GREEN); cy += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("Lv1 11-20天 普通"), x, cy, COLOR_GOLD); cy += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("Lv2 21-30天 机敏"), x, cy, 0xFF8B6914); cy += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("Lv3 31-50天 狡猾"), x, cy, COLOR_DANGER);
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
        drawSectionTitle(ctx, tr, x, cy, w, "基础属性", COLOR_ZOMBIE); cy += 14;
        drawRowExtra(ctx, tr, x, cy, w, "血量", String.format("%.0f", health),
                String.format("(x%.1f)", health / 20), COLOR_DANGER); cy += ROW_H;
        drawRowExtra(ctx, tr, x, cy, w, "攻击", String.format("%.1f", attack),
                atkMult > 1 ? String.format("x%.1f", atkMult) : "", COLOR_INK_HI); cy += ROW_H;
        drawRow(ctx, tr, x, cy, w, "护甲", String.format("%.0f", armor), COLOR_INK); cy += ROW_H;
        drawRowExtra(ctx, tr, x, cy, w, "速度", String.format("%.2f", speed),
                spdMult > 1 ? String.format("x%.2f", spdMult) : "", COLOR_INK); cy += ROW_H + 6;

        drawSectionTitle(ctx, tr, x, cy, w, "属性范围", COLOR_GOLD); cy += 14;
        ctx.drawTextWithShadow(tr, Text.literal("第1天 → 第100天"), x, cy, COLOR_INK_DIM); cy += ROW_H;
        drawRow(ctx, tr, x, cy, w, "血量", "20 → 400", COLOR_INK); cy += ROW_H;
        drawRow(ctx, tr, x, cy, w, "攻击", "3 → 15", COLOR_INK); cy += ROW_H;
        drawRow(ctx, tr, x, cy, w, "护甲", "2 → 12", COLOR_INK); cy += ROW_H;
        drawRow(ctx, tr, x, cy, w, "速度", "0.23 → 0.35", COLOR_INK); cy += ROW_H + 6;

        drawSectionTitle(ctx, tr, x, cy, w, "血月加成", COLOR_BLOOD_MOON); cy += 14;
        drawRow(ctx, tr, x, cy, w, "攻击倍率", "+20%", COLOR_BLOOD_MOON); cy += ROW_H;
        drawRow(ctx, tr, x, cy, w, "速度倍率", "+30%", COLOR_BLOOD_MOON);
    }

    // ===================== 第5页: 战斗特性 =====================

    private void renderZombieBehaviorPage(DrawContext ctx, TextRenderer tr, World world, int x, int y, int w) {
        boolean isBloodMoon = StageSystem.isBloodMoon(world);
        boolean isNight = !world.isDay();

        int cy = y;
        drawSectionTitle(ctx, tr, x, cy, w, "当前加成", COLOR_BLUE); cy += 14;
        drawRow(ctx, tr, x, cy, w, "夜晚速度", isNight ? "+15% 激活" : "未激活",
                isNight ? COLOR_GREEN : COLOR_INK_DIM); cy += ROW_H;
        drawRow(ctx, tr, x, cy, w, "血月速度", isBloodMoon ? "+30% 激活" : "未激活",
                isBloodMoon ? COLOR_BLOOD_MOON : COLOR_INK_DIM); cy += ROW_H;
        drawRow(ctx, tr, x, cy, w, "血月攻击", isBloodMoon ? "+20% 激活" : "未激活",
                isBloodMoon ? COLOR_BLOOD_MOON : COLOR_INK_DIM); cy += ROW_H;
        drawRow(ctx, tr, x, cy, w, "低血狂暴", "<30% +50%速", COLOR_DANGER); cy += ROW_H + 6;

        drawSectionTitle(ctx, tr, x, cy, w, "行为特性", COLOR_INK_HI); cy += 14;
        ctx.drawTextWithShadow(tr, Text.literal("• 防白天燃烧"), x, cy, COLOR_INK); cy += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("• 第20天后拆搭方块"), x, cy, COLOR_INK); cy += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("• 优先破坏门窗玻璃"), x, cy, COLOR_INK); cy += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("• 卡住时加速突破"), x, cy, COLOR_INK); cy += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("• 朝玩家方向搭楼梯"), x, cy, COLOR_INK); cy += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("• 空中搭天桥追击"), x, cy, COLOR_INK); cy += ROW_H + 6;

        drawSectionTitle(ctx, tr, x, cy, w, "威胁警告", COLOR_DANGER); cy += 14;
        ctx.drawTextWithShadow(tr, Text.literal("• 高墙无法完全阻挡"), x, cy, COLOR_DANGER); cy += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("• 备足武器药水"), x, cy, COLOR_DANGER);
    }

    // ===================== 第6页: 巨型僵尸 =====================

    private void renderGiantPage(DrawContext ctx, TextRenderer tr, World world, int x, int y, int w) {
        double health = StageSystem.getGiantZombieHealth(world);
        double attack = StageSystem.getGiantZombieAttack(world);
        double chance = StageSystem.getGiantZombieChance(world);
        int followRange = StageSystem.getFollowRange(world);
        double atkMult = StageSystem.getAttackMultiplier(world);

        int cy = y;
        drawSectionTitle(ctx, tr, x, cy, w, "巨型属性", COLOR_GIANT); cy += 14;
        ctx.drawTextWithShadow(tr, Text.literal("(2倍大小 · 第40天起)"), x, cy, COLOR_INK_DIM); cy += ROW_H;
        drawRowExtra(ctx, tr, x, cy, w, "血量", String.format("%.0f", health),
                String.format("(x%.1f)", health / 400), COLOR_GIANT); cy += ROW_H;
        drawRowExtra(ctx, tr, x, cy, w, "攻击", String.format("%.1f", attack),
                atkMult > 1 ? String.format("x%.1f", atkMult) : "", COLOR_GIANT); cy += ROW_H;
        drawRow(ctx, tr, x, cy, w, "生成概率", String.format("%.1f%%", chance * 100), COLOR_GIANT); cy += ROW_H;
        drawRow(ctx, tr, x, cy, w, "追踪范围", followRange + "格", COLOR_INK); cy += ROW_H + 6;

        drawSectionTitle(ctx, tr, x, cy, w, "特殊能力", COLOR_INK_HI); cy += 14;
        ctx.drawTextWithShadow(tr, Text.literal("• 2.5倍攻击距离"), x, cy, COLOR_INK); cy += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("• 可跨越2格高墙"), x, cy, COLOR_INK); cy += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("• 防火免疫"), x, cy, COLOR_GREEN); cy += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("• 血月时属性增强"), x, cy, COLOR_INK); cy += ROW_H + 6;

        drawSectionTitle(ctx, tr, x, cy, w, "掉落物", COLOR_GOLD); cy += 14;
        ctx.drawTextWithShadow(tr, Text.literal("腐肉 3-8 · 骨头 3-6"), x, cy, COLOR_INK); cy += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("铁锭 2-5 · 金锭 1-3"), x, cy, COLOR_INK); cy += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("钻石 0-2 · 绿宝石 0-3"), x, cy, COLOR_INK_HI); cy += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("附魔金苹果 0-1"), x, cy, COLOR_GIANT); cy += ROW_H;
        ctx.drawTextWithShadow(tr, Text.literal("经验瓶 1-5"), x, cy, COLOR_GREEN);
    }

    // ===================== 辅助方法 =====================

    private void drawSectionTitle(DrawContext ctx, TextRenderer tr, int x, int y, int w, String title, int color) {
        ctx.drawTextWithShadow(tr, Text.literal("§ " + title), x, y, color);
        ctx.fill(x, y + 10, x + w, y + 11, 0x558B6B45);
    }

    private void drawRow(DrawContext ctx, TextRenderer tr, int x, int y, int w,
                         String label, String value, int valueColor) {
        ctx.drawTextWithShadow(tr, Text.literal(label), x, y, COLOR_INK_DIM);
        ctx.drawTextWithShadow(tr, Text.literal(value), x + w - tr.getWidth(value) - 1, y, valueColor);
    }

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
