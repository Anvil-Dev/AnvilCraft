package dev.dubhe.anvilcraft.integration.jei.transfer;

import dev.dubhe.anvilcraft.client.rpc.StorageTerminalClientStub;
import dev.dubhe.anvilcraft.client.rpc.TerminalJeiStorageCache;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IStackHelper;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 终端支持的 JEI 配方转移补全逻辑（与具体 Handler 解耦）：
 * <ul>
 *   <li>{@link #checkSatisfies}：检查阶段（doTransfer=false）——汇总玩家绑定终端的存储
 *       物品数量，结合背包已有量计算每个配方槽的实际缺口；无缺口返回 null（"+" 可用），
 *       有缺口返回精确的缺失错误（每个缺失槽只高亮一次）。</li>
 *   <li>{@link #restockThenTransfer}：传输阶段（doTransfer=true）——按配方各槽位实际上限
 *       计算背包缺口，先从存储站补足（异步），补库完成后通过 {@link IRecipeTransferCallable}
 *       重试原传输逻辑；重试后统计背包中本次补入物品的剩余量（JEI 转移失败或部分使用时
 *       剩余），把多余材料退回存储站，避免残留在玩家背包。</li>
 * </ul>
 * 匹配语义与 JEI 自身一致（{@code RecipeTransferUtil.calculateRequiredCountsByUid}）：
 * 配方输入槽是一组可替换的变体，槽内任意变体都满足该槽；同一物品用
 * {@link IStackHelper#getUidForStack} 的 uid 判定相等（含 subtype），tag 原料（如
 * {@code #minecraft:planks}）展开的所有变体即所有木板。
 * 不注入 {@code getInventoryState}：其声明的"虚拟空槽"无法让服务端实际取到物品。
 */
public final class TerminalJeiTransferSupport {
    private TerminalJeiTransferSupport() {
    }

    /**
     * 检查阶段：结合「背包 + 合成格」与「全部绑定终端存储」计算配方缺口。
     *
     * @param stackHelper JEI 的 uid 工具（判定配方变体与背包/存储物品是否同一物品）
     * @param helper JEI 传输错误工厂（用于构造精确缺失错误）
     * @return null 表示"+"可用（存储可补足，或缓存未就绪时的乐观判定——点击后由
     *         补库 + 重试给出真实结果）；否则返回缺失错误（只高亮真正缺的槽）
     */
    public static IRecipeTransferError checkSatisfies(
        AbstractContainerMenu container,
        IRecipeSlotsView recipeSlots,
        Player player,
        IStackHelper stackHelper,
        IRecipeTransferHandlerHelper helper
    ) {
        List<UUID> storageIds = TerminalJeiStorageCache.boundStorages(player);
        if (storageIds.isEmpty()) {
            return null;
        }
        List<ItemStack> merged = new ArrayList<>();
        for (UUID storageId : storageIds) {
            TerminalJeiStorageCache.ensure(storageId);
            List<ItemStack> storageItems = TerminalJeiStorageCache.get(storageId);
            if (storageItems == null) {
                // 缓存未就绪：不拦截走原方法（原方法用空背包判断会显示"全部缺失"），
                // 而是乐观放行（"+" 可用）；点击后由补库 + 重试按实际材料给出真实结果
                return null;
            }
            for (ItemStack item : storageItems) {
                TerminalJeiTransferSupport.mergeItem(merged, item);
            }
        }
        return TerminalJeiTransferSupport.findMissingError(container, merged, recipeSlots, stackHelper, helper);
    }

    /**
     * 传输阶段：补足背包缺口后重试原传输逻辑；重试后把本次补入但未使用的材料退回存储站。
     *
     * @param craftingSlots 配方槽（与 recipeSlots 的 INPUT 视图按索引对应）
     * @param inventorySlots JEI 转移使用的物品栏槽（补库/退回的统计范围）
     * @param stackHelper JEI 的 uid 工具（判定配方变体与背包/存储物品是否同一物品）
     * @param transfer 原 Handler 的传输方法（重试调用，内部应跳过本支持类的再次编排）
     * @return true 表示本次转移已被本方法拦截（补库进行中，稍后重试原逻辑），
     *         调用方应返回成功（{@code null}）并停止原方法；false 表示无需补库，
     *         调用方应继续执行原传输逻辑
     */
    public static boolean restockThenTransfer(
        AbstractContainerMenu container,
        List<Slot> craftingSlots,
        List<Slot> inventorySlots,
        IRecipeSlotsView recipeSlots,
        Player player,
        IStackHelper stackHelper,
        boolean maxTransfer,
        IRecipeTransferCallable transfer
    ) {
        List<UUID> storageIds = TerminalJeiStorageCache.boundStorages(player);
        if (storageIds.isEmpty()) {
            return false;
        }
        List<ItemStack> missing = TerminalJeiTransferSupport.collectMissing(
            container,
            craftingSlots,
            recipeSlots,
            stackHelper,
            maxTransfer
        );
        if (missing.isEmpty()) {
            return false; // 背包已足够，调用方不应拦截，走原传输逻辑
        }
        // 补库前记录物品栏中每种需求物品的现有量：重试后用它计算"本次补入但未使用"的剩余。
        // 需求物品用其 uid 做键（tag 变体与背包物品按同一 uid 归并），统计范围是 JEI 的物品栏槽。
        Map<Object, Integer> beforeWithdraw = new HashMap<>();
        for (ItemStack need : missing) {
            Object uid = stackHelper.getUidForStack(need, UidContext.Recipe);
            beforeWithdraw.merge(
                uid,
                TerminalJeiTransferSupport.countInSlots(inventorySlots, uid, stackHelper),
                Integer::sum
            );
        }
        TerminalJeiStorageCache.setRestocking(true);
        // whenComplete：RPC 异常完成（超时/断连）时，服务端可能已执行补库（物品已入背包）
        // 但响应失败，客户端无法得知实际补入量，此时不重试原逻辑、不退回，
        // 只清除标志位避免标志位永久泄漏导致后续 transferRecipe 提前 return。
        // 断线时补库 RPC 可能永不完成（任务被登出流程丢弃），由
        // ClientEventListener.onClientPlayerDisconnect -> TerminalJeiStorageCache.clear() 复位。
        StorageTerminalClientStub.withdrawToInventory(storageIds, missing).whenComplete((withdrawn, error) ->
            Minecraft.getInstance().execute(() -> {
                try {
                    if (error != null) {
                        return; // 补库调用本身失败：保持现状，走原逻辑的"缺少材料"提示
                    }
                    if (withdrawn == null || withdrawn.isEmpty()) {
                        // 没有实际补入（存储为空/不足）：重试原逻辑，让它给出真实的
                        // 转移结果（材料够则转移，不够则提示缺材料），而不是什么都不做
                        transfer.transfer();
                        return;
                    }
                    // 补库成功后重试原传输逻辑，把补入的物品填入合成格
                    IRecipeTransferError retryError = transfer.transfer();
                    if (retryError != null) {
                        // JEI 转移失败（如材料仍不足）：本次补入的物品未被使用，全部退回存储站
                        StorageTerminalClientStub.returnExcess(storageIds, withdrawn);
                    } else {
                        // JEI 转移成功（或部分成功）：本次补入但未使用的物品退回存储站
                        // （当前物品栏量 - 补库前物品栏量，正值表示 JEI 未用完）
                        List<ItemStack> excess = new ArrayList<>();
                        for (ItemStack w : withdrawn) {
                            Object uid = stackHelper.getUidForStack(w, UidContext.Recipe);
                            int remaining = TerminalJeiTransferSupport.countInSlots(inventorySlots, uid, stackHelper)
                                - beforeWithdraw.getOrDefault(uid, 0);
                            if (remaining > 0) {
                                ItemStack e = w.copy();
                                e.setCount(remaining);
                                excess.add(e);
                            }
                        }
                        if (!excess.isEmpty()) {
                            StorageTerminalClientStub.returnExcess(storageIds, excess);
                        }
                    }
                } finally {
                    TerminalJeiStorageCache.setRestocking(false);
                }
            })
        );
        return true;
    }

    /** 把 item 的数量累加进 merged（同一物品合并计数，防溢出）。 */
    private static void mergeItem(List<ItemStack> merged, ItemStack item) {
        for (ItemStack existing : merged) {
            if (ItemStack.isSameItemSameComponents(existing, item)) {
                int add = Math.min(item.getCount(), Integer.MAX_VALUE - existing.getCount());
                if (add > 0) {
                    existing.grow(add);
                }
                return;
            }
        }
        merged.add(item.copy());
    }

    /**
     * 计算缺口并构造精确缺失错误：每个配方输入槽独立判断，槽内任意变体
     * （用 uid 判定）在「背包 + 合成格 + 存储」中够数即视为满足；没有任何变体
     * 够数时把整个槽标记为缺失（每个缺失槽只高亮一次，不因变体数量叠加）。
     * 与 JEI 自身一致：{@code RecipeTransferUtil.getCandidateGroups} 对每个 uid
     * 检查「可得量 ≥ 该槽需求」，任一 uid 满足即槽不缺失。
     *
     * @return null 表示无缺口（"+" 可用）；否则为缺失错误
     */
    private static IRecipeTransferError findMissingError(
        AbstractContainerMenu container,
        List<ItemStack> storageItems,
        IRecipeSlotsView recipeSlots,
        IStackHelper stackHelper,
        IRecipeTransferHandlerHelper helper
    ) {
        // 该槽满足与否的统计：按 uid 合并「背包+合成格+存储」的可得量
        Map<Object, Integer> availableByUid = new HashMap<>();
        TerminalJeiTransferSupport.collectAvailable(container, storageItems, stackHelper, availableByUid);
        List<IRecipeSlotView> missingSlots = new ArrayList<>();
        for (IRecipeSlotView slotView : recipeSlots.getSlotViews(RecipeIngredientRole.INPUT)) {
            Map<Object, Integer> requiredByUid = TerminalJeiTransferSupport.requiredCountsByUid(slotView, stackHelper);
            if (requiredByUid.isEmpty()) {
                continue;
            }
            boolean satisfied = requiredByUid.entrySet().stream()
                .anyMatch(e -> availableByUid.getOrDefault(e.getKey(), 0) >= e.getValue());
            if (!satisfied) {
                missingSlots.add(slotView);
            }
        }
        if (missingSlots.isEmpty()) {
            return null;
        }
        return helper.createUserErrorForMissingSlots(
            Component.translatable("jei.tooltip.error.recipe.transfer.missing"),
            missingSlots
        );
    }

    /**
     * 计算缺口并构造补库需求：按 uid 汇总全部配方输入槽的需求总量，
     * 减去背包/合成格已有量，不足部分从存储补足（服务端
     * {@code withdrawNeedsFromStorages} 按物品精确匹配）。同一物品在多个槽
     * 出现时（如 AAB 中的 A），需求正确累加，不会因"某槽已够"漏算。
     * 补库数量按各槽的实际上限（{@link Slot#getMaxStackSize}）计算。
     * 存储里没有的物品照常计入需求：服务端取不到时 {@code withdrawn} 为空，
     * 传输阶段会重试原逻辑给出真实结果。
     */
    private static List<ItemStack> collectMissing(
        AbstractContainerMenu container,
        List<Slot> craftingSlots,
        IRecipeSlotsView recipeSlots,
        IStackHelper stackHelper,
        boolean maxTransfer
    ) {
        List<IRecipeSlotView> inputViews = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT);
        // 背包/合成格中每种 uid 的可得量
        Map<Object, Integer> containerByUid = new HashMap<>();
        TerminalJeiTransferSupport.collectAvailable(container, List.of(), stackHelper, containerByUid);
        // 按 uid 汇总全部输入槽的需求总量
        Map<Object, Integer> requiredByUid = new HashMap<>();
        for (int i = 0; i < inputViews.size(); i++) {
            IRecipeSlotView slotView = inputViews.get(i);
            Map<Object, Integer> slotRequired = TerminalJeiTransferSupport.requiredCountsByUid(slotView, stackHelper);
            if (slotRequired.isEmpty()) {
                continue;
            }
            Slot craftingSlot = i < craftingSlots.size() ? craftingSlots.get(i) : null;
            for (Map.Entry<Object, Integer> entry : slotRequired.entrySet()) {
                Object uid = entry.getKey();
                int perSlot = maxTransfer
                    ? (craftingSlot == null
                        ? TerminalJeiTransferSupport.variantsOf(slotView).getFirst().getMaxStackSize()
                        : craftingSlot.getMaxStackSize(TerminalJeiTransferSupport.representativeOf(slotView, uid, stackHelper)))
                    : entry.getValue();
                requiredByUid.merge(uid, perSlot, Integer::sum);
            }
        }
        List<ItemStack> missing = new ArrayList<>();
        for (Map.Entry<Object, Integer> entry : requiredByUid.entrySet()) {
            Object uid = entry.getKey();
            int required = entry.getValue();
            int have = containerByUid.getOrDefault(uid, 0);
            int deficit = Math.max(0, required - have);
            if (deficit <= 0) {
                continue;
            }
            // 选一个代表物品：取槽内首个该 uid 变体（同 uid 变体在背包/存储层面是同一物品），
            // 存储按它精确匹配补库
            ItemStack representative = TerminalJeiTransferSupport.bestRepresentativeOf(uid, recipeSlots, stackHelper);
            if (representative.isEmpty()) {
                continue;
            }
            ItemStack m = representative.copy();
            m.setCount(deficit);
            missing.add(m);
        }
        return missing;
    }

    /**
     * 槽内 uid 对应的代表物品：同 uid 的变体在背包/存储层面是同一物品
     * （uid 已含 subtype），取槽内首个该 uid 变体即可；存储按它精确匹配补库。
     */
    private static ItemStack bestRepresentativeOf(
        Object uid,
        IRecipeSlotsView recipeSlots,
        IStackHelper stackHelper
    ) {
        for (IRecipeSlotView slotView : recipeSlots.getSlotViews(RecipeIngredientRole.INPUT)) {
            for (ItemStack variant : TerminalJeiTransferSupport.variantsOf(slotView)) {
                if (stackHelper.getUidForStack(variant, UidContext.Recipe).equals(uid)) {
                    return variant.copy();
                }
            }
        }
        return ItemStack.EMPTY;
    }

    /** 该槽各变体 uid 的需求量：槽内同一 uid 取最大数量（与 JEI {@code calculateRequiredCountsByUid} 一致）。 */
    private static Map<Object, Integer> requiredCountsByUid(IRecipeSlotView slotView, IStackHelper stackHelper) {
        Map<Object, Integer> required = new HashMap<>();
        for (ItemStack variant : TerminalJeiTransferSupport.variantsOf(slotView)) {
            Object uid = stackHelper.getUidForStack(variant, UidContext.Recipe);
            int count = Math.max(1, variant.getCount());
            required.merge(uid, count, Math::max);
        }
        return required;
    }

    /** 槽内 uid 对应的首个变体（作为补库需求的代表物品，存储按它精确匹配）。 */
    private static ItemStack representativeOf(IRecipeSlotView slotView, Object uid, IStackHelper stackHelper) {
        for (ItemStack variant : TerminalJeiTransferSupport.variantsOf(slotView)) {
            if (stackHelper.getUidForStack(variant, UidContext.Recipe).equals(uid)) {
                return variant.copy();
            }
        }
        return ItemStack.EMPTY;
    }

    /** 汇总「背包 + 合成格 + 存储」中每种 uid 的总量。 */
    private static void collectAvailable(
        AbstractContainerMenu container,
        List<ItemStack> storageItems,
        IStackHelper stackHelper,
        Map<Object, Integer> availableByUid
    ) {
        for (Slot slot : container.slots) {
            if (slot.isFake()) {
                continue;
            }
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty()) {
                Object uid = stackHelper.getUidForStack(stack, UidContext.Recipe);
                availableByUid.merge(uid, stack.getCount(), Integer::sum);
            }
        }
        ItemStack carried = container.getCarried();
        if (!carried.isEmpty()) {
            Object uid = stackHelper.getUidForStack(carried, UidContext.Recipe);
            availableByUid.merge(uid, carried.getCount(), Integer::sum);
        }
        for (ItemStack storageItem : storageItems) {
            Object uid = stackHelper.getUidForStack(storageItem, UidContext.Recipe);
            availableByUid.merge(uid, storageItem.getCount(), Integer::sum);
        }
    }

    /** 槽内全部变体（tag 原料展开的所有成员）。 */
    private static List<ItemStack> variantsOf(IRecipeSlotView slotView) {
        List<ItemStack> variants = new ArrayList<>();
        for (ITypedIngredient<?> typedIngredient : slotView.getAllIngredientsList()) {
            if (typedIngredient == null) {
                continue;
            }
            ITypedIngredient<ItemStack> typedItemStack = typedIngredient.castToItemStackType();
            if (typedItemStack != null) {
                variants.add(typedItemStack.getIngredient());
            }
        }
        return variants;
    }

    /** 指定槽列表中某 uid 的总量（tag 变体与背包物品按同一 uid 归并）。 */
    private static int countInSlots(List<Slot> slots, Object uid, IStackHelper stackHelper) {
        int count = 0;
        for (Slot slot : slots) {
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty() && stackHelper.getUidForStack(stack, UidContext.Recipe).equals(uid)) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
