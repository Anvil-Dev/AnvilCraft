package dev.dubhe.anvilcraft.recipe.mineral;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;

/**
 * 矿物涌泉可工作的高度范围，用相对世界最低高度的偏移表示。
 *
 * @param min 偏移下限（含）
 * @param max 偏移上限（含）
 */
public record HeightRange(int min, int max) {
    /** 默认范围：世界最低高度起 9 格内，与旧版硬编码行为一致。 */
    public static final HeightRange DEFAULT = new HeightRange(0, 8);
    public static final Codec<HeightRange> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.INT.optionalFieldOf("min", DEFAULT.min).forGetter(HeightRange::min),
        Codec.INT.optionalFieldOf("max", DEFAULT.max).forGetter(HeightRange::max)
    ).apply(instance, HeightRange::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, HeightRange> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, HeightRange::min,
        ByteBufCodecs.VAR_INT, HeightRange::max,
        HeightRange::new
    );

    public boolean test(Level level, BlockPos pos) {
        int offset = pos.getY() - level.getMinBuildHeight();
        return offset >= this.min && offset <= this.max;
    }
}
