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
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

public final class StorageServerStub {
    private static final int MAX_SYNC_SLOTS = 256;
    public static final int PICKUP = 0;
    public static final int QUICK_MOVE_FROM_STORAGE = 1;
    public static final int QUICK_MOVE_TO_STORAGE = 2;
    public static final int CLONE = 3;
    public static final int THROW = 4;
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

    @CallableParam(clazz = InteractionResult.class, field = "STREAM_CODEC")
    @RemoteCallable(validator = StorageAccessValidator.class)
    public static InteractionResult interact(
        @CallableParam(clazz = UUIDUtil.class, field = "STREAM_CODEC") UUID playerId,
        long sourcePos,
        int slot,
        int button,
        int action
    ) {
        if (action < StorageServerStub.PICKUP || action > StorageServerStub.THROW) {
            throw new IllegalArgumentException("Invalid storage interaction action: " + action);
        }
        if (
            action == StorageServerStub.PICKUP && button != 0 && button != 1
            || action == StorageServerStub.THROW && (button < 0 || button > 2)
        ) {
            throw new IllegalArgumentException("Invalid storage interaction button: " + button);
        }

        BaseStorage storage = StorageServerStub.getStorage(playerId, sourcePos);
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        TypeLimitItemStacksResourceHandler items = storage.getItems();
        ItemStack carried = player.inventoryMenu.getCarried();
        boolean changed = false;
        if (action == StorageServerStub.QUICK_MOVE_TO_STORAGE) {
            changed = StorageServerStub.moveInventoryStackToStorage(player, items, slot);
        } else if (action == StorageServerStub.CLONE) {
            if (
                player.hasInfiniteMaterials()
                && carried.isEmpty()
                && slot >= 0
                && slot < items.size()
                && items.getAmountAsLong(slot) > 0
            ) {
                ItemStack stack = items.getResource(slot).toStack();
                carried = stack.copyWithCount(stack.getMaxStackSize());
                player.inventoryMenu.setCarried(carried);
            }
        } else if (action == StorageServerStub.THROW) {
            changed = StorageServerStub.throwStorageStack(player, items, slot, button);
        } else if (action == StorageServerStub.QUICK_MOVE_FROM_STORAGE) {
            changed = StorageServerStub.moveStorageStackToInventory(player, items, slot);
        } else if (!carried.isEmpty()) {
            int amount = button == 0 ? carried.getCount() : 1;
            try (Transaction transaction = Transaction.openRoot()) {
                int inserted = items.insert(ItemResource.of(carried), amount, transaction);
                if (inserted > 0) {
                    transaction.commit();
                    carried.shrink(inserted);
                    changed = true;
                }
            }
        } else if (slot >= 0 && slot < items.size() && items.getAmountAsLong(slot) > 0) {
            ItemResource resource = items.getResource(slot);
            ItemStack itemStack = resource.toStack();
            int count = Math.toIntExact(items.getAmountAsLong(slot));
            int maxPickup = Math.min(itemStack.getMaxStackSize(), count);
            int amount = button == 0 ? maxPickup : Math.ceilDiv(maxPickup, 2);
            try (Transaction transaction = Transaction.openRoot()) {
                int extracted = items.extract(slot, resource, amount, transaction);
                if (extracted > 0) {
                    transaction.commit();
                    carried = itemStack.copyWithCount(extracted);
                    player.inventoryMenu.setCarried(carried);
                    changed = true;
                }
            }
        }
        if (changed) {
            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();
        }
        return new InteractionResult(carried, changed);
    }

    private static boolean moveInventoryStackToStorage(
        ServerPlayer player,
        TypeLimitItemStacksResourceHandler items,
        int slot
    ) {
        Inventory inventory = player.getInventory();
        if (slot < 0 || slot >= Inventory.INVENTORY_SIZE) {
            return false;
        }
        ItemStack stack = inventory.getItem(slot);
        if (stack.isEmpty()) {
            return false;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = items.insert(ItemResource.of(stack), stack.getCount(), transaction);
            if (inserted <= 0) {
                return false;
            }
            transaction.commit();
            stack.shrink(inserted);
            return true;
        }
    }

    private static boolean moveStorageStackToInventory(
        ServerPlayer player,
        TypeLimitItemStacksResourceHandler items,
        int slot
    ) {
        if (slot < 0 || slot >= items.size() || items.getAmountAsLong(slot) <= 0) {
            return false;
        }
        ItemResource resource = items.getResource(slot);
        ItemStack stack = resource.toStack();
        int amount = Math.min(
            Math.toIntExact(items.getAmountAsLong(slot)),
            StorageServerStub.getInventorySpace(player.getInventory(), stack)
        );
        if (amount <= 0) {
            return false;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            int extracted = items.extract(slot, resource, amount, transaction);
            if (extracted <= 0) {
                return false;
            }
            transaction.commit();
            player.getInventory().add(stack.copyWithCount(extracted));
            return true;
        }
    }

