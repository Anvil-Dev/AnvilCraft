package dev.dubhe.anvilcraft.integration.emi;

import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler;
import dev.dubhe.anvilcraft.client.gui.screen.StorageMenu;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import dev.emi.emi.api.neoforge.NeoForgeEmiStack;
import dev.emi.emi.api.recipe.EmiPlayerInventory;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import dev.emi.emi.api.recipe.handler.EmiRecipeHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.Widget;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class StorageRecipeHandler implements EmiRecipeHandler<StorageMenu> {
    @Override
    public EmiPlayerInventory getInventory(AbstractContainerScreen<StorageMenu> screen) {
        if (!(screen instanceof StorageScreen storageScreen) || storageScreen.isPreview()) {
            return new EmiPlayerInventory(List.of());
        }
        Map<UnlimitedItemStacksResourceHandler.ResourceKey, Long> materials = storageScreen.getTransferMaterials();
        List<StorageServerStub.FluidEntry> fluids = storageScreen.getTransferFluids();
        List<EmiStack> stacks = new ArrayList<>(materials.size() + fluids.size());
        for (Map.Entry<UnlimitedItemStacksResourceHandler.ResourceKey, Long> entry : materials.entrySet()) {
            if (!entry.getKey().stack().isEmpty() && entry.getValue() > 0) {
                stacks.add(EmiStack.of(entry.getKey().stack(), entry.getValue()));
            }
        }
        for (StorageServerStub.FluidEntry fluid : fluids) {
            if (!fluid.icon().isEmpty() && fluid.amount() > 0) {
                stacks.add(NeoForgeEmiStack.of(fluid.icon().copyWithAmount(fluid.amount())));
            }
        }
        return new EmiPlayerInventory(stacks);
    }

    @Override
    public boolean supportsRecipe(EmiRecipe recipe) {
        return recipe.getCategory() == VanillaEmiRecipeCategories.CRAFTING
            || recipe.getCategory() == VanillaEmiRecipeCategories.STONECUTTING;
    }

    @Override
    public boolean canCraft(EmiRecipe recipe, EmiCraftContext<StorageMenu> context) {
        if (!(context.getScreen() instanceof StorageScreen screen) || screen.isPreview() || !screen.isCraftingAvailable()) {
            return false;
        }
        PlanResult plan = StorageRecipeHandler.plan(recipe, screen);
        return plan != null && plan.missing().isEmpty();
    }

    @Override
    public boolean craft(EmiRecipe recipe, EmiCraftContext<StorageMenu> context) {
        if (!(context.getScreen() instanceof StorageScreen screen) || screen.isPreview() || !screen.isCraftingAvailable()) {
            return false;
        }
        PlanResult plan = StorageRecipeHandler.plan(recipe, screen);
        if (plan == null || !plan.missing().isEmpty()) {
            return false;
        }
        boolean stonecutter = recipe.getCategory() == VanillaEmiRecipeCategories.STONECUTTING;
        ItemStack result = stonecutter && !recipe.getOutputs().isEmpty()
            ? recipe.getOutputs().getFirst().getItemStack()
            : ItemStack.EMPTY;
        if (stonecutter && result.isEmpty()) {
            return false;
        }
        StorageServerStub.CraftDestination destination = switch (context.getDestination()) {
            case NONE -> StorageServerStub.CraftDestination.GRID;
            case CURSOR -> StorageServerStub.CraftDestination.CURSOR;
            case INVENTORY -> StorageServerStub.CraftDestination.INVENTORY;
        };
        return screen.executeRecipe(destination, context.getAmount(), stonecutter, plan.inputs(), plan.counts(), result);
    }

    @Override
    public List<ClientTooltipComponent> getTooltip(EmiRecipe recipe, EmiCraftContext<StorageMenu> context) {
        if (!(context.getScreen() instanceof StorageScreen screen) || screen.isPreview()) {
            return List.of();
        }
        if (!screen.isCraftingAvailable()) {
            return List.of(
                ClientTooltipComponent.create(Component.translatable("tooltip.anvilcraft.storage.missing_workbench").getVisualOrderText())
            );
        }
        return EmiRecipeHandler.super.getTooltip(recipe, context);
    }

    @Override
    public void render(
        EmiRecipe recipe,
        EmiCraftContext<StorageMenu> context,
        List<Widget> widgets,
        GuiGraphics draw
    ) {
        if (!(context.getScreen() instanceof StorageScreen screen) || screen.isPreview() || !screen.isCraftingAvailable()) {
            return;
        }
        PlanResult plan = StorageRecipeHandler.plan(recipe, screen);
        if (plan == null || plan.missing().isEmpty()) {
            return;
        }
        boolean stonecutter = recipe.getCategory() == VanillaEmiRecipeCategories.STONECUTTING;
        List<EmiIngredient> slots = StorageRecipeHandler.gridIngredients(recipe, stonecutter);
        if (slots == null) {
            return;
        }
        List<SlotWidget> inputWidgets = new ArrayList<>();
        for (Widget widget : widgets) {
            if (widget instanceof SlotWidget slotWidget && slotWidget.getRecipe() == null) {
                inputWidgets.add(slotWidget);
            }
        }
        if (inputWidgets.size() == slots.size()) {
            for (int index = 0; index < slots.size(); index++) {
                if (plan.missing().contains(index)) {
                    StorageRecipeHandler.fillMissingSlot(draw, inputWidgets.get(index));
                }
            }
            return;
        }
        List<SlotWidget> nonEmptyWidgets = inputWidgets.stream()
            .filter(widget -> !widget.getStack().isEmpty())
            .toList();
        int widgetIndex = 0;
        for (int slotIndex = 0; slotIndex < slots.size() && widgetIndex < nonEmptyWidgets.size(); slotIndex++) {
            if (slots.get(slotIndex).isEmpty()) {
                continue;
            }
            if (plan.missing().contains(slotIndex)) {
                StorageRecipeHandler.fillMissingSlot(draw, nonEmptyWidgets.get(widgetIndex));
            }
            widgetIndex++;
        }
    }

    private static void fillMissingSlot(GuiGraphics draw, SlotWidget slotWidget) {
        Bounds bounds = slotWidget.getBounds();
        draw.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), 0x44FF0000);
    }

    private static @Nullable PlanResult plan(EmiRecipe recipe, StorageScreen screen) {
        boolean stonecutter = recipe.getCategory() == VanillaEmiRecipeCategories.STONECUTTING;
        if (!stonecutter && recipe.getCategory() != VanillaEmiRecipeCategories.CRAFTING) {
            return null;
        }
        List<EmiIngredient> ingredients = StorageRecipeHandler.gridIngredients(recipe, stonecutter);
        if (ingredients == null) {
            return null;
        }
        Map<UnlimitedItemStacksResourceHandler.ResourceKey, Long> available = screen.getTransferMaterials();
        List<StorageServerStub.FluidEntry> fluids = screen.getTransferFluids();
        List<List<ItemStack>> variants = new ArrayList<>(ingredients.size());
        for (EmiIngredient ingredient : ingredients) {
            if (ingredient.isEmpty()) {
                variants.add(List.of());
                continue;
            }
            List<ItemStack> options = ingredient.getEmiStacks().stream()
                .map(EmiStack::getItemStack)
                .filter(stack -> !stack.isEmpty())
                .sorted(Comparator.comparingLong(
                    (ItemStack stack) -> available.getOrDefault(UnlimitedItemStacksResourceHandler.ResourceKey.of(stack), 0L)
                ).reversed())
                .toList();
            if (options.isEmpty()) {
                return null;
            }
            variants.add(options);
        }
        return StorageRecipeHandler.createPlan(variants, available, fluids);
    }

    static PlanResult createPlan(
        List<List<ItemStack>> variants,
        Map<UnlimitedItemStacksResourceHandler.ResourceKey, Long> available,
        List<StorageServerStub.FluidEntry> fluidEntries
    ) {
        Map<UnlimitedItemStacksResourceHandler.ResourceKey, Long> items = new HashMap<>(available);
        Map<FluidKey, Long> fluids = new HashMap<>();
        for (StorageServerStub.FluidEntry entry : fluidEntries) {
            if (!entry.icon().isEmpty() && entry.amount() > 0) {
                fluids.merge(FluidKey.of(entry.icon()), (long) entry.amount(), Long::sum);
            }
        }
        List<ItemStack> inputs = new ArrayList<>(variants.size());
        IntList counts = new IntArrayList(variants.size());
        IntList missing = new IntArrayList();
        for (int index = 0; index < variants.size(); index++) {
            List<ItemStack> options = variants.get(index).stream().filter(stack -> !stack.isEmpty()).toList();
            ItemStack chosen = ItemStack.EMPTY;
            for (ItemStack option : options) {
                if (StorageRecipeHandler.reserve(items, UnlimitedItemStacksResourceHandler.ResourceKey.of(option), 1)) {
                    chosen = option.copyWithCount(1);
                    break;
                }
            }
            if (chosen.isEmpty()) {
                for (ItemStack option : options) {
                    FluidContainer container = StorageRecipeHandler.container(option);
                    if (
                        container == null
                        || items.getOrDefault(container.empty(), 0L) < 1
                        || fluids.getOrDefault(container.fluid(), 0L) < container.amount()
                    ) {
                        continue;
                    }
                    StorageRecipeHandler.reserve(items, container.empty(), 1);
                    StorageRecipeHandler.reserve(fluids, container.fluid(), container.amount());
                    chosen = option.copyWithCount(1);
                    break;
                }
            }
            if (chosen.isEmpty() && !options.isEmpty()) {
                missing.add(index);
            }
            inputs.add(chosen);
            counts.add(chosen.isEmpty() ? 0 : 1);
        }
        return new PlanResult(List.copyOf(inputs), counts, missing);
    }

    private static <K> boolean reserve(Map<K, Long> resources, K resource, long count) {
        long available = resources.getOrDefault(resource, 0L);
        if (available < count) {
            return false;
        }
        resources.put(resource, available - count);
        return true;
    }

    private static @Nullable FluidContainer container(ItemStack wanted) {
        IFluidHandlerItem drainHandler = FluidUtil.getFluidHandler(wanted.copyWithCount(1)).orElse(null);
        if (drainHandler == null) {
            return null;
        }
        FluidStack fluid = drainHandler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
        if (fluid.isEmpty()) {
            return null;
        }
        ItemStack empty = drainHandler.getContainer();
        if (empty.isEmpty() || ItemStack.isSameItemSameComponents(empty, wanted)) {
            return null;
        }
        IFluidHandlerItem fillHandler = FluidUtil.getFluidHandler(empty.copyWithCount(1)).orElse(null);
        if (
            fillHandler == null
            || fillHandler.fill(fluid.copy(), IFluidHandler.FluidAction.EXECUTE) != fluid.getAmount()
            || !ItemStack.isSameItemSameComponents(fillHandler.getContainer(), wanted)
        ) {
            return null;
        }
        return new FluidContainer(
            UnlimitedItemStacksResourceHandler.ResourceKey.of(empty),
            FluidKey.of(fluid),
            fluid.getAmount()
        );
    }

    private static @Nullable List<EmiIngredient> gridIngredients(EmiRecipe recipe, boolean stonecutter) {
        List<EmiIngredient> inputs = recipe.getInputs();
        for (EmiIngredient ingredient : inputs) {
            if (!ingredient.isEmpty() && ingredient.getAmount() != 1) {
                return null;
            }
        }
        if (stonecutter) {
            return inputs.size() == 1 ? inputs : null;
        }
        if (inputs.isEmpty() || inputs.size() > 9) {
            return null;
        }
        if (inputs.size() == 9) {
            return inputs;
        }
        RecipeHolder<?> backing = recipe.getBackingRecipe();
        if (backing != null && backing.value() instanceof ShapedRecipe shaped) {
            int width = shaped.getWidth();
            int height = shaped.getHeight();
            if (width >= 1 && width <= 3 && height >= 1 && height <= 3 && inputs.size() == width * height) {
                List<EmiIngredient> padded = new ArrayList<>(9);
                for (int i = 0; i < 9; i++) {
                    padded.add(EmiIngredient.of(List.of()));
                }
                for (int row = 0; row < height; row++) {
                    for (int col = 0; col < width; col++) {
                        padded.set(row * 3 + col, inputs.get(row * width + col));
                    }
                }
                return padded;
            }
        }
        return inputs;
    }

    record PlanResult(List<ItemStack> inputs, IntList counts, IntList missing) {
    }

    private record FluidContainer(
        UnlimitedItemStacksResourceHandler.ResourceKey empty,
        FluidKey fluid,
        int amount
    ) {
    }

    private record FluidKey(FluidStack stack) {
        private static FluidKey of(FluidStack stack) {
            return new FluidKey(stack.copyWithAmount(1));
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof FluidKey(FluidStack other)
                && FluidStack.isSameFluidSameComponents(this.stack, other);
        }

        @Override
        public int hashCode() {
            return FluidStack.hashFluidAndComponents(this.stack);
        }
    }
}
