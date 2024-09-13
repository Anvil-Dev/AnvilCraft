package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModMenuTypes;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
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

    @Nullable private RecipeHolder<SmithingRecipe> selectedRecipe;

    private final List<RecipeHolder<SmithingRecipe>> recipes;

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
        this.recipes = this.level.getRecipeManager().getAllRecipesFor(RecipeType.SMITHING);
    }

    protected @NotNull ItemCombinerMenuSlotDefinition createInputSlotDefinitions() {
        return ItemCombinerMenuSlotDefinition.create()
                .withSlot(0, 8, 48, itemStack -> this.recipes.stream()
                        .anyMatch(smithingRecipe -> smithingRecipe.value().isTemplateIngredient(itemStack)))
                .withSlot(1, 44, 48, itemStack -> this.recipes.stream()
                        .anyMatch(smithingRecipe -> smithingRecipe.value().isBaseIngredient(itemStack)))
                .withSlot(2, 62, 48, itemStack -> this.recipes.stream()
                        .anyMatch(smithingRecipe -> smithingRecipe.value().isAdditionIngredient(itemStack)))
                .withResultSlot(3, 106, 48)
                .build();
    }

    protected boolean isValidBlock(@NotNull BlockState state) {
        return state.is(ModBlocks.EMBER_SMITHING_TABLE.get());
    }

    protected boolean mayPickup(@NotNull Player player, boolean hasStack) {
        return this.selectedRecipe != null && this.selectedRecipe.value().matches(this.createRecipeInput(), this.level);
    }

    protected void onTake(@NotNull Player player, @NotNull ItemStack stack) {
        stack.onCraftedBy(player.level(), player, stack.getCount());
        this.resultSlots.awardUsedRecipes(player, this.getRelevantItems());
        this.shrinkStackInSlot(1);
        this.shrinkStackInSlot(2);
        this.access.execute((level, blockPos) -> level.levelEvent(1044, blockPos, 0));
    }

    private @Unmodifiable List<ItemStack> getRelevantItems() {
        return List.of(this.inputSlots.getItem(0), this.inputSlots.getItem(1), this.inputSlots.getItem(2));
    }

    private void shrinkStackInSlot(int index) {
        ItemStack itemStack = this.inputSlots.getItem(index);
        if (!itemStack.isEmpty()) {
            itemStack.shrink(1);
            this.inputSlots.setItem(index, itemStack);
        }
    }

    private SmithingRecipeInput createRecipeInput() {
        return new SmithingRecipeInput(
                this.inputSlots.getItem(0), this.inputSlots.getItem(1), this.inputSlots.getItem(2));
    }

    @Override
    public void createResult() {
        SmithingRecipeInput smithingrecipeinput = this.createRecipeInput();
        List<RecipeHolder<SmithingRecipe>> list =
                this.level.getRecipeManager().getRecipesFor(RecipeType.SMITHING, smithingrecipeinput, this.level);
        if (list.isEmpty()) {
            this.resultSlots.setItem(0, ItemStack.EMPTY);
        } else {
            RecipeHolder<SmithingRecipe> recipeholder = list.getFirst();
            ItemStack itemstack = recipeholder.value().assemble(smithingrecipeinput, this.level.registryAccess());
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

    private static Optional<Integer> findSlotMatchingIngredient(@NotNull SmithingRecipe recipe, ItemStack stack) {
        if (recipe.isTemplateIngredient(stack)) return Optional.of(0);
        if (recipe.isBaseIngredient(stack)) return Optional.of(1);
        if (recipe.isAdditionIngredient(stack)) return Optional.of(2);
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
