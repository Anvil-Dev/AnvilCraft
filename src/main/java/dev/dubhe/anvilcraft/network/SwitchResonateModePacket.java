package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.item.ResonatorItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

public record SwitchResonateModePacket(InteractionHand hand, int mode) implements IServerboundPacket {
    public static final Type<SwitchResonateModePacket> TYPE = new Type<>(AnvilCraft.of("switch_resonate_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SwitchResonateModePacket> STREAM_CODEC = StreamCodec.composite(
        StreamCodecUtil.enumStreamCodec(InteractionHand.class),
        SwitchResonateModePacket::hand,
        ByteBufCodecs.VAR_INT,
        SwitchResonateModePacket::mode,
        SwitchResonateModePacket::new
    );

    @Override
    public Type<SwitchResonateModePacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        ResonatorItem.setMode(player, this.hand, this.mode);
    }
}
