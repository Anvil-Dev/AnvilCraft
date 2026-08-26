package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.client.rpc.StorageTerminalClientStub;
import dev.dubhe.anvilcraft.client.rpc.TerminalJeiStorageCache;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.library.transfer.BasicRecipeTransferHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * 把终端的 JEI 转移配方逻辑注入 JEI 的 {@link BasicRecipeTransferHandler#transferRecipe}：
 * <ul>
 *   <li>检查阶段（doTransfer=false）：持有绑定终端时直接返回成功，使 "+" 按钮可用
 *       （存储站物品视为可用），不实际取物。</li>
 *   <li>传输阶段（doTransfer=true）：先从存储站补足背包缺少的配方物品（异步），
 *       补库完成后重试原传输逻辑把物品填入合成格。</li>
 * </ul>
 * 不注入 {@code getInventoryState}：其声明的"虚拟空槽"无法让服务端实际取到物品。
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
            // 检查阶段：任一终端目标（背包 + 其存储）满足配方需求即可视为可用；
            // 存储站物品列表为异步缓存，首次可能未就绪——此时走原方法（+ 暂不可用），
            // 缓存加载完成后 JEI 刷新即可正确判断。
            for (UUID storageId : storageIds) {
                TerminalJeiStorageCache.ensure(storageId);
                List<ItemStack> storageItems = TerminalJeiStorageCache.get(storageId);
                if (storageItems != null
                    && JeiBasicRecipeTransferHandlerMixin.anvilcraft$containerSatisfies(
                        container,
                        storageItems,
                        recipeSlotsView
                    )) {
                    cir.setReturnValue(null);
                    break;
                }
            }
            return;
        }
        // 传输阶段：先从全部终端目标补足背包缺少的配方物品
        List<ItemStack> missing = JeiBasicRecipeTransferHandlerMixin.anvilcraft$collectMissing(
            container,
            recipeSlotsView,
            maxTransfer
        );
        if (missing.isEmpty()) {
            return; // 背包已足够，走原传输逻辑
        }
        TerminalJeiStorageCache.setRestocking(true);
        // whenComplete：RPC 异常完成（超时/断连）时同样清除标志位，避免标志位永久泄漏
        // 导致本会话内后续所有 transferRecipe 提前 return、终端补库静默失效。
        // 断线时补库 RPC 可能永不完成（任务被登出流程丢弃），由
        // ClientEventListener.onClientPlayerDisconnect -> TerminalJeiStorageCache.clear() 复位。
        StorageTerminalClientStub.withdrawToInventory(storageIds, missing).whenComplete((changed, error) ->
            Minecraft.getInstance().execute(() -> {
                try {
                    // 无论补库成功与否都重试原传输逻辑：补库失败时背包可能已被部分补入
                    // （服务端在超时/断线前可能已执行部分取出），原逻辑会按实际背包状态转移；
                    // 完全未补入时原逻辑返回"缺少材料"错误提示，而不是静默丢弃本次合成。
                    this.transferRecipe(container, recipe, recipeSlotsView, player, maxTransfer, true);
                } finally {
                    TerminalJeiStorageCache.setRestocking(false);
                }
            })
        );
        cir.setReturnValue(null);
    }

    @Unique
    private static List<ItemStack> anvilcraft$collectMissing(
        AbstractContainerMenu container,
        IRecipeSlotsView recipeSlots,
        boolean maxTransfer
    ) {
        List<ItemStack> needs = new ArrayList<>();
        List<Integer> slotCounts = new ArrayList<>();
        for (IRecipeSlotView slotView : recipeSlots.getSlotViews(RecipeIngredientRole.INPUT)) {
            List<ItemStack> variants = slotView.getItemStacks().toList();
            if (variants.isEmpty()) {
                continue;
            }
            ItemStack representative = variants.getFirst().copy();
            if (representative.getCount() <= 0) {
                representative.setCount(1);
            }
            int idx = JeiBasicRecipeTransferHandlerMixin.anvilcraft$findNeed(needs, representative);
            if (idx < 0) {
                needs.add(representative.copy());
                slotCounts.add(1);
            } else {
                needs.get(idx).grow(representative.getCount());
                slotCounts.set(idx, slotCounts.get(idx) + 1);
            }
        }
        List<ItemStack> missing = new ArrayList<>();
        for (int i = 0; i < needs.size(); i++) {
            ItemStack need = needs.get(i);
            int required = maxTransfer
                           ? need.getMaxStackSize() * slotCounts.get(i)
                           : need.getCount();
            int have = JeiBasicRecipeTransferHandlerMixin.anvilcraft$countInContainer(container, need);
            int deficit = Math.max(0, required - have);
            if (deficit > 0) {
                ItemStack m = need.copy();
                m.setCount(deficit);
                missing.add(m);
            }
        }
        return missing;
    }

    @Unique
    private static int anvilcraft$findNeed(List<ItemStack> needs, ItemStack add) {
        for (int i = 0; i < needs.size(); i++) {
            if (ItemStack.isSameItemSameComponents(needs.get(i), add)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 检查阶段：判断背包 + 合成格 + 存储站是否满足配方所有输入需求。
     */
    @Unique
    private static boolean anvilcraft$containerSatisfies(
        AbstractContainerMenu container,
        List<ItemStack> storageItems,
        IRecipeSlotsView recipeSlots
    ) {
        List<ItemStack> needs = new ArrayList<>();
        for (IRecipeSlotView slotView : recipeSlots.getSlotViews(RecipeIngredientRole.INPUT)) {
            List<ItemStack> variants = slotView.getItemStacks().toList();
            if (variants.isEmpty()) {
                continue;
            }
            ItemStack representative = variants.getFirst().copy();
            if (representative.getCount() <= 0) {
                representative.setCount(1);
            }
            int idx = JeiBasicRecipeTransferHandlerMixin.anvilcraft$findNeed(needs, representative);
            if (idx < 0) {
                needs.add(representative.copy());
            } else {
                needs.get(idx).grow(representative.getCount());
            }
        }
        for (ItemStack need : needs) {
            int have = JeiBasicRecipeTransferHandlerMixin.anvilcraft$countInContainer(container, need);
            if (have >= need.getCount()) {
                continue; // 背包/合成格已满足
            }
            // 存储站缓存的数量被 clamp 到 maxStackSize(64)，不能用于判断 >64 的需求；
            // 存储站存在该物品即视为可补足（传输阶段会按实际缺口补库）。
            boolean inStorage = false;
            for (ItemStack storageItem : storageItems) {
                if (ItemStack.isSameItemSameComponents(storageItem, need)) {
                    inStorage = true;
                    break;
                }
            }
            if (!inStorage) {
                return false;
            }
        }
        return true;
    }

    @Unique
    private static int anvilcraft$countInContainer(AbstractContainerMenu container, ItemStack resource) {
        int count = 0;
        for (Slot slot : container.slots) {
            if (slot.isFake()) {
                continue;
            }
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, resource)) {
                count += stack.getCount();
            }
        }
        ItemStack carried = container.getCarried();
        if (!carried.isEmpty() && ItemStack.isSameItemSameComponents(carried, resource)) {
            count += carried.getCount();
        }
        return count;
    }
}
