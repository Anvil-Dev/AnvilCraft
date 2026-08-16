package dev.dubhe.anvilcraft.integration.jei.handlers;

import dev.dubhe.anvilcraft.client.rpc.StorageTerminalClientStub;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.property.component.TerminalBinding;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * 携带已绑定终端时，JEI "+" 快速合成会把终端连接的存储站视为可用库存：
 * 点击传输时先从存储站取出背包缺少的物品（异步），补库完成后在主线程再次执行
 * 默认传输逻辑填入合成格。检查阶段不取物，避免打开界面就消耗存储站物品。
 */
public class TerminalRecipeTransferHandler<R extends Recipe<?>>
    implements IRecipeTransferHandler<AbstractContainerMenu, RecipeHolder<R>> {
    private final Class<? extends AbstractContainerMenu> containerClass;
    private final RecipeType<RecipeHolder<R>> recipeType;
    private final IRecipeTransferHandler<AbstractContainerMenu, RecipeHolder<R>> delegate;

    public TerminalRecipeTransferHandler(
        Class<? extends AbstractContainerMenu> containerClass,
        RecipeType<RecipeHolder<R>> recipeType,
        IRecipeTransferHandler<AbstractContainerMenu, RecipeHolder<R>> delegate
    ) {
        this.containerClass = containerClass;
        this.recipeType = recipeType;
        this.delegate = delegate;
    }

    @Override
    public Class<? extends AbstractContainerMenu> getContainerClass() {
        return this.containerClass;
    }

    @Override
    public Optional<MenuType<AbstractContainerMenu>> getMenuType() {
        return Optional.empty();
    }

    @Override
    public RecipeType<RecipeHolder<R>> getRecipeType() {
        return this.recipeType;
    }

    @Override
    public @Nullable IRecipeTransferError transferRecipe(
        AbstractContainerMenu container,
        RecipeHolder<R> recipe,
        IRecipeSlotsView recipeSlots,
        Player player,
        boolean maxTransfer,
        boolean doTransfer
    ) {
        UUID storageId = TerminalRecipeTransferHandler.boundTerminalStorage(player);
        if (storageId == null || !player.level().isClientSide()) {
            // 未携带终端或非客户端：走 JEI 默认传输逻辑
            return this.delegate.transferRecipe(container, recipe, recipeSlots, player, maxTransfer, doTransfer);
        }
        if (!doTransfer) {
            // 检查阶段：存储站物品视为可用，直接返回成功让 "+" 可用；不实际取物
            return null;
        }
        // 传输阶段：计算背包与容器（含合成格）缺少的物品（Shift 时按多组需求量）。
        // 合成格中已有的物品会在默认传输时被清出放回背包，故应计入“已有”避免重复补库。
        List<ItemStack> missing = TerminalRecipeTransferHandler.collectMissing(
            container,
            player,
            recipeSlots,
            maxTransfer
        );
        if (missing.isEmpty()) {
            // 背包已足够：直接走默认传输
            return this.delegate.transferRecipe(container, recipe, recipeSlots, player, maxTransfer, true);
        }
        // 异步补库，完成后在主线程再次执行默认传输填充合成格
        StorageTerminalClientStub.withdrawToInventory(storageId, missing).thenAccept(changed -> Minecraft.getInstance().execute(() ->
            this.delegate.transferRecipe(container, recipe, recipeSlots, player, maxTransfer, true)
        ));
        // 首次调用返回成功，实际填充由补库完成后的回调执行
        return null;
    }

    @Nullable
    private static UUID boundTerminalStorage(Player player) {
        // 终端可能在主手、副手或背包任意槽位中
        for (ItemStack stack : player.getInventory().items) {
            UUID id = TerminalRecipeTransferHandler.storageOf(stack);
            if (id != null) {
                return id;
            }
        }
        for (ItemStack stack : player.getInventory().offhand) {
            UUID id = TerminalRecipeTransferHandler.storageOf(stack);
            if (id != null) {
                return id;
            }
        }
        return null;
    }

    @Nullable
    private static UUID storageOf(ItemStack stack) {
        if (!stack.is(ModItems.HYPERDIMENSION_TERMINAL)) {
            return null;
        }
        TerminalBinding binding = stack.get(ModComponents.TERMINAL_BINDING);
        if (binding == null || binding.id().isEmpty()) {
            return null;
        }
        return binding.id().get();
    }

    /**
     * 计算容器（背包 + 合成格）缺少的物品：按配方输入槽聚合每种物品的总需求量与槽数，
     * 扣除整个容器中已有的同种物品（含合成格，因为合成格中的物品在默认传输时会被
     * 清出放回背包）。普通传输每种按一份（配方用量）计；Shift（maxTransfer）按
     * 槽数 × 每组数量计（合成格每格放满一组），服务端按背包剩余空间实际限制提取量。
     */
    private static List<ItemStack> collectMissing(
        AbstractContainerMenu container,
        Player player,
        IRecipeSlotsView recipeSlots,
        boolean maxTransfer
    ) {
        // 聚合配方所需：每种物品的总用量与出现槽数
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
            int idx = TerminalRecipeTransferHandler.findNeed(needs, representative);
            if (idx < 0) {
                needs.add(representative.copy());
                slotCounts.add(1);
            } else {
                needs.get(idx).grow(representative.getCount());
                slotCounts.set(idx, slotCounts.get(idx) + 1);
            }
        }
        // 扣除容器已有（背包 + 合成格 + 鼠标指针），得到缺口
        List<ItemStack> missing = new ArrayList<>();
        for (int i = 0; i < needs.size(); i++) {
            ItemStack need = needs.get(i);
            int required;
            if (maxTransfer) {
                // Shift：每格放满一组，目标 = 槽数 × 每组数量
                required = need.getMaxStackSize() * slotCounts.get(i);
            } else {
                required = need.getCount();
            }
            int have = TerminalRecipeTransferHandler.countInContainer(container, player, need);
            int deficit = Math.max(0, required - have);
            if (deficit > 0) {
                ItemStack m = need.copy();
                m.setCount(deficit);
                missing.add(m);
            }
        }
        return missing;
    }

    private static int findNeed(List<ItemStack> needs, ItemStack add) {
        for (int i = 0; i < needs.size(); i++) {
            if (ItemStack.isSameItemSameComponents(needs.get(i), add)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 统计整个容器（含合成格）与鼠标指针中同种物品的总量。
     * 合成格中的物品在默认传输时会被清出放回背包，因此必须计入“已有”以免重复补库。
     */
    private static int countInContainer(AbstractContainerMenu container, Player player, ItemStack resource) {
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
        // 鼠标指针上的同种物品也算作已有（避免捏着物品时重复补库）
        ItemStack carried = container.getCarried();
        if (!carried.isEmpty() && ItemStack.isSameItemSameComponents(carried, resource)) {
            count += carried.getCount();
        }
        return count;
    }
}
