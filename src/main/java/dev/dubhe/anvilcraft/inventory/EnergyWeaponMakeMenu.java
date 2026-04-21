package dev.dubhe.anvilcraft.inventory;

import dev.anvilcraft.lib.v2.util.ListUtil;
import dev.anvilcraft.lib.v2.util.Util;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.init.ModMenuTypes;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.inventory.component.FilteredSlot;
import dev.dubhe.anvilcraft.recipe.EnergyWeaponMakeRecipe;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Range;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
public class EnergyWeaponMakeMenu extends AbstractContainerMenu {
    private final Level level;
    private final Inventory inventory;
    private final boolean[] usedHand = new boolean[2]; // 序号0指示主手，序号1指示副手
    private final List<RecipeHolder<EnergyWeaponMakeRecipe>> recipes;
    private final Container inputContainer = new SimpleContainer(6);
    private int selectedIndex = -1;
    /**
     * 供客户端展示错误槽位用
     */
    @Setter
    private boolean cantCraft;

    public EnergyWeaponMakeMenu(int containerId, Inventory playerInventory) {
        this(ModMenuTypes.ENERGY_WEAPON_MAKE.get(), containerId, playerInventory);
    }

    /**
     * 能量武器平台菜单
     *
     * @param type            类型
     * @param containerId     容器id
     * @param playerInventory 背包
     */
    public EnergyWeaponMakeMenu(
        MenuType<EnergyWeaponMakeMenu> type,
        int containerId,
        Inventory playerInventory
    ) {
        super(type, containerId);
        this.level = playerInventory.player.level();
        this.inventory = playerInventory;
        this.recipes = this.level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.ENERGY_WEAPON_MAKE_TYPE.get());

        if (playerInventory.player.getMainHandItem().is(ModItems.ENERGY_WEAPON_PLATFORM)) this.usedHand[0] = true;
        if (playerInventory.player.getOffhandItem().is(ModItems.ENERGY_WEAPON_PLATFORM)) this.usedHand[1] = true;

        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < 3; column++) {
                this.addSlot(new FilteredSlot(
                    this.inputContainer,
                    column + row * 3,
                    89 + column * 18,
                    25 + row * 18
                ));
            }
        }

        int row;
        for (row = 0; row < 3; ++row) {
            for (int column = 0; column < 9; ++column) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }

        for (row = 0; row < 9; ++row) {
            this.addSlot(new Slot(playerInventory, row, 8 + row * 18, 142));
        }
    }

    public void make(Player player) {
        if (this.selectedIndex == -1) {
            this.cantCraft = true;
            return;
        }

        for (int i = 0; i < 6; i++) {
            if (!this.getFilteredSlot(i).canCraft()) {
                this.cantCraft = true;
                return;
            }
        }

        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Slot slot = this.getSlot(i);
            if (!slot.hasItem()) continue;
            stacks.add(slot.getItem());
        }
        if (stacks.isEmpty()) {
            this.cantCraft = true;
            return;
        }
        EnergyWeaponMakeRecipe.Input input = new EnergyWeaponMakeRecipe.Input(stacks);

        Optional<RecipeHolder<EnergyWeaponMakeRecipe>> recipeOp = ListUtil.safelyGet(this.recipes, this.selectedIndex);
        if (recipeOp.isEmpty()) {
            this.cantCraft = true;
            return;
        }
        EnergyWeaponMakeRecipe recipe = recipeOp.get().value();
        ItemStack result = recipe.assemble(input, this.level.registryAccess());

        if (this.usedHand[0] && player.getMainHandItem().is(ModItems.ENERGY_WEAPON_PLATFORM)) {
            player.setItemSlot(EquipmentSlot.MAINHAND, result);
        } else if (this.usedHand[1] && player.getOffhandItem().is(ModItems.ENERGY_WEAPON_PLATFORM)) {
            player.setItemSlot(EquipmentSlot.OFFHAND, result);
        } else {
            throw new IllegalStateException("How did you have more than two hands?!");
        }

        for (int i = 0; i < 6; i++) {
            FilteredSlot slot = Util.cast(this.getSlot(i));
            if (slot.isFilterEmpty()) continue;
            ItemStack stack = slot.getItem().copy();
            stack.shrink(slot.getFilter().count());
            slot.set(stack);
            slot.resetFilter();
        }
        if (player instanceof ServerPlayer serverside) serverside.closeContainer();
    }

    public void setSelectedIndex(int selectedIndex) {
        this.selectedIndex = selectedIndex;

        if (selectedIndex == -1) {
            for (int i = 0; i < 6; i++) {
                FilteredSlot slot = this.getFilteredSlot(i);
                slot.resetFilter();
                ItemStack stack = slot.getItem();
                if (stack.isEmpty()) continue;
                this.inventory.add(stack);
            }
        } else {
            RecipeHolder<EnergyWeaponMakeRecipe> recipe = this.recipes.get(selectedIndex);
            List<ItemIngredientPredicate> ingredients = recipe.value().ingredients();
            for (int i = 0; i < ingredients.size(); i++) {
                ItemIngredientPredicate ingredient = ingredients.get(i);
                this.getFilteredSlot(i).setFilter(ingredient);
            }
        }
    }

    @Override
    public void removed(Player player) {
        super.removed(player);

        for (int i = 0; i < 6; i++) {
            ItemStack stack = this.getSlot(i).getItem();
            if (stack.isEmpty()) continue;
            player.addItem(stack);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack stack;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack clickedItem = slot.getItem();
        stack = clickedItem.copy();
        if (index >= 0 && index < 6) {
            if (!this.moveItemStackTo(stack, 6, 42, false)) return ItemStack.EMPTY;
            int surplus = clickedItem.getCount() - clickedItem.getMaxStackSize();
            stack = surplus > 0 ? clickedItem.copyWithCount(surplus) : ItemStack.EMPTY;
            this.getSlot(index).setByPlayer(stack);
        } else {
            if (this.selectedIndex == -1) return ItemStack.EMPTY;

            for (int i = 0; i < 6; i++) {
                Slot destSlot = this.getSlot(i);
                if (!destSlot.mayPlace(stack)) continue;
                if (!destSlot.hasItem()) {
                    destSlot.setByPlayer(stack);
                    slot.setByPlayer(ItemStack.EMPTY);
                } else {
                    ItemStack dest = destSlot.getItem();
                    if (!ItemStack.isSameItemSameComponents(dest, stack)) continue;
                    if (dest.getCount() >= dest.getMaxStackSize()) continue;

                    int canSet = dest.getMaxStackSize() - dest.getCount();
                    canSet = Math.min(stack.getCount(), canSet);
                    dest.grow(canSet);
                    stack.shrink(canSet);
                    destSlot.setByPlayer(dest);
                    this.getSlot(index).setByPlayer(stack);
                }
                return ItemStack.EMPTY;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.usedHand[0] && player.getMainHandItem().is(ModItems.ENERGY_WEAPON_PLATFORM)
               || this.usedHand[1] && player.getOffhandItem().is(ModItems.ENERGY_WEAPON_PLATFORM);
    }

    public FilteredSlot getFilteredSlot(@Range(from = 0, to = 5) int index) {
        return Util.cast(this.getSlot(index));
    }

    public boolean canScroll() {
        return this.recipes.size() > 6;
    }
}
