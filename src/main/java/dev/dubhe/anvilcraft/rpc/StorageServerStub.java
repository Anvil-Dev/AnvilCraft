package dev.dubhe.anvilcraft.rpc;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import dev.anvilcraft.lib.v2.rpc.CallableParam;
import dev.anvilcraft.lib.v2.rpc.IRemoteCallableValidator;
import dev.anvilcraft.lib.v2.rpc.RemoteCallable;
import dev.anvilcraft.lib.v2.util.UnlimitedItemStack;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.SpaceSizeItemStacksResourceHandler;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.TypeLimitItemStacksResourceHandler;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler;
import dev.dubhe.anvilcraft.block.container.storage.CrateBlock;
import dev.dubhe.anvilcraft.block.entity.storage.CrateBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.ShulkerContainerBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.saved.setting.PlayerSetting;
import dev.dubhe.anvilcraft.saved.setting.PlayerSettings;
import dev.dubhe.anvilcraft.saved.setting.StorageSetting;
import dev.dubhe.anvilcraft.saved.setting.mode.OrderMode;
import dev.dubhe.anvilcraft.saved.setting.mode.SortMode;
import dev.dubhe.anvilcraft.saved.storage.BaseStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import dev.dubhe.anvilcraft.saved.storage.category.store.CategoryEntry;
import dev.dubhe.anvilcraft.saved.storage.category.store.CategoryMode;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
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
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

public final class StorageServerStub {
    private static final int MAX_PLAYER_STUBS = 5;
    private static final int MAX_SYNC_SLOTS = 256;
    private static final ThreadLocal<HolderLookup.@Nullable Provider> REGISTRIES = new ThreadLocal<>();
    @SuppressWarnings("unused")
    public static final StreamCodec<ByteBuf, IntList> ORDER_STREAM_CODEC = ByteBufCodecs.VAR_INT
        .apply(ByteBufCodecs.list())
        .map(IntArrayList::new, Function.identity());
    private static final Multimap<UUID, StorageServerStub> STUBS = ArrayListMultimap.create();

    private final UUID storageId;
    private long version;
    private long orderVersion;
    private final Map<SortOptions, IntList> orders = new HashMap<>();

