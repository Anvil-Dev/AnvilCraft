package dev.dubhe.anvilcraft.network.multiple;

import dev.anvilcraft.lib.v2.network.packet.IClientboundPacket;
import dev.anvilcraft.lib.v2.network.packet.IPacket;
import dev.anvilcraft.lib.v2.network.packet.IServerboundPacket;
import dev.anvilcraft.lib.v2.util1.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.inventory.state.StorageMenuState;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.jetbrains.annotations.ApiStatus;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public class StoragePackets {
    private static <T extends IPacket> CustomPacketPayload.Type<T> of(String id) {
        return IPacket.type(AnvilCraft.of("storage_" + id));
    }

    public record SyncSlots(UUID id, IntList slots) implements IServerboundPacket {
        public static final Type<SyncSlots> TYPE = StoragePackets.of("sync_slots");
        public static final StreamCodec<ByteBuf, SyncSlots> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            SyncSlots::id,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list())
                .map(IntArrayList::new, Function.identity()),
            SyncSlots::slots,
            SyncSlots::new
        );

        @Override
        public Type<SyncSlots> type() {
            return SyncSlots.TYPE;
        }

        @Override
        public void handleOnServer(Player player) {
            StorageMenuState.get(this.id).sync(this.slots);
        }
    }

    public record Sync2CFull(
        UUID id,
        int head,
        @Nullable List<UnlimitedItemStack> stacks,
        int encodedCount,
        @Nullable RegistryFriendlyByteBuf buf
    ) implements IClientboundPacket {
        public static final Type<Sync2CFull> TYPE = StoragePackets.of("sync2c_full");
        public static final StreamCodec<RegistryFriendlyByteBuf, Sync2CFull> STREAM_CODEC = StreamCodec.of(
            Sync2CFull::encode,
            Sync2CFull::decode
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, List<UnlimitedItemStack>> STACKS_STREAM_CODEC = UnlimitedItemStack
            .STREAM_CODEC
            .apply(ByteBufCodecs.list());

        /// 服务器侧
        @ApiStatus.Internal
        private Sync2CFull(UUID id, int head, int encodedCount, RegistryFriendlyByteBuf buf) {
            this(id, head, null, encodedCount, buf);
        }

        /// 客户端侧
        @ApiStatus.Internal
        private Sync2CFull(UUID id, int head, List<UnlimitedItemStack> stacks) {
            this(id, head, stacks, stacks.size(), null);
        }

        private static Sync2CFull decode(RegistryFriendlyByteBuf buf) {
            return new Sync2CFull(
                buf.readUUID(),
                buf.readVarInt(),
                Sync2CFull.STACKS_STREAM_CODEC.decode(buf)
            );
        }

        private static void encode(RegistryFriendlyByteBuf buf, Sync2CFull data) {
            buf.writeUUID(data.id);
            buf.writeVarInt(data.head);
            buf.writeVarInt(data.encodedCount);
            if (data.buf == null) {
                throw new UnsupportedOperationException("Use the FullSyncer");
            }
            buf.ensureWritable(data.buf.readableBytes());
            buf.setBytes(buf.writerIndex(), data.buf, data.buf.readableBytes());
        }

        @Override
        public Type<Sync2CFull> type() {
            return TYPE;
        }

        @Override
        public void handleOnClient(Player player) {
            StorageMenuState.get(this.id).sync(this.head, this.stacks);
        }
    }

    public static void sync(ServerPlayer player, UUID id, RegistryAccess registries) {
        new FullSyncer(player, StorageMenuState.get(id), registries).sync();
    }

    public static class FullSyncer {
        /// 单个包的最大容量
        private static final int UNCOMPRESSED_PACKET_LIMIT = 512 * 1024;
        /// 包的初始缓冲区大小
        private static final int INITIAL_BUFFER_CAPACITY = 2 * 1024;
        private final ServerPlayer player;
        private final StorageMenuState state;
        private final RegistryAccess registries;

        private final List<Sync2CFull> packets = new ArrayList<>();

        private int head = -1;
        private int encodedCount = 0;
        @Nullable
        private RegistryFriendlyByteBuf buf;

        private FullSyncer(ServerPlayer player, StorageMenuState state, RegistryAccess registries) {
            this.player = player;
            this.state = state;
            this.registries = registries;
        }

        private void sync() {
            Map<Integer, UnlimitedItemStack> changes = this.state.getChanges();

            // 找到最大值
            int max = -1;
            for (int index : changes.keySet()) {
                if (index > max) {
                    max = index;
                }
            }
            if (max == -1) {
                return;
            }

            // 遍历
            for (int i = 0; i < max; i++) {
                RegistryFriendlyByteBuf data = this.ensureBuf();

                UnlimitedItemStack stack = changes.get(i);
                // 物品栈为空
                if (stack == null || stack.isEmpty()) {
                    // 已写入数据则尝试刷入下一个包
                    if (data.writerIndex() > 0) {
                        this.flush();
                    }
                    if (stack.isEmpty()) {
                        changes.remove(i);
                    }
                    continue;
                }

                // 只有当包内存超过约 2 兆字节时才会报错，
                // 如果任何物品栈在其共享标签中写入了这么多垃圾数据，崩溃是可以接受的。
                // 我们通常会更早（32k数据）刷入下一个包
                UnlimitedItemStack.STREAM_CODEC.encode(data, stack);
                this.head = i;
                this.encodedCount++;

                if (data.writerIndex() >= UNCOMPRESSED_PACKET_LIMIT || this.encodedCount >= Short.MAX_VALUE) {
                    this.flush();
                }
            }

            // 发包
            if (!this.packets.isEmpty()) {
                Sync2CFull[] array = this.packets.toArray(new Sync2CFull[0]);
                int length = array.length - 1;
                Sync2CFull[] dest = new Sync2CFull[length];
                System.arraycopy(array, 1, dest, 0, length);
                PacketDistributor.sendToPlayer(this.player, this.packets.getFirst(), dest);
            }
        }

        private void flush() {
            if (this.buf != null) {
                // Build a packet and queue it
                var packet = new Sync2CFull(this.state.getId(), this.head, this.encodedCount, this.buf);
                this.packets.add(packet);

                // Reset
                this.encodedCount = 0;
                this.buf = null;
            }
        }

        private RegistryFriendlyByteBuf ensureBuf() {
            if (this.buf == null) {
                this.buf = new RegistryFriendlyByteBuf(
                    Unpooled.buffer(INITIAL_BUFFER_CAPACITY),
                    this.registries,
                    ConnectionType.NEOFORGE
                );
            }
            return this.buf;
        }
    }

    public record Sync2CIncremental(
        UUID id,
        @Nullable Map<Integer, UnlimitedItemStack> stacks,
        int encodedCount,
        @Nullable RegistryFriendlyByteBuf buf
    ) implements IClientboundPacket {
        public static final Type<Sync2CIncremental> TYPE = StoragePackets.of("sync2c_incremental");
        public static final StreamCodec<RegistryFriendlyByteBuf, Sync2CIncremental> STREAM_CODEC = StreamCodec.of(
            Sync2CIncremental::encode,
            Sync2CIncremental::decode
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, Map<Integer, UnlimitedItemStack>> STACKS_STREAM_CODEC = ByteBufCodecs.map(
            HashMap::new,
            ByteBufCodecs.VAR_INT,
            UnlimitedItemStack.STREAM_CODEC
        );

        /// 服务器侧
        private Sync2CIncremental(UUID id, int encodedCount, RegistryFriendlyByteBuf buf) {
            this(id, null, encodedCount, buf);
        }

        /// 客户端侧
        private Sync2CIncremental(UUID id, Map<Integer, UnlimitedItemStack> stacks) {
            this(id, stacks, stacks.size(), null);
        }

        private static Sync2CIncremental decode(RegistryFriendlyByteBuf buf) {
            return new Sync2CIncremental(
                buf.readUUID(),
                Sync2CIncremental.STACKS_STREAM_CODEC.decode(buf)
            );
        }

        private static void encode(RegistryFriendlyByteBuf buf, Sync2CIncremental data) {
            buf.writeUUID(data.id);
            buf.writeVarInt(data.encodedCount);
            if (data.buf == null) {
                throw new UnsupportedOperationException("Use the IncrementalSyncer");
            }
            buf.ensureWritable(data.buf.readableBytes());
            buf.setBytes(buf.writerIndex(), data.buf, data.buf.readableBytes());
        }

        @Override
        public Type<Sync2CIncremental> type() {
            return TYPE;
        }

        @Override
        public void handleOnClient(Player player) {
            StorageMenuState.get(this.id).sync(this.stacks);
        }
    }

    public static void syncIncremental(ServerPlayer player, UUID id, RegistryAccess registries) {
        new IncrementalSyncer(player, StorageMenuState.get(id), registries).sync();
    }

    public static class IncrementalSyncer {
        /// 单个包的最大容量
        private static final int UNCOMPRESSED_PACKET_LIMIT = 512 * 1024;
        /// 包的初始缓冲区大小
        private static final int INITIAL_BUFFER_CAPACITY = 2 * 1024;
        private final ServerPlayer player;
        private final StorageMenuState state;
        private final RegistryAccess registries;

        private final List<Sync2CIncremental> packets = new ArrayList<>();

        private int encodedCount = 0;
        @Nullable
        private RegistryFriendlyByteBuf buf;

        private IncrementalSyncer(ServerPlayer player, StorageMenuState state, RegistryAccess registries) {
            this.player = player;
            this.state = state;
            this.registries = registries;
        }

        private void sync() {
            Map<Integer, UnlimitedItemStack> changes = this.state.getChanges();

            // 遍历
            for (Iterator<Integer> iterator = changes.keySet().iterator(); iterator.hasNext(); ) {
                int i = iterator.next();
                RegistryFriendlyByteBuf data = this.ensureBuf();

                UnlimitedItemStack stack = changes.get(i);
                // 物品栈为空则直接继续
                if (stack == null || stack.isEmpty()) {
                    iterator.remove();
                    continue;
                }

                // 只有当包内存超过约 2 兆字节时才会报错，
                // 如果任何物品栈在其共享标签中写入了这么多垃圾数据，崩溃是可以接受的。
                // 我们通常会更早（32k数据）刷入下一个包
                data.writeVarInt(i);
                UnlimitedItemStack.STREAM_CODEC.encode(data, stack);
                this.encodedCount++;
                iterator.remove();

                if (data.writerIndex() >= UNCOMPRESSED_PACKET_LIMIT || this.encodedCount >= Short.MAX_VALUE) {
                    this.flush();
                }
            }

            // 发包
            if (!this.packets.isEmpty()) {
                Sync2CIncremental[] array = this.packets.toArray(new Sync2CIncremental[0]);
                int length = array.length - 1;
                Sync2CIncremental[] dest = new Sync2CIncremental[length];
                System.arraycopy(array, 0, dest, 1, length);
                PacketDistributor.sendToPlayer(this.player, this.packets.getFirst(), dest);
            }
        }

        private void flush() {
            if (this.buf != null) {
                // Build a packet and queue it
                var packet = new Sync2CIncremental(this.state.getId(), this.encodedCount, this.buf);
                this.packets.add(packet);

                // Reset
                this.encodedCount = 0;
                this.buf = null;
            }
        }

        private RegistryFriendlyByteBuf ensureBuf() {
            if (this.buf == null) {
                this.buf = new RegistryFriendlyByteBuf(
                    Unpooled.buffer(INITIAL_BUFFER_CAPACITY),
                    this.registries,
                    ConnectionType.NEOFORGE
                );
            }
            return this.buf;
        }
    }

    public record SyncFullness(UUID id, double fullness) implements IServerboundPacket {
        public static final Type<SyncFullness> TYPE = StoragePackets.of("sync_fullness");
        public static final StreamCodec<ByteBuf, SyncFullness> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC,
            SyncFullness::id,
            ByteBufCodecs.DOUBLE,
            SyncFullness::fullness,
            SyncFullness::new
        );

        @Override
        public Type<SyncFullness> type() {
            return SyncFullness.TYPE;
        }

        @Override
        public void handleOnServer(Player player) {
            StorageMenuState.get(this.id).setFullness(this.fullness);
        }
    }
}
