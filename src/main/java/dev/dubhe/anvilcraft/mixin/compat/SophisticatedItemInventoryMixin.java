package dev.dubhe.anvilcraft.mixin.compat;

import dev.dubhe.anvilcraft.integration.TerminalItemInventory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.inventory.ITrackedContentsItemResourceHandler", remap = false)
public interface SophisticatedItemInventoryMixin extends TerminalItemInventory {
}
