package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.util.StructureLoadUtil;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/** tag 为 null 时同步缺失状态，避免客户端每帧重新请求。 */
public record StructureDiskResponsePacket(String file, @Nullable CompoundTag tag) implements IClientboundPacket {
    public static final Type<StructureDiskResponsePacket> TYPE = IPacket.type(AnvilCraft.of("structure_disk_response"));
    public static final StreamCodec<ByteBuf, StructureDiskResponsePacket> STREAM_CODEC = StreamCodec.of(
        (buffer, packet) -> {
            ByteBufCodecs.STRING_UTF8.encode(buffer, packet.file());
            FriendlyByteBuf.writeNbt(buffer, packet.tag());
        },
        buffer -> new StructureDiskResponsePacket(ByteBufCodecs.STRING_UTF8.decode(buffer), readStructureNbt(buffer))
    );

    @Nullable
    private static CompoundTag readStructureNbt(ByteBuf buffer) {
        var tag = FriendlyByteBuf.readNbt(buffer, NbtAccounter.create(StructureLoadUtil.MAX_STRUCTURE_NBT_BYTES));
        if (tag == null) return null;
        if (tag instanceof CompoundTag compound) return compound;
        throw new DecoderException("Structure data must be a compound tag");
    }

    @Override
    public Type<StructureDiskResponsePacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        StructureLoadUtil.cacheStructureNbt(this.file, this.tag);
    }
}
