package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.dubhe.anvilcraft.inventory.PocketSlot;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen$SlotWrapper")
abstract class CreativePocketSlotMixin extends Slot {
    @Shadow
    @Final
    private Slot target;

    protected CreativePocketSlotMixin(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @ModifyReturnValue(method = "isActive", at = @At("RETURN"))
    private boolean anvilcraft$positionPockets(boolean active) {
        if (this.target instanceof PocketSlot pocket) {
            ((SlotPositionAccessor) this).anvilcraft$setX(pocket.x < 0 ? pocket.x : pocket.x + 19);
        }
        return active;
    }
}
