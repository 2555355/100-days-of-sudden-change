package com.zombieapocalypse.client;

import com.zombieapocalypse.config.ModConfig;
import com.zombieapocalypse.config.StageSystem;
import net.minecraft.class_1937;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_437;

/**
 * 末日情报界面 - 修正版（intermediary 命名，可直接编进 .class 替换原文件）
 *
 * 修复点：
 *  1. 不再复用原版双页 book.png，改自绘羊皮纸面板，消除书脊错位
 *  2. scissor 裁剪区 = 真实内容区，防止溢出
 *  3. 去掉非法 "§ " 格式化前缀
 *  4. 翻页按钮 bg/border 在所有分支都赋值
 *  5. 标题分隔线/页码居中改为基于宽度的动态计算
 */
public class ApocalypseBookScreen extends class_437 {

    private static final int PANEL_W = 256;
    private static final int PANEL_H = 180;
    private static final int BORDER = 8;
    private static final int CONTENT_W = PANEL_W - BORDER * 2;
    private static final int CONTENT_H = PANEL_H - BORDER * 2;
    private static final int ROW_H = 10;
    private static final int TOTAL_PAGES = 6;

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
        "末日情报 - 目录", "末日历法", "智能与AI",
        "僵尸档案", "战斗特性", "巨型僵尸"
    };
    private static final int[] PAGE_COLORS = {
        COLOR_GOLD, COLOR_GOLD, COLOR_ACCENT,
        COLOR_ZOMBIE, COLOR_INK_HI, COLOR_GIANT
    };

    private int panelX, panelY;
    private int contentX, contentY;
    private int currentPage = 0;
    private int prevBtnX, prevBtnY, nextBtnX, nextBtnY;
    private boolean prevHovered, nextHovered;

    public ApocalypseBookScreen() {
        super((class_2561) class_2561.method_43470("末日情报"));
    }

    protected void method_25426() {
        this.panelX = (this.field_22789 - PANEL_W) / 2;
        this.panelY = (this.field_22790 - PANEL_H) / 2;
        this.contentX = this.panelX + BORDER;
        this.contentY = this.panelY + BORDER;
        int btnY = this.panelY + PANEL_H - 18;
        this.prevBtnX = this.panelX + BORDER;
        this.nextBtnX = this.panelX + PANEL_W - BORDER - 23;
        this.prevBtnY = this.nextBtnY = btnY;
    }

    public void method_25394(class_332 ctx, int mouseX, int mouseY, float delta) {
        ctx.method_25294(0, 0, this.field_22789, this.field_22790, COLOR_OVERLAY);
        class_310 client = class_310.method_1551();
        if (client.field_1724 == null) return;
        class_1937 world = client.field_1724.method_37908();
        if (world == null) return;
        class_327 tr = client.field_1772;

        this.drawPanelBackground(ctx);

        ctx.method_44379(this.contentX, this.contentY,
                         this.contentX + CONTENT_W, this.contentY + CONTENT_H);
        this.renderPage(ctx, tr, world, this.contentX, this.contentY, CONTENT_W, this.currentPage);
        ctx.method_44380();

        this.prevHovered = this.inBounds(mouseX, mouseY, this.prevBtnX, this.prevBtnY, 23, 12);
        this.nextHovered = this.inBounds(mouseX, mouseY, this.nextBtnX, this.nextBtnY, 23, 12);
        this.drawPageButton(ctx, tr, this.prevBtnX, this.prevBtnY, "<",
                this.currentPage > 0, this.prevHovered);
        this.drawPageButton(ctx, tr, this.nextBtnX, this.nextBtnY, ">",
                this.currentPage < TOTAL_PAGES - 1, this.nextHovered);

        String pageInfo = (this.currentPage + 1) + " / " + TOTAL_PAGES;
        ctx.method_27534(tr, (class_2561) class_2561.method_43470(pageInfo),
                this.panelX + PANEL_W / 2, this.prevBtnY + 2, COLOR_INK_DIM);
    }

    private void drawPanelBackground(class_332 ctx) {
        int x0 = this.panelX, y0 = this.panelY;
        int x1 = x0 + PANEL_W, y1 = y0 + PANEL_H;
        ctx.method_25294(x0 - 1, y0 - 1, x1 + 1, y1 + 1, COLOR_PANEL_BORDER);
        ctx.method_25294(x0, y0, x1, y1, COLOR_PANEL_BG);
        ctx.method_25294(x0 + 2, y0 + 2, x1 - 2, y1 - 2, COLOR_PANEL_INNER);
        ctx.method_25294(x0 + 3, y0 + 3, x1 - 3, y1 - 3, COLOR_PANEL_BG);
    }

    private void renderPage(class_332 ctx, class_327 tr, class_1937 world,
                            int x, int y, int w, int page) {
        this.drawTitle(ctx, tr, x, y, w, PAGE_TITLES[page], PAGE_COLORS[page]);
        int bodyY = y + 16;
        if (page == 0) this.renderCoverPage(ctx, tr, world, x, bodyY, w);
        else if (page == 1) this.renderCalendarPage(ctx, tr, world, x, bodyY, w);
        else if (page == 2) this.renderIntelligencePage(ctx, tr, world, x, bodyY, w);
        else if (page == 3) this.renderZombieStatsPage(ctx, tr, world, x, bodyY, w);
        else if (page == 4) this.renderZombieBehaviorPage(ctx, tr, world, x, bodyY, w);
        else if (page == 5) this.renderGiantPage(ctx, tr, world, x, bodyY, w);
    }

    private void drawTitle(class_332 ctx, class_327 tr, int x, int y, int w,
                           String title, int color) {
        ctx.method_27534(tr, (class_2561) class_2561.method_43470(title), x + w / 2, y, color);
        int lineW = (int) (w * 0.6);
        int lx = x + (w - lineW) / 2;
        ctx.method_25294(lx, y + 12, lx + lineW, y + 13, color);
    }

    private void drawSectionTitle(class_332 ctx, class_327 tr, int x, int y, int w,
                                  String title, int color) {
        ctx.method_27535(tr, (class_2561) class_2561.method_43470(title), x, y, color);
        ctx.method_25294(x, y + 10, x + w, y + 11, COLOR_DIVIDER);
    }

    private void drawRow(class_332 ctx, class_327 tr, int x, int y, int w,
                         String label, String value, int valueColor) {
        ctx.method_27535(tr, (class_2561) class_2561.method_43470(label), x, y, COLOR_INK_DIM);
        ctx.method_27535(tr, (class_2561) class_2561.method_43470(value),
                x + w - tr.method_1727(value), y, valueColor);
    }

    private void drawRowExtra(class_332 ctx, class_327 tr, int x, int y, int w,
                              String label, String value, String extra, int valueColor) {
        ctx.method_27535(tr, (class_2561) class_2561.method_43470(label), x, y, COLOR_INK_DIM);
        if (extra == null || extra.isEmpty()) {
            ctx.method_27535(tr, (class_2561) class_2561.method_43470(value),
                    x + w - tr.method_1727(value), y, valueColor);
        } else {
            int extraW = tr.method_1727(extra);
            int valueW = tr.method_1727(value);
            int extraX = x + w - extraW;
            int valX = extraX - 3 - valueW;
            ctx.method_27535(tr, (class_2561) class_2561.method_43470(value), valX, y, valueColor);
            ctx.method_27535(tr, (class_2561) class_2561.method_43470(extra), extraX, y, COLOR_INK_DIM);
        }
    }

    private void drawPageButton(class_332 ctx, class_327 tr, int x, int y,
                                String arrow, boolean enabled, boolean hovered) {
        int bg, border, col;
        if (!enabled) {
            bg = 0x22000000; border = 0x44222222; col = 0xFF6B5A3F;
        } else if (hovered) {
            bg = 0xCC8B3A1A; border = 0xFFCC3030; col = 0xFFFFFFFF;
        } else {
            bg = 0x886B4A1F; border = 0xFF8B6914; col = 0xFF2A1A0A;
        }
        ctx.method_25294(x, y, x + 23, y + 12, bg);
        ctx.method_25294(x, y, x + 23, y + 1, border);
        ctx.method_25294(x, y + 11, x + 23, y + 12, border);
        ctx.method_25294(x, y, x + 1, y + 12, border);
        ctx.method_25294(x + 22, y, x + 23, y + 12, border);
        ctx.method_27534(tr, (class_2561) class_2561.method_43470(arrow), x + 11, y + 2, col);
    }

    private void renderCoverPage(class_332 ctx, class_327 tr, class_1937 world,
                                 int x, int y, int w) {
        int currentDay = StageSystem.getCurrentStage(world);
        boolean isBloodMoon = StageSystem.isBloodMoon(world);
        int intelLevel = StageSystem.getIntelligenceLevel(world);
        int cy = y;
        this.drawSectionTitle(ctx, tr, x, cy, w, "当前状态", COLOR_GOLD);
        this.drawRow(ctx, tr, x, cy += 13, w, "天数",
                currentDay + " / " + ModConfig.TOTAL_DAYS, COLOR_INK_HI);
        String bmText = isBloodMoon ? "进行中"
                : "~" + StageSystem.getDaysToNextBloodMoon(world) + "天后";
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "血月", bmText,
                isBloodMoon ? COLOR_BLOOD_MOON : COLOR_INK);
        String[] intelNames = {"迟钝", "普通", "机敏", "狡猾", "凶残", "嗜血"};
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "智能度",
                "Lv" + intelLevel + " " + intelNames[intelLevel],
                intelLevel >= 4 ? COLOR_DANGER : COLOR_INK_HI);
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "阶段",
                this.getStageTip(currentDay), COLOR_INK_HI);
        this.drawSectionTitle(ctx, tr, x, cy += 13, w, "目录索引", COLOR_ACCENT);
        cy += 13;
        String[][] toc = {
            {"P2", "末日历法"}, {"P3", "智能与AI"},
            {"P4", "僵尸档案"}, {"P5", "战斗特性"}, {"P6", "巨型僵尸"}
        };
        for (String[] e : toc) {
            ctx.method_27535(tr, (class_2561) class_2561.method_43470(e[0] + "    " + e[1]), x, cy, COLOR_INK);
            cy += ROW_H;
        }
    }

    private void renderCalendarPage(class_332 ctx, class_327 tr, class_1937 world,
                                    int x, int y, int w) {
        int currentDay = StageSystem.getCurrentStage(world);
        double progress = StageSystem.getStageProgress(world);
        boolean isBloodMoon = StageSystem.isBloodMoon(world);
        int cy = y;
        this.drawSectionTitle(ctx, tr, x, cy, w, "末日历法", COLOR_GOLD);
        this.drawRow(ctx, tr, x, cy += 13, w, "当前天数",
                currentDay + " / " + ModConfig.TOTAL_DAYS, COLOR_INK_HI);
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "阶段",
                this.getStageTip(currentDay), COLOR_INK_HI);

        int barX = x, barW = w;
        ctx.method_25294(barX, cy += 13, barX + barW, cy + 2, COLOR_INK_DIM);
        int filledW = (int) (barW * progress);
        if (filledW > 0) {
            ctx.method_25294(barX, cy, barX + filledW, cy + 2, this.getProgressColor(progress));
        }
        ctx.method_27534(tr, (class_2561) class_2561.method_43470((int) (progress * 100) + "%"),
                x + w / 2, cy + 3, COLOR_INK_DIM);

        this.drawSectionTitle(ctx, tr, x, cy += 12, w, "血月周期", COLOR_BLOOD_MOON);
        int daysToBM = StageSystem.getDaysToNextBloodMoon(world);
        String bmText = isBloodMoon ? "今日血月" : "~" + daysToBM + "天后";
        this.drawRow(ctx, tr, x, cy += 13, w, "下次血月", bmText,
                isBloodMoon ? COLOR_BLOOD_MOON : COLOR_INK);
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "刷新倍率",
                isBloodMoon ? "x2.0" : "x1.0",
                isBloodMoon ? COLOR_BLOOD_MOON : COLOR_INK_DIM);

        this.drawSectionTitle(ctx, tr, x, cy += 13, w, "生存提示", COLOR_BLUE);
        cy += 13;
        for (String[] t : this.getSurvivalTips(currentDay, isBloodMoon)) {
            ctx.method_27535(tr, (class_2561) class_2561.method_43470("* " + t[0]), x, cy,
                    Integer.parseUnsignedInt(t[1], 16));
            cy += ROW_H;
        }
    }

    private void renderIntelligencePage(class_332 ctx, class_327 tr, class_1937 world,
                                        int x, int y, int w) {
        int intelLevel = StageSystem.getIntelligenceLevel(world);
        int breakInt = StageSystem.getBreakInterval(world);
        int buildInt = StageSystem.getBuildInterval(world);
        float hardLimit = StageSystem.getHardnessLimit(world);
        int invSize = StageSystem.getBlockInventorySize(world);
        double reinChance = StageSystem.getReinforcementChance(world);
        int followRange = StageSystem.getFollowRange(world);
        int cy = y;
        this.drawSectionTitle(ctx, tr, x, cy, w, "当前智能度", COLOR_ACCENT);
        String[] intelNames = {"迟钝", "普通", "机敏", "狡猾", "凶残", "嗜血"};
        int[] intelColors = {COLOR_GREEN, COLOR_GOLD, COLOR_GOLD, COLOR_DANGER, COLOR_BLOOD_MOON, COLOR_BLOOD_MOON};
        this.drawRow(ctx, tr, x, cy += 13, w, "等级", "Lv" + intelLevel, intelColors[intelLevel]);
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "命名", intelNames[intelLevel], intelColors[intelLevel]);
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "追踪范围", followRange + "格", COLOR_INK);

        this.drawSectionTitle(ctx, tr, x, cy += 13, w, "AI能力", COLOR_INK_HI);
        this.drawRow(ctx, tr, x, cy += 13, w, "拆方块间隔", String.format("%.1fs", breakInt / 20.0), COLOR_INK);
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "搭方块间隔", String.format("%.1fs", buildInt / 20.0), COLOR_INK);
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "硬度上限", String.format("%.0f", hardLimit), COLOR_INK);
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "库存容量", invSize + "格", COLOR_INK);
        String reinStr = reinChance > 0 ? String.format("%.0f%%", reinChance * 100) : "未解锁";
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "呼叫增援", reinStr,
                reinChance > 0 ? COLOR_DANGER : COLOR_INK_DIM);

        this.drawSectionTitle(ctx, tr, x, cy += 13, w, "智能度阶梯", COLOR_GOLD);
        cy += 13;
        String[][] tiers = {
            {"Lv0  1-10天   迟钝", "FF2A6B2A"},
            {"Lv1  11-20天  普通", "FF8B6914"},
            {"Lv2  21-30天  机敏", "FF8B6914"},
            {"Lv3  31-50天  狡猾", "FFB01818"}
        };
        for (String[] t : tiers) {
            ctx.method_27535(tr, (class_2561) class_2561.method_43470(t[0]), x, cy,
                    Integer.parseUnsignedInt(t[1], 16));
            cy += ROW_H;
        }
    }

    private void renderZombieStatsPage(class_332 ctx, class_327 tr, class_1937 world,
                                       int x, int y, int w) {
        double health = StageSystem.getZombieHealth(world);
        double attack = StageSystem.getZombieAttack(world);
        double armor = StageSystem.getZombieArmor(world);
        double speed = StageSystem.getZombieSpeed(world);
        double atkMult = StageSystem.getAttackMultiplier(world);
        double spdMult = StageSystem.getSpeedMultiplier(world, 1.0f);
        int cy = y;
        this.drawSectionTitle(ctx, tr, x, cy, w, "基础属性", COLOR_ZOMBIE);
        this.drawRowExtra(ctx, tr, x, cy += 13, w, "血量",
                String.format("%.0f", health), String.format("(x%.1f)", health / 20.0), COLOR_DANGER);
        this.drawRowExtra(ctx, tr, x, cy += ROW_H, w, "攻击",
                String.format("%.1f", attack), atkMult > 1 ? String.format("x%.1f", atkMult) : "", COLOR_INK_HI);
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "护甲", String.format("%.0f", armor), COLOR_INK);
        this.drawRowExtra(ctx, tr, x, cy += ROW_H, w, "速度",
                String.format("%.2f", speed), spdMult > 1 ? String.format("x%.2f", spdMult) : "", COLOR_INK);

        this.drawSectionTitle(ctx, tr, x, cy += 13, w, "属性范围 (第1天 → 第100天)", COLOR_GOLD);
        this.drawRow(ctx, tr, x, cy += 13, w, "血量", "20 → 400", COLOR_INK);
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "攻击", "3 → 15", COLOR_INK);
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "护甲", "2 → 12", COLOR_INK);
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "速度", "0.23 → 0.35", COLOR_INK);

        this.drawSectionTitle(ctx, tr, x, cy += 13, w, "血月加成", COLOR_BLOOD_MOON);
        this.drawRow(ctx, tr, x, cy += 13, w, "攻击倍率", "+20%", COLOR_BLOOD_MOON);
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "速度倍率", "+30%", COLOR_BLOOD_MOON);
    }

    private void renderZombieBehaviorPage(class_332 ctx, class_327 tr, class_1937 world,
                                          int x, int y, int w) {
        boolean isBloodMoon = StageSystem.isBloodMoon(world);
        boolean isNight = !world.method_8530();
        int cy = y;
        this.drawSectionTitle(ctx, tr, x, cy, w, "当前加成", COLOR_BLUE);
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

        this.drawSectionTitle(ctx, tr, x, cy += 13, w, "行为特性", COLOR_INK_HI);
        cy += 13;
        String[] behaviors = {
            "* 防白天燃烧",
            "* 第20天后开始拆搭方块",
            "* 优先破坏门窗玻璃",
            "* 卡住时加速突破",
            "* 朝玩家方向搭楼梯",
            "* 空中搭天桥追击"
        };
        for (String b : behaviors) {
            ctx.method_27535(tr, (class_2561) class_2561.method_43470(b), x, cy, COLOR_INK);
            cy += ROW_H;
        }

        this.drawSectionTitle(ctx, tr, x, cy += 4, w, "威胁警告", COLOR_DANGER);
        ctx.method_27535(tr, (class_2561) class_2561.method_43470("* 高墙无法完全阻挡"), x, cy += 13, COLOR_DANGER);
        ctx.method_27535(tr, (class_2561) class_2561.method_43470("* 备足武器与药水"), x, cy += ROW_H, COLOR_DANGER);
    }

    private void renderGiantPage(class_332 ctx, class_327 tr, class_1937 world,
                                 int x, int y, int w) {
        double health = StageSystem.getGiantZombieHealth(world);
        double attack = StageSystem.getGiantZombieAttack(world);
        double chance = StageSystem.getGiantZombieChance(world);
        int followRange = StageSystem.getFollowRange(world);
        double atkMult = StageSystem.getAttackMultiplier(world);
        int cy = y;
        this.drawSectionTitle(ctx, tr, x, cy, w, "巨型属性 (2倍大小 - 第40天起)", COLOR_GIANT);
        this.drawRowExtra(ctx, tr, x, cy += 13, w, "血量",
                String.format("%.0f", health), String.format("(x%.1f)", health / 400.0), COLOR_GIANT);
        this.drawRowExtra(ctx, tr, x, cy += ROW_H, w, "攻击",
                String.format("%.1f", attack), atkMult > 1 ? String.format("x%.1f", atkMult) : "", COLOR_GIANT);
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "生成概率", String.format("%.1f%%", chance * 100), COLOR_GIANT);
        this.drawRow(ctx, tr, x, cy += ROW_H, w, "追踪范围", followRange + "格", COLOR_INK);

        this.drawSectionTitle(ctx, tr, x, cy += 13, w, "特殊能力", COLOR_INK_HI);
        cy += 13;
        String[] abilities = {
            "* 2.5倍攻击距离", "* 可跨越2格高墙",
            "* 防火免疫", "* 血月时属性增强"
        };
        for (String a : abilities) {
            ctx.method_27535(tr, (class_2561) class_2561.method_43470(a), x, cy, COLOR_GIANT);
            cy += ROW_H;
        }

        this.drawSectionTitle(ctx, tr, x, cy += 4, w, "掉落物", COLOR_GOLD);
        ctx.method_27535(tr, (class_2561) class_2561.method_43470("* 腐肉 3-8  -  骨头 3-6"), x, cy += 13, COLOR_INK);
        ctx.method_27535(tr, (class_2561) class_2561.method_43470("* 铁锭 2-5  -  金锭 1-3"), x, cy += ROW_H, COLOR_INK);
        ctx.method_27535(tr, (class_2561) class_2561.method_43470("* 钻石 0-2  -  绿宝石 0-3"), x, cy += ROW_H, COLOR_INK_HI);
        ctx.method_27535(tr, (class_2561) class_2561.method_43470("* 附魔金苹果 0-1"), x, cy += ROW_H, COLOR_GIANT);
        ctx.method_27535(tr, (class_2561) class_2561.method_43470("* 经验瓶 1-5"), x, cy += ROW_H, COLOR_GREEN);
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

    private String[][] getSurvivalTips(int day, boolean isBloodMoon) {
        if (isBloodMoon) return new String[][]{
            {"血月刷新翻倍", "FFB01818"},
            {"加固门窗死守", "FFB01818"},
            {"警惕巨型僵尸", "FF8B3A1A"}
        };
        if (day <= 10) return new String[][]{
            {"收集木头建庇护所", "FF2A6B2A"},
            {"制作石制装备", "FF2A6B2A"},
            {"留意血月来临", "FF8B6914"}
        };
        if (day <= 30) return new String[][]{
            {"升级铁制装备", "FF8B6914"},
            {"准备弓箭远程", "FF8B6914"},
            {"注意僵尸变强", "FF8B3A1A"}
        };
        if (day <= 50) return new String[][]{
            {"携带钻石装备", "FF8B3A1A"},
            {"加固墙体防御", "FFB01818"},
            {"远离巨型僵尸", "FFB01818"}
        };
        return new String[][]{
            {"末日降临谨慎行动", "FFB01818"},
            {"备足药水防御", "FFB01818"},
            {"高墙护城河最佳", "FFB01818"}
        };
    }

    public boolean method_25402(double mouseX, double mouseY, int button) {
        if (this.prevHovered && this.currentPage > 0) {
            this.currentPage--;
            return true;
        }
        if (this.nextHovered && this.currentPage < TOTAL_PAGES - 1) {
            this.currentPage++;
            return true;
        }
        return super.method_25402(mouseX, mouseY, button);
    }

    public boolean method_25421() {
        return false;
    }
}
