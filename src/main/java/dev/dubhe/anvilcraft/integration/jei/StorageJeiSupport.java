package dev.dubhe.anvilcraft.integration.jei;

import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.screen.StorageMenu;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.client.rpc.StorageClientStub;
import dev.dubhe.anvilcraft.integration.StorageJeiBridge;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IStackHelper;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.runtime.IRecipesGui;
import mezz.jei.common.transfer.RecipeTransferOperationsResult;
import mezz.jei.common.transfer.RecipeTransferUtil;
import mezz.jei.common.transfer.TransferOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.StonecutterRecipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;

/**
 * 仓储界面（{@link StorageScreen}）的 JEI 支持（仅在 JEI 安装时由
 * {@link AnvilCraftJeiPlugin} 加载，StorageScreen 通过 {@link StorageJeiBridge}
 * 反射调用，未装 JEI 时安全降级）：
 * <ul>
 *   <li>{@link #openStonecutterRecipes} / {@link #openCraftingRecipes}：点击③/④ 结果槽
 *       区域的 JEI 打开区时显示对应的配方类别。</li>
 *   <li>{@link #registerRecipeTransferHandlers}：为仓储菜单（{@link StorageMenu}，
 *       即 StorageScreen 打开期间的 JEI 父容器）注册切石机/合成的转移 handler；
 *       把配方输入放入 ①/② 输入槽（材料从背包扣取）。</li>
 * </ul>
 */
public final class StorageJeiSupport {
    private static @Nullable IJeiRuntime runtime;

    private StorageJeiSupport() {
    }

