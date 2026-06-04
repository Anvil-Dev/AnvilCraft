package dev.dubhe.anvilcraft.recipe.display.slot;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.display.DisplayContentsFactory;
import net.minecraft.world.item.crafting.display.SlotDisplay;

import java.util.stream.Stream;

public record PillSlotDemo(SlotDisplay potions, SlotDisplay pill) implements SlotDisplay {
    public static final Type<PillSlotDemo> TYPE = new Type<>(
        RecordCodecBuilder.mapCodec(inst -> inst.group(
            SlotDisplay.CODEC
                .fieldOf("cannedFood")
                .forGetter(PillSlotDemo::potions),
            SlotDisplay.CODEC
                .fieldOf("pill")
                .forGetter(PillSlotDemo::pill)
        ).apply(inst, PillSlotDemo::new)),
        StreamCodec.composite(
            SlotDisplay.STREAM_CODEC,
            PillSlotDemo::potions,
            SlotDisplay.STREAM_CODEC,
            PillSlotDemo::pill,
            PillSlotDemo::new
        )
    );

    @Override
    public <T> Stream<T> resolve(ContextMap context, DisplayContentsFactory<T> builder) {
        return SlotDisplay.applyDemoTransformation(context, builder, this.potions, this.pill, PillSlotDemo::canFood);
    }

    private static ItemStack canFood(ItemStack potion, ItemStack pill) {
        ItemStack result = pill.copy();
        result.set(DataComponents.POTION_CONTENTS, potion.get(DataComponents.POTION_CONTENTS));
        return result;
    }

    @Override
    public Type<PillSlotDemo> type() {
        return PillSlotDemo.TYPE;
    }
}
