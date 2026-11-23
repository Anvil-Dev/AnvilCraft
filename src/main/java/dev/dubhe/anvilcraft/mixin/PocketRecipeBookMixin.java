package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.dubhe.anvilcraft.inventory.PocketInventory;
import dev.dubhe.anvilcraft.inventory.PocketLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.RecipeBookMenu;
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
    protected RecipeBookMenu<?, ?> menu;

    @Accessor("xOffset")
    public abstract int anvilcraft$getHorizontalOffset();

    @Accessor("width")
    public abstract int anvilcraft$getScreenWidth();

    @Accessor("height")
    public abstract int anvilcraft$getScreenHeight();

    @ModifyConstant(method = "initVisuals", constant = @Constant(intValue = 86))
    private int anvilcraft$makeRoomForPockets(int original) {
        if (!(this.menu instanceof InventoryMenu) || this.minecraft.player == null) {
            return original;
        }
        int capacity = PocketInventory.capacity(this.minecraft.player);
        return original + (capacity == 0 ? 0 : PocketLayout.of(capacity, 0, 0, 0).width() + 2);
    }

    @ModifyReturnValue(method = "isOffsetNextToMainGUI", at = @At("RETURN"))
    private boolean anvilcraft$recognizePocketOffset(boolean original) {
        return original || this.menu instanceof InventoryMenu && this.anvilcraft$getHorizontalOffset() > 86;
    }

    /**
     * 口袋把配方书整体左移后，原版按 {@code leftPos - 147} 划定的保护区不再覆盖书页实际位置，
     * 书页左侧空白会被判为界外点击并丢出光标上的物品。这里按书页真实原点补充保护。
     */
    @ModifyReturnValue(method = "hasClickedOutside", at = @At("RETURN"))
    private boolean anvilcraft$protectMovedRecipeBook(boolean original, double mouseX, double mouseY) {
        if (!original || !(this.menu instanceof InventoryMenu) || this.minecraft.player == null) {
            return original;
        }
        int capacity = PocketInventory.capacity(this.minecraft.player);
        if (capacity <= 0) {
            return original;
        }
        int bookX = (this.anvilcraft$getScreenWidth() - RecipeBookComponent.IMAGE_WIDTH) / 2
            - this.anvilcraft$getHorizontalOffset();
        int bookY = (this.anvilcraft$getScreenHeight() - RecipeBookComponent.IMAGE_HEIGHT) / 2;
        if (mouseX >= bookX && mouseX < bookX + RecipeBookComponent.IMAGE_WIDTH
            && mouseY >= bookY && mouseY < bookY + RecipeBookComponent.IMAGE_HEIGHT) {
            return false;
        }
        return original;
    }
}
