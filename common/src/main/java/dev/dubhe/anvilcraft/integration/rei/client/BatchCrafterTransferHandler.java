package dev.dubhe.anvilcraft.integration.rei.client;

import dev.dubhe.anvilcraft.inventory.BatchCrafterMenu;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandler;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class BatchCrafterTransferHandler implements TransferHandler {
    @Override
    public Result handle(Context context) {
        AbstractContainerMenu menu = context.getMenu();
        if (menu instanceof BatchCrafterMenu batchCrafterMenu) {
            if (!context.isActuallyCrafting()) {
                return Result.createSuccessful();
            }

            return Result.createSuccessful();
        } else {
            return Result.createNotApplicable();
        }
    }
}
