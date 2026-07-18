package dev.dubhe.anvilcraft.rpc;

import dev.anvilcraft.lib.v2.rpc.CallableParam;
import dev.anvilcraft.lib.v2.rpc.IRemoteCallableValidator;
import dev.anvilcraft.lib.v2.rpc.RemoteCallable;
import dev.anvilcraft.lib.v2.util.UnlimitedItemStack;
import dev.dubhe.anvilcraft.api.itemhandler.TypeLimitItemStacksResourceHandler;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.saved.setting.PlayerSettings;
import dev.dubhe.anvilcraft.saved.setting.StorageSetting;
import dev.dubhe.anvilcraft.saved.setting.mode.OrderMode;
import dev.dubhe.anvilcraft.saved.setting.mode.SortMode;
import dev.dubhe.anvilcraft.saved.storage.BaseStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public final class StorageServerStub {
    private static final int MAX_SYNC_SLOTS = 256;
    public static final StreamCodec<ByteBuf, IntList> ORDER_STREAM_CODEC = ByteBufCodecs.VAR_INT
        .apply(ByteBufCodecs.list())
        .map(IntArrayList::new, Function.identity());
    private static final Map<UUID, StorageServerStub> STUBS = new HashMap<>();

    private long version;
    private long orderVersion;
    private final Map<SortOptions, IntList> orders = new HashMap<>();

    @CallableParam(clazz = Metadata.class, field = "STREAM_CODEC")
    @RemoteCallable(validator = StorageAccessValidator.class)
    public static Metadata load(
        @CallableParam(clazz = UUIDUtil.class, field = "STREAM_CODEC") UUID playerId,
        long sourcePos
    ) {
        BaseStorage storage = StorageServerStub.getStorage(playerId, sourcePos);
        StorageServerStub stub = StorageServerStub.get(storage.getId());
        return new Metadata(stub.version, stub.orderVersion, storage.getItems().getFullness());
    }

    @CallableParam(clazz = StorageServerStub.class, field = "ORDER_STREAM_CODEC")
    @RemoteCallable(validator = StorageAccessValidator.class)
    public static IntList reorder(
        @CallableParam(clazz = UUIDUtil.class, field = "STREAM_CODEC") UUID playerId,
        long sourcePos
    ) {
        BaseStorage storage = StorageServerStub.getStorage(playerId, sourcePos);
        StorageServerStub stub = StorageServerStub.get(storage.getId());
        StorageSetting setting = PlayerSettings.getSetting(playerId).storage();
        IntList order = stub.getOrder(storage.getItems(), setting);
        return new IntArrayList(order);
    }

    @CallableParam(clazz = SyncResult.class, field = "STREAM_CODEC")
    @RemoteCallable(validator = StorageAccessValidator.class)
    public static SyncResult sync(
        @CallableParam(clazz = UUIDUtil.class, field = "STREAM_CODEC") UUID playerId,
        long sourcePos,
        @CallableParam(clazz = StorageServerStub.class, field = "ORDER_STREAM_CODEC") IntList slots
    ) {
        if (slots.size() > StorageServerStub.MAX_SYNC_SLOTS) {
            throw new IllegalArgumentException("Cannot sync more than " + StorageServerStub.MAX_SYNC_SLOTS + " slots at once");
        }

        BaseStorage storage = StorageServerStub.getStorage(playerId, sourcePos);
        StorageServerStub stub = StorageServerStub.get(storage.getId());
        TypeLimitItemStacksResourceHandler items = storage.getItems();
        List<StackUpdate> updates = new ArrayList<>();
        IntOpenHashSet visited = new IntOpenHashSet(slots.size());
        for (int index : slots) {
            if (index < 0 || !visited.add(index)) {
                continue;
            }
            updates.add(new StackUpdate(index, StorageServerStub.getStack(items, index)));
        }
        return new SyncResult(
            stub.version,
            storage.getItems().getFullness(),
            updates
        );
    }

    public static void onContentsChanged(UUID storageId) {
        StorageServerStub stub = StorageServerStub.get(storageId);
        stub.version++;
        stub.orderVersion = stub.version;
        stub.orders.clear();
    }

    public static void clear() {
        StorageServerStub.STUBS.clear();
    }

    public static final class StorageAccessValidator implements IRemoteCallableValidator {
        @Override
        public boolean validate(@NonNull IPayloadContext ctx, @NonNull Method method, Object @NonNull [] args) {
            if (
                !(ctx.player() instanceof ServerPlayer player)
                || args.length < 2
                || !(args[0] instanceof UUID playerId)
                || !player.getGameProfile().id().equals(playerId)
                || !(args[1] instanceof Long sourcePos)
            ) {
                return false;
            }
            BlockPos pos = BlockPos.of(sourcePos);
            BlockEntity blockEntity = player.level().getBlockEntity(pos);
            return blockEntity instanceof StorageBlockEntity storage
                && storage.getId() != null
                && Storages.get().get(storage.getId()).isPresent()
                && AbstractContainerMenu.stillValid(
                    ContainerLevelAccess.create(player.level(), pos),
                    player,
                    storage.getBlockState().getBlock()
                );
        }
    }

    public record Metadata(long version, long orderVersion, double fullness) {
        public static final StreamCodec<ByteBuf, Metadata> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG,
            Metadata::version,
            ByteBufCodecs.VAR_LONG,
            Metadata::orderVersion,
            ByteBufCodecs.DOUBLE,
            Metadata::fullness,
            Metadata::new
        );
    }

    public record StackUpdate(int index, UnlimitedItemStack stack) {
        public static final StreamCodec<RegistryFriendlyByteBuf, StackUpdate> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            StackUpdate::index,
            UnlimitedItemStack.STREAM_CODEC,
            StackUpdate::stack,
            StackUpdate::new
        );
    }

    public record SyncResult(long version, double fullness, List<StackUpdate> updates) {
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncResult> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG,
            SyncResult::version,
            ByteBufCodecs.DOUBLE,
            SyncResult::fullness,
            StackUpdate.STREAM_CODEC.apply(ByteBufCodecs.list()),
            SyncResult::updates,
            SyncResult::new
        );
    }

    private static StorageServerStub get(UUID storageId) {
        return StorageServerStub.STUBS.computeIfAbsent(storageId, _ -> new StorageServerStub());
    }

    private static UnlimitedItemStack getStack(TypeLimitItemStacksResourceHandler items, int index) {
        if (index >= items.size() || items.getAmountAsLong(index) <= 0) {
            return UnlimitedItemStack.EMPTY;
        }
        return new UnlimitedItemStack(items.getResource(index), Math.toIntExact(items.getAmountAsLong(index)));
    }

    private IntList getOrder(TypeLimitItemStacksResourceHandler items, StorageSetting setting) {
        SortOptions options = new SortOptions(setting.getSort(), setting.getOrder());
        return this.orders.computeIfAbsent(options, _ -> StorageServerStub.createOrder(items, options));
    }

    private static IntList createOrder(TypeLimitItemStacksResourceHandler items, SortOptions options) {
        List<OrderEntry> entries = new ArrayList<>(items.size());
        for (int index = 0; index < items.size(); index++) {
            long amount = items.getAmountAsLong(index);
            if (amount <= 0) {
                continue;
            }
            var stack = items.getResource(index).toStack();
            Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            entries.add(new OrderEntry(index, amount, id, stack.getHoverName().getString()));
        }

        Comparator<OrderEntry> comparator = StorageServerStub.getComparator(options);
        entries.sort(comparator);

        IntArrayList order = new IntArrayList(entries.size());
        for (OrderEntry entry : entries) {
            order.add(entry.index());
        }
        return order;
    }

    private static @NonNull Comparator<OrderEntry> getComparator(SortOptions options) {
        Comparator<OrderEntry> comparator = switch (options.sort()) {
            case COUNT -> Comparator.comparingLong(OrderEntry::amount);
            case MOD -> Comparator.comparing(entry -> entry.id().getNamespace());
            case NAME -> Comparator.comparing(OrderEntry::name, String.CASE_INSENSITIVE_ORDER);
        };
        comparator = comparator
            .thenComparing(entry -> entry.id().toString())
            .thenComparingInt(OrderEntry::index);
        if (options.order() == OrderMode.REVERSE) {
            comparator = comparator.reversed();
        }
        return comparator;
    }

    private static BaseStorage getStorage(UUID playerId, long sourcePos) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            throw new IllegalStateException("Cannot access storage without a running server");
        }
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            throw new IllegalStateException("Cannot access storage without a server player");
        }
        BlockEntity blockEntity = player.level().getBlockEntity(BlockPos.of(sourcePos));
        if (!(blockEntity instanceof StorageBlockEntity storage) || storage.getId() == null) {
            throw new IllegalStateException("Cannot access storage without a storage block entity");
        }
        return Storages.get().get(storage.getId()).orElseThrow();
    }

    private StorageServerStub() {
    }

    private record SortOptions(SortMode sort, OrderMode order) {
    }

    private record OrderEntry(int index, long amount, Identifier id, String name) {
    }
}
