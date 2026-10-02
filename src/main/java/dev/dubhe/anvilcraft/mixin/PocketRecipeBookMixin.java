package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.dubhe.anvilcraft.inventory.PocketInventory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(RecipeBookComponent.class)
abstract class PocketRecipeBookMixin {
    @Shadow
    protected Minecraft minecraft;
    @Shadow
    @Final
    protected RecipeBookMenu menu;

    @Accessor("xOffset")
    public abstract int anvilcraft$getHorizontalOffset();

    @ModifyConstant(method = "initVisuals", constant = @Constant(intValue = 86))
    private int anvilcraft$makeRoomForPockets(int original) {
        if (!(this.menu instanceof InventoryMenu) || this.minecraft.player == null) return original;
        int capacity = PocketInventory.capacity(this.minecraft.player);
        return original + (capacity == 0 ? 0 : capacity == 12 ? 46 : 28);
    }

    @ModifyReturnValue(method = "isOffsetNextToMainGUI", at = @At("RETURN"))
    private boolean anvilcraft$recognizePocketOffset(boolean original) {
        return original || this.menu instanceof InventoryMenu && this.anvilcraft$getHorizontalOffset() > 86;
    }
}
