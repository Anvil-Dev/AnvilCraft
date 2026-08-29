package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.client.rpc.TerminalJeiStorageCache;
import dev.dubhe.anvilcraft.integration.jei.transfer.TerminalJeiTransferSupport;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import mezz.jei.library.transfer.BasicRecipeTransferHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * 把终端的 JEI 转移补全逻辑注入 JEI 的 {@link BasicRecipeTransferHandler#transferRecipe}。
 * 具体补全逻辑在 {@link TerminalJeiTransferSupport}，本 mixin 只做适配：
 * <ul>
 *   <li>检查阶段（doTransfer=false）：结合存储计算精确缺口，可补足时返回成功（"+" 可用），
 *       不足时返回只高亮真正缺失槽的错误；</li>
 *   <li>传输阶段（doTransfer=true）：补足背包缺口后重试原传输逻辑，重试后退回多余材料。</li>
 * </ul>
 * 其它模组 Handler 的 mixin 可复用 {@link TerminalJeiTransferSupport}（仅需适配
 * 各自的 transferRecipe 签名与 transferInfo 获取方式）。
 */
@Mixin(BasicRecipeTransferHandler.class)
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

    @Final
    @Shadow
    private IRecipeTransferInfo<C, R> transferInfo;

    @Final
    @Shadow
    private IRecipeTransferHandlerHelper handlerHelper;

    @Inject(method = "transferRecipe", at = @At("HEAD"), cancellable = true)
    private void anvilcraft$restockOrAllow(
        C container,
        R recipe,
        IRecipeSlotsView recipeSlotsView,
        Player player,
        boolean maxTransfer,
        boolean doTransfer,
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
            // 检查阶段：null 表示存储可补足（"+" 可用，必须拦截返回成功，否则走原方法
            // 会用空背包判断导致"全部缺失"）；非 null 为精确缺失错误（只高亮真正缺的槽）
            IRecipeTransferError error = TerminalJeiTransferSupport.checkSatisfies(
                container,
                recipeSlotsView,
                player,
                this.handlerHelper
            );
            cir.setReturnValue(error);
            return;
        }
        List<Slot> craftingSlots = this.transferInfo.getRecipeSlots(container, recipe);
        List<Slot> inventorySlots = this.transferInfo.getInventorySlots(container, recipe);
        TerminalJeiTransferSupport.restockThenTransfer(
            container,
            craftingSlots,
            inventorySlots,
            recipeSlotsView,
            player,
            maxTransfer,
            () -> this.transferRecipe(container, recipe, recipeSlotsView, player, maxTransfer, true)
        );
        cir.setReturnValue(null);
    }
}
