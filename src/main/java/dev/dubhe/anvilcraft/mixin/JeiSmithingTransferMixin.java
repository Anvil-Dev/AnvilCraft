package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.dubhe.anvilcraft.inventory.AdjacentSmithingMenu;
import mezz.jei.common.transfer.BasicRecipeTransferHandlerServer;
import mezz.jei.common.transfer.TransferOperation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(BasicRecipeTransferHandlerServer.class)
public abstract class JeiSmithingTransferMixin {
    @WrapMethod(method = "setItems")
    private static void anvilcraft$transferSmithingInputs(
        Player player,
        List<TransferOperation> operations,
        List<Slot> craftingSlots,
        List<Slot> inventorySlots,
        boolean maxTransfer,
        boolean requireCompleteSets,
        Operation<Void> original
    ) {
        if (player.containerMenu instanceof AdjacentSmithingMenu menu) {
            menu.runRecipeTransfer(() -> original.call(
                player, operations, craftingSlots, inventorySlots, maxTransfer, requireCompleteSets
            ));
        } else {
            original.call(player, operations, craftingSlots, inventorySlots, maxTransfer, requireCompleteSets);
        }
    }
}
