package dev.dubhe.anvilcraft.recipe.multiblock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.anvilcraft.lib.recipe.util.CodecUtil;
import dev.dubhe.anvilcraft.AnvilCraft;
import lombok.Getter;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

@Getter
public class BlockPredicateWithState implements Predicate<BlockState> {
    private final Block block;
    private final Map<Property<?>, Comparable<?>> properties;
    private BlockState defaultState;

    private static Method setValueMethod = null;

    static {
        try {
            setValueMethod = BlockState.class.getMethod("setValue", Property.class, Comparable.class);
        } catch (NoSuchMethodException e) {
            AnvilCraft.LOGGER.warn("No such method: BlockState#setValue");
        }
    }

    public static final Codec<BlockPredicateWithState> CODEC = Raw.CODEC_RAW
        .comapFlatMap(raw -> {
            try {
                return DataResult.success(new BlockPredicateWithState(raw));
            } catch (Exception e) {
                return DataResult.error(() -> "invalid property names or values");
            }
        }, BlockPredicateWithState::toRaw);

    public static final StreamCodec<RegistryFriendlyByteBuf, BlockPredicateWithState> STREAM_CODEC =
        Raw.STREAM_CODEC_RAW
            .map(BlockPredicateWithState::new, BlockPredicateWithState::toRaw);

    private BlockPredicateWithState(final Block block, final Map<Property<?>, Comparable<?>> properties) {
        this.block = block;
        this.properties = properties;
    }

    private BlockPredicateWithState(Raw raw) {
        this.block = raw.block();
        this.properties = new HashMap<>();
        raw.propertiesMap().forEach(this::hasState);
    }

    public BlockPredicateWithState(Block block) {
        this.block = block;
        this.properties = new HashMap<>();
    }

    public <T extends Comparable<T>> BlockPredicateWithState hasState(Property<T> property, T value) {
        properties.put(property, value);
        return this;
    }

    public BlockPredicateWithState hasState(String stateName, String stateValue) {
        Property<?> property = this.block.getStateDefinition().getProperty(stateName);
        this.properties.put(property, Optional.ofNullable(property)
            .flatMap(p -> p.getValue(stateValue))
            .orElseThrow());
        return this;
    }

    public <T extends Comparable<T>> BlockPredicateWithState copyPropertyFrom(BlockState state, Property<T> property) {
        return this.hasState(property, state.getValue(property));
    }

    public <T extends Comparable<T>> boolean hasProperty(Property<T> property) {
        return properties.containsKey(property);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends Comparable<T>> T getPropertyValue(Property<T> property) {
        return (T) properties.getOrDefault(property, null);
    }

    @Contract("_ -> new")
    public static BlockPredicateWithState of(Block block) {
        return new BlockPredicateWithState(block);
    }

    @Contract("_ -> new")
    public static BlockPredicateWithState of(Holder<Block> block) {
        return of(block.value());
    }

    public static BlockPredicateWithState of(String blockName) {
        return of(BuiltInRegistries.BLOCK.get(ResourceLocation.parse(blockName)));
    }

    @Override
    public boolean test(@Nullable BlockState state) {
        if (state == null) return false;
        if (!state.is(this.block)) return false;
        return properties.entrySet().stream()
            .allMatch(entry -> state.hasProperty(entry.getKey())
                && state.getValue(entry.getKey()).equals(entry.getValue()));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof BlockPredicateWithState predicate) {
            return block == predicate.block && properties.equals(predicate.properties);
        }
        return false;
    }

    public BlockState getDefaultState() {
        if (this.defaultState == null) {
            this.defaultState = this.block.defaultBlockState();
            if (setValueMethod == null) return this.defaultState;
            this.properties.forEach((property, value) -> {
                try {
                    this.defaultState = (BlockState) setValueMethod.invoke(this.defaultState, property, value);
                } catch (Exception e) {
                    AnvilCraft.LOGGER.warn("Invalid property or value: property:{}, value:{}", property, value);
                }
            });
        }
        return this.defaultState;
    }

    public static String getNameOf(Object value) {
        return value instanceof StringRepresentable representable ? representable.getSerializedName() : value.toString();
    }

    private Raw toRaw() {
        Map<String, String> propertiesMap = new HashMap<>();
        this.properties.forEach((property, value) -> propertiesMap.put(property.getName(), getNameOf(value)));
        return new Raw(this.block, propertiesMap);
    }

    public record Raw(Block block, Map<String, String> propertiesMap) {

        public static final Codec<Raw> CODEC_RAW = RecordCodecBuilder.create(ins -> ins.group(
                CodecUtil.BLOCK_CODEC.fieldOf("block").forGetter(Raw::block),
                Codec.unboundedMap(Codec.STRING, Codec.STRING)
                    .optionalFieldOf("properties", Collections.emptyMap())
                    .forGetter(Raw::propertiesMap))
            .apply(ins, Raw::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Raw> STREAM_CODEC_RAW =
            StreamCodec.composite(
                CodecUtil.BLOCK_STREAM_CODEC,
                Raw::block,
                ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.STRING_UTF8),
                Raw::propertiesMap,
                Raw::new);
    }
}
