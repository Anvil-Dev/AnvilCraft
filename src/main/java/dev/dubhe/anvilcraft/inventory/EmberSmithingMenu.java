package dev.dubhe.anvilcraft.inventory;

import dev.anvilcraft.lib.v2.util.ListUtil;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.item.template.mto.BaseMultipleToOneTemplateItem;
import dev.dubhe.anvilcraft.recipe.multiple.BaseMultipleToOneSmithingRecipe;
import dev.dubhe.anvilcraft.recipe.multiple.MultipleToOneSmithingRecipeInput;
import dev.dubhe.anvilcraft.recipe.sync.RecipesRecord;
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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class EmberSmithingMenu extends ItemCombinerMenu {
    private final Level level;

    @Nullable
    private RecipeHolder<BaseMultipleToOneSmithingRecipe> selectedRecipe;

    private final List<RecipeHolder<BaseMultipleToOneSmithingRecipe>> recipes;

    public EmberSmithingMenu(MenuType<EmberSmithingMenu> type, int containerId, Inventory playerInventory) {
        this(type, containerId, playerInventory, ContainerLevelAccess.NULL);
    }

    public EmberSmithingMenu(int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        this(ModMenuTypes.EMBER_SMITHING.get(), containerId, playerInventory, access);
    }

    /**
     * 余烬锻造台菜单
     *
     * @param type            类型
     * @param containerId     容器id
     * @param playerInventory 背包
     * @param access          检查
     */
    public EmberSmithingMenu(MenuType<EmberSmithingMenu> type, int containerId, Inventory playerInventory, ContainerLevelAccess access) {
        List<RecipeHolder<BaseMultipleToOneSmithingRecipe>> recipes = List.copyOf(
            RecipesRecord.RECIPES.byType(ModRecipeTypes.MULTIPLE_TO_ONE_SMITHING.get())
        );
        super(type, containerId, playerInventory, access, EmberSmithingMenu.createInputSlotDefinitions(recipes));
        this.level = playerInventory.player.level();
        this.recipes = recipes;
    }

    protected static ItemCombinerMenuSlotDefinition createInputSlotDefinitions(
        List<RecipeHolder<BaseMultipleToOneSmithingRecipe>> recipes
    ) {
        return ItemCombinerMenuSlotDefinition.create().withSlot(
            0,
            8,
            48,
            stack -> recipes.stream().anyMatch(recipe -> recipe.value().isTemplateIngredient(stack))
        ).withSlot(
            1,
            80,
            36,
            stack -> recipes.stream().anyMatch(recipe -> recipe.value().isMaterialIngredient(stack))
        ).withSlot(
            2,
            80,
            18,
            stack -> recipes.stream().anyMatch(recipe -> recipe.value().isInputIngredient(0, stack))
        ).withSlot(
            3,
            80,
            54,
            stack -> recipes.stream().anyMatch(recipe -> recipe.value().isInputIngredient(1, stack))
        ).withSlot(
            4,
            62,
            36,
            stack -> recipes.stream().anyMatch(recipe -> recipe.value().isInputIngredient(2, stack))
        ).withSlot(
            5,
            98,
            36,
            stack -> recipes.stream().anyMatch(recipe -> recipe.value().isInputIngredient(3, stack))
        ).withSlot(
            6,
            62,
            18,
            stack -> recipes.stream().anyMatch(recipe -> recipe.value().isInputIngredient(4, stack))
        ).withSlot(
            7,
            98,
            18,
            stack -> recipes.stream().anyMatch(recipe -> recipe.value().isInputIngredient(5, stack))
        ).withSlot(
            8,
            62,
            54,
            stack -> recipes.stream().anyMatch(recipe -> recipe.value().isInputIngredient(6, stack))
        ).withSlot(
            9,
            98,
            54,
            stack -> recipes.stream().anyMatch(recipe -> recipe.value().isInputIngredient(7, stack))
        ).withResultSlot(10, 151, 48).build();
    }

    @Override
    protected boolean isValidBlock(BlockState state) {
        return state.is(ModBlocks.EMBER_SMITHING_TABLE.get());
    }

    @Override
    protected void onTake(Player player, ItemStack stack) {
        stack.onCraftedBy(player, stack.getCount());
        this.resultSlots.awardUsedRecipes(player, this.getRelevantItems());
        for (int i = 2; i < 10; i++) {
            this.shrinkStackInSlot(i);
        }
        this.shrinkStackInSlot(1);
        this.access.execute((level, blockPos) -> level.levelEvent(1044, blockPos, 0));
    }

    private @Unmodifiable List<ItemStack> getRelevantItems() {
        return ListUtil.createWithValues(10, this.inputSlots::getItem);
    }

    private MultipleToOneSmithingRecipeInput createRecipeInput() {
        ItemStack[] inputs = new ItemStack[this.getInputSize()];
        for (int i = 0; i < this.getInputSize(); i++) {
            inputs[i] = this.inputSlots.getItem(Math.min(i + 2, 9));
        }
        return new MultipleToOneSmithingRecipeInput(
            this.inputSlots.getItem(0),
            this.inputSlots.getItem(1),
            inputs
        );
    }

    private void shrinkStackInSlot(int index) {
        ItemStack stack = this.inputSlots.getItem(index);
        if (!stack.isEmpty()) {
            stack.shrink(1);
            this.inputSlots.setItem(index, stack);
        }
    }

    @Override
    public void slotsChanged(Container inventory) {
        super.slotsChanged(inventory);
        if (inventory == this.inputSlots) {
            if (this.inputSlots.getItem(0).isEmpty()) {
                for (int i = 1; i < 10; i++) {
                    ItemStack stack = this.inputSlots.getItem(i);
                    if (!stack.isEmpty()) {
                        this.inputSlots.removeItemNoUpdate(i);
                        this.moveItemStackTo(stack, 11, 47, false);
                    }
                }
            } else if (this.inputSlots.getItem(1).isEmpty()) {
                for (int i = 2; i < 10; i++) {
                    ItemStack stack = this.inputSlots.getItem(i);
                    if (!stack.isEmpty()) {
                        this.inputSlots.removeItemNoUpdate(i);
                        this.moveItemStackTo(stack, 11, 47, false);
                    }
                }
            }
        }
    }

    @Override
    public void createResult() {
        if (!this.canCreateResult()) {
            this.resultSlots.setItem(0, ItemStack.EMPTY);
            return;
        }
        MultipleToOneSmithingRecipeInput input = this.createRecipeInput();
        List<RecipeHolder<BaseMultipleToOneSmithingRecipe>> list =
            RecipesRecord.RECIPES.getRecipesFor(ModRecipeTypes.MULTIPLE_TO_ONE_SMITHING.get(), input, this.level).toList();
        if (list.isEmpty()) {
            this.resultSlots.setItem(0, ItemStack.EMPTY);
        } else {
            RecipeHolder<BaseMultipleToOneSmithingRecipe> recipe = list.getFirst();
            ItemStack stack = recipe.value().assemble(input, this.level);
            if (stack.isItemEnabled(this.level.enabledFeatures())) {
                this.selectedRecipe = recipe;
                this.resultSlots.setRecipeUsed(recipe);
                this.resultSlots.setItem(0, stack);
            }
        }
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != this.resultSlots && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public boolean canMoveIntoInputSlots(ItemStack stack) {
        return this.recipes.stream()
            .anyMatch(recipe -> this.isMatchingRecipe(recipe.value(), stack));
    }

    @Override
    protected boolean mayPickup(Player player, boolean hasStack) {
        return this.selectedRecipe != null && this.selectedRecipe.value().matches(this.createRecipeInput(), this.level);
    }

    public int getInputSize() {
        ItemStack stack = this.inputSlots.getItem(0);
        if (stack.getItem() instanceof BaseMultipleToOneTemplateItem template) {
            return template.getSize();
        } else {
            return 0;
        }
    }

    public List<ItemStack> getInputStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 2; i < this.inputSlots.getContainerSize(); i++) {
            stacks.add(this.inputSlots.getItem(i));
        }
        return stacks;
    }

    private boolean isMatchingRecipe(
        BaseMultipleToOneSmithingRecipe recipe,
        ItemStack stack
    ) {
        if (recipe.isTemplateIngredient(stack)) return this.getSlot(0).hasItem();
        if (recipe.isMaterialIngredient(stack)) return recipe.isTemplateIngredient(this.getSlot(0).getItem());
        if (!recipe.isTemplateIngredient(this.getSlot(0).getItem()) || !recipe.isMaterialIngredient(this.getSlot(1).getItem())) {
            return false;
        }
        for (int i = 0; i < 8; i++) {
            if (recipe.isInputIngredient(i, stack)) return true;
        }
        return false;
    }

    public boolean canCreateResult() {
        if (!this.getSlot(0).hasItem() || !this.getSlot(1).hasItem()) return false;
        ItemStack template = this.getSlot(0).getItem();
        if (template.getItem() instanceof BaseMultipleToOneTemplateItem templateItem) {
            for (int i = 2; i < 2 + templateItem.getSize(); i++) {
                if (!this.getSlot(i).hasItem()) return false;
            }
        }
        return true;
    }
}
