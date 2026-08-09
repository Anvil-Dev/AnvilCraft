package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.screen.SliderScreen;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

public record SliderInitPacket(int value) implements IClientboundPacket {
    public static final Type<SliderInitPacket> TYPE = IPacket.type(AnvilCraft.of("slider_init"));
    public static final StreamCodec<ByteBuf, SliderInitPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        SliderInitPacket::value,
        SliderInitPacket::new
    );

    @Override
    public Type<SliderInitPacket> type() {
        return SliderInitPacket.TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (!(Minecraft.getInstance().screen instanceof SliderScreen screen)) return;
        screen.setValue(this.value);
    }
}
