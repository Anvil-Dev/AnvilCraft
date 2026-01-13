package dev.dubhe.anvilcraft.inventory;

import com.google.common.collect.ImmutableList;
import dev.dubhe.anvilcraft.api.recipe.result.RecipeResult;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.frost.DeformationRecipe;
import dev.dubhe.anvilcraft.recipe.frost.FrostSmithingRecipeInput;
import dev.dubhe.anvilcraft.recipe.frost.IFrostSmithingRecipe;
import dev.dubhe.anvilcraft.recipe.frost.PermutationRecipe;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Optional;

public class FrostSmithingMenu extends ItemCombinerMenu {
    private final Level level;

    private final List<RecipeHolder<? extends IFrostSmithingRecipe>> recipes;

    private RecipeHolder<? extends IFrostSmithingRecipe> selectedRecipe = null;
    public int selected = -1;
    public List<RecipeResult> results = null;

    public FrostSmithingMenu(MenuType<FrostSmithingMenu> type, int containerId, Inventory playerInventory) {
        this(type, containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public FrostSmithingMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        this(ModMenuTypes.FROST_SMITHING.get(), containerId, playerInventory, access);
    }

    /**
     * 浮霜锻造台菜单
     *
     * @param type            类型
     * @param containerId     容器id
     * @param playerInventory 背包
     * @param access          检查
     */
    public FrostSmithingMenu(
        MenuType<FrostSmithingMenu> type, int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        super(type, containerId, playerInventory, access);
        this.level = playerInventory.player.level();
        this.recipes = ImmutableList.<RecipeHolder<? extends IFrostSmithingRecipe>>builder()
            .addAll(this.level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.PERMUTATION_TYPE.get()))
            .addAll(this.level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.DEFORMATION_TYPE.get()))
            .build();
    }

    @Override
    protected ItemCombinerMenuSlotDefinition createInputSlotDefinitions() {
        return ItemCombinerMenuSlotDefinition.create()
            .withSlot(
                0,
                8,
                48,
                stack -> this.recipes.stream().anyMatch(recipe -> recipe.value().isTemplate(stack))
            ).withSlot(
                1,
                44,
                48,
                stack -> this.recipes.stream().anyMatch(recipe -> recipe.value().isMaterial(stack))
            ).withSlot(
                2,
                62,
                48,
                stack -> !this.inputSlots.getItem(0).isEmpty()
                         && !this.inputSlots.getItem(1).isEmpty()
                         && this.recipes.stream().anyMatch(recipe -> recipe.value().isInput(stack))
            ).withResultSlot(3, 106, 48)
            .build();
    }

    @Override
    protected boolean isValidBlock(BlockState state) {
        return state.is(ModBlocks.FROST_SMITHING_TABLE.get());
    }

    private FrostSmithingRecipeInput createRecipeInput() {
        return new FrostSmithingRecipeInput(
            this.inputSlots.getItem(0),
            this.inputSlots.getItem(1),
            this.inputSlots.getItem(2)
        );
    }

    @Override
    protected boolean mayPickup(Player player, boolean hasStack) {
        return this.selectedRecipe != null
               && this.selected != -1
               && this.results != null
               && this.selectedRecipe.value().matches(this.createRecipeInput(), this.level);
    }

    @Override
    public void slotsChanged(Container inventory) {
        super.slotsChanged(inventory);
        if (inventory != this.inputSlots) return;
        if (this.inputSlots.getItem(0).isEmpty()) {
            for (int i = 1; i < 3; i++) {
                ItemStack stack = this.inputSlots.getItem(i);
                if (stack.isEmpty()) continue;
                this.inputSlots.removeItemNoUpdate(i);
                this.moveItemStackTo(stack, 4, 40, false);
                this.selectedRecipe = null;
            }
        } else if (this.inputSlots.getItem(1).isEmpty()) {
            ItemStack stack = this.inputSlots.getItem(2);
            if (stack.isEmpty()) return;
            this.inputSlots.removeItemNoUpdate(2);
            this.moveItemStackTo(stack, 4, 40, false);
            this.selectedRecipe = null;
        }
    }

    @Override
    protected void onTake(Player player, ItemStack stack) {
        stack.onCraftedBy(player.level(), player, stack.getCount());
        this.resultSlots.awardUsedRecipes(player, this.getRelevantItems());
        this.shrinkStackInSlot(2);
        this.shrinkStackInSlot(1);
        this.access.execute((level, blockPos) -> level.levelEvent(1044, blockPos, 0));
    }

    private @Unmodifiable List<ItemStack> getRelevantItems() {
        return List.of(this.inputSlots.getItem(0), this.inputSlots.getItem(1), this.inputSlots.getItem(2));
    }

    private void shrinkStackInSlot(int index) {
        ItemStack stack = this.inputSlots.getItem(index);
        if (stack.isEmpty()) return;
        stack.shrink(1);
        this.inputSlots.setItem(index, stack);
    }

    @Override
    public void createResult() {
        FrostSmithingRecipeInput input = this.createRecipeInput();

        List<RecipeHolder<PermutationRecipe>> permuts = this.level.getRecipeManager()
            .getRecipesFor(ModRecipeTypes.PERMUTATION_TYPE.get(), input, this.level);
        if (!permuts.isEmpty()) {
            RecipeHolder<PermutationRecipe> holder = permuts.getFirst();
            this.results = holder.value().inputs(input.input());
            for (RecipeResult result : this.results) {
                if (!result.result().isEnabled(this.level.enabledFeatures())) {
                    this.selectedRecipe = null;
                    this.selected = -1;
                    this.results = null;
                    this.resultSlots.setItem(0, ItemStack.EMPTY);
                    return;
                }
            }
            this.selectedRecipe = holder;
            this.selected = 0;
            this.resultSlots.setRecipeUsed(holder);
            this.resultSlots.setItem(0, this.selectedRecipe.value().assemble(this.selected, this.createRecipeInput(), this.level));
            return;
        }

        List<RecipeHolder<DeformationRecipe>> deforms = this.level.getRecipeManager()
            .getRecipesFor(ModRecipeTypes.DEFORMATION_TYPE.get(), input, this.level);
        if (!deforms.isEmpty()) {
            RecipeHolder<DeformationRecipe> holder = deforms.getFirst();
            this.results = holder.value().inputs(input.input());
            for (RecipeResult result : this.results) {
                if (!result.result().isEnabled(this.level.enabledFeatures())) {
                    this.selectedRecipe = null;
                    this.selected = -1;
                    this.results = null;
                    this.resultSlots.setItem(0, ItemStack.EMPTY);
                    return;
                }
            }
            this.selectedRecipe = holder;
            this.selected = 0;
            this.resultSlots.setRecipeUsed(holder);
            this.resultSlots.setItem(0, this.selectedRecipe.value().assemble(this.selected, this.createRecipeInput(), this.level));
            return;
        }

        this.selectedRecipe = null;
        this.selected = -1;
        this.results = null;
        this.resultSlots.setItem(0, ItemStack.EMPTY);
    }

    @Override
    public int getSlotToQuickMoveTo(ItemStack stack) {
        return this.recipes.stream()
            .map(recipe -> FrostSmithingMenu.findSlotMatchingIngredient(recipe.value(), stack))
            .findFirst()
            .filter(Optional::isPresent)
            .orElse(Optional.of(0))
            .get();
    }

    private static Optional<Integer> findSlotMatchingIngredient(IFrostSmithingRecipe recipe, ItemStack stack) {
        if (recipe.isTemplate(stack)) return Optional.of(0);
        if (recipe.isMaterial(stack)) return Optional.of(1);
        if (recipe.isInput(stack)) return Optional.of(2);
        return Optional.empty();
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != this.resultSlots && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public boolean canMoveIntoInputSlots(ItemStack stack) {
        return this.recipes.stream()
            .map(recipe -> FrostSmithingMenu.findSlotMatchingIngredient(recipe.value(), stack))
            .anyMatch(Optional::isPresent);
    }

    public void sync(int selected, List<RecipeResult> results) {
        if (this.selectedRecipe == null) return;
        this.selected = selected;
        this.results = results.isEmpty() ? this.results : results;
    }

    public void turn(boolean left) {
        if (this.selected == -1 || this.results == null) return;
        this.selected = (this.selected + (left ? -1 : 1)) % this.results.size();
        if (this.selected < 0) this.selected += this.results.size();
        this.resultSlots.setItem(0, this.selectedRecipe.value().assemble(this.selected, this.createRecipeInput(), this.level));
    }
}
