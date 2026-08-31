package dev.dubhe.anvilcraft.integration.jei;

import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.client.rpc.StorageClientStub;
import dev.dubhe.anvilcraft.integration.StorageJeiBridge;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.runtime.IRecipesGui;
import mezz.jei.library.transfer.PlayerRecipeTransferHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.StonecutterRecipe;

import java.util.ArrayList;
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
 *   <li>{@link #registerRecipeTransferHandlers}：为玩家背包菜单（{@link InventoryMenu}，
 *       StorageScreen 打开期间它就是 JEI 的父容器）注册切石机/合成的转移 handler；
 *       当前屏幕是 StorageScreen 时把配方输入放入 ①/② 输入槽（材料从背包扣取），
 *       否则切石机转移不可用、合成转移委托原玩家 2×2 合成转移。</li>
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

    /** 当前屏幕是否为仓储界面。 */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean isStorageScreen() {
        return Minecraft.getInstance().screen instanceof StorageScreen;
    }

    /** 配方输入槽的物品列表（与槽一一对应，空槽为 EMPTY）。 */
    private static List<ItemStack> collectInputs(IRecipeSlotsView recipeSlots) {
        List<ItemStack> inputs = new ArrayList<>();
        for (IRecipeSlotView slotView : recipeSlots.getSlotViews(RecipeIngredientRole.INPUT)) {
            inputs.add(slotView.getDisplayedItemStack().map(ItemStack::copy).orElse(ItemStack.EMPTY));
        }
        return inputs;
    }

    /** 把配方输入放入终端输入槽（异步 RPC，成功后刷新合成面板）。 */
    private static void transferIntoStorage(boolean stonecutter, List<ItemStack> inputs) {
        if (!(Minecraft.getInstance().screen instanceof StorageScreen screen)) {
            return;
        }
        StorageClientStub.craftingTransfer(screen.getSourcePos(), stonecutter, inputs).whenCompleteAsync(
            (changed, error) -> {
                if (changed != null && changed && Minecraft.getInstance().screen instanceof StorageScreen current) {
                    current.loadCrafting(stonecutter);
                }
            },
            Minecraft.getInstance()
        );
    }

    /** 切石机配方转移：把第一个输入放入①（StorageScreen 打开时）。 */
    private static final class StorageScreenStonecutterTransferHandler
        implements IRecipeTransferHandler<InventoryMenu, RecipeHolder<StonecutterRecipe>> {
        private final IRecipeTransferHandlerHelper helper;

        private StorageScreenStonecutterTransferHandler(IRecipeTransferHandlerHelper helper) {
            this.helper = helper;
        }

        @Override
        public Class<? extends InventoryMenu> getContainerClass() {
            return InventoryMenu.class;
        }

        @Override
        public Optional<MenuType<InventoryMenu>> getMenuType() {
            return Optional.empty();
        }

        @Override
        public RecipeType<RecipeHolder<StonecutterRecipe>> getRecipeType() {
            return RecipeTypes.STONECUTTING;
        }

        @Override
        public @Nullable IRecipeTransferError transferRecipe(
            InventoryMenu container,
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
                StorageJeiSupport.transferIntoStorage(true, StorageJeiSupport.collectInputs(recipeSlots));
            }
            return null;
        }
    }

    /** 合成配方转移：把 9 宫格输入放入②（StorageScreen 打开时），否则委托原玩家 2×2 转移。 */
    private static final class StorageScreenCraftingTransferHandler
        implements IRecipeTransferHandler<InventoryMenu, RecipeHolder<CraftingRecipe>> {
        private final IRecipeTransferHandler<InventoryMenu, RecipeHolder<CraftingRecipe>> delegate;

        private StorageScreenCraftingTransferHandler(IRecipeTransferHandlerHelper helper) {
            this.delegate = new PlayerRecipeTransferHandler(helper);
        }

        @Override
        public Class<? extends InventoryMenu> getContainerClass() {
            return InventoryMenu.class;
        }

        @Override
        public Optional<MenuType<InventoryMenu>> getMenuType() {
            return Optional.empty();
        }

        @Override
        public RecipeType<RecipeHolder<CraftingRecipe>> getRecipeType() {
            return RecipeTypes.CRAFTING;
        }

        @Override
        public @Nullable IRecipeTransferError transferRecipe(
            InventoryMenu container,
            RecipeHolder<CraftingRecipe> recipe,
            IRecipeSlotsView recipeSlots,
            Player player,
            boolean maxTransfer,
            boolean doTransfer
        ) {
            if (!StorageJeiSupport.isStorageScreen()) {
                return this.delegate.transferRecipe(container, recipe, recipeSlots, player, maxTransfer, doTransfer);
            }
            if (doTransfer) {
                StorageJeiSupport.transferIntoStorage(false, StorageJeiSupport.collectInputs(recipeSlots));
            }
            return null;
        }
    }
}
