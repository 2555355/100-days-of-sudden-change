package com.zombieapocalypse.mixin;

import com.zombieapocalypse.config.ModConfig;
import com.zombieapocalypse.config.StageSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
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
 * HandledScreen Mixin - 为生存背包界面添加自定义末日风格分类标签
 * 当"天数"标签激活时，完全替换物品栏显示
 */
@Mixin(HandledScreen.class)
public abstract class InventoryScreenMixin extends Screen {

    @Shadow
    protected int x;
    @Shadow
    protected int y;

    @Unique
    private static final int TAB_WIDTH = 56;
    @Unique
    private static final int TAB_HEIGHT = 24;
    @Unique
    private static final int TAB_GAP = 4;
    @Unique
    private int currentTab = 1; // 0=天数, 1=背包
    @Unique
    private int hoveredTab = -1;

    protected InventoryScreenMixin(Text title) {
        super(title);
    }

    @Unique
    private boolean isInventoryScreen() {
        return (Object) this instanceof InventoryScreen;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void adjustLayout(CallbackInfo ci) {
        if (!isInventoryScreen()) return;
        this.y += TAB_HEIGHT + 4;
    }

    /**
     * HEAD 注入：当选中"天数"标签时，取消物品栏渲染，只显示面板
     */
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRenderHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!isInventoryScreen()) return;
        if (currentTab == 0) {
            // 绘制暗色背景
            this.renderBackground(context, mouseX, mouseY, delta);
            // 绘制天数面板
            renderDayPanel(context, mouseX, mouseY);
            // 取消物品栏渲染
            ci.cancel();
        }
    }

    /**
     * TAIL 注入：始终渲染标签按钮（背包模式由正常渲染处理）
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderTail(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!isInventoryScreen()) return;
        renderTabButtons(context, mouseX, mouseY);
    }

    /**
     * 渲染标签按钮
     */
    @Unique
    private void renderTabButtons(DrawContext context, int mouseX, int mouseY) {
        int tabY = this.y - TAB_HEIGHT - 4;

        // 检测悬停
        hoveredTab = -1;
        if (mouseY >= tabY && mouseY <= tabY + TAB_HEIGHT) {
            int tab0X = this.x + 4;
            if (mouseX >= tab0X && mouseX <= tab0X + TAB_WIDTH) hoveredTab = 0;
            int tab1X = tab0X + TAB_WIDTH + TAB_GAP;
            if (mouseX >= tab1X && mouseX <= tab1X + TAB_WIDTH) hoveredTab = 1;
        }

        // 绘制 "天数" 标签
        int tab0X = this.x + 4;
        drawCustomTab(context, tab0X, tabY, TAB_WIDTH, TAB_HEIGHT, currentTab == 0, hoveredTab == 0, "☠", "天数");

        // 绘制 "背包" 标签
        int tab1X = tab0X + TAB_WIDTH + TAB_GAP;
        drawCustomTab(context, tab1X, tabY, TAB_WIDTH, TAB_HEIGHT, currentTab == 1, hoveredTab == 1, "⛁", "背包");
    }

    /**
     * 绘制自定义末日风格标签按钮
     */
    @Unique
    private void drawCustomTab(DrawContext context, int x, int y, int w, int h,
                               boolean selected, boolean hovered, String icon, String label) {
        int bgColor, borderColor, textColor;

        if (selected) {
            bgColor = 0xCC3A0000;
            borderColor = 0xFFFF3333;
            textColor = 0xFFFF4444;
        } else if (hovered) {
            bgColor = 0x88220000;
            borderColor = 0xCC883333;
            textColor = 0xCCCCCC;
        } else {
            bgColor = 0x88111111;
            borderColor = 0x66333333;
            textColor = 0x888888;
        }

        context.fill(x, y, x + w, y + h, bgColor);
        context.fill(x, y, x + w, y + 1, borderColor);
        context.fill(x, y + h - 1, x + w, y + h, borderColor);
        context.fill(x, y, x + 1, y + h, borderColor);
        context.fill(x + w - 1, y, x + w, y + h, borderColor);

        if (selected) {
            context.fill(x + 2, y + h - 2, x + w - 2, y + h - 1, 0xFFFF3333);
        }

        var renderer = MinecraftClient.getInstance().textRenderer;
        String displayText = icon + " " + label;
        int textWidth = renderer.getWidth(displayText);
        context.drawTextWithShadow(renderer,
                Text.literal(displayText),
                x + (w - textWidth) / 2, y + (h - 8) / 2, textColor);
    }

    /**
     * 渲染天数信息面板 (末日风格，完整替换物品栏)
     */
    @Unique
    private void renderDayPanel(DrawContext context, int mouseX, int mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        World world = client.player.getWorld();
        if (world == null) return;

        int currentDay = StageSystem.getCurrentStage(world);
        double progress = StageSystem.getStageProgress(world);
        int centerX = this.x + 88;
        var renderer = client.textRenderer;
        int panelW = 176;
        int panelH = 166;

        // ===== 面板背景 =====
        context.fill(this.x, this.y, this.x + panelW, this.y + panelH, 0xFF0A0A0A);
        // 红色边框
        context.fill(this.x, this.y, this.x + panelW, this.y + 2, 0xFFFF2222);
        context.fill(this.x, this.y + panelH - 1, this.x + panelW, this.y + panelH, 0x88FF2222);
        context.fill(this.x, this.y, this.x + 1, this.y + panelH, 0x88FF2222);
        context.fill(this.x + panelW - 1, this.y, this.x + panelW, this.y + panelH, 0x88FF2222);
        context.fill(this.x, this.y, this.x + 4, this.y + 1, 0xFFFF3333);
        context.fill(this.x + panelW - 4, this.y, this.x + panelW, this.y + 1, 0xFFFF3333);

        // ===== 标题区域 =====
        int titleY = this.y + 10;
        context.fill(this.x + 8, titleY - 2, this.x + panelW - 8, titleY + 16, 0x66330000);
        context.fill(this.x + 8, titleY - 2, this.x + panelW - 8, titleY - 1, 0xCCFF3333);
        context.fill(this.x + 8, titleY + 15, this.x + panelW - 8, titleY + 16, 0xCCFF3333);
        context.drawCenteredTextWithShadow(renderer,
                Text.literal("§c§l☠ 惊变 100 天 ☠"), centerX, titleY + 1, 0xFF6666);

        // ===== 天数卡片 =====
        int cardY = titleY + 24;
        int cardW = panelW - 16;
        int cardX = this.x + 8;

        drawPanelCard(context, cardX, cardY, cardW, 42);
        context.drawTextWithShadow(renderer,
                Text.literal("§f当前天数: §e§l" + currentDay + " §7/ " + ModConfig.TOTAL_DAYS),
                cardX + 10, cardY + 8, 0xFFFFFF);

        int progY = cardY + 24;
        int progH = 10;
        context.fill(cardX + 10, progY, cardX + cardW - 10, progY + progH, 0xFF000000);
        int filledW = (int) ((cardW - 20) * progress);
        int barColor = progress < 0.3 ? 0xFF44AA44 : progress < 0.6 ? 0xFFCCAA44 :
                progress < 0.8 ? 0xFFFF8844 : 0xFFFF3333;
        context.fill(cardX + 10, progY + 1, cardX + 10 + filledW, progY + progH - 1, barColor);
        context.drawCenteredTextWithShadow(renderer,
                Text.literal(String.format("§f%.1f%%", progress * 100)),
                centerX, progY + 1, 0xFFFFFF);

        // ===== 僵尸属性卡片 =====
        cardY += 50;
        drawPanelCard(context, cardX, cardY, cardW, 64);

        double zombieHealth = StageSystem.getZombieHealth(world);
        double zombieAttack = StageSystem.getZombieAttack(world);
        double zombieArmor = ModConfig.ZOMBIE_BASE_ARMOR +
                (ModConfig.ZOMBIE_MAX_ARMOR - ModConfig.ZOMBIE_BASE_ARMOR) * progress;
        double zombieSpeed = StageSystem.getZombieSpeed(world);
        double giantHealth = StageSystem.getGiantZombieHealth(world);
        double giantAttack = StageSystem.getGiantZombieAttack(world);
        double giantChance = StageSystem.getGiantZombieChance(world);

        context.drawTextWithShadow(renderer,
                Text.literal("§c§l⚔ 僵尸属性"), cardX + 10, cardY + 6, 0xFF4444);

        int rowY = cardY + 22;
        drawAttributeRow(context, renderer, cardX + 10, rowY, cardW - 20,
                "§7血量", String.format("§c%.0f", zombieHealth),
                "§7攻击", String.format("§c%.1f", zombieAttack));
        rowY += 14;
        drawAttributeRow(context, renderer, cardX + 10, rowY, cardW - 20,
                "§7护甲", String.format("§c%.0f", zombieArmor),
                "§7速度", String.format("§e%.2f", zombieSpeed));
        rowY += 14;
        drawAttributeRow(context, renderer, cardX + 10, rowY, cardW - 20,
                "§5巨型血量", String.format("§5%.0f", giantHealth),
                "§5巨型攻击", String.format("§5%.1f", giantAttack));
        rowY += 14;
        context.drawTextWithShadow(renderer,
                Text.literal(String.format("§d巨型生成概率: §d%.1f%%", giantChance * 100)),
                cardX + 10, rowY, 0xAAAAAA);

        // ===== 阶段提示 =====
        cardY += 72;
        drawPanelCard(context, cardX, cardY, cardW, 24);

        String tip;
        if (currentDay <= 10) {
            tip = "§a初期阶段 §7- 僵尸较弱，抓紧收集资源";
        } else if (currentDay <= 30) {
            tip = "§e发展阶段 §7- 僵尸变强，建造防御工事";
        } else if (currentDay <= 50) {
            tip = "§6中期阶段 §7- 僵尸很强，注意防守";
        } else if (currentDay <= 70) {
            tip = "§c后期阶段 §7- 巨型僵尸频繁出现！";
        } else {
            tip = "§4最终阶段 §7- 拼尽全力生存下去！";
        }
        context.drawCenteredTextWithShadow(renderer,
                Text.literal(tip), centerX, cardY + 8, 0xFFFFFF);
    }

    @Unique
    private void drawAttributeRow(DrawContext context, net.minecraft.client.font.TextRenderer renderer,
                                  int x, int y, int width,
                                  String label1, String value1, String label2, String value2) {
        int midX = x + width / 2;
        context.drawTextWithShadow(renderer,
                Text.literal(label1 + ": " + value1), x, y, 0xAAAAAA);
        context.drawTextWithShadow(renderer,
                Text.literal(label2 + ": " + value2), midX, y, 0xAAAAAA);
    }

    @Unique
    private void drawPanelCard(DrawContext context, int x, int y, int w, int h) {
        context.fill(x, y, x + w, y + h, 0x66222222);
        context.fill(x, y, x + w, y + 1, 0x88444444);
        context.fill(x, y + h - 1, x + w, y + h, 0x88333333);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void handleTabClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!isInventoryScreen()) return;
        int tabY = this.y - TAB_HEIGHT - 4;

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

        // 在"天数"面板内点击时拦截事件
        if (currentTab == 0) {
            if (mouseX >= this.x && mouseX <= this.x + 176 &&
                    mouseY >= this.y && mouseY <= this.y + 166) {
                cir.setReturnValue(true);
            }
        }
    }
}