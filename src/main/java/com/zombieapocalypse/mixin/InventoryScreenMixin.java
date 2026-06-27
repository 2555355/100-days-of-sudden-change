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
 * 末日风格UI - v2.0.0
 * 主标签: 生存背包 / 末日情报
 * 末日情报内含3个子页面: 基础信息 / 僵尸详情 / 巨型僵尸详情
 */
@Mixin(HandledScreen.class)
public abstract class InventoryScreenMixin {

    @Shadow protected int x;
    @Shadow protected int y;

    // 配色方案
    @Unique private static final int COLOR_BG_DARK    = 0xF0100A0A;
    @Unique private static final int COLOR_BG_PANEL   = 0xFF0D0808;
    @Unique private static final int COLOR_BG_CARD    = 0xCC1A1212;
    @Unique private static final int COLOR_BG_SUBTAB  = 0x88151010;
    @Unique private static final int COLOR_BG_SUBTAB_SEL = 0xEE2A0808;
    @Unique private static final int COLOR_BORDER_RED = 0xFFFF2A2A;
    @Unique private static final int COLOR_BORDER_DIM = 0x66883333;
    @Unique private static final int COLOR_ACCENT     = 0xFFFF4444;
    @Unique private static final int COLOR_TEXT       = 0xFFE0E0E0;
    @Unique private static final int COLOR_TEXT_DIM   = 0xFF999999;
    @Unique private static final int COLOR_TEXT_HI    = 0xFFFFAA00;
    @Unique private static final int COLOR_DANGER     = 0xFFFF3333;
    @Unique private static final int COLOR_BLOOD_MOON = 0xFFFF0000;
    @Unique private static final int COLOR_ZOMBIE     = 0xFFFF6644;
    @Unique private static final int COLOR_GIANT      = 0xFFFF66FF;

    @Unique private static final int TAB_WIDTH = 62;
    @Unique private static final int TAB_HEIGHT = 26;
    @Unique private static final int TAB_GAP = 3;
    @Unique private static final int PANEL_W = 176;
    @Unique private static final int PANEL_H = 166;
    @Unique private static final int SUBTAB_W = 52;
    @Unique private static final int SUBTAB_H = 14;

    @Unique private int currentTab = 1;      // 0=末日情报, 1=生存背包
    @Unique private int currentSubTab = 0;   // 0=基础, 1=僵尸, 2=巨型僵尸
    @Unique private int hoveredTab = -1;
    @Unique private int hoveredSubTab = -1;

    @Unique
    private boolean isInventoryScreen() {
        return (Object) this instanceof InventoryScreen;
    }

