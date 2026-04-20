package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.ISensitiveBiPacket;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.screen.BaseMachineScreen;
import dev.dubhe.anvilcraft.inventory.BaseMachineMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

public record MachineOutputDirectionPacket(Direction direction) implements ISensitiveBiPacket {
    public static final Type<MachineOutputDirectionPacket> TYPE = IPacket.type(AnvilCraft.of("machine_output_direction"));
    public static final StreamCodec<ByteBuf, MachineOutputDirectionPacket> STREAM_CODEC = StreamCodec.composite(
        Direction.STREAM_CODEC,
        MachineOutputDirectionPacket::direction,
        MachineOutputDirectionPacket::new
    );

    @Override
    public Type<MachineOutputDirectionPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (!(Minecraft.getInstance().screen instanceof BaseMachineScreen<?> screen)) return;
        if (screen.getDirectionButton() == null) return;
        screen.getDirectionButton().setDirection(this.direction);
    }

    @Override
    public void handleOnServer(Player player) {
        if (!(player.containerMenu instanceof BaseMachineMenu menu)) return;
        Direction direction = this.direction();
        menu.setDirection(direction);
        PacketDistributor.sendToPlayer(Util.cast(player), this);
    }
}
