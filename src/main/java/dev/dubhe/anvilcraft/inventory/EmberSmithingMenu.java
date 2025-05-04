package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.init.ModMenuTypes;

import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.item.template.MultipleToOneTemplateItem;
import dev.dubhe.anvilcraft.recipe.multiple.BaseMultipleToOneSmithingRecipe;
import dev.dubhe.anvilcraft.recipe.multiple.EightToOneSmithingRecipe;
import dev.dubhe.anvilcraft.recipe.multiple.MultipleToOneSmithingRecipeInput;
import dev.dubhe.anvilcraft.recipe.multiple.TwoToOneSmithingRecipe;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("unused")
public class EmberSmithingMenu extends ItemCombinerMenu {
    private final Level level;

    @Nullable
    private RecipeHolder<BaseMultipleToOneSmithingRecipe<?>> selectedRecipe;

    private final List<RecipeHolder<BaseMultipleToOneSmithingRecipe<?>>> recipes;

    public EmberSmithingMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public EmberSmithingMenu(MenuType<EmberSmithingMenu> type, int containerId, Inventory playerInventory) {
        this(type, containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public EmberSmithingMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        this(ModMenuTypes.EMBER_SMITHING.get(), containerId, playerInventory, access);
    }

    /**
     * 皇家锻造台菜单
     *
     * @param type            类型
     * @param containerId     容器id
     * @param playerInventory 背包
     * @param access          检查
     */
    public EmberSmithingMenu(
        MenuType<EmberSmithingMenu> type, int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(type, containerId, playerInventory, access);
        this.level = playerInventory.player.level();
        this.recipes = this.level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.MULTIPLE_TO_ONE_SMITHING_TYPE.get());
    }

    @Override
    protected @NotNull ItemCombinerMenuSlotDefinition createInputSlotDefinitions() {
        return ItemCombinerMenuSlotDefinition.create()
            .withSlot(0, 8, 48, itemStack -> this.recipes.stream()
                .anyMatch(smithingRecipe -> smithingRecipe.value().isTemplateIngredient(itemStack)))
            .withSlot(1, 80, 36, itemStack ->
                !this.inputSlots.getItem(0).isEmpty() && this.recipes.stream()
                .anyMatch(smithingRecipe -> smithingRecipe.value().isMaterialIngredient(itemStack)))
            .withSlot(2, 80, 18, itemStack ->
                !this.inputSlots.getItem(0).isEmpty() && this.recipes.stream()
                    .anyMatch(smithingRecipe -> smithingRecipe.value().isInputIngredient(0, itemStack)))
            .withSlot(3, 80, 54, itemStack ->
                !this.inputSlots.getItem(0).isEmpty() && this.recipes.stream()
                    .anyMatch(smithingRecipe -> smithingRecipe.value().isInputIngredient(1, itemStack)))
            .withSlot(4, 62, 36, itemStack ->
                !this.inputSlots.getItem(0).is(ModItems.TWO_TO_ONE_SMITHING_TEMPLATE) && this.recipes.stream()
                    .anyMatch(smithingRecipe ->
                                  !(smithingRecipe.value() instanceof TwoToOneSmithingRecipe<?>)
                                  && smithingRecipe.value().isInputIngredient(2, itemStack)))
            .withSlot(5, 98, 36, itemStack ->
                !this.inputSlots.getItem(0).is(ModItems.TWO_TO_ONE_SMITHING_TEMPLATE) && this.recipes.stream()
                    .anyMatch(smithingRecipe ->
                                  !(smithingRecipe.value() instanceof TwoToOneSmithingRecipe<?>)
                                  && smithingRecipe.value().isInputIngredient(3, itemStack)))
            .withSlot(6, 62, 18, itemStack ->
                this.inputSlots.getItem(0).is(ModItems.EIGHT_TO_ONE_SMITHING_TEMPLATE) && this.recipes.stream()
                    .anyMatch(smithingRecipe ->
                                  smithingRecipe.value() instanceof EightToOneSmithingRecipe<?>
                                  && smithingRecipe.value().isInputIngredient(4, itemStack)))
            .withSlot(7, 98, 18, itemStack ->
                this.inputSlots.getItem(0).is(ModItems.EIGHT_TO_ONE_SMITHING_TEMPLATE) && this.recipes.stream()
                    .anyMatch(smithingRecipe ->
                                  smithingRecipe.value() instanceof EightToOneSmithingRecipe<?>
                                  && smithingRecipe.value().isInputIngredient(5, itemStack)))
            .withSlot(8, 62, 54, itemStack ->
                this.inputSlots.getItem(0).is(ModItems.EIGHT_TO_ONE_SMITHING_TEMPLATE) && this.recipes.stream()
                    .anyMatch(smithingRecipe ->
                                  smithingRecipe.value() instanceof EightToOneSmithingRecipe<?>
                                  && smithingRecipe.value().isInputIngredient(6, itemStack)))
            .withSlot(9, 98, 54, itemStack ->
                this.inputSlots.getItem(0).is(ModItems.EIGHT_TO_ONE_SMITHING_TEMPLATE) && this.recipes.stream()
                    .anyMatch(smithingRecipe ->
                                  smithingRecipe.value() instanceof EightToOneSmithingRecipe<?>
                                  && smithingRecipe.value().isInputIngredient(7, itemStack)))
            .withResultSlot(10, 151, 48)
            .build();
    }

