package dev.dubhe.anvilcraft.item.food;

import dev.dubhe.anvilcraft.api.item.IExtraItemDisplay;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.StoredItem;
import lombok.Getter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

@Getter
public class CannedFoodItem extends Item implements IExtraItemDisplay {
    public CannedFoodItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack getDisplayedItem(ItemStack stack) {
        StoredItem displayItem = stack.get(ModComponents.DISPLAY_ITEM);
        return displayItem != null ? displayItem.stored() : ItemStack.EMPTY;
    }

    public ItemStack setFood(ItemStack canStack, ItemStack foodStack) {
        ItemStack displayStack = foodStack.copy();
        if (displayStack.has(DataComponents.RARITY)) {
            canStack.set(DataComponents.RARITY, displayStack.get(DataComponents.RARITY));
        }
        canStack.set(ModComponents.DISPLAY_ITEM, new StoredItem(displayStack));
        FoodProperties copiedFood = displayStack.get(DataComponents.FOOD);
        if (copiedFood != null) {
            int nutrition = copiedFood.nutrition();
            float magnification = switch (foodStack.getCount()) {
                case 1 -> 1;
                case 2 -> 1.8F;
                case 3 -> 2.4F;
                case 4 -> 2.8F;
                case 5 -> 3;
                default -> throw new IndexOutOfBoundsException(foodStack.getCount());
            };
            canStack.set(DataComponents.FOOD, new FoodProperties(
                (int) (nutrition * magnification),
                copiedFood.saturation() * magnification,
                false
            ));
        }
        return canStack;
    }

    @Override
    public int offsetX(ItemStack stack) {
        return 5;
    }

    @Override
    public int offsetY(ItemStack stack) {
        return 2;
    }

    @Override
    public float scale(ItemStack stack) {
        return 0.5F;
    }
}
