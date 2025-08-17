package dev.dubhe.anvilcraft.api.injection.block.state;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.Encoder;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

public interface IBlockStateExtension {
    private BlockState self() {
        return (BlockState) this;
    }

    /**
     * Determines if this block can stick to another block when pushed by a piston.
     *
     * @param pos      My pos
     * @param otherPos Other pos
     * @param other    Other state
     * @return True to link blocks
     */
    default boolean canStickTo(BlockPos pos, BlockPos otherPos, BlockState other) {
        return self().getBlock().canStickTo(pos, self(), otherPos, other);
    }

    MapCodec<BlockState> MAP_CODEC = BuiltInRegistries.BLOCK.byNameCodec().dispatchMap(
        "block",
        BlockBehaviour.BlockStateBase::getBlock,
        block -> {
            BlockState state = block.defaultBlockState();
            return state.propertiesCodec()
                .codec()
                .lenientOptionalFieldOf("state", state);
        }
    );

    StreamCodec<RegistryFriendlyByteBuf, BlockState> STREAM_CODEC = StreamCodec.of(
        (buffer, value) -> {
            RegistryOps<Tag> ops = HolderLookup.Provider
                .create(Stream.of(BuiltInRegistries.BLOCK.asLookup()))
                .createSerializationContext(NbtOps.INSTANCE);
            DataResult<Tag> encode = BlockState.CODEC.encode(value, NbtOps.INSTANCE, ops.empty());
            Tag tag = encode.getOrThrow();
            buffer.writeNbt(tag);
        },
        buffer -> {
            RegistryOps<Tag> ops = HolderLookup.Provider
                .create(Stream.of(BuiltInRegistries.BLOCK.asLookup()))
                .createSerializationContext(NbtOps.INSTANCE);
            return BlockState.CODEC.decode(ops, buffer.readNbt()).getOrThrow().getFirst();
        }
    );

    default <T extends Comparable<T>> MapCodec<BlockState> propertiesCodec() {
        BlockState state = self();
        MapCodec<BlockState> mapcodec = MapCodec.of(Encoder.empty(), Decoder.unit(state));
        for (Map.Entry<Property<?>, Comparable<?>> entry : state.getValues().entrySet()) {
            Property<?> key = entry.getKey();
            //noinspection unchecked
            mapcodec = appendPropertyCodec(
                mapcodec,
                () -> state,
                key.getName(),
                (Property<T>) key,
                (Property.Value<T>) key.value(state)
            );
        }
        return mapcodec;
    }

    static <T extends Comparable<T>> MapCodec<BlockState> appendPropertyCodec(
        MapCodec<BlockState> propertyCodec,
        Supplier<BlockState> holderSupplier,
        String value,
        @NotNull Property<T> property,
        Property.Value<T> defValue
    ) {
        return Codec.mapPair(
                propertyCodec,
                property.valueCodec()
                    .optionalFieldOf(value, defValue)
                    .orElseGet(
                        key -> {
                        },
                        () -> property.value(holderSupplier.get())
                    )
            )
            .xmap(
                pair -> pair.getFirst().setValue(property, pair.getSecond().value()),
                state -> Pair.of(state, property.value(state))
            );
    }
}
