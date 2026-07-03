package dev.dubhe.anvilcraft.recipe.multiblock;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.v2.codec.CodecUtil;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.dubhe.anvilcraft.AnvilCraft;
import lombok.Getter;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;

@Getter
public class BlockPredicateWithState implements Predicate<BlockState> {
    private final Either<Block, TagKey<Block>> block;
    private final Map<Property<?>, Comparable<?>> properties;
    private @Nullable Either<BlockState, List<BlockState>> defaultStates;

    private static final @Nullable Method SET_VALUE;

    static {
        Method method;
        try {
            method = BlockState.class.getMethod("setValue", Property.class, Comparable.class);
        } catch (NoSuchMethodException e) {
            AnvilCraft.LOGGER.warn("No such method: BlockState#setValue");
            method = null;
        }
        SET_VALUE = method;
    }

    public static final Codec<BlockPredicateWithState> CODEC = Raw.CODEC
        .xmap(BlockPredicateWithState::new, BlockPredicateWithState::toRaw);
    public static final StreamCodec<RegistryFriendlyByteBuf, BlockPredicateWithState> STREAM_CODEC = Raw.STREAM_CODEC
        .map(BlockPredicateWithState::new, BlockPredicateWithState::toRaw);

    private BlockPredicateWithState(final Block block, final Map<Property<?>, Comparable<?>> properties) {
        this.block = Either.left(block);
        this.properties = properties;
    }

    private BlockPredicateWithState(Raw raw) {
        this.block = raw.block();
        this.properties = new HashMap<>();
        raw.propertiesMap().forEach((stateName, stateValue) -> this.hasState(
            BuiltInRegistries.BLOCK,
            stateName,
            stateValue
        ));
    }

    public BlockPredicateWithState(Block block) {
        this.block = Either.left(block);
        this.properties = new HashMap<>();
    }

    public BlockPredicateWithState(TagKey<Block> blocks) {
        this.block = Either.right(blocks);
        this.properties = new HashMap<>();
    }

    public <T extends Comparable<T>> BlockPredicateWithState hasState(Property<T> property, T value) {
        this.properties.put(property, value);
        return this;
    }

    public BlockPredicateWithState hasState(HolderGetter<Block> blocks, String stateName, String stateValue) {
        Property<?> property = this.block.map(
            block -> block.getStateDefinition().getProperty(stateName),
            tag -> {
                for (Holder<Block> holder : blocks.getOrThrow(tag)) {
                    Property<?> property1 = holder.value().getStateDefinition().getProperty(stateName);
                    if (property1 != null) {
                        return property1;
                    }
                }
                return null;
            }
        );
        if (property == null) {
            return this;
        }
        this.properties.put(property, property.getValue(stateValue).orElseThrow());
        return this;
    }

    public <T extends Comparable<T>> BlockPredicateWithState copyPropertyFrom(BlockState state, Property<T> property) {
        return this.hasState(property, state.getValue(property));
    }

    public <T extends Comparable<T>> boolean hasProperty(Property<T> property) {
        return this.properties.containsKey(property);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends Comparable<T>> T getPropertyValue(Property<T> property) {
        return (T) this.properties.getOrDefault(property, null);
    }

    public static BlockPredicateWithState of(Block block) {
        return new BlockPredicateWithState(block);
    }

    public static BlockPredicateWithState of(Holder<Block> block) {
        return of(block.value());
    }

    public static BlockPredicateWithState of(String blockName) {
        return of(BuiltInRegistries.BLOCK.getValue(Identifier.parse(blockName)));
    }

    public static BlockPredicateWithState of(TagKey<Block> blocks) {
        return new BlockPredicateWithState(blocks);
    }

    @Override
    public boolean test(@Nullable BlockState state) {
        if (state == null) return false;
        if (!this.block.map(state::is, state::is)) return false;
        return this.properties.entrySet().stream()
            .allMatch(entry -> state.hasProperty(entry.getKey())
                && state.getValue(entry.getKey()).equals(entry.getValue()));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof BlockPredicateWithState predicate) {
            return this.block == predicate.block && this.properties.equals(predicate.properties);
        }
        return false;
    }

    public BlockState getDefaultState() {
        if (this.defaultStates == null) {
            this.defaultStates = this.block.mapBoth(
                block -> {
                    AtomicReference<BlockState> state = new AtomicReference<>(block.defaultBlockState());
                    if (SET_VALUE == null) return state.get();
                    this.properties.forEach((property, value) -> {
                        try {
                            state.set((BlockState) SET_VALUE.invoke(state.get(), property, value));
                        } catch (Exception e) {
                            AnvilCraft.LOGGER.warn(
                                "Invalid property or value on state {}: property:{}, value:{}",
                                state.get(),
                                property,
                                value,
                                e
                            );
                        }
                    });
                    return state.get();
                },
                tag -> {
                    List<BlockState> states = new ArrayList<>();
                    for (Holder<Block> holder : BuiltInRegistries.BLOCK.getOrThrow(tag)) {
                        AtomicReference<BlockState> state = new AtomicReference<>(holder.value().defaultBlockState());
                        if (SET_VALUE == null) {
                            states.add(state.get());
                            continue;
                        }
                        this.properties.forEach((property, value) -> {
                            try {
                                state.set((BlockState) SET_VALUE.invoke(state.get(), property, value));
                            } catch (Exception _) {
                                // do nothing
                            }
                        });
                        states.add(state.get());
                    }
                    return states;
                }
            );
        }
        return this.defaultStates.map(
            Function.identity(),
            states -> states.get((int) ((System.currentTimeMillis() / 1000) % states.size()))
        );
    }

    public static String getNameOf(Object value) {
        return value instanceof StringRepresentable representable ? representable.getSerializedName() : value.toString();
    }

    private Raw toRaw() {
        Map<String, String> propertiesMap = new HashMap<>();
        this.properties.forEach((property, value) -> propertiesMap.put(property.getName(), getNameOf(value)));
        return new Raw(this.block, propertiesMap);
    }

    public record Raw(Either<Block, TagKey<Block>> block, Map<String, String> propertiesMap) {

        public static final Codec<Raw> CODEC = RecordCodecBuilder.create(ins -> ins.group(
            Codec.either(CodecUtil.BLOCK, TagKey.hashedCodec(Registries.BLOCK))
                .fieldOf("block")
                .forGetter(Raw::block),
            Codec.unboundedMap(Codec.STRING, Codec.STRING)
                .optionalFieldOf("properties", Collections.emptyMap())
                .forGetter(Raw::propertiesMap)
        ).apply(ins, Raw::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Raw> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.either(StreamCodecUtil.BLOCK, TagKey.streamCodec(Registries.BLOCK)),
            Raw::block,
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.STRING_UTF8),
            Raw::propertiesMap,
            Raw::new
        );
    }
}