    private static HolderLookup.Provider getAndClear() {
        HolderLookup.Provider registries = StorageServerStub.REGISTRIES.get();
        StorageServerStub.REGISTRIES.remove();
        return Objects.requireNonNull(registries);
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static Metadata load(UUID playerId, long sourcePos) {
        StorageView view = StorageServerStub.getView(StorageServerStub.getAndClear(), playerId, sourcePos);
        StorageServerStub stub = StorageServerStub.get(playerId, view.primary().getId());
        return new Metadata(stub.version, stub.orderVersion, view.fullness(), view.capacity());
    }

    @RemoteCallable(validator = StorageOpenStateValidator.class)
    public static void setOpen(UUID playerId, long sourcePos, boolean opened) {
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        BlockEntity blockEntity = player.level().getBlockEntity(BlockPos.of(sourcePos));
        if (blockEntity instanceof ShulkerContainerBlockEntity shulkerContainer) {
            shulkerContainer.setOpen(player, opened);
        }
    }

    @CallableParam(clazz = StorageServerStub.class, field = "ORDER_STREAM_CODEC")
    @RemoteCallable(validator = StorageAccessValidator.class)
    public static IntList reorder(UUID playerId, long sourcePos) {
        HolderLookup.Provider registries = StorageServerStub.getAndClear();
        StorageView view = StorageServerStub.getView(registries, playerId, sourcePos);
        StorageServerStub stub = StorageServerStub.get(playerId, view.primary().getId());
        PlayerSetting setting = PlayerSettings.getSetting(registries, playerId);
        IntList order = stub.getOrder(view, setting);
        return new IntArrayList(order);
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static SyncResult sync(
        UUID playerId,
        long sourcePos,
        @CallableParam(clazz = StorageServerStub.class, field = "ORDER_STREAM_CODEC") IntList slots
    ) {
        if (slots.size() > StorageServerStub.MAX_SYNC_SLOTS) {
            REGISTRIES.remove();
            throw new IllegalArgumentException("Cannot sync more than " + StorageServerStub.MAX_SYNC_SLOTS + " slots at once");
        }

        StorageView view = StorageServerStub.getView(StorageServerStub.getAndClear(), playerId, sourcePos);
        StorageServerStub stub = StorageServerStub.get(playerId, view.primary().getId());
        List<StackUpdate> updates = new ArrayList<>();
        IntOpenHashSet visited = new IntOpenHashSet(slots.size());
        for (int index : slots) {
            if (index < 0 || !visited.add(index)) {
                continue;
            }
            updates.add(new StackUpdate(index, StorageServerStub.getStack(view, index)));
        }
        return new SyncResult(stub.version, view.fullness(), updates);
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static InteractionResult interact(UUID playerId, long sourcePos, int slot, int button, StorageInput action) {
        if (!action.isValid(button)) {
            REGISTRIES.remove();
            throw new IllegalArgumentException("Invalid storage interaction button: " + button);
        }

        StorageView view = StorageServerStub.getView(StorageServerStub.getAndClear(), playerId, sourcePos);
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        ItemStack carried = player.inventoryMenu.getCarried();
        boolean changed = false;
        if (action == StorageInput.QUICK_MOVE_TO_STORAGE) {
            changed = StorageServerStub.moveInventoryStackToStorage(player, view, slot);
        } else if (action == StorageInput.CLONE) {
            if (
                player.hasInfiniteMaterials()
                && carried.isEmpty()
                && slot >= 0
                && slot < view.size()
                && view.amount(slot) > 0
            ) {
                ItemStack stack = view.resource(slot).toStack();
                carried = stack.copyWithCount(stack.getMaxStackSize());
                player.inventoryMenu.setCarried(carried);
            }
        } else if (action == StorageInput.THROW) {
            changed = StorageServerStub.throwStorageStack(player, view, slot, button);
        } else if (action == StorageInput.QUICK_MOVE_FROM_STORAGE) {
            changed = StorageServerStub.moveStorageStackToInventory(player, view, slot);
        } else if (!carried.isEmpty()) {
            int amount = button == 0 ? carried.getCount() : 1;
            try (Transaction transaction = Transaction.openRoot()) {
                int inserted = view.insert(ItemResource.of(carried), amount, transaction);
                if (inserted > 0) {
                    transaction.commit();
                    carried.shrink(inserted);
                    changed = true;
                }
            }
        } else if (slot >= 0 && slot < view.size() && view.amount(slot) > 0) {
            ItemResource resource = view.resource(slot);
            ItemStack itemStack = resource.toStack();
            int count = view.amount(slot);
            int maxPickup = Math.min(itemStack.getMaxStackSize(), count);
            int amount = button == 0 ? maxPickup : Math.ceilDiv(maxPickup, 2);
            try (Transaction transaction = Transaction.openRoot()) {
                int extracted = view.extract(slot, resource, amount, transaction);
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

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static DepositResult deposit(UUID playerId, long sourcePos, boolean all) {
        StorageView view = StorageServerStub.getView(StorageServerStub.getAndClear(), playerId, sourcePos);
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        boolean changed = false;
        for (int slot = Inventory.SELECTION_SIZE; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty() || !all && !StorageServerStub.matchesStorageItem(view, stack)) {
                continue;
            }
            try (Transaction transaction = Transaction.openRoot()) {
                int inserted = view.insert(ItemResource.of(stack), stack.getCount(), transaction);
                if (inserted > 0) {
                    transaction.commit();
                    stack.shrink(inserted);
                    changed = true;
                }
            }
        }
        if (changed) {
            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();
        }
        return new DepositResult(changed);
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static DepositResult take(UUID playerId, long sourcePos) {
        StorageView view = StorageServerStub.getView(StorageServerStub.getAndClear(), playerId, sourcePos);
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        boolean changed = false;
        Inventory inventory = player.getInventory();
        for (int slot = Inventory.SELECTION_SIZE; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            ItemResource resource = ItemResource.of(stack);
            int amount = inventory.getMaxStackSize(stack) - stack.getCount();
            if (amount <= 0) {
                continue;
            }
            for (int index = 0; index < view.size() && amount > 0; index++) {
                if (
                    view.amount(index) <= 0
                    || !ItemStack.isSameItemSameComponents(view.resource(index).toStack(), stack)
                ) {
                    continue;
                }
                try (Transaction transaction = Transaction.openRoot()) {
                    int extracted = view.extract(index, resource, amount, transaction);
                    if (extracted > 0) {
                        transaction.commit();
                        stack.grow(extracted);
                        amount -= extracted;
                        changed = true;
                    }
                }
            }
        }
        if (changed) {
            inventory.setChanged();
            player.inventoryMenu.broadcastChanges();
        }
        return new DepositResult(changed);
    }

    private static boolean matchesStorageItem(StorageView view, ItemStack stack) {
        for (int index = 0; index < view.size(); index++) {
            if (view.amount(index) > 0 && ItemStack.isSameItemSameComponents(view.resource(index).toStack(), stack)) {
                return true;
            }
        }
        return false;
    }

    private static boolean moveInventoryStackToStorage(
        ServerPlayer player,
        StorageView view,
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
            int inserted = view.insert(ItemResource.of(stack), stack.getCount(), transaction);
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
        StorageView view,
        int slot
    ) {
        if (slot < 0 || slot >= view.size() || view.amount(slot) <= 0) {
            return false;
        }
        ItemResource resource = view.resource(slot);
        ItemStack stack = resource.toStack();
        int amount = Math.min(
            view.amount(slot),
            StorageServerStub.getInventorySpace(player.getInventory(), stack)
        );
        if (amount <= 0) {
            return false;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            int extracted = view.extract(slot, resource, amount, transaction);
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
        StorageView view,
        int slot,
        int button
    ) {
        if (
            !player.inventoryMenu.getCarried().isEmpty()
            || !player.canDropItems()
            || slot < 0
            || slot >= view.size()
            || view.amount(slot) <= 0
        ) {
            return false;
        }
        ItemResource resource = view.resource(slot);
        ItemStack stack = resource.toStack();
        int stackCount = stack.getMaxStackSize();
        long requested = button == 0 ? 1 : (long) stackCount * (button == 1 ? 1 : 9);
        int amount = Math.min(view.amount(slot), Math.toIntExact(requested));
        try (Transaction transaction = Transaction.openRoot()) {
            int extracted = view.extract(slot, resource, amount, transaction);
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
        for (StorageServerStub stub : StorageServerStub.STUBS.values()) {
            if (stub.storageId.equals(storageId)) {
                stub.version++;
                stub.orderVersion = stub.version;
                stub.orders.clear();
            }
        }
    }

    public static void remove(UUID playerId) {
        StorageServerStub.STUBS.removeAll(playerId);
    }

    public static void clear() {
        StorageServerStub.STUBS.clear();
    }

    public static final class StorageAccessValidator implements IRemoteCallableValidator {
        @Override
        public boolean validate(IPayloadContext ctx, Method method, Object[] args) {
            boolean valid = this.isValid(ctx, args);
            if (valid) {
                StorageServerStub.REGISTRIES.set(ctx.player().registryAccess());
            }
            return valid;
        }

        private boolean isValid(IPayloadContext ctx, Object[] args) {
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
                   && AbstractContainerMenu.stillValid(
                ContainerLevelAccess.create(player.level(), pos),
                player,
                storage.getBlockState().getBlock()
            );
        }
    }

    public static final class StorageOpenStateValidator implements IRemoteCallableValidator {
        @Override
        public boolean validate(IPayloadContext ctx, Method method, Object[] args) {
            if (
                !(ctx.player() instanceof ServerPlayer player)
                || args.length != 3
                || !(args[0] instanceof UUID playerId)
                || !player.getGameProfile().id().equals(playerId)
                || !(args[1] instanceof Long sourcePos)
                || !(args[2] instanceof Boolean opened)
            ) {
                return false;
            }
            BlockPos pos = BlockPos.of(sourcePos);
            BlockEntity blockEntity = player.level().getBlockEntity(pos);
            return blockEntity instanceof ShulkerContainerBlockEntity
                   && (!opened || AbstractContainerMenu.stillValid(
                ContainerLevelAccess.create(player.level(), pos),
                player,
                blockEntity.getBlockState().getBlock()
            ));
        }
    }

    public record Metadata(long version, long orderVersion, double fullness, Capacity capacity) {
        public static final StreamCodec<ByteBuf, Metadata> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG,
            Metadata::version,
            ByteBufCodecs.VAR_LONG,
            Metadata::orderVersion,
            ByteBufCodecs.DOUBLE,
            Metadata::fullness,
            Capacity.STREAM_CODEC,
            Metadata::capacity,
            Metadata::new
        );
    }

    public record Capacity(int space, int spaceSize, int typeCount, int typeLimit) {
        public static final StreamCodec<ByteBuf, Capacity> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            Capacity::space,
            ByteBufCodecs.VAR_INT,
            Capacity::spaceSize,
            ByteBufCodecs.VAR_INT,
            Capacity::typeCount,
            ByteBufCodecs.VAR_INT,
            Capacity::typeLimit,
            Capacity::new
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

    public record DepositResult(boolean changed) {
        public static final StreamCodec<ByteBuf, DepositResult> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            DepositResult::changed,
            DepositResult::new
        );
    }

    private static StorageServerStub get(UUID playerId, UUID storageId) {
        Collection<StorageServerStub> stubs = StorageServerStub.STUBS.get(playerId);
        StorageServerStub cached = null;
        Iterator<StorageServerStub> iterator = stubs.iterator();
        while (iterator.hasNext()) {
            StorageServerStub stub = iterator.next();
            if (stub.storageId.equals(storageId)) {
                cached = stub;
                iterator.remove();
                break;
            }
        }
        if (cached != null) {
            StorageServerStub.STUBS.put(playerId, cached);
            return cached;
        }
        if (stubs.size() >= StorageServerStub.MAX_PLAYER_STUBS) {
            iterator = stubs.iterator();
            iterator.next();
            iterator.remove();
        }
        StorageServerStub stub = new StorageServerStub(storageId);
        StorageServerStub.STUBS.put(playerId, stub);
        return stub;
    }

    private static UnlimitedItemStack getStack(StorageView view, int index) {
        if (index >= view.size() || view.amount(index) <= 0) {
            return UnlimitedItemStack.EMPTY;
        }
        return new UnlimitedItemStack(view.resource(index), view.amount(index));
    }

    private IntList getOrder(StorageView view, PlayerSetting setting) {
        StorageSetting storageSetting = setting.storage();
        SortOptions options = new SortOptions(storageSetting.getSort(), storageSetting.getOrder());
        String search = storageSetting.getSearchContent().strip().toLowerCase(Locale.ROOT);
        List<CategoryEntry> categories = setting.listed();
        boolean hasCategoryRestriction = categories.stream()
            .anyMatch(entry -> entry.getMode() != CategoryMode.UNLIMITED);
        if (!search.isEmpty() || hasCategoryRestriction) {
            return StorageServerStub.createOrder(view, options, search, categories);
        }
        return this.orders.computeIfAbsent(options, _ -> StorageServerStub.createOrder(view, options, "", categories));
    }

    private static IntList createOrder(
        StorageView view,
        SortOptions options,
        String search,
        List<CategoryEntry> categories
    ) {
        List<OrderEntry> entries = new ArrayList<>(view.size());
        boolean requiresName = options.sort() == SortMode.NAME
            || search.isEmpty()
            || search.charAt(0) != '@' && search.charAt(0) != '#';
        for (int index = 0; index < view.size(); index++) {
            long amount = view.amount(index);
            if (amount <= 0) {
                continue;
            }
            ItemResource resource = view.resource(index);
            ItemStack stack = resource.toStack();
            Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            String name = requiresName ? stack.getHoverName().getString() : "";
            UnlimitedItemStack unlimitedStack = new UnlimitedItemStack(resource, Math.toIntExact(amount));
            if (!StorageServerStub.matchesFilters(resource.typeHolder(), unlimitedStack, id, name, search, categories)) {
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

    private static boolean matchesFilters(
        Holder<Item> item,
        UnlimitedItemStack stack,
        Identifier id,
        String name,
        String search,
        List<CategoryEntry> categories
    ) {
        boolean matchesSearch = search.isEmpty()
            || search.charAt(0) == '@' && id.getNamespace().toLowerCase(Locale.ROOT).contains(search.substring(1))
            || search.charAt(0) == '#'
               && item.tags().anyMatch(tag -> StorageServerStub.matchesTag(tag.location(), search.substring(1)))
            || search.charAt(0) != '@' && search.charAt(0) != '#'
               && (
                   name.toLowerCase(Locale.ROOT).contains(search)
                   || id.getPath().toLowerCase(Locale.ROOT).contains(search)
               );
        if (!matchesSearch) {
            return false;
        }

        for (CategoryEntry entry : categories) {
            if (entry.getMode() == CategoryMode.UNLIMITED) continue;
            if (entry.getMode() == CategoryMode.ALLOWLIST != entry.getCategory().test(stack)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesTag(Identifier id, String search) {
        return id.toString().toLowerCase(Locale.ROOT).contains(search)
            || id.getPath().toLowerCase(Locale.ROOT).contains(search);
    }

    private static Comparator<OrderEntry> getComparator(SortOptions options) {
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

    private StorageServerStub(UUID storageId) {
        this.storageId = storageId;
    }

    private static StorageView getView(HolderLookup.Provider registries, UUID playerId, long sourcePos) {
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        BlockPos pos = BlockPos.of(sourcePos);
        BlockEntity blockEntity = player.level().getBlockEntity(pos);
        if (!(blockEntity instanceof StorageBlockEntity storage)) {
            throw new IllegalStateException("Cannot access storage without a storage block entity");
        }
        UUID id = storage.getId();
        if (id == null) {
            id = UUID.randomUUID();
            storage.setId(id);
        }
        BaseStorage<?> primary = Storages.get().getOrCreate(id, storage.getStorageType().clazz());
        String search = PlayerSettings.getSetting(registries, playerId).storage().getSearchContent().strip();
        if (search.isEmpty() || !(storage instanceof CrateBlockEntity)) {
            return new StorageView(List.of(primary), List.of());
        }
        List<BaseStorage<?>> storages = new ArrayList<>();
        for (CrateBlockEntity crate : CrateBlock.getNearbyCrates(player.level(), pos)) {
            if (crate.getId() != null) {
                Storages.get().get(crate.getId()).ifPresent(storages::add);
            }
        }
        return new StorageView(storages, List.of());
    }

    private record SortOptions(SortMode sort, OrderMode order) {
    }

    private record OrderEntry(int index, long amount, Identifier id, String name) {
    }

    private static final class StorageView {
        private final List<BaseStorage<?>> storages;
        private final List<Entry> entries = new ArrayList<>();

        private StorageView(List<BaseStorage<?>> storages, List<Entry> ignored) {
            this.storages = storages;
            Map<ItemResource, Entry> merged = new HashMap<>();
            for (int storageIndex = 0; storageIndex < storages.size(); storageIndex++) {
                UnlimitedItemStacksResourceHandler items = storages.get(storageIndex).getItems();
                for (int slot = 0; slot < items.size(); slot++) {
                    if (items.getAmountAsLong(slot) <= 0) continue;
                    ItemResource resource = items.getResource(slot);
                    Entry entry = merged.get(resource);
                    if (entry == null) {
                        entry = new Entry(resource, 0, storageIndex, slot);
                        merged.put(resource, entry);
                        this.entries.add(entry);
                    }
                    entry.amount += Math.toIntExact(items.getAmountAsLong(slot));
                }
            }
        }

        BaseStorage<?> primary() {
            return this.storages.getLast();
        }

        int size() {
            return this.entries.size();
        }

        int amount(int index) {
            return this.entries.get(index).amount;
        }

        ItemResource resource(int index) {
            return this.entries.get(index).resource;
        }

        double fullness() {
            return this.primary().getItems().getFullness();
        }

        Capacity capacity() {
            UnlimitedItemStacksResourceHandler items = this.primary().getItems();
            int space = 0;
            int spaceSize = Integer.MAX_VALUE;
            if (items instanceof SpaceSizeItemStacksResourceHandler spaceHandler) {
                space = spaceHandler.getSpace();
                spaceSize = spaceHandler.getSpaceSize();
            } else if (items instanceof TypeLimitItemStacksResourceHandler typeHandler) {
                spaceSize = typeHandler.getSpaceSize();
            }
            return new Capacity(space, spaceSize, items.getTypeCount(), items.getTypeLimit());
        }

        int insert(ItemResource resource, int amount, Transaction tx) {
            int inserted = 0;
            for (int i = 0; i < this.storages.size() - 1; i++) {
                UnlimitedItemStacksResourceHandler items = this.storages.get(i).getItems();
                if (!contains(items, resource)) continue;
                inserted += items.insert(resource, amount - inserted, tx);
                if (inserted == amount) return inserted;
            }
            UnlimitedItemStacksResourceHandler primaryItems = this.primary().getItems();
            inserted += primaryItems.insert(resource, amount - inserted, tx);
            if (inserted == amount) return inserted;
            for (int i = 0; i < this.storages.size() - 1; i++) {
                inserted += this.storages.get(i).getItems().insert(resource, amount - inserted, tx);
                if (inserted == amount) return inserted;
            }
            return inserted;
        }

        private static boolean contains(UnlimitedItemStacksResourceHandler items, ItemResource resource) {
            for (int i = 0; i < items.size(); i++) {
                if (items.getAmountAsLong(i) > 0 && items.getResource(i).equals(resource)) return true;
            }
            return false;
        }

        int extract(int index, ItemResource resource, int amount, Transaction tx) {
            Entry e = this.entries.get(index);
            return this.storages.get(e.storageIndex).getItems().extract(e.slot, resource, amount, tx);
        }

        private static final class Entry {
            final ItemResource resource;
            int amount;
            final int storageIndex;
            final int slot;

            Entry(ItemResource resource, int amount, int storageIndex, int slot) {
                this.resource = resource;
                this.amount = amount;
                this.storageIndex = storageIndex;
                this.slot = slot;
            }
        }
    }
}
