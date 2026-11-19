package dev.dubhe.anvilcraft.integration.jei;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.screen.StorageMenu;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.integration.StorageJeiBridge;
import dev.dubhe.anvilcraft.integration.StorageRecipeTransferPlan;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
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
        StorageJeiBridge.setRecipeParent(screen -> Minecraft.getInstance().screen == jeiRuntime.getRecipesGui()
            && jeiRuntime.getRecipesGui().getParentScreen().orElse(null) == screen);
        StorageJeiBridge.setRecipeOpener(stonecutter -> jeiRuntime.getRecipesGui().showTypes(
            stonecutter ? List.of(RecipeTypes.STONECUTTING) : List.of(RecipeTypes.CRAFTING)));
    }

    public static void onRuntimeUnavailable() {
        runtime = null;
        StorageJeiBridge.setRecipeParent(null);
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
            if (screen == null) return this.helper.createInternalError();
            if (!screen.isCraftingUnlocked()) return this.helper.createInternalError();
            var slots = recipeSlots.getSlotViews(RecipeIngredientRole.INPUT);
            if (slots.isEmpty() || slots.size() > (this.stonecutter ? 1 : 9)) return this.helper.createInternalError();
            var variants = slots.stream().map(slot -> slot.getItemStacks().toList()).toList();
            var plan = StorageRecipeTransferPlan.create(variants, screen.getTransferMaterials(), screen.getTransferFluids());
            boolean incomplete = !this.stonecutter && StorageJeiSupport.incompleteFillEnabled();
            if (StorageJeiSupport.nothingAvailable(variants, plan)) {
                return this.helper.createUserErrorForMissingSlots(
                    Component.translatable("jei.tooltip.error.recipe.transfer.missing"),
                    plan.missing().intStream().mapToObj(slots::get).toList());
            }
            if (!plan.missing().isEmpty() && !incomplete) {
                return this.helper.createUserErrorForMissingSlots(Component.translatable("jei.tooltip.error.recipe.transfer.missing"),
                    plan.missing().intStream().mapToObj(slots::get).toList());
            }
            if (doTransfer) {
                screen.transferRecipe(this.stonecutter, maxTransfer, plan, this.result.apply(recipe), incomplete);
            } else if (incomplete && !plan.missing().isEmpty()) {
                return new IncompleteTransferError(
                    Component.translatable("jei.anvilcraft.tooltip.storage.incomplete"),
                    plan.missing().intStream().mapToObj(slots::get).toList()
                );
            }
            return null;
        }
    }

    private static final class IncompleteTransferError implements IRecipeTransferError {
        private final Component message;
        private final List<IRecipeSlotView> missingSlots;

        private IncompleteTransferError(Component message, List<IRecipeSlotView> missingSlots) {
            this.message = message;
            this.missingSlots = missingSlots;
        }

        @Override
        public Type getType() {
            return Type.COSMETIC;
        }

        @Override
        public void getTooltip(ITooltipBuilder tooltip) {
            tooltip.add(Component.translatable("jei.tooltip.transfer"));
            tooltip.add(this.message.copy().withStyle(ChatFormatting.RED));
        }

        @Override
        public void showError(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, IRecipeSlotsView recipeSlotsView, int recipeX, int recipeY
        ) {
            var pose = graphics.pose();
            pose.pushMatrix();
            try {
                pose.translate(recipeX, recipeY);
                for (IRecipeSlotView slot : this.missingSlots) {
                    slot.drawHighlight(graphics, IRecipeTransferError.DEFAULT_BUTTON_HIGHLIGHT_COLOR);
                }
            } finally {
                pose.popMatrix();
            }
        }

        @Override
        public int getMissingCountHint() {
            return this.missingSlots.size();
        }
    }

    private static boolean incompleteFillEnabled() {
        return AnvilCraft.CLIENT_CONFIG.ui.storageJeiIncompleteFill;
    }

    private static boolean nothingAvailable(List<List<ItemStack>> variants, StorageRecipeTransferPlan.Result plan) {
        long required = variants.stream().filter(options -> options.stream().anyMatch(stack -> !stack.isEmpty())).count();
        return required > 0 && plan.missing().size() == required;
    }
}
