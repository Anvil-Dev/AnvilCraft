package dev.dubhe.anvilcraft.network.split;

import dev.anvilcraft.lib.v2.network.packet.IInsensitiveBiPacket;
import dev.dubhe.anvilcraft.AnvilCraft;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class PacketSplitter {
    public static final PacketSplitter INSTANCE = new PacketSplitter();
    private static final int DEFAULT_PART_SIZE = 1640;
    private final ExecutorService workThread = Executors.newFixedThreadPool(2);

    public PacketSplitter() {
    }

    public <T extends CustomPacketPayload> void split(
        final CustomPacketPayload.Type<T> type,
        final StreamCodec<? super FriendlyByteBuf, T> codec,
        final T payload,
        Consumer<CustomPacketPayload> sender
    ) {
        this.split(type, codec, payload, PacketSplitter.DEFAULT_PART_SIZE, sender);
    }

    public <T extends CustomPacketPayload> void split(
        final CustomPacketPayload.Type<T> type,
        final StreamCodec<? super FriendlyByteBuf, T> codec,
        final T payload,
        int partSize,
        Consumer<CustomPacketPayload> sender
    ) {
        this.workThread.submit(() -> {
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            codec.encode(buffer, payload);
            buffer.capacity(buffer.readableBytes());
            int bufferSize = buffer.readableBytes();
            if (bufferSize <= partSize) {
                sender.accept(payload);
                buffer.release();
                return;
            }

            UUID id = UUID.randomUUID();
            sender.accept(new SplitPacketHeader(id, Math.ceilDiv(bufferSize, partSize), type.id()));
            int i = 0;
            for (int index = 0; index < bufferSize; index += partSize) {
                int resolvedPartSize = Math.min(bufferSize - index, partSize);
                byte[] data = new byte[resolvedPartSize];
                buffer.readBytes(data);
                sender.accept(new SplitPacketBody(id, i, data));
                i++;
            }
            buffer.release();
        });
    }

    public <T extends CustomPacketPayload> void split(
        final CustomPacketPayload.Type<T> type,
        final StreamCodec<RegistryFriendlyByteBuf, T> codec,
        final T payload,
        RegistryAccess registryAccess,
        Consumer<CustomPacketPayload> sender
    ) {
        this.split(type, codec, payload, PacketSplitter.DEFAULT_PART_SIZE, registryAccess, sender);
    }

    public <T extends CustomPacketPayload> void split(
        final CustomPacketPayload.Type<T> type,
        final StreamCodec<RegistryFriendlyByteBuf, T> codec,
        final T payload,
        int partSize,
        RegistryAccess registryAccess,
        Consumer<CustomPacketPayload> sender
    ) {
        this.workThread.submit(() -> {
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess, ConnectionType.NEOFORGE);
            codec.encode(buffer, payload);
            buffer.capacity(buffer.readableBytes());
            int bufferSize = buffer.readableBytes();
            if (bufferSize <= partSize) {
                sender.accept(payload);
                buffer.release();
                return;
            }

            UUID id = UUID.randomUUID();
            sender.accept(new SplitPacketHeader(id, Math.ceilDiv(bufferSize, partSize), type.id()));
            int i = 0;
            for (int index = 0; index < bufferSize; index += partSize) {
                int resolvedPartSize = Math.min(bufferSize - index, partSize);
                byte[] data = new byte[resolvedPartSize];
                buffer.readBytes(data);
                sender.accept(new SplitPacketBody(id, i, data));
                i++;
            }
            buffer.release();
        });
    }

    record SplitPacketHeader(UUID id, int total, ResourceLocation typeId) implements IInsensitiveBiPacket {
        public static final Type<SplitPacketHeader> TYPE = new Type<>(AnvilCraft.of("split_packet_header"));
        public static final StreamCodec<ByteBuf, SplitPacketHeader> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            SplitPacketHeader::id,
            ByteBufCodecs.VAR_INT,
            SplitPacketHeader::total,
            ResourceLocation.STREAM_CODEC,
            SplitPacketHeader::typeId,
            SplitPacketHeader::new
        );

        @Override
        public Type<SplitPacketHeader> type() {
            return TYPE;
        }

        @Override
        public void bidirectionalHandler(IPayloadContext ctx) {
            PacketCollector.header(this, ctx);
        }

        @Override
        public void handleOnBothSide(Player player) {
        }
    }

    record SplitPacketBody(UUID id, int index, byte[] data) implements IInsensitiveBiPacket {
        public static final Type<SplitPacketBody> TYPE = new Type<>(AnvilCraft.of("split_packet_body"));
        public static final StreamCodec<ByteBuf, SplitPacketBody> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            SplitPacketBody::id,
            ByteBufCodecs.VAR_INT,
            SplitPacketBody::index,
            ByteBufCodecs.BYTE_ARRAY,
            SplitPacketBody::data,
            SplitPacketBody::new
        );

        @Override
        public Type<SplitPacketBody> type() {
            return TYPE;
        }

        @Override
        public void handleOnBothSide(Player player) {
            PacketCollector.body(this);
        }
    }
}