    private static int getInventorySpace(Inventory inventory, ItemStack stack) {
        long space = 0;
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack existing = inventory.getItem(slot);
            if (existing.isEmpty()) {
                space += stack.getMaxStackSize();
            } else if (ItemStack.isSameItemSameComponents(existing, stack) && existing.isStackable()) {
                space += Math.max(0, inventory.getMaxStackSize(existing) - existing.getCount());
            }
        }
        return (int) Math.min(Integer.MAX_VALUE, space);
    }

    private static boolean throwStorageStack(
        ServerPlayer player,
        TypeLimitItemStacksResourceHandler items,
        int slot,
        int button
    ) {
        if (
            !player.inventoryMenu.getCarried().isEmpty()
            || !player.canDropItems()
            || slot < 0
            || slot >= items.size()
            || items.getAmountAsLong(slot) <= 0
        ) {
            return false;
        }
        ItemResource resource = items.getResource(slot);
        ItemStack stack = resource.toStack();
        int stackCount = stack.getMaxStackSize();
        long requested = button == 0 ? 1 : (long) stackCount * (button == 1 ? 1 : 9);
        int amount = Math.toIntExact(Math.min(items.getAmountAsLong(slot), requested));
        try (Transaction transaction = Transaction.openRoot()) {
            int extracted = items.extract(slot, resource, amount, transaction);
            if (extracted <= 0) {
                return false;
            }
            transaction.commit();
            int remaining = extracted;
            while (remaining > 0) {
                int dropCount = Math.min(stackCount, remaining);
                ItemStack dropped = stack.copyWithCount(dropCount);
                player.drop(dropped, true);
                player.handleCreativeModeItemDrop(dropped);
                remaining -= dropCount;
            }
            return true;
        }
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
            UnlimitedItemStack.OPTIONAL_STREAM_CODEC,
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

    public record InteractionResult(ItemStack carried, boolean changed) {
        public static final StreamCodec<RegistryFriendlyByteBuf, InteractionResult> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC,
            InteractionResult::carried,
            ByteBufCodecs.BOOL,
            InteractionResult::changed,
            InteractionResult::new
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
        String search = setting.getSearchContent().strip().toLowerCase(Locale.ROOT);
        if (!search.isEmpty()) {
            return StorageServerStub.createOrder(items, options, search);
        }
        return this.orders.computeIfAbsent(options, _ -> StorageServerStub.createOrder(items, options, ""));
    }

    private static IntList createOrder(TypeLimitItemStacksResourceHandler items, SortOptions options, String search) {
        List<OrderEntry> entries = new ArrayList<>(items.size());
        boolean requiresName = options.sort() == SortMode.NAME
            || search.isEmpty()
            || search.charAt(0) != '@' && search.charAt(0) != '#';
        for (int index = 0; index < items.size(); index++) {
            long amount = items.getAmountAsLong(index);
            if (amount <= 0) {
                continue;
            }
            ItemResource resource = items.getResource(index);
            ItemStack stack = resource.toStack();
            Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            String name = requiresName ? stack.getHoverName().getString() : "";
            if (!StorageServerStub.matchesSearch(resource.typeHolder(), id, name, search)) {
                continue;
            }
            entries.add(new OrderEntry(index, amount, id, name));
        }

        Comparator<OrderEntry> comparator = StorageServerStub.getComparator(options);
        entries.sort(comparator);

        IntArrayList order = new IntArrayList(entries.size());
        for (OrderEntry entry : entries) {
            order.add(entry.index());
        }
        return order;
    }

    private static boolean matchesSearch(Holder<Item> item, Identifier id, String name, String search) {
        if (search.isEmpty()) {
            return true;
        }
        if (search.charAt(0) == '@') {
            return id.getNamespace().toLowerCase(Locale.ROOT).contains(search.substring(1));
        }
        if (search.charAt(0) == '#') {
            String tagSearch = search.substring(1);
            return item.tags().anyMatch(tag -> StorageServerStub.matchesTag(tag.location(), tagSearch));
        }
        return name.toLowerCase(Locale.ROOT).contains(search)
            || id.getPath().toLowerCase(Locale.ROOT).contains(search);
    }

    private static boolean matchesTag(Identifier id, String search) {
        return id.toString().toLowerCase(Locale.ROOT).contains(search)
            || id.getPath().toLowerCase(Locale.ROOT).contains(search);
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
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        BlockEntity blockEntity = player.level().getBlockEntity(BlockPos.of(sourcePos));
        if (!(blockEntity instanceof StorageBlockEntity storage) || storage.getId() == null) {
            throw new IllegalStateException("Cannot access storage without a storage block entity");
        }
        return Storages.get().get(storage.getId()).orElseThrow();
    }

    private static ServerPlayer getServerPlayer(UUID playerId) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            throw new IllegalStateException("Cannot access storage without a running server");
        }
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            throw new IllegalStateException("Cannot access storage without a server player");
        }
        return player;
    }

    private StorageServerStub() {
    }

    private record SortOptions(SortMode sort, OrderMode order) {
    }

    private record OrderEntry(int index, long amount, Identifier id, String name) {
    }
}
