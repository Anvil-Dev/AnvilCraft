package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.client.rpc.TerminalJeiStorageCache;
import dev.dubhe.anvilcraft.integration.jei.transfer.TerminalJeiTransferSupport;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IStackHelper;
import mezz.jei.api.recipe.transfer.IRecipeTransferContext;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import mezz.jei.library.transfer.BasicRecipeTransferHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.UUID;

/** 在 JEI 完成菜单校验后，为标准处理器的两种调用入口补齐仓储材料。 */
@Mixin(value = BasicRecipeTransferHandler.class, remap = false)
public abstract class JeiBasicRecipeTransferHandlerMixin<C extends AbstractContainerMenu, R> {
    @Shadow
    public abstract @Nullable IRecipeTransferError transferRecipe(
        C container,
        R recipe,
        IRecipeSlotsView recipeSlotsView,
        Player player,
        boolean maxTransfer,
        boolean doTransfer
    );

    @Shadow
    public abstract @Nullable IRecipeTransferError transferRecipe(IRecipeTransferContext<R, C> context, boolean doTransfer);

    @Final
    @Shadow
    private IRecipeTransferInfo<C, R> transferInfo;

    @Final
    @Shadow
    private IRecipeTransferHandlerHelper handlerHelper;

    @Final
    @Shadow
    private IStackHelper stackHelper;

    @Inject(method = "transferRecipeInternal", at = @At(value = "INVOKE",
        target = "Lmezz/jei/common/transfer/RecipeTransferUtil;getRecipeTransferOperations("
            + "Lmezz/jei/api/helpers/IStackHelper;Ljava/util/Map;Ljava/util/List;Ljava/util/List;)"
            + "Lmezz/jei/common/transfer/RecipeTransferOperationsResult;"), cancellable = true)
    private void anvilcraft$restockOrAllow(
        C container,
        R recipe,
        IRecipeSlotsView recipeSlotsView,
        Player player,
        boolean maxTransfer,
        boolean doTransfer,
        @Nullable IRecipeTransferContext<R, C> context,
        CallbackInfoReturnable<IRecipeTransferError> cir
    ) {
        if (!player.level().isClientSide() || TerminalJeiStorageCache.isRestocking()) {
            return;
        }
        List<UUID> storageIds = TerminalJeiStorageCache.boundStorages(player);
        if (storageIds.isEmpty()) {
            return;
        }
        if (!doTransfer) {
            // 可由仓储补齐时允许填充，否则只高亮实际缺失的槽位。
            IRecipeTransferError error = TerminalJeiTransferSupport.checkSatisfies(
                container,
                recipeSlotsView,
                player,
                this.stackHelper,
                this.handlerHelper
            );
            cir.setReturnValue(error);
            return;
        }
        List<Slot> craftingSlots = this.transferInfo.getRecipeSlots(container, recipe);
        List<Slot> inventorySlots = this.transferInfo.getInventorySlots(container, recipe);
        boolean restocked = TerminalJeiTransferSupport.restockThenTransfer(
            container,
            craftingSlots,
            inventorySlots,
            recipeSlotsView,
            player,
            this.stackHelper,
            maxTransfer,
            () -> context == null ? this.transferRecipe(container, recipe, recipeSlotsView, player, maxTransfer, true)
                : this.transferRecipe(context, true)
        );
        if (restocked) {
            // 异步补齐后复用当前上下文重试，保留 JEI 的传输完成通知。
            cir.setReturnValue(null);
        }
    }
}
