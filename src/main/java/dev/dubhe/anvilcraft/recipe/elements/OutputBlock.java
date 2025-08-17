package dev.dubhe.anvilcraft.recipe.elements;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.tterrag.registrate.util.entry.BlockEntry;
import dev.dubhe.anvilcraft.util.CodecUtil;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@Getter
public class OutputBlock {
    public static final Codec<OutputBlock> CODEC = RecordCodecBuilder.create(it -> it.group(
            CodecUtil.BLOCK_CODEC.fieldOf("id").forGetter(OutputBlock::getBlock),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("states", new HashMap<>()).forGetter(OutputBlock::getStates),
            CompoundTag.CODEC.optionalFieldOf("beData", new CompoundTag()).forGetter(OutputBlock::getBeData),
            Codec.FLOAT.orElse(1f).fieldOf("chance").forGetter(OutputBlock::getChance)
        ).apply(it, OutputBlock::apply)
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, OutputBlock> STREAM_CODEC = StreamCodec.of(
        OutputBlock::encode, OutputBlock::decode
    );

    private static void encode(RegistryFriendlyByteBuf buf, OutputBlock outputBlock) {
        CodecUtil.BLOCK_STATE_STREAM_CODEC.encode(buf, outputBlock.blockState);
        buf.writeFloat(outputBlock.chance);
    }

    private static OutputBlock decode(RegistryFriendlyByteBuf buf) {
        return new OutputBlock(
            CodecUtil.BLOCK_STATE_STREAM_CODEC.decode(buf),
            buf.readFloat()
        );
    }

    final BlockState blockState;
    final CompoundTag beData;
    final float chance;

    public OutputBlock(BlockState blockState, float chance) {
        this(blockState, new CompoundTag(), chance);
    }

    public OutputBlock(BlockState blockState, CompoundTag beData, float chance) {
        this.blockState = blockState;
        this.beData = beData;
        this.chance = chance;
    }

    public static OutputBlock of(BlockEntry<? extends Block> block) {
        return new OutputBlock(block.getDefaultState(), 1f);
    }

    public static OutputBlock of(BlockEntry<? extends Block> block, CompoundTag beData) {
        return new OutputBlock(block.getDefaultState(), beData, 1f);
    }

    private static OutputBlock apply(Block block, Map<String, String> states, CompoundTag beData, float chance) {
        return new OutputBlock(
            new BlockItemStateProperties(states).apply(block.defaultBlockState()),
            beData,
            chance
        );
    }

    private Block getBlock() {
        return blockState.getBlock();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Map<String, String> getStates() {
        HashMap<String, String> states = new HashMap<>();
        for (Map.Entry<Property<?>, Comparable<?>> entry : blockState.getValues().entrySet()) {
            states.put(entry.getKey().getName(), ((Property) entry.getKey()).getName(entry.getValue()));
        }
        return states;
    }

    @Nullable
    public Pair<BlockState, CompoundTag> getResult(RandomSource randomSource) {
        if (randomSource.nextFloat() <= chance) {
            return new Pair<>(blockState, beData);
        } else return null;
    }
}
