package dev.dubhe.anvilcraft.integration.jei.transfer;

import dev.dubhe.anvilcraft.client.rpc.StorageTerminalClientStub;
import dev.dubhe.anvilcraft.client.rpc.TerminalJeiStorageCache;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
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
 *       物品数量，结合背包已有量计算每种配方的实际缺口；无缺口返回 null（"+" 可用），
 *       有缺口返回精确的缺失错误（只高亮真正缺的槽，如 AAB 中缺 1 个 A 只高亮一个 A 槽）。</li>
 *   <li>{@link #restockThenTransfer}：传输阶段（doTransfer=true）——按配方各槽位实际上限
 *       计算背包缺口，先从存储站补足（异步），补库完成后通过 {@link IRecipeTransferCallable}
 *       重试原传输逻辑；重试后统计背包中本次补入物品的剩余量（JEI 转移失败或部分使用时
 *       剩余），把多余材料退回存储站，避免残留在玩家背包。</li>
 * </ul>
 * 不注入 {@code getInventoryState}：其声明的"虚拟空槽"无法让服务端实际取到物品。
 */
public final class TerminalJeiTransferSupport {
    private TerminalJeiTransferSupport() {
    }

    /**
     * 检查阶段：结合「背包 + 合成格」与「全部绑定终端存储」计算配方缺口。
     *
     * @param helper JEI 传输错误工厂（用于构造精确缺失错误）
     * @return null 表示"+"可用（存储可补足，或缓存未就绪时的乐观判定——点击后由
     *         补库 + 重试给出真实结果）；否则返回缺失错误（只高亮真正缺的槽）
     */
    public static IRecipeTransferError checkSatisfies(
        AbstractContainerMenu container,
        IRecipeSlotsView recipeSlots,
        Player player,
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
        return TerminalJeiTransferSupport.findMissingError(container, merged, recipeSlots, helper);
    }

    /**
     * 传输阶段：补足背包缺口后重试原传输逻辑；重试后把本次补入但未使用的材料退回存储站。
     *
     * @param craftingSlots 配方槽（与 recipeSlots 的 INPUT 视图按索引对应）
     * @param inventorySlots JEI 转移使用的物品栏槽（补库/退回的统计范围）
     * @param transfer 原 Handler 的传输方法（重试调用，内部应跳过本支持类的再次编排）
     */
    public static void restockThenTransfer(
        AbstractContainerMenu container,
        List<Slot> craftingSlots,
        List<Slot> inventorySlots,
        IRecipeSlotsView recipeSlots,
        Player player,
        boolean maxTransfer,
        IRecipeTransferCallable transfer
    ) {
        List<UUID> storageIds = TerminalJeiStorageCache.boundStorages(player);
        if (storageIds.isEmpty()) {
            return;
        }
        List<ItemStack> missing = TerminalJeiTransferSupport.collectMissing(
            container,
            craftingSlots,
            recipeSlots,
            maxTransfer
        );
        if (missing.isEmpty()) {
            return; // 背包已足够，走原传输逻辑
        }
        // 补库前记录物品栏中每种需求物品的现有量：重试后用它计算"本次补入但未使用"的剩余
        Map<ItemStack, Integer> beforeWithdraw = new HashMap<>();
        for (ItemStack need : missing) {
            beforeWithdraw.put(need, TerminalJeiTransferSupport.countInSlots(inventorySlots, need));
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
                        return; // 没有实际补入（存储为空/不足），原逻辑会正确提示缺材料
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
                            int remaining = TerminalJeiTransferSupport.countInSlots(inventorySlots, w)
                                - beforeWithdraw.getOrDefault(w, 0);
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
     * 计算缺口并构造精确缺失错误：对每种配方物品，缺口 = 需求 - 背包已有 - 存储总量；
     * 缺口为正时把对应数量的配方输入槽标记为缺失。
     * 标记靠后的槽：JEI 转移时按槽顺序优先填充靠前的槽（服务端
     * {@code removeOneSetOfItemsFromInventory} 遍历需求映射），因此材料不足时
     * 靠后的槽先变空，缺失高亮与实际空槽一致。
     *
     * @return null 表示无缺口（"+" 可用）；否则为缺失错误
     */
    private static IRecipeTransferError findMissingError(
        AbstractContainerMenu container,
        List<ItemStack> storageItems,
        IRecipeSlotsView recipeSlots,
        IRecipeTransferHandlerHelper helper
    ) {
        List<ItemStack> needs = new ArrayList<>();
        List<List<IRecipeSlotView>> needSlotViews = new ArrayList<>();
        TerminalJeiTransferSupport.aggregateNeeds(recipeSlots, needs, needSlotViews);
        List<IRecipeSlotView> missingSlots = new ArrayList<>();
        for (int i = 0; i < needs.size(); i++) {
            ItemStack need = needs.get(i);
            int have = TerminalJeiTransferSupport.countInContainer(container, need);
            int inStorage = TerminalJeiTransferSupport.countInStorage(storageItems, need);
            int shortfall = need.getCount() - have - inStorage;
            if (shortfall <= 0) {
                continue;
            }
            // 标记靠后的 shortfall 个该物品的输入槽为缺失（与 JEI 实际空槽一致）
            List<IRecipeSlotView> slotViews = needSlotViews.get(i);
            int from = Math.max(0, slotViews.size() - shortfall);
            for (int s = from; s < slotViews.size(); s++) {
                missingSlots.add(slotViews.get(s));
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

    private static List<ItemStack> collectMissing(
        AbstractContainerMenu container,
        List<Slot> craftingSlots,
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
            int idx = TerminalJeiTransferSupport.findNeed(needs, representative);
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
            int required = TerminalJeiTransferSupport.requiredAmount(
                need,
                slotCounts.get(i),
                craftingSlots,
                recipeSlots,
                maxTransfer
            );
            int have = TerminalJeiTransferSupport.countInContainer(container, need);
            int deficit = Math.max(0, required - have);
            if (deficit > 0) {
                ItemStack m = need.copy();
                m.setCount(deficit);
                missing.add(m);
            }
        }
        return missing;
    }

    /**
     * 计算某种配方物品在本次转移中的需求总量。
     * <ul>
     *   <li>非 maxTransfer（单击）：仅需求一组配方（该物品在所有输入槽中出现的数量）。</li>
     *   <li>maxTransfer（Shift 单击）：JEI 会把每个配方槽填到槽位上限
     *       （{@link Slot#getMaxStackSize}，可能小于物品本身上限），因此需求为各配方槽
     *       实际上限之和，而不是物品 maxStackSize × 槽数——后者会在槽位上限较小时
     *       多补材料到背包。</li>
     * </ul>
     */
    private static int requiredAmount(
        ItemStack need,
        int slotCount,
        List<Slot> craftingSlots,
        IRecipeSlotsView recipeSlots,
        boolean maxTransfer
    ) {
        if (!maxTransfer) {
            return need.getCount();
        }
        int required = 0;
        List<IRecipeSlotView> inputViews = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT);
        for (int i = 0; i < inputViews.size() && slotCount > 0; i++) {
            IRecipeSlotView slotView = inputViews.get(i);
            if (slotView.getItemStacks().noneMatch(variant -> ItemStack.isSameItemSameComponents(variant, need))) {
                continue;
            }
            slotCount--;
            Slot slot = i < craftingSlots.size() ? craftingSlots.get(i) : null;
            required += slot == null ? need.getMaxStackSize() : slot.getMaxStackSize(need);
        }
        return required;
    }

    /** 聚合配方输入视图：needs（每种物品需求总量）+ needSlotViews（每种物品对应的输入槽视图）。 */
    private static void aggregateNeeds(
        IRecipeSlotsView recipeSlots,
        List<ItemStack> needs,
        List<List<IRecipeSlotView>> needSlotViews
    ) {
        for (IRecipeSlotView slotView : recipeSlots.getSlotViews(RecipeIngredientRole.INPUT)) {
            List<ItemStack> variants = slotView.getItemStacks().toList();
            if (variants.isEmpty()) {
                continue;
            }
            ItemStack representative = variants.getFirst().copy();
            if (representative.getCount() <= 0) {
                representative.setCount(1);
            }
            int idx = TerminalJeiTransferSupport.findNeed(needs, representative);
            if (idx < 0) {
                needs.add(representative.copy());
                List<IRecipeSlotView> views = new ArrayList<>();
                views.add(slotView);
                needSlotViews.add(views);
            } else {
                needs.get(idx).grow(representative.getCount());
                needSlotViews.get(idx).add(slotView);
            }
        }
    }

    private static int findNeed(List<ItemStack> needs, ItemStack add) {
        for (int i = 0; i < needs.size(); i++) {
            if (ItemStack.isSameItemSameComponents(needs.get(i), add)) {
                return i;
            }
        }
        return -1;
    }

    private static int countInStorage(List<ItemStack> storageItems, ItemStack need) {
        int count = 0;
        for (ItemStack storageItem : storageItems) {
            if (ItemStack.isSameItemSameComponents(storageItem, need)) {
                count += storageItem.getCount();
                if (count >= need.getCount()) {
                    break;
                }
            }
        }
        return count;
    }

    /** 统计指定槽列表（JEI 的物品栏槽）中该物品的总量。 */
    private static int countInSlots(List<Slot> slots, ItemStack resource) {
        int count = 0;
        for (Slot slot : slots) {
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, resource)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static int countInContainer(AbstractContainerMenu container, ItemStack resource) {
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