    @Unique
    private int getTabOffset() {
        return TAB_HEIGHT + 3;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void adjustLayout(CallbackInfo ci) {
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRenderHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!isInventoryScreen()) return;
        if (currentTab == 0) {
            MinecraftClient client = MinecraftClient.getInstance();
            int sw = client.getWindow().getScaledWidth();
            int sh = client.getWindow().getScaledHeight();
            context.fill(0, 0, sw, sh, COLOR_BG_DARK);
            context.fill(0, 0, sw, sh / 4, 0x44000000);
            int panelY = this.y + getTabOffset();
            renderDayPanel(context, mouseX, mouseY, this.x, panelY);
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
        int tabY = this.y - TAB_HEIGHT;
        // 主标签
        if (mouseY >= tabY && mouseY <= tabY + TAB_HEIGHT) {
            int tab0X = this.x + 4;
            if (mouseX >= tab0X && mouseX <= tab0X + TAB_WIDTH) {
                currentTab = 1;
                cir.setReturnValue(true);
                return;
            }
            int tab1X = tab0X + TAB_WIDTH + TAB_GAP;
            if (mouseX >= tab1X && mouseX <= tab1X + TAB_WIDTH) {
                currentTab = 0;
                cir.setReturnValue(true);
                return;
            }
        }
        // 末日情报面板区域
        if (currentTab == 0) {
            int panelY = this.y + getTabOffset();
            if (mouseX >= this.x && mouseX <= this.x + PANEL_W &&
                    mouseY >= panelY && mouseY <= panelY + PANEL_H) {
                // 检查子标签点击
                int subTabY = panelY + 24;
                if (mouseY >= subTabY && mouseY <= subTabY + SUBTAB_H) {
                    int subTab0X = this.x + 6;
                    if (mouseX >= subTab0X && mouseX <= subTab0X + SUBTAB_W) {
                        currentSubTab = 0;
                        cir.setReturnValue(true);
                        return;
                    }
                    int subTab1X = subTab0X + SUBTAB_W + 2;
                    if (mouseX >= subTab1X && mouseX <= subTab1X + SUBTAB_W) {
                        currentSubTab = 1;
                        cir.setReturnValue(true);
                        return;
                    }
                    int subTab2X = subTab1X + SUBTAB_W + 2;
                    if (mouseX >= subTab2X && mouseX <= subTab2X + SUBTAB_W) {
                        currentSubTab = 2;
                        cir.setReturnValue(true);
                        return;
                    }
                }
                cir.setReturnValue(true);
            }
        }
    }

    // ===================== 主标签按钮 =====================

    @Unique
    private void renderTabButtons(DrawContext context, int mouseX, int mouseY) {
        int tabY = this.y - TAB_HEIGHT;
        hoveredTab = -1;
        if (mouseY >= tabY && mouseY <= tabY + TAB_HEIGHT) {
            int tab0X = this.x + 4;
            if (mouseX >= tab0X && mouseX <= tab0X + TAB_WIDTH) hoveredTab = 1;
            int tab1X = tab0X + TAB_WIDTH + TAB_GAP;
            if (mouseX >= tab1X && mouseX <= tab1X + TAB_WIDTH) hoveredTab = 0;
        }

        int tab0X = this.x + 4;
        drawTab(context, tab0X, tabY, "⛏", "生存背包", currentTab == 1, hoveredTab == 1);
        int tab1X = tab0X + TAB_WIDTH + TAB_GAP;
        drawTab(context, tab1X, tabY, "☠", "末日情报", currentTab == 0, hoveredTab == 0);
    }

    @Unique
    private void drawTab(DrawContext ctx, int x, int y, String icon, String label,
                         boolean selected, boolean hovered) {
        int w = TAB_WIDTH;
        int h = TAB_HEIGHT;
        int bg, border, textCol, iconCol;

        if (selected) {
            bg = 0xEE2A0808; border = COLOR_BORDER_RED; textCol = 0xFFFF6666; iconCol = COLOR_ACCENT;
        } else if (hovered) {
            bg = 0xBB1A0A0A; border = 0xAAFF4444; textCol = 0xFFCCCCCC; iconCol = 0xFFCC8888;
        } else {
            bg = 0x88101010; border = COLOR_BORDER_DIM; textCol = COLOR_TEXT_DIM; iconCol = 0xFF886666;
        }

        ctx.fill(x, y, x + w, y + h, bg);
        ctx.fill(x, y, x + 1, y + h, border);
        ctx.fill(x + w - 1, y, x + w, y + h, border);
        ctx.fill(x, y, x + w, y + 1, border);
        if (selected) {
            ctx.fill(x + 1, y + h - 2, x + w - 1, y + h, COLOR_ACCENT);
            ctx.fill(x + 2, y + 1, x + w - 2, y + 2, 0x44FF6666);
        } else {
            ctx.fill(x, y + h - 1, x + w, y + h, border);
        }

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int iconW = tr.getWidth(icon);
        ctx.drawTextWithShadow(tr, Text.literal(icon), x + 6, y + (h - 8) / 2, iconCol);
        int labelX = x + 6 + iconW + 4;
        ctx.drawTextWithShadow(tr, Text.literal(label), labelX, y + (h - 8) / 2, textCol);
    }

    // ===================== 末日情报面板 =====================

    @Unique
    private void renderDayPanel(DrawContext ctx, int mouseX, int mouseY, int px, int py) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        World world = client.player.getWorld();
        if (world == null) return;

        TextRenderer tr = client.textRenderer;
        int pw = PANEL_W;
        int ph = PANEL_H;
        int centerX = px + pw / 2;

        boolean isBloodMoon = StageSystem.isBloodMoon(world);

        // 面板背景
        ctx.fill(px, py, px + pw, py + ph, COLOR_BG_PANEL);
        for (int i = 0; i < 4; i++) {
            int alpha = 0xFF - i * 0x30;
            ctx.fill(px, py + i, px + pw, py + i + 1, (alpha << 24) | 0x2A0808);
        }
        drawBorder(ctx, px, py, pw, ph, COLOR_BORDER_RED, 1);
        drawCornerDecor(ctx, px, py, pw, ph);

        // 标题横幅
        int titleY = py + 5;
        int titleH = 15;
        ctx.fill(px + 6, titleY, px + pw - 6, titleY + titleH, 0x993A0505);
        ctx.fill(px + 6, titleY, px + pw - 6, titleY + 1, COLOR_ACCENT);
        ctx.fill(px + 6, titleY + titleH - 1, px + pw - 6, titleY + titleH, COLOR_ACCENT);
        int titleColor = isBloodMoon ? COLOR_BLOOD_MOON : 0xFFFF5555;
        ctx.drawCenteredTextWithShadow(tr, Text.literal("☠ 惊变100天 ☠"), centerX, titleY + 3, titleColor);

        // 血月警告
        if (isBloodMoon) {
            int warnY = titleY + titleH + 1;
            ctx.fill(px + 6, warnY, px + pw - 6, warnY + 11, 0xCC660000);
            ctx.fill(px + 6, warnY, px + pw - 6, warnY + 1, COLOR_BLOOD_MOON);
            ctx.fill(px + 6, warnY + 10, px + pw - 6, warnY + 11, COLOR_BLOOD_MOON);
            ctx.drawCenteredTextWithShadow(tr, Text.literal("● 血月进行中 ●"), centerX, warnY + 1, COLOR_BLOOD_MOON);
        }

        // 子标签栏
        int subTabY = py + 24;
        renderSubTabs(ctx, tr, px, subTabY, mouseX, mouseY);

        // 内容区域起始
        int contentY = subTabY + SUBTAB_H + 3;
        int cardW = pw - 12;
        int cardX = px + 6;

        switch (currentSubTab) {
            case 0 -> renderBasicPage(ctx, tr, world, cardX, contentY, cardW, py, ph);
            case 1 -> renderZombiePage(ctx, tr, world, cardX, contentY, cardW, py, ph);
            case 2 -> renderGiantPage(ctx, tr, world, cardX, contentY, cardW, py, ph);
        }
    }

