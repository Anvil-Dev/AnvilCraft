package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.item.tool.MultitoolItem;
import dev.dubhe.anvilcraft.item.tool.MultitoolMode;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

public record SwitchMultitoolModePacket(InteractionHand hand, MultitoolMode mode) implements IServerboundPacket {
    public static final Type<SwitchMultitoolModePacket> TYPE = IPacket.type(AnvilCraft.of("switch_multitool_mode"));
    public static final StreamCodec<ByteBuf, SwitchMultitoolModePacket> STREAM_CODEC = StreamCodec.composite(
        StreamCodecUtil.enumStreamCodec(InteractionHand.class),
        SwitchMultitoolModePacket::hand,
        MultitoolMode.STREAM_CODEC,
        SwitchMultitoolModePacket::mode,
        SwitchMultitoolModePacket::new
    );

    @Override
    public Type<SwitchMultitoolModePacket> type() {
        return SwitchMultitoolModePacket.TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        MultitoolItem.setMode(player, this.hand, this.mode);
    }
}
