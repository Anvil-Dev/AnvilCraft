package dev.dubhe.anvilcraft.recipe.display.slot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.crafting.display.DisplayContentsFactory;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public record WithAnyPotionsExcept(SlotDisplay display, List<Identifier> excepts) implements SlotDisplay {
    public static final MapCodec<WithAnyPotionsExcept> MAP_CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
        SlotDisplay.CODEC
            .fieldOf("contents")
            .forGetter(WithAnyPotionsExcept::display),
        Identifier.CODEC
            .listOf()
            .fieldOf("excepts")
            .forGetter(WithAnyPotionsExcept::excepts)
    ).apply(inst, WithAnyPotionsExcept::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, WithAnyPotionsExcept> STREAM_CODEC = StreamCodec.composite(
        SlotDisplay.STREAM_CODEC,
        WithAnyPotionsExcept::display,
        Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()),
        WithAnyPotionsExcept::excepts,
        WithAnyPotionsExcept::new
    );
    public static final Type<WithAnyPotionsExcept> TYPE = new Type<>(MAP_CODEC, STREAM_CODEC);

    @Override
    public Type<WithAnyPotionsExcept> type() {
        return WithAnyPotionsExcept.TYPE;
    }

    @Override
    public <T> Stream<T> resolve(ContextMap context, DisplayContentsFactory<T> factory) {
        if (!(factory instanceof DisplayContentsFactory.ForStacks<T> stacks)) return Stream.empty();
        List<ItemStack> displayItems = this.display.resolveForStacks(context);
        Optional<? extends HolderLookup.RegistryLookup<Potion>> potions = Optional.ofNullable(
            context.getOptional(SlotDisplayContext.REGISTRIES)
        ).flatMap(registries -> registries.lookup(Registries.POTION));
        return potions.stream()
            .flatMap(HolderLookup::listElements)
            .filter(potion -> !this.excepts.contains(potion.key().identifier()))
            .flatMap(potion -> {
                PotionContents contents = new PotionContents(potion);
                return displayItems.stream().map(stack -> {
                    ItemStack copied = stack.copy();
                    copied.set(DataComponents.POTION_CONTENTS, contents);
                    return stacks.forStack(copied);
                });
            });
    }
}