    // ===================== 子标签 =====================

    @Unique
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

    @Unique
    private void renderBasicPage(DrawContext ctx, TextRenderer tr, World world,
                                 int cardX, int cardY, int cardW, int py, int ph) {
        int currentDay = StageSystem.getCurrentStage(world);
        double progress = StageSystem.getStageProgress(world);
        boolean isBloodMoon = StageSystem.isBloodMoon(world);
        int intelLevel = StageSystem.getIntelligenceLevel(world);
        int centerX = cardX + cardW / 2;

        // 天数卡片
        int dayCardH = 34;
        drawCard(ctx, cardX, cardY, cardW, dayCardH);
        String dayStr = String.valueOf(currentDay);
        ctx.drawTextWithShadow(tr, Text.literal("§f第"), cardX + 6, cardY + 5, COLOR_TEXT_DIM);
        ctx.drawTextWithShadow(tr, Text.literal("§e§l" + dayStr), cardX + 18, cardY + 4, COLOR_TEXT_HI);
        ctx.drawTextWithShadow(tr, Text.literal("§f天 §7/" + ModConfig.TOTAL_DAYS),
                cardX + 18 + tr.getWidth(dayStr) + 2, cardY + 5, COLOR_TEXT_DIM);
        // 血月倒计时
        int currentDayRaw = StageSystem.getCurrentDay(world);
        int nextBM = ((currentDayRaw / ModConfig.BLOOD_MOON_INTERVAL) + 1) * ModConfig.BLOOD_MOON_INTERVAL;
        int daysToBM = nextBM - currentDayRaw;
        String bmText = isBloodMoon ? "§4● 血月今日" : "§7下次血月: §c" + daysToBM + "天后";
        ctx.drawTextWithShadow(tr, Text.literal(bmText), cardX + 6, cardY + 16, COLOR_TEXT_DIM);
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
        ctx.drawTextWithShadow(tr, Text.literal("§7智能度"), cardX + 6, cardY + 6, COLOR_TEXT_DIM);
        String[] intelNames = {"迟钝", "普通", "机敏", "狡猾", "凶残", "嗜血"};
        String[] intelColors = {"§a", "§e", "§6", "§c", "§4", "§4§l"};
        String intelText = intelColors[intelLevel] + "Lv" + intelLevel + " " + intelNames[intelLevel];
        int intelW = tr.getWidth(intelText.replaceAll("§[0-9a-fklmnor]", ""));
        ctx.drawTextWithShadow(tr, Text.literal(intelText), cardX + cardW - 6 - intelW, cardY + 6, COLOR_TEXT);

        // AI能力卡片
        cardY += intelCardH + 2;
        int aiCardH = ph - (cardY - py) - 4;
        if (aiCardH > 10) {
            drawCard(ctx, cardX, cardY, cardW, aiCardH);
            ctx.drawTextWithShadow(tr, Text.literal("§e⚙ AI能力"), cardX + 6, cardY + 4, COLOR_TEXT_HI);

            int aiY = cardY + 15;
            int breakInt = StageSystem.getBreakInterval(world);
            int buildInt = StageSystem.getBuildInterval(world);
            float hardLimit = StageSystem.getHardnessLimit(world);
            double reinChance = StageSystem.getReinforcementChance(world);
            int invSize = StageSystem.getBlockInventorySize(world);

            ctx.drawTextWithShadow(tr, Text.literal(
                    String.format("§7拆: §c%.1fs  §7搭: §c%.1fs", breakInt / 20.0, buildInt / 20.0)),
                    cardX + 6, aiY, COLOR_TEXT);
            aiY += 11;
            ctx.drawTextWithShadow(tr, Text.literal(
                    String.format("§7硬度上限: §c%.0f  §7库存: §c%d", hardLimit, invSize)),
                    cardX + 6, aiY, COLOR_TEXT);
            aiY += 11;
            String reinStr = reinChance > 0
                    ? String.format("§7增援概率: §c%.0f%%", reinChance * 100)
                    : "§7增援: §8未解锁";
            ctx.drawTextWithShadow(tr, Text.literal(reinStr), cardX + 6, aiY, COLOR_TEXT);
            aiY += 11;
            ctx.drawTextWithShadow(tr, Text.literal("§7夜速§a+15% §7血月§c+30%速§c+20%攻"),
                    cardX + 6, aiY, COLOR_TEXT);
            aiY += 11;
            if (aiY < cardY + aiCardH - 2) {
                ctx.drawTextWithShadow(tr, Text.literal(getStageTip(currentDay)),
                        cardX + 6, aiY, COLOR_TEXT);
            }
        }
    }

