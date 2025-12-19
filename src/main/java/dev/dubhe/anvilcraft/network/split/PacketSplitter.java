package dev.dubhe.anvilcraft.network.split;

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
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

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
                var buffer1 = buffer.retainedSlice(buffer.readerIndex(), resolvedPartSize);
                buffer.skipBytes(resolvedPartSize);
                var packet = new SplitPacketBody(id, i, buffer1.array());
                sender.accept(packet);
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
                var buffer1 = buffer.retainedSlice(buffer.readerIndex(), resolvedPartSize);
                buffer.skipBytes(resolvedPartSize);
                sender.accept(new SplitPacketBody(id, i, buffer1.array()));
                i++;
            }
            buffer.release();
        });
    }

    public static void registerSplitPackets(PayloadRegistrar registrar) {
        registrar.playBidirectional(
            SplitPacketHeader.TYPE,
            SplitPacketHeader.STREAM_CODEC,
            SplitPacketHeader.HANDLER
        );
        registrar.playBidirectional(
            SplitPacketBody.TYPE,
            SplitPacketBody.STREAM_CODEC,
            SplitPacketBody.HANDLER
        );
    }

    record SplitPacketHeader(UUID id, int total, ResourceLocation typeId) implements CustomPacketPayload {
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
        public static final IPayloadHandler<SplitPacketHeader> HANDLER = PacketCollector::header;

        @Override
        public Type<SplitPacketHeader> type() {
            return TYPE;
        }
    }

    record SplitPacketBody(UUID id, int index, byte[] data) implements CustomPacketPayload {
        public static final Type<SplitPacketBody> TYPE = new Type<>(AnvilCraft.of("split_packet"));
        public static final StreamCodec<ByteBuf, SplitPacketBody> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            SplitPacketBody::id,
            ByteBufCodecs.VAR_INT,
            SplitPacketBody::index,
            ByteBufCodecs.BYTE_ARRAY,
            SplitPacketBody::data,
            SplitPacketBody::new
        );
        public static final IPayloadHandler<SplitPacketBody> HANDLER = PacketCollector::body;

        @Override
        public Type<SplitPacketBody> type() {
            return TYPE;
        }
    }
}
