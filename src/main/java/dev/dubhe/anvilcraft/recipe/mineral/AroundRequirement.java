package dev.dubhe.anvilcraft.recipe.mineral;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public record AroundRequirement(BlockStatePredicate block, int count) {
    public static final Codec<AroundRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        BlockStatePredicate.CODEC.fieldOf("block").forGetter(AroundRequirement::block),
        Codec.INT.optionalFieldOf("count", 1).forGetter(AroundRequirement::count)
    ).apply(instance, AroundRequirement::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, AroundRequirement> STREAM_CODEC = StreamCodec.composite(
        BlockStatePredicate.STREAM_CODEC, AroundRequirement::block,
        ByteBufCodecs.VAR_INT, AroundRequirement::count,
        AroundRequirement::new
    );

    public boolean test(List<BlockState> around) {
        long matched = around.stream().filter(state -> this.block.testWithoutEntity(state)).count();
        return matched >= this.count;
    }
}
