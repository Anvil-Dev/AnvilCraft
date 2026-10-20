package dev.dubhe.anvilcraft.recipe.mineral;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.level.Level;

public record HeightRange(int min, int max) {

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
        int offset = pos.getY() - level.getMinY();
        return offset >= this.min && offset <= this.max;
    }
}
