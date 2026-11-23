package dev.dubhe.anvilcraft.item.property.component;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record CanTakeOutAmmo(boolean value) {
    public static final CanTakeOutAmmo CAN = new CanTakeOutAmmo(true);
    public static final CanTakeOutAmmo CANT = new CanTakeOutAmmo(false);
    public static final Codec<CanTakeOutAmmo> CODEC = Codec.BOOL
        .xmap(CanTakeOutAmmo::new, CanTakeOutAmmo::value);
    public static final StreamCodec<ByteBuf, CanTakeOutAmmo> STREAM_CODEC = ByteBufCodecs.BOOL
        .map(CanTakeOutAmmo::new, CanTakeOutAmmo::value);
}
