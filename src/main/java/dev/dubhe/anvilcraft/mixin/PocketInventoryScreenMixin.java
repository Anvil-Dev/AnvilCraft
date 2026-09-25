package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.PocketInventory;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
abstract class PocketInventoryScreenMixin extends AbstractContainerScreen<InventoryMenu> {
    @Unique
    private int anvilcraft$pocketCapacity;

    protected PocketInventoryScreenMixin(InventoryMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void anvilcraft$rememberPockets(CallbackInfo ci) {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.anvilcraft$pocketCapacity = PocketInventory.capacity(this.minecraft.player);
        }
    }

    @Inject(method = "containerTick", at = @At("TAIL"))
    private void anvilcraft$updatePocketLayout(CallbackInfo ci) {
        if (this.minecraft != null && this.minecraft.player != null
            && this.anvilcraft$pocketCapacity != PocketInventory.capacity(this.minecraft.player)) {
            this.rebuildWidgets();
        }
    }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void anvilcraft$drawPockets(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (this.minecraft == null || this.minecraft.player == null) return;
        int capacity = PocketInventory.capacity(this.minecraft.player);
        if (capacity == 0) return;
        int width = capacity == 12 ? 44 : 26;
        String texture = capacity == 12 ? "weatherproof_spacesuit_leggings" : "pockets_leggings";
        for (int side = 0; side < 2; side++) {
            int posX = this.leftPos + (side == 0 ? -width - 2 : this.imageWidth + 2);
            graphics.blit(RenderPipelines.GUI_TEXTURED, AnvilCraft.of("textures/gui/misc/equipment/" + texture + ".png"),
                posX, this.topPos + 70, 0, 0, width, 73, width, 73);
        }
    }

}
