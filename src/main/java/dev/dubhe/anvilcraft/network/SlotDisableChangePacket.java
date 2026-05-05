package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.ISensitiveBiPacket;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.screen.IFilterScreen;
import dev.dubhe.anvilcraft.inventory.IFilterMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

public record SlotDisableChangePacket(int index, boolean state) implements ISensitiveBiPacket {
    public static final Type<SlotDisableChangePacket> TYPE = IPacket.type(AnvilCraft.of("slot_disable_change"));
    public static final StreamCodec<ByteBuf, SlotDisableChangePacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.INT,
        SlotDisableChangePacket::index,
        ByteBufCodecs.BOOL,
        SlotDisableChangePacket::state,
        SlotDisableChangePacket::new
    );

    @Override
    public Type<SlotDisableChangePacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        if (!(Minecraft.getInstance().screen instanceof IFilterScreen<?> screen)) return;
        screen.setSlotDisabled(this.index, this.state);
    }

    @Override
    public void handleOnServer(Player player) {
        if (!(player.containerMenu instanceof IFilterMenu menu)) return;
        menu.setSlotDisabled(this.index, this.state);
        menu.flush();
        PacketDistributor.sendToPlayer(Util.cast(player), this);
    }
}
