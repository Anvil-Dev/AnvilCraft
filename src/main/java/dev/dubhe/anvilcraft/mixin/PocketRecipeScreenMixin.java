package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.dubhe.anvilcraft.inventory.PocketInventory;
import dev.dubhe.anvilcraft.inventory.PocketLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(AbstractRecipeBookScreen.class)
abstract class PocketRecipeScreenMixin {
    // 配方书展开时 GUI 会右移让位，右侧口袋面板可能被推出屏幕。
    // 这里按两侧面板宽度抬高窄屏阈值，让界面在空间不足时改用配方书覆盖 GUI 的原版窄屏表现。
    @ModifyConstant(method = "init", constant = @Constant(intValue = 379))
    private int anvilcraft$allowRoomForPockets(int original) {
        Minecraft client = Minecraft.getInstance();
        if (!((Object) this instanceof InventoryScreen) || client.player == null) {
            return original;
        }
        int capacity = PocketInventory.capacity(client.player);
        if (capacity == 0) {
            return original;
        }
        return original + PocketLayout.of(capacity, 0, 0, 0).width()
            + PocketLayout.of(capacity, 1, 0, 0).width() + 4;
    }

    @ModifyReturnValue(method = "hasClickedOutside", at = @At("RETURN"))
    private boolean anvilcraft$includePockets(boolean original, double mouseX, double mouseY, int left, int top) {
        Minecraft client = Minecraft.getInstance();
        if (!original || !((Object) this instanceof InventoryScreen) || client.player == null) {
            return original;
        }
        int capacity = PocketInventory.capacity(client.player);
        if (capacity == 0) {
            return original;
        }
        int imageWidth = ((AbstractContainerScreen<?>) (Object) this).getImageWidth();
        double localX = mouseX - left;
        double localY = mouseY - top;
        if (PocketLayout.of(capacity, 0, imageWidth, 70).contains(localX, localY)
            || PocketLayout.of(capacity, 1, imageWidth, 70).contains(localX, localY)) {
            return false;
        }
        return original;
    }
}
