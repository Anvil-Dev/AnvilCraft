package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModComponents;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;

@MethodsReturnNonnullByDefault
public record SwitchPhasePacket() implements CustomPacketPayload {
    public static final Type<SwitchPhasePacket> TYPE = new Type<>(AnvilCraft.of("change_enchantment_space"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SwitchPhasePacket> STREAM_CODEC =
        CustomPacketPayload.codec(SwitchPhasePacket::encode, SwitchPhasePacket::decode);
    public static final IPayloadHandler<SwitchPhasePacket> HANDLER = SwitchPhasePacket::serverHandler;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void encode(@NotNull RegistryFriendlyByteBuf buf) {
    }

    public static SwitchPhasePacket decode(@NotNull RegistryFriendlyByteBuf buf) {
        return new SwitchPhasePacket();
    }

    public static void serverHandler(SwitchPhasePacket data, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        context.enqueueWork(() -> {
            if (!(player.level() instanceof ServerLevel)) return;
            Optional.of(player.getMainHandItem()).filter(stack -> stack.get(ModComponents.MULTIPHASE) != null)
                .or(() -> Optional.of(player.getOffhandItem()).filter(stack -> stack.get(ModComponents.MULTIPHASE) != null))
                .ifPresent(stack -> Objects.requireNonNull(stack.get(ModComponents.MULTIPHASE)).cyclePhases(stack));
        });
    }
}
