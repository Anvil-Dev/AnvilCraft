package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.SliderMenu;
import dev.dubhe.anvilcraft.util.Callback;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

public record SliderUpdatePacket(int value) implements IServerboundPacket {
    public static final Type<SliderUpdatePacket> TYPE = IPacket.type(AnvilCraft.of("slider_update"));
    public static final StreamCodec<ByteBuf, SliderUpdatePacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        SliderUpdatePacket::value,
        SliderUpdatePacket::new
    );

    @Override
    public Type<SliderUpdatePacket> type() {
        return SliderUpdatePacket.TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        if (!(player.containerMenu instanceof SliderMenu menu)) return;
        Callback<Integer> callback = menu.getCallback();
        if (callback == null) return;
        callback.onValueChange(this.value);
    }
}
