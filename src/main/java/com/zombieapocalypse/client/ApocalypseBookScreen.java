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
 * 末日情报书界面 - v3.1.0
 * 使用原版成书纹理的双页书本布局，三个章节通过翻页按钮切换:
 *   第1展开页 - 基础信息(左:天数/血月/智能度, 右:AI能力/生存提示)
 *   第2展开页 - 僵尸详情(左:属性, 右:战斗能力/加成)
 *   第3展开页 - 巨型僵尸详情(左:属性, 右:掉落物)
 * 内容重新分配到左右页, 单页可容纳, 无需滚动。
 */
public class ApocalypseBookScreen extends Screen {

    // 原版成书纹理(双页书本背景 292x180)
    private static final Identifier BOOK_TEXTURE = new Identifier("minecraft", "textures/gui/book.png");
    private static final int BOOK_W = 292;
    private static final int BOOK_H = 180;

    // 配色方案(暗黑典籍风)
    private static final int COLOR_BG_DARK    = 0xF0100A0A;
    private static final int COLOR_BG_CARD    = 0x881A0A0A;
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

    // 章节标题与配色
    private static final String[] CHAPTER_TITLES = {"基础情报", "僵尸档案", "巨型僵尸"};
    private static final int[] CHAPTER_COLORS = {COLOR_TEXT_HI, COLOR_ZOMBIE, COLOR_GIANT};

    private int bookX, bookY;
    private int currentSpread = 0;   // 0=基础, 1=僵尸, 2=巨型
    private final int maxSpread = 2;
    // 翻页按钮区域(在 mouseClicked/render 中使用)
    private int prevBtnX, prevBtnY, nextBtnX, nextBtnY;
    private static final int BTN_W = 23, BTN_H = 13;
    private boolean prevHovered, nextHovered;

    public ApocalypseBookScreen() {
        super(Text.literal("末日情报"));
    }

