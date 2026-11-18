package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.integration.jei.transfer.TerminalJeiTransferSupport;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IStackHelper;
import mezz.jei.api.recipe.transfer.IRecipeTransferContext;
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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = BasicRecipeTransferHandler.class, remap = false)
public abstract class JeiBasicRecipeTransferHandlerMixin<C extends AbstractContainerMenu, R> {
    @Shadow @Final private IRecipeTransferInfo<C, R> transferInfo;
    @Shadow @Final private IRecipeTransferHandlerHelper handlerHelper;
    @Shadow @Final private IStackHelper stackHelper;

    @Shadow
    public abstract @Nullable IRecipeTransferError transferRecipe(C menu, R recipe, IRecipeSlotsView slots,
                                                                  Player player, boolean maximum, boolean transfer);

    @Shadow
    public abstract @Nullable IRecipeTransferError transferRecipe(IRecipeTransferContext<R, C> context, boolean transfer);

    @Unique
    private @Nullable IRecipeSlotsView anvilcraft$resolveRecipeSlots(Object raw) {
        if (raw instanceof IRecipeSlotsView view) return view;
        if (raw instanceof List<?> list) {
            @SuppressWarnings("unchecked")
            List<IRecipeSlotView> slotViews = (List<IRecipeSlotView>) list;
            return this.handlerHelper.createRecipeSlotsView(slotViews);
        }
        return null;
    }

    @Inject(method = "transferRecipeInternal", at = @At(value = "INVOKE",
        target = "Lmezz/jei/common/transfer/RecipeTransferUtil;getRecipeTransferOperations("
            + "Lmezz/jei/api/helpers/IStackHelper;Ljava/util/Map;Ljava/util/List;Ljava/util/List;)"
            + "Lmezz/jei/common/transfer/RecipeTransferOperationsResult;"), cancellable = true)
    private void anvilcraft$restock(C menu, R recipe, IRecipeSlotsView slots, Player player, boolean maximum, boolean transfer,
                                    @Nullable IRecipeTransferContext<R, C> context,
                                    CallbackInfoReturnable<IRecipeTransferError> callback) {
        IRecipeSlotsView resolved = this.anvilcraft$resolveRecipeSlots(slots);
        if (resolved == null) return;
        TerminalJeiTransferSupport.handle(menu, resolved, player, this.stackHelper, this.handlerHelper,
            this.transferInfo.getRecipeSlots(menu, recipe), this.transferInfo.getInventorySlots(menu, recipe), maximum, transfer,
            () -> context == null ? this.transferRecipe(menu, recipe, resolved, player, maximum, true)
                : this.transferRecipe(context, true), callback::setReturnValue);
    }
}
