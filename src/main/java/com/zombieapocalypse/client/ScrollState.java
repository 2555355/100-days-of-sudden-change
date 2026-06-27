package com.zombieapocalypse.client;

/**
 * 滚动状态共享 - 连接 InventoryScreenMixin 与 MouseScrollMixin
 * 游戏同时只显示一个背包界面，因此使用静态字段是安全的
 */
public class ScrollState {
    // 每个子页面独立的滚动偏移
    public static int scrollBasic = 0;
    public static int scrollZombie = 0;
    public static int scrollGiant = 0;

    // 当前状态（由 InventoryScreenMixin 在渲染时更新）
    public static int currentTab = 1;      // 0=末日情报, 1=生存背包
    public static int currentSubTab = 0;   // 0=基础, 1=僵尸, 2=巨型
    public static int currentContentHeight = 0;

    // 面板可视区域坐标（由 InventoryScreenMixin 在渲染时更新）
    public static int panelX = 0;
    public static int panelY = 0;
    public static int panelW = 176;
    public static int panelH = 166;
    public static int contentTop = 0;
    public static int contentBottom = 0;
    public static boolean isActive = false;

    public static final int SCROLL_STEP = 12;

    public static int getCurrentScroll() {
        return switch (currentSubTab) {
            case 0 -> scrollBasic;
            case 1 -> scrollZombie;
            default -> scrollGiant;
        };
    }

    public static void setCurrentScroll(int v) {
        switch (currentSubTab) {
            case 0 -> scrollBasic = v;
            case 1 -> scrollZombie = v;
            default -> scrollGiant = v;
        }
    }

    public static int getVisibleHeight() {
        return Math.max(0, contentBottom - contentTop);
    }

    public static int getMaxScroll() {
        return Math.max(0, currentContentHeight - getVisibleHeight());
    }

    /**
     * 应用一次滚轮滚动，返回是否被消费
     */
    public static boolean applyScroll(double mouseX, double mouseY, double amount) {
        if (!isActive) return false;
        if (currentTab != 0) return false;
        // 鼠标需在面板范围内
        if (mouseX < panelX || mouseX > panelX + panelW) return false;
        if (mouseY < panelY || mouseY > panelY + panelH) return false;

        int maxScroll = getMaxScroll();
        if (maxScroll <= 0) return false;

        // amount > 0 向上滚（内容下移），< 0 向下滚（内容上移）
        int delta = amount > 0 ? -SCROLL_STEP : SCROLL_STEP;
        int next = getCurrentScroll() + delta;
        next = Math.max(0, Math.min(maxScroll, next));
        if (next == getCurrentScroll()) return false;
        setCurrentScroll(next);
        return true;
    }
}
