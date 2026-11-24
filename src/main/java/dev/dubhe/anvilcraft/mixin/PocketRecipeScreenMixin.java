package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.dubhe.anvilcraft.inventory.PocketInventory;
import dev.dubhe.anvilcraft.inventory.PocketLayout;
import dev.dubhe.anvilcraft.inventory.PocketSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractRecipeBookScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(AbstractRecipeBookScreen.class)
abstract class PocketRecipeScreenMixin {
    // 配方书展开时 GUI 会右移让位，右侧口袋面板可能被推出屏幕。
    // 这里按配方书与两侧面板各自所需的最小宽度抬高窄屏阈值，让界面在空间不足时改用配方书覆盖 GUI 的原版窄屏表现。
    @ModifyConstant(method = "init", constant = @Constant(intValue = 379))
    private int anvilcraft$allowRoomForPockets(int original) {
        Minecraft client = Minecraft.getInstance();
        if (!((Object) this instanceof InventoryScreen) || client.player == null) {
            return original;
        }
        int minWidth = PocketLayout.recipeBookMinWidth(
            PocketInventory.capacity(client.player),
            ((AbstractContainerScreen<?>) (Object) this).getImageWidth()
        );
        return Math.max(original, minWidth);
    }

    /**
     * 口袋面板位于 GUI 左右两侧，原版界外判定不认识它们；台阶面板还要按收窄后的轮廓判断。
     * 此外口袋会把配方书整体左移，原版按 {@code leftPos - 147} 划定的保护区不再覆盖书页实际位置，
     * 书页左侧空白会被判为界外点击并丢出光标上的物品，这里一并按书页真实原点补充保护。
     */
    @ModifyReturnValue(method = "hasClickedOutside", at = @At("RETURN"))
    private boolean anvilcraft$includePockets(boolean original, double mouseX, double mouseY, int left, int top) {
        if (!original || !((Object) this instanceof InventoryScreen)) {
            return original;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || PocketInventory.capacity(client.player) <= 0) {
            return original;
        }
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        double localX = mouseX - left;
        double localY = mouseY - top;
        if (PocketLayout.containsPocket(
            PocketInventory.capacity(client.player),
            screen.getImageWidth(),
            PocketSlot.SCREEN_PANEL_TOP,
            localX,
            localY
        )) {
            return false;
        }
        PocketRecipeBookAccessor book = (PocketRecipeBookAccessor) ((PocketRecipeScreenAccessor) this)
            .anvilcraft$getRecipeBookComponent();
        int bookX = (screen.width - RecipeBookComponent.IMAGE_WIDTH) / 2 - book.anvilcraft$getHorizontalOffset();
        int bookY = (screen.height - RecipeBookComponent.IMAGE_HEIGHT) / 2;
        if (mouseX >= bookX && mouseX < bookX + RecipeBookComponent.IMAGE_WIDTH
            && mouseY >= bookY && mouseY < bookY + RecipeBookComponent.IMAGE_HEIGHT) {
            return false;
        }
        return original;
    }
}
