package dev.dubhe.anvilcraft.mixin.compat;

import com.llamalad7.mixinextras.sugar.Local;
import dev.dubhe.anvilcraft.integration.jei.transfer.TerminalJeiTransferSupport;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IStackHelper;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Pseudo
@Mixin(targets = "net.p3pp3rf1y.sophisticatedcore.compat.recipeviewers.jei.JeiCraftingContainerRecipeTransferHandlerBase", remap = false)
public abstract class SophisticatedJeiRecipeTransferMixin {
    @Final
    @Shadow
    private IRecipeTransferHandlerHelper handlerHelper;

    @Final
    @Shadow
    private IStackHelper stackHelper;

    @Inject(
        method = "transferRecipe",
        at = @At(
            value = "INVOKE",
            target = "Lmezz/jei/common/transfer/RecipeTransferUtil;getRecipeTransferOperations"
                + "(Lmezz/jei/api/helpers/IStackHelper;Ljava/util/Map;Ljava/util/List;Ljava/util/List;)"
                + "Lmezz/jei/common/transfer/RecipeTransferOperationsResult;"
        ),
        cancellable = true
    )
    @SuppressWarnings("unchecked")
    private void anvilcraft$restockCraftingUpgrade(
        @Coerce AbstractContainerMenu container,
        RecipeHolder<?> recipe,
        IRecipeSlotsView recipeSlots,
        Player player,
        boolean maxTransfer,
        boolean doTransfer,
        CallbackInfoReturnable<IRecipeTransferError> cir,
        @Local(name = "craftingSlots") List<Slot> craftingSlots,
        @Local(name = "inventorySlots") List<Slot> inventorySlots
    ) {
        IRecipeTransferHandler<AbstractContainerMenu, RecipeHolder<?>> handler =
            (IRecipeTransferHandler<AbstractContainerMenu, RecipeHolder<?>>) (Object) this;
        TerminalJeiTransferSupport.handle(container, recipeSlots, player, this.stackHelper, this.handlerHelper,
            craftingSlots, inventorySlots, maxTransfer, doTransfer,
            () -> {
                IRecipeTransferError error = handler.transferRecipe(container, recipe, recipeSlots, player, maxTransfer, true);
                Minecraft client = Minecraft.getInstance();
                if (error == null && client.screen instanceof AbstractContainerScreen<?> screen && screen.getMenu() == container) {
                    // The asynchronous retry can open an upgrade tab after JEI has already restored this screen.
                    screen.resize(client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
                }
                return error;
            }, cir::setReturnValue);
    }
}
