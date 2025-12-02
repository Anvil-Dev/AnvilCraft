package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.api.item.IExtraItemDisplay;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModFoodItems;
import dev.dubhe.anvilcraft.item.property.component.StoredItem;
import lombok.Getter;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Optional;

@Getter
public class CannedFoodItem extends Item implements IExtraItemDisplay {
    public CannedFoodItem(Properties properties) {
        super(properties);
    }

    @Override
    public void verifyComponentsAfterLoad(ItemStack stack) {
        if (!stack.has(ModComponents.DISPLAY_ITEM)) {
            this.setFood(stack, ModFoodItems.BEEF_MUSHROOM_STEW.asStack());
        }
        super.verifyComponentsAfterLoad(stack);
    }

    @Override
    public ItemStack getDisplayedItem(ItemStack stack) {
        return Optional.ofNullable(stack.get(ModComponents.DISPLAY_ITEM))
            .map(StoredItem::stored)
            .orElse(ItemStack.EMPTY);
    }

    public ItemStack setFood(ItemStack canStack, ItemStack foodStack) {
        ItemStack displayStack = foodStack.copy();
        if (displayStack.has(DataComponents.RARITY)) {
            canStack.set(DataComponents.RARITY, displayStack.get(DataComponents.RARITY));
        }
        canStack.set(ModComponents.DISPLAY_ITEM, new StoredItem(displayStack));
        FoodProperties copiedFood = displayStack.getFoodProperties(null);
        if (copiedFood != null) {
            int nutrition = copiedFood.nutrition();
            float magnification = switch (foodStack.getCount()) {
                case 1 -> 1;
                case 2 -> 1.8f;
                case 3 -> 2.4f;
                case 4 -> 2.8f;
                case 5 -> 3;
                default -> throw new IndexOutOfBoundsException(foodStack.getCount());
            };
            canStack.set(DataComponents.FOOD, new FoodProperties(
                (int) (nutrition * magnification),
                copiedFood.saturation() * magnification,
                false,
                0.8f,
                Optional.empty(),
                List.of()
            ));
        }
        return canStack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        StoredItem foodInfo = stack.getOrDefault(ModComponents.DISPLAY_ITEM, new StoredItem(ItemStack.EMPTY));
        ItemStack food = foodInfo.stored();
        if (!food.isEmpty()) {
            if (food.getCount() == 1) {
                tooltipComponents.add(food.getHoverName());
            } else {
                tooltipComponents.add(food.getHoverName().copy().append(" x").append(String.valueOf(food.getCount())));
            }
        }
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
        return 0.5f;
    }
}
