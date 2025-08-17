package dev.dubhe.anvilcraft.recipe.anvil.collision;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.recipe.elements.InputBlock;
import dev.dubhe.anvilcraft.recipe.elements.OutputBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * 方块转换类，用于定义方块的输入、输出和转换规则
 * 该类表示一个方块从一种状态转换为另一种状态的规则，包括概率和最大数量限制
 */
public record BlockTransform(
    InputBlock inputBlock, // 输入方块
    OutputBlock outputBlock, // 输出方块
    float chance, // 转换概率
    int maxCount // 最大转换数量
) {
    /**
     * Map编解码器
     */
    public static final Codec<BlockTransform> CODEC = RecordCodecBuilder.create(it -> it.group(
            InputBlock.CODEC.fieldOf("input").forGetter(BlockTransform::inputBlock),
            OutputBlock.CODEC.fieldOf("output").forGetter(BlockTransform::outputBlock),
            Codec.FLOAT.fieldOf("chance").forGetter(BlockTransform::chance),
            Codec.INT.fieldOf("max_count").forGetter(BlockTransform::maxCount)
        ).apply(it, BlockTransform::new)
    );

    /**
     * 流编解码器
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, BlockTransform> STREAM_CODEC = StreamCodec.of(
        BlockTransform::encode, BlockTransform::decode
    );

    /**
     * 编码方块转换到字节缓冲区
     *
     * @param buf            字节缓冲区
     * @param blockTransform 方块转换
     */
    private static void encode(RegistryFriendlyByteBuf buf, BlockTransform blockTransform) {
        InputBlock.STREAM_CODEC.encode(buf, blockTransform.inputBlock);
        OutputBlock.STREAM_CODEC.encode(buf, blockTransform.outputBlock);
        buf.writeFloat(blockTransform.chance);
        buf.writeVarInt(blockTransform.maxCount);
    }

    /**
     * 从字节缓冲区解码方块转换
     *
     * @param buf 字节缓冲区
     * @return 方块转换
     */
    private static BlockTransform decode(RegistryFriendlyByteBuf buf) {
        return new BlockTransform(
            InputBlock.STREAM_CODEC.decode(buf),
            OutputBlock.STREAM_CODEC.decode(buf),
            buf.readFloat(),
            buf.readVarInt()
        );
    }

    /**
     * 执行方块转换过程
     *
     * @param level 世界
     * @param pos   方块位置
     * @return 是否成功转换
     */
    public Boolean progress(Level level, BlockPos pos) {
        Pair<BlockState, CompoundTag> output;
        if (inputBlock.is(level.getBlockState(pos)) && (output = outputBlock.getResult(level.random)) != null) {
            if (chance < level.random.nextFloat()) return false;
            level.setBlockAndUpdate(pos, output.getFirst());
            Optional.ofNullable(level.getBlockEntity(pos))
                .ifPresent(be -> be.loadCustomOnly(output.getSecond(), level.registryAccess()));
            return true;
        }
        return false;
    }
}