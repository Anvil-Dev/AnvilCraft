package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.ItemDetectorMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import org.jetbrains.annotations.NotNull;

import static dev.dubhe.anvilcraft.block.entity.ItemDetectorBlockEntity.Mode;

public record MachineCycleFilterModePacket(Mode filterMode) implements CustomPacketPayload {
    public static final Type<MachineCycleFilterModePacket> TYPE = new Type<>(AnvilCraft.of("machine_cycle_filter_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MachineCycleFilterModePacket> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.INT,
            p -> p.filterMode().ordinal(),
            i -> new MachineCycleFilterModePacket(Mode.values()[i])
        );
    public static final IPayloadHandler<MachineCycleFilterModePacket> HANDLER = MachineCycleFilterModePacket::serverHandler;

    @Override
    @NotNull
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void serverHandler(MachineCycleFilterModePacket data, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        context.enqueueWork(() -> {
            if (!player.hasContainerOpen()) return;
            if (!(player.containerMenu instanceof ItemDetectorMenu menu)) return;
            menu.setFilterMode(data.filterMode);
            menu.flush();
        });
    }
}
