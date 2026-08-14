package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.CreativeLaserBlockEntity;
import dev.dubhe.anvilcraft.block.state.LensType;
import dev.dubhe.anvilcraft.inventory.CreativeLaserMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

public record CreativeLaserUpdatePacket(int level, LensType lensType, boolean gamma) implements IServerboundPacket {
    public static final Type<CreativeLaserUpdatePacket> TYPE = IPacket.type(AnvilCraft.of("creative_laser_update"));
    public static final StreamCodec<ByteBuf, CreativeLaserUpdatePacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        CreativeLaserUpdatePacket::level,
        ByteBufCodecs.STRING_UTF8.map(LensType::valueOf, LensType::name),
        CreativeLaserUpdatePacket::lensType,
        ByteBufCodecs.BOOL,
        CreativeLaserUpdatePacket::gamma,
        CreativeLaserUpdatePacket::new
    );

    @Override
    public Type<CreativeLaserUpdatePacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (!(player.containerMenu instanceof CreativeLaserMenu menu)) return;
        CreativeLaserBlockEntity blockEntity = menu.getBlockEntity();
        if (blockEntity == null) return;
        blockEntity.setConfiguredLevel(this.level);
        blockEntity.setLensType(this.lensType);
        blockEntity.setGamma(this.gamma);
    }
}
