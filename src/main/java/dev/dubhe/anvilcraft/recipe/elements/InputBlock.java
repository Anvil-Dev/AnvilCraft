package dev.dubhe.anvilcraft.recipe.elements;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tterrag.registrate.util.entry.BlockEntry;
import dev.dubhe.anvilcraft.util.CodecUtil;
import lombok.Getter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Map;
import java.util.Optional;

@Getter
public class InputBlock {
    public static final Codec<InputBlock> CODEC =
        RecordCodecBuilder.create(it -> it.group(
            TagKey.codec(Registries.BLOCK).optionalFieldOf("tag").forGetter(InputBlock::getOptionalTag),
            CodecUtil.BLOCK_CODEC.optionalFieldOf("id").forGetter(InputBlock::getOptionalBlock),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("states").forGetter(InputBlock::getOptionalStates)
        ).apply(it, InputBlock::new));

    private Optional<Block> getOptionalBlock() {
        return Optional.ofNullable(this.block);
    }

    private Optional<Map<String, String>> getOptionalStates() {
        return Optional.ofNullable(this.states == null || this.states.isEmpty() ? null : this.states);
    }

    private Optional<TagKey<Block>> getOptionalTag() {
        return Optional.ofNullable(this.tag);
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, InputBlock> STREAM_CODEC = StreamCodec.of(
        InputBlock::encode, InputBlock::decode
    );

    private static void encode(RegistryFriendlyByteBuf buf, InputBlock inputBlock) {
        if (inputBlock.tag == null) {
            buf.writeBoolean(true);
            CodecUtil.BLOCK_STREAM_CODEC.encode(buf, inputBlock.block);
            buf.writeMap(inputBlock.states == null ? Map.of() : inputBlock.states, (buf1, str) -> buf.writeUtf(str), (buf1, str) -> buf.writeUtf(str));
        } else {
            buf.writeBoolean(false);
            buf.writeResourceLocation(inputBlock.tag.location());
        }
    }

    private static InputBlock decode(RegistryFriendlyByteBuf buf) {
        if (buf.readBoolean()) {
            return new InputBlock(
                CodecUtil.BLOCK_STREAM_CODEC.decode(buf),
                buf.readMap((buf1) -> buf.readUtf(), (buf1) -> buf.readUtf())
            );
        } else {
            return new InputBlock(
                new TagKey<>(Registries.BLOCK, buf.readResourceLocation())
            );
        }
    }

    final TagKey<Block> tag;
    final Block block;
    final Map<String, String> states;

    public InputBlock(TagKey<Block> tag) {
        this.tag = tag;
        this.block = null;
        this.states = null;
    }

    public InputBlock(Block block, Map<String, String> states) {
        this.tag = null;
        this.block = block;
        this.states = states;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private InputBlock(Optional<TagKey<Block>> tagKey, Optional<Block> block, Optional<Map<String, String>> stringStringMap) {
        this.tag = tagKey.orElse(null);
        this.block = block.orElse(null);
        this.states = stringStringMap.orElse(null);
    }

    public static InputBlock of(BlockEntry<? extends Block> block) {
        return new InputBlock(block.get(), Map.of());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public boolean is(BlockState blockState) {
        StateDefinition<Block, BlockState> stateDef = blockState.getBlock().getStateDefinition();
        if (this.block != null) {
            if (!blockState.is(this.block))
                return false;
            if (states == null) return true;
            for (String key : this.states.keySet()) {
                Property property = stateDef.getProperty(key);
                if (property == null)
                    return false;
                if (property.getName(blockState.getValue(property)).equals(key))
                    return false;
            }
            return true;
        } else {
            assert tag != null;
            return blockState.is(tag);
        }
    }

    public String getKey() {
        return this.tag == null ? BuiltInRegistries.BLOCK.getKey(this.block).getPath() : this.tag.location().getPath();
    }
}
