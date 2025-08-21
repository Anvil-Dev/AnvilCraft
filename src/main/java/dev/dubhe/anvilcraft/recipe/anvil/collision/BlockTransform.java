package dev.dubhe.anvilcraft.recipe.anvil.collision;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.recipe.component.BlockStatePredicate;
import dev.dubhe.anvilcraft.recipe.component.ChanceBlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

/**
 * 方块转换类，用于定义方块的输入、输出和转换规则
 * 该类表示一个方块从一种状态转换为另一种状态的规则，包括概率和最大数量限制
 */
public record BlockTransform(
    BlockStatePredicate inputBlock, // 输入方块
    ChanceBlockState outputBlock, // 输出方块
    int maxCount // 最大转换数量
) {
    /**
     * Map编解码器
     */
    public static final Codec<BlockTransform> CODEC = RecordCodecBuilder.create(it -> it.group(
        BlockStatePredicate.CODEC.fieldOf("input").forGetter(BlockTransform::inputBlock),
        ChanceBlockState.CODEC.fieldOf("output").forGetter(BlockTransform::outputBlock),
        Codec.INT.fieldOf("max_count").forGetter(BlockTransform::maxCount)
    ).apply(it, BlockTransform::new));

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
    private static void encode(RegistryFriendlyByteBuf buf, @NotNull BlockTransform blockTransform) {
        BlockStatePredicate.STREAM_CODEC.encode(buf, blockTransform.inputBlock);
        ChanceBlockState.STREAM_CODEC.encode(buf, blockTransform.outputBlock);
        buf.writeVarInt(blockTransform.maxCount);
    }

    /**
     * 从字节缓冲区解码方块转换
     *
     * @param buf 字节缓冲区
     * @return 方块转换
     */
    private static @NotNull BlockTransform decode(RegistryFriendlyByteBuf buf) {
        return new BlockTransform(
            BlockStatePredicate.STREAM_CODEC.decode(buf),
            ChanceBlockState.STREAM_CODEC.decode(buf),
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
    public @NotNull Boolean progress(@NotNull Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return false;
        Map.Entry<BlockState, CompoundTag> output;
        if (
            inputBlock.test(level, level.getBlockState(pos), level.getBlockEntity(pos))
            && (output = outputBlock.getResult(serverLevel)) != null
        ) {
            level.setBlockAndUpdate(pos, output.getKey());
            Optional.ofNullable(level.getBlockEntity(pos))
                .ifPresent(be -> be.loadCustomOnly(output.getValue(), level.registryAccess()));
            return true;
        }
        return false;
    }
}