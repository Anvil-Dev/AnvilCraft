package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.BaseMachineScreen;
import dev.dubhe.anvilcraft.inventory.BaseMachineMenu;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class MachineOutputDirectionPacket implements CustomPacketPayload {
    public static final Type<MachineOutputDirectionPacket> TYPE = new Type<>(AnvilCraft.of("machine_output_direction"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MachineOutputDirectionPacket> STREAM_CODEC =
            StreamCodec.ofMember(MachineOutputDirectionPacket::encode, MachineOutputDirectionPacket::new);
    public static final IPayloadHandler<MachineOutputDirectionPacket> HANDLER = new DirectionalPayloadHandler<>(
            MachineOutputDirectionPacket::clientHandler, MachineOutputDirectionPacket::serverHandler);
    private final Direction direction;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public MachineOutputDirectionPacket(Direction direction) {
        this.direction = direction;
    }

    public MachineOutputDirectionPacket(@NotNull FriendlyByteBuf buf) {
        this(buf.readEnum(Direction.class));
    }

    public void encode(@NotNull RegistryFriendlyByteBuf buf) {
        buf.writeEnum(this.getDirection());
    }

    /**
     *
     */
    public static void serverHandler(MachineOutputDirectionPacket data, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        context.enqueueWork(() -> {
            if (!player.hasContainerOpen()) return;
            if (!(player.containerMenu instanceof BaseMachineMenu menu)) return;
            Direction direction = data.getDirection();
            menu.setDirection(direction);
            PacketDistributor.sendToPlayer(player, data);
        });
    }

    /**
     *
     */
    public static void clientHandler(MachineOutputDirectionPacket data, IPayloadContext context) {
        Minecraft client = Minecraft.getInstance();
        context.enqueueWork(() -> {
            if (!(client.screen instanceof BaseMachineScreen<?> screen)) return;
            if (screen.getDirectionButton() == null) return;
            screen.getDirectionButton().setDirection(data.direction);
        });
    }
}