    @Override
    protected boolean isValidBlock(@NotNull BlockState state) {
        return state.is(ModBlocks.EMBER_SMITHING_TABLE.get());
    }

    @Override
    protected boolean mayPickup(@NotNull Player player, boolean hasStack) {
        return this.selectedRecipe != null && this.selectedRecipe.value().matches(this.createRecipeInput(), this.level);
    }

    @Override
    public void slotsChanged(@NotNull Container inventory) {
        super.slotsChanged(inventory);
        if (inventory == this.inputSlots && this.inputSlots.getItem(0).isEmpty()) {
            for (int i = 1; i < 10; i++) {
                ItemStack stack = this.inputSlots.getItem(i);
                if (!stack.isEmpty()) {
                    this.inputSlots.removeItemNoUpdate(i);
                    this.moveItemStackTo(stack, 11, 47, false);
                }
            }
        }
    }

    @Override
    protected void onTake(@NotNull Player player, @NotNull ItemStack stack) {
        stack.onCraftedBy(player.level(), player, stack.getCount());
        this.resultSlots.awardUsedRecipes(player, this.getRelevantItems());
        this.shrinkStackInSlot(1);
        this.shrinkStackInSlot(2);
        this.shrinkStackInSlot(3);
        this.shrinkStackInSlot(4);
        this.shrinkStackInSlot(5);
        this.shrinkStackInSlot(6);
        this.shrinkStackInSlot(7);
        this.shrinkStackInSlot(8);
        this.shrinkStackInSlot(9);
        this.access.execute((level, blockPos) -> level.levelEvent(1044, blockPos, 0));
    }

    private @Unmodifiable List<ItemStack> getRelevantItems() {
        return List.of(
            this.inputSlots.getItem(0),
            this.inputSlots.getItem(1),
            this.inputSlots.getItem(2),
            this.inputSlots.getItem(3),
            this.inputSlots.getItem(4),
            this.inputSlots.getItem(5),
            this.inputSlots.getItem(6),
            this.inputSlots.getItem(7),
            this.inputSlots.getItem(8),
            this.inputSlots.getItem(9)
        );
    }

    private void shrinkStackInSlot(int index) {
        ItemStack itemStack = this.inputSlots.getItem(index);
        if (!itemStack.isEmpty()) {
            itemStack.shrink(1);
            this.inputSlots.setItem(index, itemStack);
        }
    }

