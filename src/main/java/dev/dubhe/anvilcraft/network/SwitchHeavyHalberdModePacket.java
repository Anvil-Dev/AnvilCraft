package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.item.HeavyHalberdItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

public record SwitchHeavyHalberdModePacket(InteractionHand hand, int mode) implements IServerboundPacket {
    private static final int CYCLE_MODE = -1;
    public static final Type<SwitchHeavyHalberdModePacket> TYPE = new Type<>(AnvilCraft.of("switch_heavy_halberd_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SwitchHeavyHalberdModePacket> STREAM_CODEC = StreamCodec.composite(
        StreamCodecUtil.enumStreamCodec(InteractionHand.class),
        SwitchHeavyHalberdModePacket::hand,
        ByteBufCodecs.VAR_INT,
        SwitchHeavyHalberdModePacket::mode,
        SwitchHeavyHalberdModePacket::new
    );

    public SwitchHeavyHalberdModePacket(InteractionHand hand) {
        this(hand, CYCLE_MODE);
    }

    @Override
    public Type<SwitchHeavyHalberdModePacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        int targetMode = this.mode;
        if (targetMode == CYCLE_MODE) {
            int currentMode = HeavyHalberdItem.getMode(player.getItemInHand(this.hand));
            // 轮盘槽位按逆时针排列，顺时针切换需要递减模式编号。
            targetMode = currentMode <= HeavyHalberdItem.TRIDENT_MODE ? HeavyHalberdItem.MACE_MODE : currentMode - 1;
        }
        HeavyHalberdItem.setMode(player, this.hand, targetMode);
    }
}
