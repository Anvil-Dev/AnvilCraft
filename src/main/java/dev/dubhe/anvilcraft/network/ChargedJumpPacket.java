package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.item.EquipmentAbilities;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

public record ChargedJumpPacket(float jumpPower) implements IServerboundPacket {
    public static final Type<ChargedJumpPacket> TYPE = IPacket.type(AnvilCraft.of("charged_jump"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChargedJumpPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT,
        ChargedJumpPacket::jumpPower,
        ChargedJumpPacket::new
    );

    @Override
    public Type<ChargedJumpPacket> type() {
        return TYPE;
    }

    @Override
    public void handleOnServer(Player player) {
        EquipmentAbilities.applyChargedJump(player, this.jumpPower);
    }
}
