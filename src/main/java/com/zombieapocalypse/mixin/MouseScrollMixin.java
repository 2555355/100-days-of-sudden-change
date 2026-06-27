package com.zombieapocalypse.mixin;

import com.zombieapocalypse.client.ScrollState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 拦截鼠标滚轮 GLFW 回调，转发给 ScrollState
 *
 * 为什么不用 @Mixin(HandledScreen/Screen) 的 mouseScrolled：
 *   1.20.1 中 mouseScrolled 是 ParentElement 接口的 default 方法，
 *   HandledScreen / Screen / InventoryScreen 均未 override，
 *   @Mixin(HandledScreen.class) 找不到目标会触发 Critical injection failure。
 *   改在 Mouse.onMouseScroll（GLFW 底层回调，类中真实声明）层注入最可靠。
 */
@Mixin(Mouse.class)
public abstract class MouseScrollMixin {

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void zombieapocalypse$onScroll(long window, double horizontalAmount, double verticalAmount, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        if (!(client.currentScreen instanceof InventoryScreen)) return;

        // 垂直滚轮优先；部分鼠标只有 horizontal
        double amount = verticalAmount != 0 ? verticalAmount : horizontalAmount;
        if (amount == 0) return;

        // 将屏幕像素坐标换算为 GUI 缩放坐标
        double scaleFactor = client.getWindow().getScaleFactor();
        double mouseX = client.mouse.getX() / scaleFactor;
        double mouseY = client.mouse.getY() / scaleFactor;

        if (ScrollState.applyScroll(mouseX, mouseY, amount)) {
            ci.cancel();
        }
    }
}
