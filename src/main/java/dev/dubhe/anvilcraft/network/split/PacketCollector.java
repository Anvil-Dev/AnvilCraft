package dev.dubhe.anvilcraft.network.split;

import dev.anvilcraft.lib.v2.util.Util;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.extensions.ICommonPacketListener;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.NetworkRegistry;

import java.util.HashMap;
import java.util.UUID;

public class PacketCollector {
    private static final HashMap<UUID, PacketCollector> COLLECTORS = new HashMap<>();
    private final ResourceLocation typeId;
    private final byte[][] received;

    private final RegistryAccess registryAccess;
    private final ICommonPacketListener listener;
    private final ConnectionProtocol protocol;
    private final PacketFlow flow;

    public PacketCollector(
        int total,
        ResourceLocation typeId,
        RegistryAccess registryAccess,
        ICommonPacketListener listener,
        ConnectionProtocol protocol,
        PacketFlow flow
    ) {
        this.typeId = typeId;
        this.received = new byte[total][];
        this.registryAccess = registryAccess;
        this.listener = listener;
        this.protocol = protocol;
        this.flow = flow;
    }

    static void header(PacketSplitter.SplitPacketHeader header, IPayloadContext ctx) {
        COLLECTORS.put(
            header.id(),
            new PacketCollector(header.total(), header.typeId(), ctx.player().registryAccess(), ctx.listener(), ctx.protocol(), ctx.flow())
        );
    }

    static void body(PacketSplitter.SplitPacketBody body) {
        COLLECTORS.get(body.id()).getBody(body);
    }

    private void getBody(PacketSplitter.SplitPacketBody body) {
        this.received[body.index()] = body.data();
        for (byte[] bytes : this.received) {
            if (bytes == null) return;
        }
        this.constructAndLoadPacket();
    }

    @SuppressWarnings({"UnstableApiUsage", "DataFlowIssue"})
    private void constructAndLoadPacket() {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), this.registryAccess, ConnectionType.NEOFORGE);
        for (byte[] bytes : this.received) {
            buf.writeBytes(bytes);
        }
        var packet = NetworkRegistry.getCodec(this.typeId, this.protocol, this.flow).decode(buf);
        if (this.flow.isServerbound()) {
            NetworkRegistry.handleModdedPayload(Util.cast(this.listener), packet.toVanillaServerbound());
        } else if (this.flow.isClientbound()) {
            NetworkRegistry.handleModdedPayload(Util.cast(this.listener), packet.toVanillaClientbound());
        }
    }
}
