package dev.dubhe.anvilcraft.recipe.mineral;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.api.heat.HeatTier;
import dev.dubhe.anvilcraft.api.heat.HeatTierLine;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record MineralFountainHeat(HeatTier tier, int duration, int count) {
    public static final Codec<MineralFountainHeat> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        HeatTier.LOWER_NAME_CODEC.fieldOf("tier").forGetter(MineralFountainHeat::tier),
        Codec.INT.optionalFieldOf("duration", 20).forGetter(MineralFountainHeat::duration),
        Codec.INT.optionalFieldOf("count", 1).forGetter(MineralFountainHeat::count)
    ).apply(instance, MineralFountainHeat::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, MineralFountainHeat> STREAM_CODEC = StreamCodec.composite(
        HeatTier.STREAM_CODEC, MineralFountainHeat::tier,
        ByteBufCodecs.VAR_INT, MineralFountainHeat::duration,
        ByteBufCodecs.VAR_INT, MineralFountainHeat::count,
        MineralFountainHeat::new
    );

    public HeatTierLine toLine() {
        return HeatTierLine.always(this.tier, this.duration);
    }
}
