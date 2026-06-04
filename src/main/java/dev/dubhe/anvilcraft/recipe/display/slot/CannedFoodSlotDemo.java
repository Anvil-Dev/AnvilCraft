package dev.dubhe.anvilcraft.recipe.display.slot;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.StoredItem;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.DisplayContentsFactory;
import net.minecraft.world.item.crafting.display.SlotDisplay;

import java.util.stream.Stream;

public record CannedFoodSlotDemo(SlotDisplay foods, SlotDisplay cannedFood) implements SlotDisplay {
    public static final SlotDisplay.Type<CannedFoodSlotDemo> TYPE = new SlotDisplay.Type<>(
        RecordCodecBuilder.mapCodec(inst -> inst.group(
            SlotDisplay.CODEC
                .fieldOf("foods")
                .forGetter(CannedFoodSlotDemo::foods),
            SlotDisplay.CODEC
                .fieldOf("cannedFood")
                .forGetter(CannedFoodSlotDemo::cannedFood)
        ).apply(inst, CannedFoodSlotDemo::new)),
        StreamCodec.composite(
            SlotDisplay.STREAM_CODEC,
            CannedFoodSlotDemo::foods,
            SlotDisplay.STREAM_CODEC,
            CannedFoodSlotDemo::cannedFood,
            CannedFoodSlotDemo::new
        )
    );

    @Override
    public <T> Stream<T> resolve(ContextMap context, DisplayContentsFactory<T> builder) {
        return SlotDisplay.applyDemoTransformation(context, builder, this.cannedFood, this.foods, CannedFoodSlotDemo::canFood);
    }

    private static ItemStack canFood(ItemStack can, ItemStack food) {
        ItemStack result = can.copy();
        result.set(ModComponents.DISPLAY_ITEM, new StoredItem(food.copy()));
        return result;
    }

    @Override
    public Type<CannedFoodSlotDemo> type() {
        return CannedFoodSlotDemo.TYPE;
    }
}
