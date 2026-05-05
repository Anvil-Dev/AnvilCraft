package dev.dubhe.anvilcraft.inventory.component;

import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.util.RecipeUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 可过滤槽位，允许设置自定义过滤器
 *
 * <p>PS: 与{@link dev.dubhe.anvilcraft.item.FilterItem 过滤器（物品）}无关</p>
 */
@Getter
@Setter
public class FilteredSlot extends Slot {
    private ItemIngredientPredicate filter = RecipeUtil.EMPTY_ITEM_INGREDIENT;

    public FilteredSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return !this.isFilterEmpty() && this.filter.testIgnoreCount(stack);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean canCraft() {
        return this.isFilterEmpty() || this.filter.testCount(this.getItem().getCount());
    }

    public boolean isFilterEmpty() {
        return this.filter.equals(RecipeUtil.EMPTY_ITEM_INGREDIENT);
    }

    public void resetFilter() {
        this.filter = RecipeUtil.EMPTY_ITEM_INGREDIENT;
    }
}
