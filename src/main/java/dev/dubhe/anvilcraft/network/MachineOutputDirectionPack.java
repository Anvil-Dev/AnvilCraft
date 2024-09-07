package dev.dubhe.anvilcraft.network;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.gui.screen.inventory.BaseMachineScreen;
import dev.dubhe.anvilcraft.inventory.BaseMachineMenu;
import lombok.Getter;
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
import org.jetbrains.annotations.NotNull;

@Getter
public class MachineOutputDirectionPack implements CustomPacketPayload {
    public static final Type<MachineOutputDirectionPack> TYPE = new Type<>(AnvilCraft.of("machine_output_direction"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MachineOutputDirectionPack> STREAM_CODEC = StreamCodec.ofMember(
            MachineOutputDirectionPack::encode,
            MachineOutputDirectionPack::new
    );
    public static final IPayloadHandler<MachineOutputDirectionPack> HANDLER = new DirectionalPayloadHandler<>(
            MachineOutputDirectionPack::clientHandler,
            MachineOutputDirectionPack::serverHandler
    );
    private final Direction direction;

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public MachineOutputDirectionPack(Direction direction) {
        this.direction = direction;
    }

    public MachineOutputDirectionPack(@NotNull FriendlyByteBuf buf) {
        this(buf.readEnum(Direction.class));
    }

    public void encode(@NotNull RegistryFriendlyByteBuf buf) {
        buf.writeEnum(this.getDirection());
    }

    public static void serverHandler(MachineOutputDirectionPack data, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        context.enqueueWork(() -> {
                    if (!player.hasContainerOpen()) return;
                    if (!(player.containerMenu instanceof BaseMachineMenu menu)) return;
                    Direction direction = data.getDirection();
                    menu.setDirection(direction);
                    PacketDistributor.sendToPlayer(player, data);
                }
        );
    }


    public static void clientHandler(MachineOutputDirectionPack data, IPayloadContext context) {
        Minecraft client = Minecraft.getInstance();
        context.enqueueWork(() -> {
            if (!(client.screen instanceof BaseMachineScreen<?> screen)) return;
            if (screen.getDirectionButton() == null) return;
            screen.getDirectionButton().setDirection(data.direction);
        });
    }
}
