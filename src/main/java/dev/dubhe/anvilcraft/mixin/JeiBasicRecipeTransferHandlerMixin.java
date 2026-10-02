package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.integration.jei.transfer.TerminalJeiTransferSupport;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IStackHelper;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import mezz.jei.library.transfer.BasicRecipeTransferHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BasicRecipeTransferHandler.class, remap = false)
public abstract class JeiBasicRecipeTransferHandlerMixin<C extends AbstractContainerMenu, R> {
    @Shadow @Final private IRecipeTransferInfo<C, R> transferInfo;
    @Shadow @Final private IRecipeTransferHandlerHelper handlerHelper;
    @Shadow @Final private IStackHelper stackHelper;

    @Shadow
    public abstract @Nullable IRecipeTransferError transferRecipe(C menu, R recipe, IRecipeSlotsView slots,
                                                                  Player player, boolean maximum, boolean transfer);

    @Inject(method = "transferRecipe", at = @At(value = "INVOKE",
        target = "Lmezz/jei/common/transfer/RecipeTransferUtil;getRecipeTransferOperations("
            + "Lmezz/jei/api/helpers/IStackHelper;Ljava/util/Map;Ljava/util/List;Ljava/util/List;)"
            + "Lmezz/jei/common/transfer/RecipeTransferOperationsResult;"), cancellable = true)
    private void anvilcraft$restock(C menu, R recipe, IRecipeSlotsView slots, Player player, boolean maximum, boolean transfer,
                                    CallbackInfoReturnable<IRecipeTransferError> callback) {
        TerminalJeiTransferSupport.handle(menu, slots, player, this.stackHelper, this.handlerHelper,
            this.transferInfo.getRecipeSlots(menu, recipe), this.transferInfo.getInventorySlots(menu, recipe), maximum, transfer,
            () -> this.transferRecipe(menu, recipe, slots, player, maximum, true), callback::setReturnValue);
    }
}
