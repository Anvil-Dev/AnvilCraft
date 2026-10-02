package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.inventory.PocketInventory;
import dev.dubhe.anvilcraft.inventory.PocketSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InventoryMenu.class)
abstract class InventoryMenuMixin extends AbstractContainerMenu {
    protected InventoryMenuMixin(@Nullable MenuType<?> type, int id) {
        super(type, id);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void anvilcraft$addPockets(Inventory inventory, boolean active, Player owner, CallbackInfo ci) {
        for (int index = 0; index < PocketInventory.MAX_SIZE; index++) this.addSlot(new PocketSlot(owner, index));
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void anvilcraft$lockLeggings(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (index == 7 && PocketInventory.isLocked(player)) cir.setReturnValue(ItemStack.EMPTY);
    }
}
