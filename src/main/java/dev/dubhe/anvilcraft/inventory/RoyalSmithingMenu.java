package dev.dubhe.anvilcraft.inventory;

import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ItemCombinerMenuSlotDefinition;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeAccess;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("unused")
public class RoyalSmithingMenu extends AdjacentSmithingMenu {
    private final Level level;
    private final RecipePropertySet baseItemTest;
    private final RecipePropertySet templateItemTest;
    private final RecipePropertySet additionItemTest;
    private final DataSlot hasRecipeError = DataSlot.standalone();

    public RoyalSmithingMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public RoyalSmithingMenu(MenuType<RoyalSmithingMenu> type, int containerId, Inventory inventory) {
        this(type, containerId, inventory, ContainerLevelAccess.NULL, inventory.player.level());
    }

    public RoyalSmithingMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        this(ModMenuTypes.ROYAL_SMITHING.get(), containerId, inventory, access, inventory.player.level());
    }

    private RoyalSmithingMenu(
        MenuType<RoyalSmithingMenu> type,
        int containerId,
        Inventory inventory,
        ContainerLevelAccess access,
        Level level
    ) {
        super(type, containerId, inventory, access, RoyalSmithingMenu.createInputSlotDefinitions(level.recipeAccess()));
        this.level = level;
        this.baseItemTest = level.recipeAccess().propertySet(RecipePropertySet.SMITHING_BASE);
        this.templateItemTest = level.recipeAccess().propertySet(RecipePropertySet.SMITHING_TEMPLATE);
        this.additionItemTest = level.recipeAccess().propertySet(RecipePropertySet.SMITHING_ADDITION);
        this.addDataSlot(this.hasRecipeError).set(0);
    }

    private static ItemCombinerMenuSlotDefinition createInputSlotDefinitions(RecipeAccess recipes) {
        RecipePropertySet baseItemTest = recipes.propertySet(RecipePropertySet.SMITHING_BASE);
        RecipePropertySet templateItemTest = recipes.propertySet(RecipePropertySet.SMITHING_TEMPLATE);
        RecipePropertySet additionItemTest = recipes.propertySet(RecipePropertySet.SMITHING_ADDITION);
        return ItemCombinerMenuSlotDefinition.create()
            .withSlot(0, 8, 48, templateItemTest::test)
            .withSlot(1, 44, 48, baseItemTest::test)
            .withSlot(2, 62, 48, additionItemTest::test)
            .withResultSlot(3, 106, 48)
            .build();
    }

    @Override
    protected boolean isUsableTemplate(ItemStack stack) {
        return this.templateItemTest.test(stack);
    }

    @Override
    protected boolean isValidBlock(BlockState state) {
        return state.is(ModBlocks.ROYAL_SMITHING_TABLE);
    }

    @Override
    protected void onTake(Player player, ItemStack carried) {
        carried.onCraftedBy(player, carried.getCount());
        this.resultSlots.awardUsedRecipes(player, this.getRelevantItems());
        this.shrinkStackInSlot(1);
        this.shrinkStackInSlot(2);
        this.access.execute((level, pos) -> level.levelEvent(1044, pos, 0));
    }

    private List<ItemStack> getRelevantItems() {
        return List.of(this.inputSlots.getItem(0), this.inputSlots.getItem(1), this.inputSlots.getItem(2));
    }

    private SmithingRecipeInput createRecipeInput() {
        return new SmithingRecipeInput(this.inputSlots.getItem(0), this.inputSlots.getItem(1), this.inputSlots.getItem(2));
    }

    private void shrinkStackInSlot(int slot) {
        ItemStack stack = this.inputSlots.getItem(slot);
        if (!stack.isEmpty()) {
            stack.shrink(1);
            this.inputSlots.setItem(slot, stack);
        }
    }

    @Override
    public void slotsChanged(Container container) {
        super.slotsChanged(container);
        if (this.level instanceof ServerLevel) {
            boolean hasRecipeError = this.getSlot(0).hasItem()
                                     && this.getSlot(1).hasItem()
                                     && this.getSlot(2).hasItem()
                                     && !this.getSlot(this.getResultSlot()).hasItem();
            this.hasRecipeError.set(hasRecipeError ? 1 : 0);
        }
    }

    @Override
    public void createResult() {
        SmithingRecipeInput input = this.createRecipeInput();
        Optional<RecipeHolder<SmithingRecipe>> foundRecipe;
        if (this.level instanceof ServerLevel serverLevel) {
            foundRecipe = serverLevel.recipeAccess().getRecipeFor(RecipeType.SMITHING, input, serverLevel);
        } else {
            foundRecipe = Optional.empty();
        }

        foundRecipe.ifPresentOrElse(recipe -> {
            ItemStack result = recipe.value().assemble(input);
            this.resultSlots.setRecipeUsed(recipe);
            this.resultSlots.setItem(0, result);
        }, () -> {
            this.resultSlots.setRecipeUsed(null);
            this.resultSlots.setItem(0, ItemStack.EMPTY);
        });
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
        return target.container != this.resultSlots && super.canTakeItemForPickAll(carried, target);
    }

    @Override
    public boolean canMoveIntoInputSlots(ItemStack stack) {
        if (this.templateItemTest.test(stack) && !this.getSlot(0).hasItem()) {
            return true;
        } else {
            return this.baseItemTest.test(stack) && !this.getSlot(1).hasItem()
                   || this.additionItemTest.test(stack) && !this.getSlot(2).hasItem();
        }
    }

    public boolean hasRecipeError() {
        return this.hasRecipeError.get() > 0;
    }
}
