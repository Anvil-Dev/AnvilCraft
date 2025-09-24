package dev.dubhe.anvilcraft.network.split;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.constant.Constants;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class PacketSplitter {
    public static final PacketSplitter INSTANCE = new PacketSplitter();
    private final ExecutorService workThread = Executors.newFixedThreadPool(2);

    public PacketSplitter() {
    }

    public <T extends CustomPacketPayload> void split(
        final CustomPacketPayload.Type<T> type,
        final StreamCodec<? super RegistryFriendlyByteBuf, T> codec,
        final T payload,
        int partSize,
        Consumer<CustomPacketPayload> sender
    ) {
        workThread.submit(() -> {
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY, ConnectionType.NEOFORGE);
            codec.encode(buffer, payload);
            buffer.capacity(buffer.readableBytes());
            int bufferSize = buffer.readableBytes();
            if (bufferSize <= partSize) {
                sender.accept(payload);
                return;
            }

            UUID id = UUID.randomUUID();
            sender.accept(new SplitPacketHeader(id, Math.ceilDiv(bufferSize, partSize), type));
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

    record SplitPacketHeader(UUID id, int total, Type<?> type) implements CustomPacketPayload {
        public static final Type<SplitPacketHeader> TYPE = new Type<>(AnvilCraft.of("split_packet_header"));
        public static final StreamCodec<ByteBuf, SplitPacketHeader> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            SplitPacketHeader::id,
            ByteBufCodecs.VAR_INT,
            SplitPacketHeader::total,
            Constants.PAYLOAD_TYPE_STREAM_CODEC,
            SplitPacketHeader::type,
            SplitPacketHeader::new
        );
        public static final IPayloadHandler<SplitPacketHeader> HANDLER = PacketCollector::header;

        @Override
        public Type<? extends CustomPacketPayload> type() {
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
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
