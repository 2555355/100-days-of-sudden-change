package com.zombieapocalypse.client;

import com.zombieapocalypse.config.StageSystem;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.world.World;

/**
 * 血月HUD提示
 *
 * 行为:
 *   1. 检测血月状态变化(非血月 → 血月), 在屏幕中央显示大字警告, 持续约10秒带淡入淡出。
 *   2. 血月日的夜晚期间, 在屏幕中央偏上持续显示"血月降临"小标识。
 *
 * 状态追踪:
 *   - lastBloodMoon: 上一tick的血月状态, 用于检测变化
 *   - announcementStartTick: 大字提示开始的世界时间tick
 */
public class BloodMoonHudOverlay implements HudRenderCallback, ClientTickEvents.EndTick {

    // 大字提示持续时长 (毫秒)
    private static final long ANNOUNCE_DURATION_MS = 10000L;
    // 淡入淡出时长 (毫秒)
    private static final long FADE_DURATION_MS = 800L;

    // 配色
    private static final int COLOR_BLOOD_RED      = 0xFFFF1A1A;
    private static final int COLOR_BLOOD_DARK     = 0xFF8B0000;
    private static final int COLOR_PULSE_DIM      = 0xFFCC4040;

    // 状态
    private static boolean lastBloodMoon = false;
    private static boolean initialized = false;
    private static long announcementStartMs = 0L;
    private static boolean announcementActive = false;
    // 缓存当前tick血月状态, onHudRender直接复用, 避免每帧重算
    private static boolean cachedBloodMoon = false;

    // 预创建静态Text对象, 避免每帧分配
    private static final Text MAIN_TEXT = Text.literal("血月降临");
    private static final Text SUB_TEXT = Text.literal("僵尸速度 +30%  攻击 +20%  刷新翻倍");
    private static final Text PERSIST_TEXT = Text.literal("☠ 血月进行中 ☠");

    public static void register() {
        BloodMoonHudOverlay instance = new BloodMoonHudOverlay();
        HudRenderCallback.EVENT.register(instance);
        ClientTickEvents.END_CLIENT_TICK.register(instance);
    }

    @Override
    public void onEndTick(MinecraftClient client) {
        // 仅在游戏内追踪 (不在菜单/暂停界面)
        if (client.world == null || client.player == null) {
            initialized = false;
            return;
        }

        World world = client.world;
        boolean currentBloodMoon = StageSystem.isBloodMoon(world);
        cachedBloodMoon = currentBloodMoon;

        // 首次初始化: 不触发提示, 仅记录当前状态
        if (!initialized) {
            lastBloodMoon = currentBloodMoon;
            initialized = true;
            return;
        }

        // 检测从非血月变为血月 (新一天进入血月)
        if (!lastBloodMoon && currentBloodMoon) {
            // 进入血月: 触发大字提示
            announcementStartMs = System.currentTimeMillis();
            announcementActive = true;
        }
        lastBloodMoon = currentBloodMoon;
    }

    @Override
    public void onHudRender(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return;
        if (client.options.hudHidden) return;

        TextRenderer tr = client.textRenderer;
        boolean isBloodMoon = cachedBloodMoon;
        boolean isNight = !client.world.isDay();
        int screenW = context.getScaledWindowWidth();
        int screenH = context.getScaledWindowHeight();
        int centerX = screenW / 2;
        int centerY = screenH / 2;

        // 1. 大字提示 (进入血月时显示10秒, 带淡入淡出)
        if (announcementActive) {
            long elapsed = System.currentTimeMillis() - announcementStartMs;
            if (elapsed >= ANNOUNCE_DURATION_MS) {
                announcementActive = false;
            } else {
                float alpha = computeAlpha(elapsed);
                renderAnnouncement(context, tr, centerX, centerY, alpha);
            }
        }

        // 2. 血月夜持续标识 (屏幕中央偏上, 持续提醒)
        if (isBloodMoon && isNight && !announcementActive) {
            renderPersistentMarker(context, tr, centerX, 18);
        }
    }