    // ===================== 僵尸详情页 =====================

    @Unique
    private void renderZombiePage(DrawContext ctx, TextRenderer tr, World world,
                                  int cardX, int cardY, int cardW, int py, int ph) {
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

        // 属性卡片
        int attrCardH = 58;
        drawCard(ctx, cardX, cardY, cardW, attrCardH);
        ctx.drawTextWithShadow(tr, Text.literal("§c⚔ 普通僵尸属性"), cardX + 6, cardY + 4, COLOR_ZOMBIE);

        int colW = (cardW - 12) / 2;
        int rowY = cardY + 16;
        // 血量
        drawDetailRow(ctx, tr, cardX + 6, rowY, cardW - 12, "血量", String.format("%.0f / 20", health),
                String.format("(%.1fx)", health / 20), COLOR_DANGER);
        rowY += 11;
        // 攻击
        double atkMult = StageSystem.getAttackMultiplier(world);
        drawDetailRow(ctx, tr, cardX + 6, rowY, cardW - 12, "攻击", String.format("%.1f", attack),
                atkMult > 1 ? String.format("(x%.1f)", atkMult) : "", COLOR_TEXT_HI);
        rowY += 11;
        // 护甲
        drawDetailRow(ctx, tr, cardX + 6, rowY, cardW - 12, "护甲", String.format("%.0f", armor), "", COLOR_TEXT);
        rowY += 11;
        // 速度
        double spdMult = StageSystem.getSpeedMultiplier(world, 1.0f);
        String spdExtra = spdMult > 1 ? String.format("(x%.2f)", spdMult) : "";
        drawDetailRow(ctx, tr, cardX + 6, rowY, cardW - 12, "速度", String.format("%.2f", speed), spdExtra, COLOR_TEXT);
        rowY += 11;
        // 追踪范围
        drawDetailRow(ctx, tr, cardX + 6, rowY, cardW - 12, "追踪范围", followRange + " 格", "", COLOR_TEXT);

        // 战斗能力卡片
        cardY += attrCardH + 2;
        int combatCardH = 46;
        drawCard(ctx, cardX, cardY, cardW, combatCardH);
        ctx.drawTextWithShadow(tr, Text.literal("§e⚙ 战斗能力"), cardX + 6, cardY + 4, COLOR_TEXT_HI);

        rowY = cardY + 16;
        drawDetailRow(ctx, tr, cardX + 6, rowY, cardW - 12, "拆方块", String.format("%.1fs", breakInt / 20.0), "", COLOR_TEXT);
        rowY += 11;
        drawDetailRow(ctx, tr, cardX + 6, rowY, cardW - 12, "搭方块", String.format("%.1fs", buildInt / 20.0), "", COLOR_TEXT);
        rowY += 11;
        drawDetailRow(ctx, tr, cardX + 6, rowY, cardW - 12, "硬度上限", String.format("%.0f", hardLimit), "", COLOR_TEXT);
        rowY += 11;
        String reinStr = reinChance > 0 ? String.format("%.0f%%", reinChance * 100) : "未解锁";
        drawDetailRow(ctx, tr, cardX + 6, rowY, cardW - 12, "呼叫增援", reinStr, "", COLOR_TEXT);

        // 加成状态卡片
        cardY += combatCardH + 2;
        int buffCardH = ph - (cardY - py) - 4;
        if (buffCardH > 10) {
            drawCard(ctx, cardX, cardY, cardW, buffCardH);
            ctx.drawTextWithShadow(tr, Text.literal("§b★ 当前加成"), cardX + 6, cardY + 4, 0xFF66DDFF);

            rowY = cardY + 16;
            String nightStr = isNight ? "§a激活 +15%速" : "§7未激活";
            drawDetailRow(ctx, tr, cardX + 6, rowY, cardW - 12, "夜晚加成", nightStr, "", COLOR_TEXT);
            rowY += 11;
            String bmStr = isBloodMoon ? "§4激活 +30%速 +20%攻" : "§7未激活";
            drawDetailRow(ctx, tr, cardX + 6, rowY, cardW - 12, "血月加成", bmStr, "", COLOR_TEXT);
            rowY += 11;
            drawDetailRow(ctx, tr, cardX + 6, rowY, cardW - 12, "库存容量", invSize + " 格", "", COLOR_TEXT);
        }
    }

