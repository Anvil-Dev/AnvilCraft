package dev.dubhe.anvilcraft.integration.jei.transfer;

import dev.dubhe.anvilcraft.client.building.BlueprintClientFiles;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.inventory.StructureScannerMenu;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.Optional;
import javax.annotation.Nullable;

public final class StructureScannerRecipeTransferHandler<R extends RecipeHolder<?>>
    implements IRecipeTransferHandler<StructureScannerMenu, R> {
    private final RecipeType<R> type;
    private final IRecipeTransferHandlerHelper helper;

    private StructureScannerRecipeTransferHandler(RecipeType<R> type, IRecipeTransferHandlerHelper helper) {
        this.type = type;
        this.helper = helper;
    }

    public static void register(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new StructureScannerRecipeTransferHandler<>(
            AnvilCraftJeiPlugin.MULTIBLOCK_CRAFTING, registration.getTransferHelper()), AnvilCraftJeiPlugin.MULTIBLOCK_CRAFTING);
        registration.addRecipeTransferHandler(new StructureScannerRecipeTransferHandler<>(
            AnvilCraftJeiPlugin.MULTIBLOCK_CONVERSION, registration.getTransferHelper()), AnvilCraftJeiPlugin.MULTIBLOCK_CONVERSION);
    }

    @Override
    public Class<? extends StructureScannerMenu> getContainerClass() {
        return StructureScannerMenu.class;
    }

    @Override
    public Optional<MenuType<StructureScannerMenu>> getMenuType() {
        return Optional.of(ModMenuTypes.STRUCTURE_SCANNER.get());
    }

    @Override
    public RecipeType<R> getRecipeType() {
        return this.type;
    }

    @Override
    @Nullable
    public IRecipeTransferError transferRecipe(
        StructureScannerMenu menu, R recipe, IRecipeSlotsView slots, Player player, boolean maxTransfer, boolean doTransfer
    ) {
        if (!menu.stillValid(player) || BlueprintClientFiles.isBusy()) return this.helper.createInternalError();
        if (doTransfer) BlueprintClientFiles.requestRecipe(menu.containerId, recipe.id());
        return null;
    }
}