    /**
     * 计算淡入淡出alpha (0.0~1.0)
     * 前 FADE_DURATION_MS 淡入, 后 FADE_DURATION_MS 淡出, 中间全程显示
     */
    private float computeAlpha(long elapsedMs) {
        long remaining = ANNOUNCE_DURATION_MS - elapsedMs;
        float fadeIn = Math.min(1.0f, elapsedMs / (float) FADE_DURATION_MS);
        float fadeOut = remaining < FADE_DURATION_MS
                ? Math.max(0.0f, remaining / (float) FADE_DURATION_MS)
                : 1.0f;
        return Math.min(fadeIn, fadeOut);
    }

    /**
     * 屏幕中央大字提示: 血月降临
     * 包含主标题 + 副标题 + 阴影背景
     */
    private void renderAnnouncement(DrawContext ctx, TextRenderer tr, int cx, int cy, float alpha) {
        int a = (int) (alpha * 255) & 0xFF;
        if (a <= 0) return;

        // 半透明黑色蒙版(中央区域, 让文字更突出)
        int maskAlpha = (int) (alpha * 120) & 0xFF;
        int maskColor = (maskAlpha << 24);
        ctx.fill(cx - 130, cy - 38, cx + 130, cy + 38, maskColor);

        // 主标题: 血月降临 (2倍缩放)
        int mainColor = withAlpha(COLOR_BLOOD_RED, a);
        int mainW = tr.getWidth(MAIN_TEXT) * 2;
        int mainX = cx - mainW / 2;
        int mainY = cy - 26;

        // 主标题阴影层(深红色)
        drawScaledText(ctx, tr, MAIN_TEXT, mainX + 2, mainY + 2, withAlpha(COLOR_BLOOD_DARK, a), 2);
        // 主标题本体
        drawScaledText(ctx, tr, MAIN_TEXT, mainX, mainY, mainColor, 2);

        // 副标题: 危险等级提升
        int subColor = withAlpha(COLOR_PULSE_DIM, a);
        int subW = tr.getWidth(SUB_TEXT);
        ctx.drawTextWithShadow(tr, SUB_TEXT, cx - subW / 2, cy + 6, subColor);

        // 装饰横线
        int lineColor = withAlpha(COLOR_BLOOD_DARK, a);
        ctx.fill(cx - 80, cy + 20, cx + 80, cy + 21, lineColor);
    }

    /**
     * 血月夜持续标识 (屏幕中央上方)
     * 小型脉冲红字提示当前血月进行中
     */
    private void renderPersistentMarker(DrawContext ctx, TextRenderer tr, int cx, int cy) {
        long t = System.currentTimeMillis();
        float pulse = 0.7f + 0.3f * (float) Math.sin(t * 0.005);
        int a = (int) (pulse * 230) & 0xFF;

        int color = withAlpha(COLOR_BLOOD_RED, a);
        int w = tr.getWidth(PERSIST_TEXT);

        // 小型半透明背景
        int bgAlpha = (int) (pulse * 90) & 0xFF;
        ctx.fill(cx - w / 2 - 6, cy - 3, cx + w / 2 + 6, cy + 11, (bgAlpha << 24) | 0x400000);
        // 边框
        ctx.fill(cx - w / 2 - 6, cy - 3, cx + w / 2 + 6, cy - 2, color);
        ctx.fill(cx - w / 2 - 6, cy + 10, cx + w / 2 + 6, cy + 11, color);

        ctx.drawTextWithShadow(tr, PERSIST_TEXT, cx - w / 2, cy, color);
    }

    /**
     * 绘制缩放文字(整数倍放大, 用多次偏移绘制实现)
     * Minecraft 1.20.1 DrawContext 没有直接scale文字API, 用矩阵缩放
     */
    private void drawScaledText(DrawContext ctx, TextRenderer tr, Text text,
                                int x, int y, int color, int scale) {
        if (scale <= 1) {
            ctx.drawTextWithShadow(tr, text, x, y, color);
            return;
        }
        // 使用矩阵栈缩放
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x, y, 0);
        ctx.getMatrices().scale(scale, scale, 1.0f);
        ctx.drawTextWithShadow(tr, text, 0, 0, color);
        ctx.getMatrices().pop();
    }

    /**
     * 给ARGB颜色替换alpha分量
     */
    private int withAlpha(int argb, int alpha) {
        return (argb & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }
}
