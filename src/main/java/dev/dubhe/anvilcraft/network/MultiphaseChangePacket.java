package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.item.property.Merciless;
import dev.dubhe.anvilcraft.init.ModComponents;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

@MethodsReturnNonnullByDefault
public record MultiphaseChangePacket(InteractionHand hand, byte index, boolean merciless) implements CustomPacketPayload {
    public static final Type<MultiphaseChangePacket> TYPE = new Type<>(AnvilCraft.of("multiphase_change"));
    public static final StreamCodec<FriendlyByteBuf, MultiphaseChangePacket> STREAM_CODEC = StreamCodec.ofMember(
        MultiphaseChangePacket::encode, MultiphaseChangePacket::decode
    );
    public static final IPayloadHandler<MultiphaseChangePacket> HANDLER = MultiphaseChangePacket::serverHandler;

    public MultiphaseChangePacket(InteractionHand hand, int index, boolean merciless) {
        this(hand, (byte) index, merciless);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeEnum(this.hand);
        buf.writeByte(this.index);
        buf.writeBoolean(this.merciless);
    }

    public static MultiphaseChangePacket decode(@NotNull FriendlyByteBuf buf) {
        return new MultiphaseChangePacket(buf.readEnum(InteractionHand.class), buf.readByte(), buf.readBoolean());
    }

    public void serverHandler(IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        context.enqueueWork(() -> Optional.of(player.getItemInHand(hand))
            .filter(stack -> stack.has(ModComponents.MULTIPHASE) && stack.has(ModComponents.MERCILESS))
            .ifPresent(stack -> {
                stack.set(ModComponents.MERCILESS, new Merciless(this.merciless));
                Objects.requireNonNull(stack.get(ModComponents.MULTIPHASE)).cyclePhases(stack, this.index);
            }));
    }
}
