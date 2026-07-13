package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.entity.fluid.ControlValveBlockEntity;
import dev.dubhe.anvilcraft.inventory.ControlValveMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

public record ControlValveUpdatePacket(int maxRate) implements IServerboundPacket {
    public static final Type<ControlValveUpdatePacket> TYPE = IPacket.type(AnvilCraft.of("control_valve_update"));
    public static final StreamCodec<ByteBuf, ControlValveUpdatePacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        ControlValveUpdatePacket::maxRate,
        ControlValveUpdatePacket::new
    );

    @Override
    public Type<ControlValveUpdatePacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (!(player.containerMenu instanceof ControlValveMenu menu)) return;
        ControlValveBlockEntity be = menu.getBlockEntity();
        if (be == null) return;
        be.setMaxRate(this.maxRate);
        be.sendUpdate();
    }
}
