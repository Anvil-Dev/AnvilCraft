package dev.dubhe.anvilcraft.inventory;

import com.google.common.collect.ImmutableList;
import dev.dubhe.anvilcraft.api.recipe.result.RecipeResult;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.frost.DeformationRecipe;
import dev.dubhe.anvilcraft.recipe.frost.FrostSmithingOption;
import dev.dubhe.anvilcraft.recipe.frost.FrostSmithingRecipeInput;
import dev.dubhe.anvilcraft.recipe.frost.IFrostSmithingRecipe;
import dev.dubhe.anvilcraft.recipe.frost.PermutationRecipe;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
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
import javax.annotation.Nullable;

public class FrostSmithingMenu extends AdjacentSmithingMenu {
    private final Level level;

    private final List<RecipeHolder<? extends IFrostSmithingRecipe>> recipes;

    private @Nullable RecipeHolder<? extends IFrostSmithingRecipe> selectedRecipe = null;
    public int selected = -1;
    public @Nullable List<RecipeResult> results = null;
    private @Nullable List<FrostSmithingOption> options = null;

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
                IFrostSmithingRecipe.TEMPLATE_SLOT,
                8,
                48,
                stack -> this.recipes.stream().anyMatch(recipe -> recipe.value().isTemplate(stack))
            ).withSlot(
                IFrostSmithingRecipe.INPUT_SLOT,
                44,
                48,
                stack -> this.hasTemplateForPlacement()
                         && this.recipes.stream()
                             .anyMatch(recipe -> recipe.value().isTemplate(this.template())
                                                 && recipe.value().isInput(stack))
            ).withSlot(
                IFrostSmithingRecipe.MATERIAL_SLOT,
                62,
                48,
                stack -> this.hasTemplateForPlacement()
                         && this.recipes.stream()
                             .anyMatch(recipe -> recipe.value().isTemplate(this.template())
                                                 && recipe.value().acceptsMaterial(this.createRecipeInput(), stack))
            ).withResultSlot(3, 106, 48)
            .build();
    }

    private ItemStack template() {
        return this.inputSlots.getItem(IFrostSmithingRecipe.TEMPLATE_SLOT);
    }

    private ItemStack input() {
        return this.inputSlots.getItem(IFrostSmithingRecipe.INPUT_SLOT);
    }

    private ItemStack material() {
        return this.inputSlots.getItem(IFrostSmithingRecipe.MATERIAL_SLOT);
    }

    private boolean hasTemplateForPlacement() {
        return this.isRecipeTransferInProgress() || !this.template().isEmpty();
    }

    @Override
    protected boolean isUsableTemplate(ItemStack stack) {
        return this.recipes.stream().anyMatch(recipe -> recipe.value().isTemplate(stack));
    }

    @Override
    protected boolean isValidBlock(BlockState state) {
        return state.is(ModBlocks.FROST_SMITHING_TABLE.get());
    }

    private FrostSmithingRecipeInput createRecipeInput() {
        return new FrostSmithingRecipeInput(this.template(), this.input(), this.material());
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
        if (this.isRecipeTransferInProgress()) return;
        super.slotsChanged(inventory);
        if (inventory != this.inputSlots) return;
        this.selectedRecipe = null;
        this.createResult();
    }

    @Override
    protected void onTake(Player player, ItemStack stack) {
        // 必须在消耗装备前计算，消耗装备会立刻重算结果并清空当前选择
        final int cost = this.selectedRecipe == null || this.selected < 0 || this.options == null || this.selected >= this.options.size()
                         ? 0
                         : this.options.get(this.selected).cost(this.selectedRecipe.value(), this.createRecipeInput());
        stack.onCraftedBy(player.level(), player, stack.getCount());
        this.resultSlots.awardUsedRecipes(player, this.getRelevantItems());
        this.shrinkStackInSlot(IFrostSmithingRecipe.INPUT_SLOT, 1);
        this.shrinkStackInSlot(IFrostSmithingRecipe.MATERIAL_SLOT, cost);
        this.access.execute((level, blockPos) -> level.levelEvent(1044, blockPos, 0));
    }

    private @Unmodifiable List<ItemStack> getRelevantItems() {
        return List.of(this.template(), this.input(), this.material());
    }

    private void shrinkStackInSlot(int index, int count) {
        if (count <= 0) return;
        ItemStack stack = this.inputSlots.getItem(index);
        if (stack.isEmpty()) return;
        stack.shrink(count);
        this.inputSlots.setItem(index, stack);
    }

    @Override
    public void createResult() {
        FrostSmithingRecipeInput input = this.createRecipeInput();

        List<RecipeHolder<PermutationRecipe>> permuts = this.level.getRecipeManager()
            .getRecipesFor(ModRecipeTypes.PERMUTATION_TYPE.get(), input, this.level);
        if (!permuts.isEmpty()) {
            this.setupResult(permuts.getFirst(), input);
            return;
        }

        List<RecipeHolder<DeformationRecipe>> deforms = this.level.getRecipeManager()
            .getRecipesFor(ModRecipeTypes.DEFORMATION_TYPE.get(), input, this.level);
        if (!deforms.isEmpty()) {
            this.setupResult(deforms.getFirst(), input);
            return;
        }

        this.clearResult();
    }

    private void setupResult(RecipeHolder<? extends IFrostSmithingRecipe> holder, FrostSmithingRecipeInput input) {
        List<FrostSmithingOption> available = holder.value().options(input);
        for (FrostSmithingOption option : available) {
            if (!option.result().result().isEnabled(this.level.enabledFeatures())) {
                this.clearResult();
                return;
            }
        }
        this.selectedRecipe = holder;
        this.options = available;
        this.results = available.stream().map(FrostSmithingOption::result).toList();
        this.selected = 0;
        this.resultSlots.setRecipeUsed(holder);
        this.resultSlots.setItem(0, holder.value().assemble(this.selected, input, this.level));
    }

    private void clearResult() {
        this.selectedRecipe = null;
        this.selected = -1;
        this.results = null;
        this.options = null;
        this.resultSlots.setItem(0, ItemStack.EMPTY);
    }

    @Override
    public int getSlotToQuickMoveTo(ItemStack stack) {
        return this.recipes.stream()
            .map(recipe -> this.findSlotMatchingIngredient(recipe.value(), stack))
            .filter(Optional::isPresent)
            .findFirst()
            .orElse(Optional.of(IFrostSmithingRecipe.TEMPLATE_SLOT))
            .orElseThrow(); // isPresent filter + of construct
    }

    private Optional<Integer> findSlotMatchingIngredient(IFrostSmithingRecipe recipe, ItemStack stack) {
        if (recipe.isTemplate(stack)) return Optional.of(IFrostSmithingRecipe.TEMPLATE_SLOT);
        if (recipe.isInput(stack)) return Optional.of(IFrostSmithingRecipe.INPUT_SLOT);
        if (recipe.acceptsMaterial(this.createRecipeInput(), stack)) return Optional.of(IFrostSmithingRecipe.MATERIAL_SLOT);
        return Optional.empty();
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != this.resultSlots && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public boolean canMoveIntoInputSlots(ItemStack stack) {
        if (this.template().isEmpty()) return false;
        return this.recipes.stream()
            .map(recipe -> this.findSlotMatchingIngredient(recipe.value(), stack))
            .anyMatch(Optional::isPresent);
    }

    public void turn(boolean left) {
        if (this.selected == -1 || this.options == null || this.options.isEmpty() || this.selectedRecipe == null) return;
        this.selected = Math.floorMod(this.selected + (left ? -1 : 1), this.options.size());
        this.resultSlots.setItem(0, this.selectedRecipe.value().assemble(this.selected, this.createRecipeInput(), this.level));
    }
}
