package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.item.tool.HeavyHalberdItem;
import dev.dubhe.anvilcraft.item.tool.HeavyHalberdMode;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

public record SwitchHeavyHalberdModePacket(InteractionHand hand, HeavyHalberdMode mode) implements IServerboundPacket {
    public static final Type<SwitchHeavyHalberdModePacket> TYPE = new Type<>(AnvilCraft.of("switch_heavy_halberd_mode"));
    public static final StreamCodec<ByteBuf, SwitchHeavyHalberdModePacket> STREAM_CODEC = StreamCodec.composite(
        StreamCodecUtil.enumStreamCodec(InteractionHand.class),
        SwitchHeavyHalberdModePacket::hand,
        HeavyHalberdMode.STREAM_CODEC,
        SwitchHeavyHalberdModePacket::mode,
        SwitchHeavyHalberdModePacket::new
    );

    @Override
    public Type<SwitchHeavyHalberdModePacket> type() {
        return SwitchHeavyHalberdModePacket.TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        HeavyHalberdItem.setMode(player, this.hand, this.mode);
    }
}
