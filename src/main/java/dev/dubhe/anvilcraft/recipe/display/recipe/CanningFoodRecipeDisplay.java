package dev.dubhe.anvilcraft.recipe.display.recipe;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.Lazy;
import dev.dubhe.anvilcraft.init.item.ModFoodItems;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.recipe.display.slot.CannedFoodSlotDemo;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.neoforged.neoforge.common.Tags;

public record CanningFoodRecipeDisplay(
    SlotDisplay can,
    SlotDisplay foods,
    SlotDisplay result,
    SlotDisplay craftingStation
) implements RecipeDisplay {
    public static final Lazy<CanningFoodRecipeDisplay> INSTANCE = new Lazy<>(() -> {
        SlotDisplay.TagSlotDisplay foods = new SlotDisplay.TagSlotDisplay(Tags.Items.FOODS);
        return new CanningFoodRecipeDisplay(
            new SlotDisplay.ItemSlotDisplay(ModItems.TIN_CAN),
            foods,
            new CannedFoodSlotDemo(foods, new SlotDisplay.ItemSlotDisplay(ModFoodItems.CANNED_FOOD)),
            new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)
        );
    });
    public static final RecipeDisplay.Type<CanningFoodRecipeDisplay> TYPE = new Type<>(
        RecordCodecBuilder.mapCodec(inst -> inst.group(
            SlotDisplay.CODEC
                .fieldOf("can")
                .forGetter(CanningFoodRecipeDisplay::can),
            SlotDisplay.CODEC
                .fieldOf("foods")
                .forGetter(CanningFoodRecipeDisplay::foods),
            SlotDisplay.CODEC
                .fieldOf("result")
                .forGetter(CanningFoodRecipeDisplay::result),
            SlotDisplay.CODEC
                .fieldOf("craftingStation")
                .forGetter(CanningFoodRecipeDisplay::craftingStation)
        ).apply(inst, CanningFoodRecipeDisplay::new)),
        StreamCodec.composite(
            SlotDisplay.STREAM_CODEC,
            CanningFoodRecipeDisplay::can,
            SlotDisplay.STREAM_CODEC,
            CanningFoodRecipeDisplay::foods,
            SlotDisplay.STREAM_CODEC,
            CanningFoodRecipeDisplay::result,
            SlotDisplay.STREAM_CODEC,
            CanningFoodRecipeDisplay::craftingStation,
            CanningFoodRecipeDisplay::new
        )
    );

    @Override
    public Type<CanningFoodRecipeDisplay> type() {
        return CanningFoodRecipeDisplay.TYPE;
    }
}
