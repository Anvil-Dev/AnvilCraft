package dev.dubhe.anvilcraft.recipe.display.recipe;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.Lazy;
import dev.dubhe.anvilcraft.init.item.ModFoodItems;
import dev.dubhe.anvilcraft.recipe.display.slot.PillSlotDemo;
import dev.dubhe.anvilcraft.recipe.display.slot.WithAnyPotionsExcept;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;

import java.util.List;

public record PillRecipeDisplay(
    SlotDisplay pill,
    SlotDisplay potions,
    SlotDisplay result,
    SlotDisplay craftingStation
) implements RecipeDisplay {
    public static final Lazy<PillRecipeDisplay> INSTANCE = new Lazy<>(() -> {
        SlotDisplay.ItemSlotDisplay pill = new SlotDisplay.ItemSlotDisplay(ModFoodItems.PILL);
        WithAnyPotionsExcept potions = new WithAnyPotionsExcept(
            new SlotDisplay.Composite(List.of(
                new SlotDisplay.ItemSlotDisplay(Items.POTION),
                new SlotDisplay.ItemSlotDisplay(Items.SPLASH_POTION),
                new SlotDisplay.ItemSlotDisplay(Items.LINGERING_POTION)
            )),
            List.of(
                Potions.WATER.key().identifier(),
                Potions.MUNDANE.key().identifier(),
                Potions.THICK.key().identifier(),
                Potions.AWKWARD.key().identifier()
            )
        );
        return new PillRecipeDisplay(
            pill,
            potions,
            new PillSlotDemo(potions, pill),
            new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)
        );
    });
    public static final Type<PillRecipeDisplay> TYPE = new Type<>(
        RecordCodecBuilder.mapCodec(inst -> inst.group(
            SlotDisplay.CODEC
                .fieldOf("pill")
                .forGetter(PillRecipeDisplay::pill),
            SlotDisplay.CODEC
                .fieldOf("potions")
                .forGetter(PillRecipeDisplay::potions),
            SlotDisplay.CODEC
                .fieldOf("result")
                .forGetter(PillRecipeDisplay::result),
            SlotDisplay.CODEC
                .fieldOf("craftingStation")
                .forGetter(PillRecipeDisplay::craftingStation)
        ).apply(inst, PillRecipeDisplay::new)),
        StreamCodec.composite(
            SlotDisplay.STREAM_CODEC,
            PillRecipeDisplay::pill,
            SlotDisplay.STREAM_CODEC,
            PillRecipeDisplay::potions,
            SlotDisplay.STREAM_CODEC,
            PillRecipeDisplay::result,
            SlotDisplay.STREAM_CODEC,
            PillRecipeDisplay::craftingStation,
            PillRecipeDisplay::new
        )
    );

    @Override
    public Type<PillRecipeDisplay> type() {
        return PillRecipeDisplay.TYPE;
    }
}
