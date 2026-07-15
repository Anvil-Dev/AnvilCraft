package dev.dubhe.anvilcraft.network;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

public record RocketJumpPacket(double power) implements IClientboundPacket {
    public static final Type<RocketJumpPacket> TYPE = new Type<>(AnvilCraft.of("rocket_jump"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RocketJumpPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.DOUBLE,
        RocketJumpPacket::power,
        RocketJumpPacket::new
    );
    public static final IPayloadHandler<RocketJumpPacket> HANDLER = RocketJumpPacket::clientHandler;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @Override
    public void handleOnClient(Player player) {
        Vec3 movement = player.getDeltaMovement();
        player.setDeltaMovement(movement.x, this.power, movement.z);
    }
}
