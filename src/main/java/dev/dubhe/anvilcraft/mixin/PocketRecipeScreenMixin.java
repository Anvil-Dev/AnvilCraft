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

@Mixin(AbstractRecipeBookScreen.class)
abstract class PocketRecipeScreenMixin {
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