    /** JEI 运行时可用回调：保存运行时供打开配方界面使用。 */
    public static void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        StorageJeiSupport.runtime = jeiRuntime;
    }

    /** JEI 是否可用（已安装且运行时已就绪）。 */
    public static boolean isAvailable() {
        return StorageJeiSupport.runtime != null;
    }

    /** 打开切石机配方 JEI 界面。 */
    public static void openStonecutterRecipes() {
        StorageJeiSupport.showTypes(RecipeTypes.STONECUTTING);
    }

    /** 打开合成配方 JEI 界面。 */
    public static void openCraftingRecipes() {
        StorageJeiSupport.showTypes(RecipeTypes.CRAFTING);
    }

    private static void showTypes(RecipeType<?> recipeType) {
        IRecipesGui recipesGui = StorageJeiSupport.runtime == null ? null : StorageJeiSupport.runtime.getRecipesGui();
        if (recipesGui != null) {
            recipesGui.showTypes(List.of(recipeType));
        }
    }

    /** 注册 StorageScreen 场景的配方转移 handler（在 JeiPlugin 的 registerRecipeTransferHandlers 中调用）。 */
    public static void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(
            new StorageScreenStonecutterTransferHandler(registration.getTransferHelper()),
            RecipeTypes.STONECUTTING
        );
        registration.addRecipeTransferHandler(
            new StorageScreenCraftingTransferHandler(registration.getTransferHelper()),
            RecipeTypes.CRAFTING
        );
    }

    /**
     * 当前是否处于仓储界面的 JEI 转移场景。
     * JEI 配方界面打开期间 {@code Minecraft.getInstance().screen} 是 JEI 的 RecipesGui，
     * 原仓储屏幕保存在 {@link IRecipesGui#getParentScreen()}；书签等直接在仓储屏幕
     * 上触发的转移则 {@code screen} 就是 StorageScreen。
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean isStorageScreen() {
        return StorageJeiSupport.parentScreen() instanceof StorageScreen
            || Minecraft.getInstance().screen instanceof StorageScreen;
    }

    /** JEI 配方界面的父屏幕（未打开配方界面时可能为 null）。 */
    private static @Nullable Screen parentScreen() {
        IRecipesGui recipesGui = StorageJeiSupport.runtime == null ? null : StorageJeiSupport.runtime.getRecipesGui();
        return recipesGui == null ? null : recipesGui.getParentScreen().orElse(null);
    }

    /**
     * 配方输入槽的物品列表（与槽一一对应，空槽为 EMPTY）。
     * 每个槽在所有 ItemStack 变体中挑选：优先选背包/存储中已有且**余量充足**
     * 的变体（该变体总可用数量 > 已分配给前面槽的数量），跨槽贪心分配——
     * 同种物品足够多时各槽都选同种（可堆叠），同种不足时自动换下一个变体
     * （标签配方如 AAA 且 A 为标签时，存储中标签内不同物品各 1 个也能凑齐）。
     * 所有变体都无余量时退回当前显示的变体。
     */
    private static List<ItemStack> collectInputs(IRecipeSlotsView recipeSlots, Player player) {
        StorageScreen screen = StorageJeiSupport.storageScreen();
        List<ItemStack> inputs = new ArrayList<>();
        List<ItemStack> allocatedKeys = new ArrayList<>();
        List<Integer> allocatedCounts = new ArrayList<>();
        for (IRecipeSlotView slotView : recipeSlots.getSlotViews(RecipeIngredientRole.INPUT)) {
            List<ItemStack> variants = slotView.getItemStacks().toList();
            if (variants.isEmpty()) {
                inputs.add(ItemStack.EMPTY);
                continue;
            }
            ItemStack chosen = null;
            for (ItemStack variant : variants) {
                long available = StorageJeiSupport.availableCount(player, screen, variant);
                int used = StorageJeiSupport.allocatedCount(allocatedKeys, allocatedCounts, variant);
                if (available > used) {
                    chosen = variant;
                    break;
                }
            }
            if (chosen == null) {
                chosen = slotView.getDisplayedItemStack()
                    .orElse(variants.getFirst());
            }
            StorageJeiSupport.allocate(allocatedKeys, allocatedCounts, chosen);
            inputs.add(chosen.copy());
        }
        return inputs;
    }

    /**
     * 检查阶段：校验材料是否可得且数量充足——玩家背包或当前打开的存储站
     * （客户端缓存）中每个输入槽可分配到至少 1 个物品（按变体余量跨槽分配，
     * 与 {@link #collectInputs} 的贪心一致：标签配方多槽可凑不同变体）。
     *
     * @return 缺失错误（高亮缺失槽），材料齐全返回 null
     */
    private static @Nullable IRecipeTransferError checkMissingInputs(
        IRecipeTransferHandlerHelper helper,
        IRecipeSlotsView recipeSlots,
        Player player
    ) {
        StorageScreen screen = StorageJeiSupport.storageScreen();
        List<IRecipeSlotView> inputViews = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT);
        List<IRecipeSlotView> missing = new ArrayList<>();
        List<ItemStack> allocatedKeys = new ArrayList<>();
        List<Integer> allocatedCounts = new ArrayList<>();
        for (IRecipeSlotView slotView : inputViews) {
            List<ItemStack> variants = slotView.getItemStacks().toList();
            if (variants.isEmpty()) {
                continue;
            }
            boolean hasAny = false;
            for (ItemStack variant : variants) {
                long available = StorageJeiSupport.availableCount(player, screen, variant);
                int used = StorageJeiSupport.allocatedCount(allocatedKeys, allocatedCounts, variant);
                if (available > used) {
                    StorageJeiSupport.allocate(allocatedKeys, allocatedCounts, variant);
                    hasAny = true;
                    break;
                }
            }
            if (!hasAny) {
                missing.add(slotView);
            }
        }
        if (missing.isEmpty()) {
            return null;
        }
        Component message = Component.translatable("jei.tooltip.error.recipe.transfer.missing");
        return helper.createUserErrorForMissingSlots(message, missing);
    }

    /** 玩家背包 + 当前存储站（客户端缓存）中与 target 同种同组件物品的总数量。 */
    private static long availableCount(Player player, @Nullable StorageScreen screen, ItemStack target) {
        if (target.isEmpty()) {
            return Long.MAX_VALUE;
        }
        long count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (ItemStack.isSameItemSameComponents(stack, target)) {
                count += stack.getCount();
            }
        }
        if (screen != null) {
            for (UnlimitedItemStack stack : screen.getContents().values()) {
                if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack.toStack(), target)) {
                    count += stack.getCount();
                }
            }
        }
        return count;
    }

    /**
     * 已分配给前面槽位的同种（同物品同组件）物品数量。
     * ItemStack 未重写 equals（引用相等），不能用 Map 做内容 key，改用并行列表 + 内容比较。
     */
    private static int allocatedCount(List<ItemStack> keys, List<Integer> counts, ItemStack target) {
        for (int i = 0; i < keys.size(); i++) {
            if (ItemStack.isSameItemSameComponents(keys.get(i), target)) {
                return counts.get(i);
            }
        }
        return 0;
    }

    /** 记录一个槽位分配了该物品（同种合并计数）。 */
    private static void allocate(List<ItemStack> keys, List<Integer> counts, ItemStack chosen) {
        for (int i = 0; i < keys.size(); i++) {
            if (ItemStack.isSameItemSameComponents(keys.get(i), chosen)) {
                counts.set(i, counts.get(i) + 1);
                return;
            }
        }
        keys.add(chosen.copy());
        counts.add(1);
    }

    /** 当前场景的仓储屏幕（JEI 打开时为父屏幕，否则为当前屏幕）。 */
    private static @Nullable StorageScreen storageScreen() {
        if (StorageJeiSupport.parentScreen() instanceof StorageScreen screen) {
            return screen;
        }
        return Minecraft.getInstance().screen instanceof StorageScreen screen ? screen : null;
    }

    /**
     * 用 JEI 自身的分配算法（{@link RecipeTransferUtil#getRecipeTransferOperations}）计算
     * 每个合成槽应放入的物品份数。可用物品包括：玩家背包各槽 + 当前仓储屏幕缓存中的
     * 存储内容（每逻辑槽映射为一个虚拟 Slot）。
     *
     * @return 长度 9 的份数列表（按②合成格顺序）；材料不足时返回 null
     */
    private static @Nullable IntList computeRequestedCounts(
        StorageMenu container,
        IRecipeSlotsView recipeSlots,
        Player player
    ) {
        StorageScreen screen = StorageJeiSupport.storageScreen();
        List<Slot> craftingSlots = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            craftingSlots.add(new VirtualSlot(2 + i, ItemStack.EMPTY));
        }
        Map<Slot, ItemStack> availableItemStacks = new HashMap<>();
        // StorageMenu 槽 9~44 与玩家物品栏 9~35（主物品栏）及 0~8（快捷栏）一一对应；
        // 槽 0~8 是隐藏假槽/盔甲槽，不可作为合成材料来源。
        for (int menuSlot = 9; menuSlot <= 44; menuSlot++) {
            Slot slot = container.getSlot(menuSlot);
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty()) {
                availableItemStacks.put(slot, stack.copy());
            }
        }
        if (screen != null) {
            for (UnlimitedItemStack stack : screen.getContents().values()) {
                if (stack.isEmpty()) {
                    continue;
                }
                ItemStack item = stack.toStack();
                if (item.isEmpty()) {
                    continue;
                }
                // 存储格数量可超过 ItemStack 上限（64）：按 maxStackSize 拆成多个虚拟槽，
                // 避免 JEI 分配出超过合成格上限的份数，同时正确反映可填满多轮的总量。
                int maxStack = item.getMaxStackSize();
                int remaining = stack.getCount();
                while (remaining > 0) {
                    int chunk = Math.min(remaining, maxStack);
                    ItemStack virtual = item.copyWithCount(chunk);
                    availableItemStacks.put(new VirtualSlot(1000 + availableItemStacks.size(), virtual), virtual.copy());
                    remaining -= chunk;
                }
            }
        }
        IStackHelper stackHelper = StorageJeiSupport.runtime == null
            ? null
            : StorageJeiSupport.runtime.getJeiHelpers().getStackHelper();
        if (stackHelper == null) {
            return null;
        }
        List<IRecipeSlotView> inputViews = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT);
        RecipeTransferOperationsResult operations;
        try {
            operations = RecipeTransferUtil.getRecipeTransferOperations(
                stackHelper,
                availableItemStacks,
                inputViews,
                craftingSlots
            );
        } catch (RuntimeException e) {
            return null;
        }

        if (!operations.missingItems.isEmpty()) {
            return null;
        }
        int[] counts = new int[9];
        for (TransferOperation operation : operations.results) {
            int craftingSlotId = operation.craftingSlotId();
            int index = craftingSlotId - 2;
            if (index >= 0 && index < 9) {
                counts[index] += operation.count();
            }
        }
        IntList result = new IntArrayList(9);
        for (int count : counts) {
            result.add(count);
        }

        return result;
    }

    /** 把配方输入放入终端输入槽（异步 RPC，成功后刷新合成面板）。
     *  {@code stonecutterResult}：切石机场景为 JEI 当前配方产物（用于选中配方），合成场景为 EMPTY。 */
    private static void transferIntoStorage(
        StorageMenu container,
        boolean stonecutter,
        boolean maxTransfer,
        List<ItemStack> inputs,
        ItemStack stonecutterResult,
        IntList requestedCounts
    ) {
        StorageClientStub.craftingTransfer(
            container.getSourcePos(),
            stonecutter,
            maxTransfer,
            inputs,
            stonecutterResult,
            requestedCounts
        )
            .whenCompleteAsync(
            (changed, error) -> {
                if (error != null) {
                    AnvilCraft.LOGGER.error("Storage JEI transfer failed", error);
                    return;
                }

                if (changed != null && changed) {
                    // 优先通过 JEI 父屏幕刷新；书签等直接在仓储屏幕上触发的转移用当前屏幕
                    if (StorageJeiSupport.parentScreen() instanceof StorageScreen current) {
                        current.loadCrafting(stonecutter);
                    } else if (Minecraft.getInstance().screen instanceof StorageScreen current) {
                        current.loadCrafting(stonecutter);
                    }
                }
            },
            Minecraft.getInstance()
        );
    }

    /** 切石机配方转移：把第一个输入放入①（StorageScreen 打开时）。 */
    private static final class StorageScreenStonecutterTransferHandler
        implements IRecipeTransferHandler<StorageMenu, RecipeHolder<StonecutterRecipe>> {
        private final IRecipeTransferHandlerHelper helper;

        private StorageScreenStonecutterTransferHandler(IRecipeTransferHandlerHelper helper) {
            this.helper = helper;
        }

        @Override
        public Class<? extends StorageMenu> getContainerClass() {
            return StorageMenu.class;
        }

        @Override
        public Optional<MenuType<StorageMenu>> getMenuType() {
            return Optional.empty();
        }

        @Override
        public RecipeType<RecipeHolder<StonecutterRecipe>> getRecipeType() {
            return RecipeTypes.STONECUTTING;
        }

        @Override
        public @Nullable IRecipeTransferError transferRecipe(
            StorageMenu container,
            RecipeHolder<StonecutterRecipe> recipe,
            IRecipeSlotsView recipeSlots,
            Player player,
            boolean maxTransfer,
            boolean doTransfer
        ) {
            if (!StorageJeiSupport.isStorageScreen()) {
                // 非仓储界面：切石机配方无处转移，与 JEI 默认一致（按钮不可见）
                return this.helper.createInternalError();
            }
            if (doTransfer) {
                List<ItemStack> inputs = StorageJeiSupport.collectInputs(recipeSlots, player);
                ItemStack result = recipe.value().getResultItem(player.level().registryAccess());
                // ① 单槽转移：份数由服务端按上限自行计算，客户端无需提供
                StorageJeiSupport.transferIntoStorage(container, true, maxTransfer, inputs, result, new IntArrayList(0));
            } else {
                // 检查阶段：背包缺材料时高亮缺失槽并禁用转移按钮
                return StorageJeiSupport.checkMissingInputs(this.helper, recipeSlots, player);
            }
            return null;
        }
    }

    /** 合成配方转移：把 9 宫格输入放入②（StorageScreen 打开时）。 */
    private static final class StorageScreenCraftingTransferHandler
        implements IRecipeTransferHandler<StorageMenu, RecipeHolder<CraftingRecipe>> {
        private final IRecipeTransferHandlerHelper helper;

        private StorageScreenCraftingTransferHandler(IRecipeTransferHandlerHelper helper) {
            this.helper = helper;
        }

        @Override
        public Class<? extends StorageMenu> getContainerClass() {
            return StorageMenu.class;
        }

        @Override
        public Optional<MenuType<StorageMenu>> getMenuType() {
            return Optional.empty();
        }

        @Override
        public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
            return RecipeTypes.CRAFTING;
        }

        @Override
        public @Nullable IRecipeTransferError transferRecipe(
            StorageMenu container,
            RecipeHolder<CraftingRecipe> recipe,
            IRecipeSlotsView recipeSlots,
            Player player,
            boolean maxTransfer,
            boolean doTransfer
        ) {
            if (!StorageJeiSupport.isStorageScreen()) {
                // StorageMenu 仅在仓储界面作为父容器，防御性兜底
                return this.helper.createInternalError();
            }
            if (doTransfer) {
                List<ItemStack> inputs = StorageJeiSupport.collectInputs(recipeSlots, player);
                IntList requestedCounts = StorageJeiSupport.computeRequestedCounts(container, recipeSlots, player);
                if (requestedCounts == null) {
                    // 材料不足：与 JEI 默认一致，高亮缺失槽并禁用转移按钮
                    return this.helper.createUserErrorForMissingSlots(
                        Component.translatable("jei.tooltip.error.recipe.transfer.missing"),
                        recipeSlots.getSlotViews(RecipeIngredientRole.INPUT)
                    );
                }
                StorageJeiSupport.transferIntoStorage(container, false, maxTransfer, inputs, ItemStack.EMPTY, requestedCounts);
            } else {
                // 检查阶段：背包缺材料时高亮缺失槽并禁用转移按钮
                return StorageJeiSupport.checkMissingInputs(this.helper, recipeSlots, player);
            }
            return null;
        }
    }

    /** 仅供 JEI 分配算法使用的虚拟槽：只提供 index 与 getItem()，不绑定任何容器。 */
    private static final class VirtualSlot extends Slot {
        private final ItemStack stack;

        private VirtualSlot(int index, ItemStack stack) {
            super(null, index, 0, 0);
            this.index = index;
            this.stack = stack;
        }

        @Override
        public ItemStack getItem() {
            return this.stack;
        }
    }
}
