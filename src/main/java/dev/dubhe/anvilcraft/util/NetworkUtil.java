package dev.dubhe.anvilcraft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public class NetworkUtil {
    public static void writeVarIntBlockPos(FriendlyByteBuf buf, BlockPos pos) {
        buf.writeVarInt(pos.getX());
        buf.writeVarInt(pos.getY());
        buf.writeVarInt(pos.getZ());
    }

    public static BlockPos readVarIntBlockPos(FriendlyByteBuf buf) {
        return new BlockPos(
            buf.readVarInt(),
            buf.readVarInt(),
            buf.readVarInt()
        );
    }
}
