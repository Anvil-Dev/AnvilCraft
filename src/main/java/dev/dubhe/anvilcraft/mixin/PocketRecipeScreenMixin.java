package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.dubhe.anvilcraft.inventory.PocketInventory;
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
    @ModifyConstant(method = "init", constant = @Constant(intValue = 379))
    private int anvilcraft$allowRoomForPockets(int original) {
        var player = Minecraft.getInstance().player;
        if (!((Object) this instanceof InventoryScreen) || player == null) return original;
        int capacity = PocketInventory.capacity(player);
        return original + (capacity == 0 ? 0 : capacity == 12 ? 92 : 56);
    }

    @ModifyReturnValue(method = "hasClickedOutside", at = @At("RETURN"))
    private boolean anvilcraft$includePockets(boolean original, double mouseX, double mouseY, int left, int top) {
        Minecraft client = Minecraft.getInstance();
        if (!original || !((Object) this instanceof InventoryScreen) || client.player == null) return original;
        int capacity = PocketInventory.capacity(client.player);
        if (capacity == 0 || mouseY < top + 70 || mouseY >= top + 143) return original;
        int width = capacity == 12 ? 44 : 26;
        int imageWidth = ((AbstractContainerScreen<?>) (Object) this).getImageWidth();
        return !(mouseX >= left - width - 2 && mouseX < left - 2
            || mouseX >= left + imageWidth + 2 && mouseX < left + imageWidth + width + 2);
    }
}