    // ===================== 巨型僵尸详情页 =====================

    @Unique
    private void renderGiantPage(DrawContext ctx, TextRenderer tr, World world,
                                 int cardX, int cardY, int cardW, int py, int ph) {
        double health = StageSystem.getGiantZombieHealth(world);
        double attack = StageSystem.getGiantZombieAttack(world);
        double chance = StageSystem.getGiantZombieChance(world);
        int followRange = StageSystem.getFollowRange(world);
        boolean isBloodMoon = StageSystem.isBloodMoon(world);

        // 属性卡片
        int attrCardH = 70;
        drawCard(ctx, cardX, cardY, cardW, attrCardH);
        ctx.drawTextWithShadow(tr, Text.literal("§5▾ 巨型僵尸属性"), cardX + 6, cardY + 4, COLOR_GIANT);
        ctx.drawTextWithShadow(tr, Text.literal("§7(2倍缩放)"), cardX + cardW - 6 - tr.getWidth("(2倍缩放)"),
                cardY + 5, COLOR_TEXT_DIM);

        int rowY = cardY + 18;
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
        int dropCardH = ph - (cardY - py) - 4;
        if (dropCardH > 10) {
            drawCard(ctx, cardX, cardY, cardW, dropCardH);
            ctx.drawTextWithShadow(tr, Text.literal("§6◈ 掉落物"), cardX + 6, cardY + 4, 0xFFFFAA00);

            rowY = cardY + 16;
            ctx.drawTextWithShadow(tr, Text.literal("§7腐肉 §f3-8  §7骨头 §f3-6"), cardX + 6, rowY, COLOR_TEXT);
            rowY += 11;
            ctx.drawTextWithShadow(tr, Text.literal("§7铁锭 §f2-5  §7金锭 §f1-3"), cardX + 6, rowY, COLOR_TEXT);
            rowY += 11;
            ctx.drawTextWithShadow(tr, Text.literal("§7钻石 §f0-2  §7绿宝石 §f0-3"), cardX + 6, rowY, COLOR_TEXT);
            rowY += 11;
            ctx.drawTextWithShadow(tr, Text.literal("§d附魔金苹果 §f0-1 §7(稀有)"), cardX + 6, rowY, COLOR_TEXT);
            if (rowY + 11 < cardY + dropCardH - 2) {
                rowY += 11;
                ctx.drawTextWithShadow(tr, Text.literal("§a经验瓶 §f1-5"), cardX + 6, rowY, COLOR_TEXT);
            }
        }
    }

    // ===================== 辅助绘制方法 =====================

    @Unique
    private void drawDetailRow(DrawContext ctx, TextRenderer tr, int x, int y, int w,
                               String label, String value, String extra, int valueColor) {
        ctx.drawTextWithShadow(tr, Text.literal("§7" + label), x, y, COLOR_TEXT_DIM);
        int extraW = extra.isEmpty() ? 0 : tr.getWidth(extra) + 4;
        int valW = tr.getWidth(value);
        ctx.drawTextWithShadow(tr, Text.literal("§f" + value), x + w - valW - extraW - 2, y, valueColor);
        if (!extra.isEmpty()) {
            ctx.drawTextWithShadow(tr, Text.literal("§8" + extra), x + w - extraW + 2, y, COLOR_TEXT_DIM);
        }
    }

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
        ctx.fill(x, y, x + w, y + 1, 0x33FF4444);
        ctx.fill(x, y + h - 1, x + w, y + h, 0x44110000);
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
