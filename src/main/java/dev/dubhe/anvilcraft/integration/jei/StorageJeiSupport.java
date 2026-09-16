package dev.dubhe.anvilcraft.integration.jei;

import dev.dubhe.anvilcraft.client.gui.screen.StorageMenu;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.integration.StorageJeiBridge;
import dev.dubhe.anvilcraft.integration.StorageRecipeTransferPlan;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class StorageJeiSupport {
    private static @Nullable IJeiRuntime runtime;

    private StorageJeiSupport() {
    }

    public static void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        StorageJeiBridge.setRecipeOpener(stonecutter -> jeiRuntime.getRecipesGui().showTypes(
            stonecutter ? List.of(RecipeTypes.STONECUTTING) : List.of(RecipeTypes.CRAFTING)));
    }

    public static void onRuntimeUnavailable() {
        runtime = null;
        StorageJeiBridge.setRecipeOpener(null);
    }

    public static void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new Handler<RecipeHolder<CraftingRecipe>>(
            registration.getTransferHelper(), RecipeTypes.CRAFTING, false, recipe -> ItemStack.EMPTY), RecipeTypes.CRAFTING);
        registration.addRecipeTransferHandler(new Handler<RecipeHolder<StonecutterRecipe>>(
            registration.getTransferHelper(), RecipeTypes.STONECUTTING, true,
            recipe -> recipe.value().assemble(new SingleRecipeInput(ItemStack.EMPTY))), RecipeTypes.STONECUTTING);
    }

    private static @Nullable StorageScreen storageScreen(StorageMenu menu) {
        if (Minecraft.getInstance().screen instanceof StorageScreen screen && screen.getMenu() == menu) return screen;
        if (runtime != null && runtime.getRecipesGui().getParentScreen().orElse(null) instanceof StorageScreen screen
            && screen.getMenu() == menu) return screen;
        return null;
    }

    private static final class Handler<R> implements IRecipeTransferHandler<StorageMenu, R> {
        private final IRecipeTransferHandlerHelper helper;
        private final IRecipeType<R> recipeType;
        private final boolean stonecutter;
        private final Function<R, ItemStack> result;

        private Handler(
            IRecipeTransferHandlerHelper helper, IRecipeType<R> recipeType, boolean stonecutter, Function<R, ItemStack> result
        ) {
            this.helper = helper;
            this.recipeType = recipeType;
            this.stonecutter = stonecutter;
            this.result = result;
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
        public IRecipeType<R> getRecipeType() {
            return this.recipeType;
        }

        @Override
        public @Nullable IRecipeTransferError transferRecipe(
            StorageMenu menu, R recipe, IRecipeSlotsView recipeSlots, Player player, boolean maxTransfer, boolean doTransfer
        ) {
            StorageScreen screen = storageScreen(menu);
            if (screen == null || !screen.canTransferRecipe()) return this.helper.createInternalError();
            var slots = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT);
            if (slots.isEmpty() || slots.size() > (this.stonecutter ? 1 : 9)) return this.helper.createInternalError();
            var variants = slots.stream().map(slot -> slot.getItemStacks().toList()).toList();
            var plan = StorageRecipeTransferPlan.create(variants, screen.getTransferMaterials(), screen.getTransferFluids());
            if (!plan.missing().isEmpty()) {
                return this.helper.createUserErrorForMissingSlots(Component.translatable("jei.tooltip.error.recipe.transfer.missing"),
                    plan.missing().intStream().mapToObj(slots::get).toList());
            }
            if (doTransfer) screen.transferRecipe(this.stonecutter, maxTransfer, plan, this.result.apply(recipe));
            return null;
        }
    }
}