    @Override
    protected void init() {
        bookX = (this.width - BOOK_W) / 2;
        bookY = (this.height - BOOK_H) / 2;
        // 翻页按钮位于书本底部两侧
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

        // 书页区域半透明暗色遮罩, 让末日配色文字可读
        int pageMargin = 16;
        context.fill(bookX + pageMargin, bookY + pageMargin,
                bookX + BOOK_W - pageMargin, bookY + BOOK_H - pageMargin - 4, 0x66050005);
        // 书脊暗化
        context.fill(bookX + BOOK_W / 2 - 1, bookY + pageMargin,
                bookX + BOOK_W / 2 + 1, bookY + BOOK_H - pageMargin - 4, 0x55000000);

        // 左右页内容区域
        int leftX = bookX + 22;
        int rightX = bookX + BOOK_W / 2 + 10;
        int pageW = BOOK_W / 2 - 32;   // 约 114
        int contentTop = bookY + 18;
        int contentBottom = bookY + BOOK_H - 22;

        // 章节标题(每页顶部居中)
        String chapterTitle = "☠ " + CHAPTER_TITLES[currentSpread] + " ☠";
        int titleColor = CHAPTER_COLORS[currentSpread];
        context.drawCenteredTextWithShadow(tr, Text.literal(chapterTitle),
                bookX + BOOK_W / 4, contentTop - 6, titleColor);
        context.drawCenteredTextWithShadow(tr, Text.literal(chapterTitle),
                bookX + BOOK_W * 3 / 4, contentTop - 6, titleColor);

        // 血月横幅(若有)
        boolean isBloodMoon = StageSystem.isBloodMoon(world);
        if (isBloodMoon) {
            int bmY = contentTop + 6;
            context.fill(leftX, bmY, leftX + pageW, bmY + 11, 0xCC660000);
            context.fill(leftX, bmY, leftX + pageW, bmY + 1, COLOR_BLOOD_MOON);
            context.fill(leftX, bmY + 10, leftX + pageW, bmY + 11, COLOR_BLOOD_MOON);
            context.drawCenteredTextWithShadow(tr, Text.literal("● 血月进行中 ●"),
                    leftX + pageW / 2, bmY + 1, COLOR_BLOOD_MOON);
        }

        // 章节内容
        switch (currentSpread) {
            case 0 -> renderBasicSpread(context, tr, world, leftX, rightX, contentTop + (isBloodMoon ? 18 : 4), pageW, contentBottom);
            case 1 -> renderZombieSpread(context, tr, world, leftX, rightX, contentTop + 4, pageW, contentBottom);
            case 2 -> renderGiantSpread(context, tr, world, leftX, rightX, contentTop + 4, pageW, contentBottom);
        }

        // 翻页按钮
        prevHovered = mouseX >= prevBtnX && mouseX <= prevBtnX + BTN_W && mouseY >= prevBtnY && mouseY <= prevBtnY + BTN_H;
        nextHovered = mouseX >= nextBtnX && mouseX <= nextBtnX + BTN_W && mouseY >= nextBtnY && mouseY <= nextBtnY + BTN_H;
        drawPageButton(context, tr, prevBtnX, prevBtnY, "◀", currentSpread > 0, prevHovered);
        drawPageButton(context, tr, nextBtnX, nextBtnY, "▶", currentSpread < maxSpread, nextHovered);

        // 页码与提示
        String pageInfo = (currentSpread + 1) + " / " + (maxSpread + 1);
        context.drawCenteredTextWithShadow(tr, Text.literal(pageInfo), bookX + BOOK_W / 2, bookY + BOOK_H - 14, COLOR_TEXT_DIM);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (prevHovered && currentSpread > 0) {
            currentSpread--;
            return true;
        }
        if (nextHovered && currentSpread < maxSpread) {
            currentSpread++;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    // ===================== 翻页按钮 =====================

    private void drawPageButton(DrawContext ctx, TextRenderer tr, int x, int y, String arrow, boolean enabled, boolean hovered) {
        int bg = enabled ? (hovered ? 0xCC3A0808 : 0x881A0A0A) : 0x33000000;
        int border = enabled ? (hovered ? COLOR_ACCENT : COLOR_BORDER_DIM) : 0x44222222;
        int col = enabled ? (hovered ? 0xFFFF6666 : COLOR_TEXT_DIM) : 0xFF555555;
        ctx.fill(x, y, x + BTN_W, y + BTN_H, bg);
        ctx.fill(x, y, x + BTN_W, y + 1, border);
        ctx.fill(x, y + BTN_H - 1, x + BTN_W, y + BTN_H, border);
        ctx.fill(x, y, x + 1, y + BTN_H, border);
        ctx.fill(x + BTN_W - 1, y, x + BTN_W, y + BTN_H, border);
        ctx.drawCenteredTextWithShadow(tr, Text.literal(arrow), x + BTN_W / 2, y + 2, col);
    }

    // ===================== 第1展开页: 基础情报 =====================

    private void renderBasicSpread(DrawContext ctx, TextRenderer tr, World world,
                                   int leftX, int rightX, int top, int pageW, int bottom) {
        int currentDay = StageSystem.getCurrentStage(world);
        double progress = StageSystem.getStageProgress(world);
        boolean isBloodMoon = StageSystem.isBloodMoon(world);
        int intelLevel = StageSystem.getIntelligenceLevel(world);

        // ===== 左页: 天数 + 智能度 + 进度 =====
        int y = top;
        // 天数卡片
        drawCard(ctx, leftX, y, pageW, 30);
        String dayStr = String.valueOf(currentDay);
        ctx.drawTextWithShadow(tr, Text.literal("第"), leftX + 6, y + 5, COLOR_TEXT_DIM);
        ctx.drawTextWithShadow(tr, Text.literal(dayStr), leftX + 16, y + 5, COLOR_TEXT_HI);
        ctx.drawTextWithShadow(tr, Text.literal("天 /" + ModConfig.TOTAL_DAYS),
                leftX + 16 + tr.getWidth(dayStr) + 2, y + 5, COLOR_TEXT_DIM);
        // 血月倒计时
        int daysToBM = StageSystem.getDaysToNextBloodMoon(world);
        String bmText = isBloodMoon ? "● 血月今日" : "下次血月: ~" + daysToBM + "天后";
        ctx.drawTextWithShadow(tr, Text.literal(bmText), leftX + 6, y + 16,
                isBloodMoon ? COLOR_BLOOD_MOON : COLOR_TEXT_DIM);
        // 进度条
        int barY = y + 26;
        ctx.fill(leftX + 6, barY, leftX + pageW - 6, barY + 3, 0xFF050505);
        int filledW = (int) ((pageW - 12) * progress);
        if (filledW > 0) ctx.fill(leftX + 6, barY, leftX + 6 + filledW, barY + 3, getProgressColor(progress));

        y += 32;
        // 智能度卡片
        drawCard(ctx, leftX, y, pageW, 20);
        ctx.drawTextWithShadow(tr, Text.literal("智能度"), leftX + 6, y + 6, COLOR_TEXT_DIM);
        String[] intelNames = {"迟钝", "普通", "机敏", "狡猾", "凶残", "嗜血"};
        int[] intelColors = {0xFF33AA33, 0xFFFFAA00, 0xFFCC6622, 0xFFFF3333, 0xFFFF0000, 0xFFFF0000};
        String intelText = "Lv" + intelLevel + " " + intelNames[intelLevel];
        ctx.drawTextWithShadow(tr, Text.literal(intelText), leftX + pageW - 6 - tr.getWidth(intelText), y + 6, intelColors[intelLevel]);

        y += 22;
        // 阶段提示卡片
        drawCard(ctx, leftX, y, pageW, 20);
        ctx.drawTextWithShadow(tr, Text.literal("阶段"), leftX + 6, y + 6, COLOR_TEXT_DIM);
        String tip = getStageTip(currentDay);
        ctx.drawTextWithShadow(tr, Text.literal(tip), leftX + pageW - 6 - tr.getWidth(tip), y + 6, COLOR_TEXT_HI);

        // ===== 右页: AI能力 + 生存提示 =====
        int ry = top;
        int breakInt = StageSystem.getBreakInterval(world);
        int buildInt = StageSystem.getBuildInterval(world);
        float hardLimit = StageSystem.getHardnessLimit(world);
        double reinChance = StageSystem.getReinforcementChance(world);
        int invSize = StageSystem.getBlockInventorySize(world);

        drawCard(ctx, rightX, ry, pageW, 4 + 11 * 4 + 4);
        ctx.drawTextWithShadow(tr, Text.literal("⚙ AI能力"), rightX + 6, ry + 4, COLOR_TEXT_HI);
        int aiY = ry + 16;
        ctx.drawTextWithShadow(tr, Text.literal(String.format("拆: %.1fs  搭: %.1fs", breakInt / 20.0, buildInt / 20.0)),
                rightX + 6, aiY, COLOR_TEXT);
        aiY += 11;
        ctx.drawTextWithShadow(tr, Text.literal(String.format("硬度: %.0f  库存: %d", hardLimit, invSize)),
                rightX + 6, aiY, COLOR_TEXT);
        aiY += 11;
        String reinStr = reinChance > 0 ? String.format("增援: %.0f%%", reinChance * 100) : "增援: 未解锁";
        ctx.drawTextWithShadow(tr, Text.literal(reinStr), rightX + 6, aiY, reinChance > 0 ? COLOR_DANGER : COLOR_TEXT_DIM);
        aiY += 11;
        ctx.drawTextWithShadow(tr, Text.literal("夜+15%速 血月+30%速"), rightX + 6, aiY, COLOR_TEXT);

        ry += 4 + 11 * 4 + 4 + 2;
        // 生存提示卡片
        String[][] tips = getSurvivalTips(currentDay, isBloodMoon);
        int tipCardH = 4 + 11 * tips.length + 4;
        drawCard(ctx, rightX, ry, pageW, tipCardH);
        ctx.drawTextWithShadow(tr, Text.literal("★ 生存提示"), rightX + 6, ry + 4, 0xFF66DDFF);
        int tipY = ry + 16;
        for (String[] t : tips) {
            ctx.drawTextWithShadow(tr, Text.literal(t[0]), rightX + 6, tipY, Integer.parseUnsignedInt(t[1], 16));
            tipY += 11;
        }
    }

    // ===================== 第2展开页: 僵尸档案 =====================

    private void renderZombieSpread(DrawContext ctx, TextRenderer tr, World world,
                                    int leftX, int rightX, int top, int pageW, int bottom) {
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
        drawCard(ctx, leftX, y, pageW, 4 + 11 * 5 + 4);
        ctx.drawTextWithShadow(tr, Text.literal("⚔ 属性"), leftX + 6, y + 4, COLOR_ZOMBIE);
        int ry = y + 16;
        drawDetailRow(ctx, tr, leftX + 6, ry, pageW - 12, "血量", String.format("%.0f/20", health),
                String.format("(%.1fx)", health / 20), COLOR_DANGER); ry += 11;
        double atkMult = StageSystem.getAttackMultiplier(world);
        drawDetailRow(ctx, tr, leftX + 6, ry, pageW - 12, "攻击", String.format("%.1f", attack),
                atkMult > 1 ? String.format("(x%.1f)", atkMult) : "", COLOR_TEXT_HI); ry += 11;
        drawDetailRow(ctx, tr, leftX + 6, ry, pageW - 12, "护甲", String.format("%.0f", armor), "", COLOR_TEXT); ry += 11;
        double spdMult = StageSystem.getSpeedMultiplier(world, 1.0f);
        drawDetailRow(ctx, tr, leftX + 6, ry, pageW - 12, "速度", String.format("%.2f", speed),
                spdMult > 1 ? String.format("(x%.2f)", spdMult) : "", COLOR_TEXT); ry += 11;
        drawDetailRow(ctx, tr, leftX + 6, ry, pageW - 12, "追踪", followRange + "格", "", COLOR_TEXT);

        // 右页: 战斗能力 + 加成
        int rry = top;
        drawCard(ctx, rightX, rry, pageW, 4 + 11 * 4 + 4);
        ctx.drawTextWithShadow(tr, Text.literal("⚙ 战斗能力"), rightX + 6, rry + 4, COLOR_TEXT_HI);
        int cy = rry + 16;
        drawDetailRow(ctx, tr, rightX + 6, cy, pageW - 12, "拆方块", String.format("%.1fs", breakInt / 20.0), "", COLOR_TEXT); cy += 11;
        drawDetailRow(ctx, tr, rightX + 6, cy, pageW - 12, "搭方块", String.format("%.1fs", buildInt / 20.0), "", COLOR_TEXT); cy += 11;
        drawDetailRow(ctx, tr, rightX + 6, cy, pageW - 12, "硬度上限", String.format("%.0f", hardLimit), "", COLOR_TEXT); cy += 11;
        String reinStr = reinChance > 0 ? String.format("%.0f%%", reinChance * 100) : "未解锁";
        drawDetailRow(ctx, tr, rightX + 6, cy, pageW - 12, "呼叫增援", reinStr, "", reinChance > 0 ? COLOR_DANGER : COLOR_TEXT_DIM);

        rry += 4 + 11 * 4 + 4 + 2;
        drawCard(ctx, rightX, rry, pageW, 4 + 11 * 3 + 4);
        ctx.drawTextWithShadow(tr, Text.literal("★ 当前加成"), rightX + 6, rry + 4, 0xFF66DDFF);
        int by = rry + 16;
        drawDetailRow(ctx, tr, rightX + 6, by, pageW - 12, "夜晚加成", isNight ? "激活 +15%速" : "未激活", "",
                isNight ? 0xFF33AA33 : COLOR_TEXT_DIM); by += 11;
        drawDetailRow(ctx, tr, rightX + 6, by, pageW - 12, "血月加成", isBloodMoon ? "激活 +30%速" : "未激活", "",
                isBloodMoon ? COLOR_BLOOD_MOON : COLOR_TEXT_DIM); by += 11;
        drawDetailRow(ctx, tr, rightX + 6, by, pageW - 12, "库存容量", invSize + "格", "", COLOR_TEXT);
    }

    // ===================== 第3展开页: 巨型僵尸 =====================

    private void renderGiantSpread(DrawContext ctx, TextRenderer tr, World world,
                                   int leftX, int rightX, int top, int pageW, int bottom) {
        double health = StageSystem.getGiantZombieHealth(world);
        double attack = StageSystem.getGiantZombieAttack(world);
        double chance = StageSystem.getGiantZombieChance(world);
        int followRange = StageSystem.getFollowRange(world);

        // 左页: 属性
        int y = top;
        drawCard(ctx, leftX, y, pageW, 4 + 11 * 6 + 4);
        ctx.drawTextWithShadow(tr, Text.literal("▾ 巨型属性"), leftX + 6, y + 4, COLOR_GIANT);
        ctx.drawTextWithShadow(tr, Text.literal("(2x)"), leftX + pageW - 6 - tr.getWidth("(2x)"), y + 4, COLOR_TEXT_DIM);
        int ry = y + 16;
        double atkMult = StageSystem.getAttackMultiplier(world);
        drawDetailRow(ctx, tr, leftX + 6, ry, pageW - 12, "血量", String.format("%.0f/400", health),
                String.format("(%.1fx)", health / 400), COLOR_GIANT); ry += 11;
        drawDetailRow(ctx, tr, leftX + 6, ry, pageW - 12, "攻击", String.format("%.1f", attack),
                atkMult > 1 ? String.format("(x%.1f)", atkMult) : "", COLOR_GIANT); ry += 11;
        drawDetailRow(ctx, tr, leftX + 6, ry, pageW - 12, "生成概率", String.format("%.1f%%", chance * 100), "", COLOR_GIANT); ry += 11;
        drawDetailRow(ctx, tr, leftX + 6, ry, pageW - 12, "追踪", followRange + "格", "", COLOR_GIANT); ry += 11;
        drawDetailRow(ctx, tr, leftX + 6, ry, pageW - 12, "攻击范围", "2.5x", "", COLOR_GIANT); ry += 11;
        drawDetailRow(ctx, tr, leftX + 6, ry, pageW - 12, "防火", "免疫", "", COLOR_TEXT);

        // 右页: 掉落物
        int rry = top;
        drawCard(ctx, rightX, rry, pageW, 4 + 11 * 5 + 4);
        ctx.drawTextWithShadow(tr, Text.literal("◈ 掉落物"), rightX + 6, rry + 4, 0xFFFFAA00);
        int dy = rry + 16;
        ctx.drawTextWithShadow(tr, Text.literal("腐肉 3-8  骨头 3-6"), rightX + 6, dy, COLOR_TEXT); dy += 11;
        ctx.drawTextWithShadow(tr, Text.literal("铁锭 2-5  金锭 1-3"), rightX + 6, dy, COLOR_TEXT); dy += 11;
        ctx.drawTextWithShadow(tr, Text.literal("钻石 0-2  绿宝石 0-3"), rightX + 6, dy, COLOR_TEXT); dy += 11;
        ctx.drawTextWithShadow(tr, Text.literal("附魔金苹果 0-1"), rightX + 6, dy, 0xFFFF66FF); dy += 11;
        ctx.drawTextWithShadow(tr, Text.literal("经验瓶 1-5"), rightX + 6, dy, 0xFF33AA33);

        // 补充: 巨型特性说明
        rry += 4 + 11 * 5 + 4 + 2;
        drawCard(ctx, rightX, rry, pageW, 4 + 11 * 2 + 4);
        ctx.drawTextWithShadow(tr, Text.literal("☠ 特性"), rightX + 6, rry + 4, COLOR_GIANT);
        int fy = rry + 16;
        ctx.drawTextWithShadow(tr, Text.literal("可跨越2格高方块"), rightX + 6, fy, COLOR_TEXT); fy += 11;
        ctx.drawTextWithShadow(tr, Text.literal("2.5倍攻击范围 约束4.6格"), rightX + 6, fy, COLOR_TEXT);
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
        ctx.fill(x, y + h - 1, x + w, y + h, 0x44110000);
        ctx.fill(x, y, x + 1, y + h, 0x22FF3333);
    }
}