    private MultipleToOneSmithingRecipeInput createRecipeInput() {
        ItemStack[] inputs = new ItemStack[this.getInputSize()];
        for (int i = 0; i < this.getInputSize(); i++) {
            inputs[i] = this.inputSlots.getItem(Math.min(i + 2, 9));
        }
        return new MultipleToOneSmithingRecipeInput(
            this.inputSlots.getItem(0),
            this.inputSlots.getItem(1),
            inputs,
            this.getInputSize()
        );
    }

    public int getInputSize() {
        ItemStack stack = this.inputSlots.getItem(0);
        if (stack.getItem() instanceof MultipleToOneTemplateItem template) {
            return template.getSize();
        } else {
            return 0;
        }
    }

    @Override
    public void createResult() {
        MultipleToOneSmithingRecipeInput input = this.createRecipeInput();
        List<RecipeHolder<BaseMultipleToOneSmithingRecipe<?>>> list =
            this.level.getRecipeManager().getRecipesFor(ModRecipeTypes.MULTIPLE_TO_ONE_SMITHING_TYPE.get(), input, this.level);
        if (list.isEmpty()) {
            this.resultSlots.setItem(0, ItemStack.EMPTY);
        } else {
            RecipeHolder<BaseMultipleToOneSmithingRecipe<?>> recipeholder = list.getFirst();
            ItemStack itemstack = recipeholder.value().assemble(input, this.level.registryAccess());
            if (itemstack.isItemEnabled(this.level.enabledFeatures())) {
                this.selectedRecipe = recipeholder;
                this.resultSlots.setRecipeUsed(recipeholder);
                this.resultSlots.setItem(0, itemstack);
            }
        }
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Override
    public int getSlotToQuickMoveTo(@NotNull ItemStack stack) {
        return this.recipes.stream()
            .map(smithingRecipe -> EmberSmithingMenu.findSlotMatchingIngredient(smithingRecipe.value(), stack))
            .filter(Optional::isPresent)
            .findFirst()
            .orElse(Optional.of(0))
            .get();
    }

    private static Optional<Integer> findSlotMatchingIngredient(@NotNull BaseMultipleToOneSmithingRecipe<?> recipe, ItemStack stack) {
        if (recipe.isTemplateIngredient(stack)) return Optional.of(0);
        if (recipe.isMaterialIngredient(stack)) return Optional.of(1);
        if (recipe.isInputIngredient(0, stack)) return Optional.of(2);
        if (recipe.isInputIngredient(1, stack)) return Optional.of(3);
        if (!(recipe instanceof TwoToOneSmithingRecipe<?>) && recipe.isInputIngredient(2, stack)) return Optional.of(4);
        if (!(recipe instanceof TwoToOneSmithingRecipe<?>) && recipe.isInputIngredient(3, stack)) return Optional.of(5);
        if (recipe instanceof EightToOneSmithingRecipe<?> && recipe.isInputIngredient(4, stack)) return Optional.of(6);
        if (recipe instanceof EightToOneSmithingRecipe<?> && recipe.isInputIngredient(5, stack)) return Optional.of(7);
        if (recipe instanceof EightToOneSmithingRecipe<?> && recipe.isInputIngredient(6, stack)) return Optional.of(8);
        if (recipe instanceof EightToOneSmithingRecipe<?> && recipe.isInputIngredient(7, stack)) return Optional.of(9);
        return Optional.empty();
    }

    @Override
    public boolean canTakeItemForPickAll(@NotNull ItemStack stack, @NotNull Slot slot) {
        return slot.container != this.resultSlots && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public boolean canMoveIntoInputSlots(@NotNull ItemStack stack) {
        return this.recipes.stream()
            .map(smithingRecipe -> EmberSmithingMenu.findSlotMatchingIngredient(smithingRecipe.value(), stack))
            .anyMatch(Optional::isPresent);
    }
}
