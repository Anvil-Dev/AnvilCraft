package dev.dubhe.anvilcraft.rpc;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.rpc.CallableParam;
import dev.anvilcraft.lib.v2.rpc.IRemoteCallableValidator;
import dev.anvilcraft.lib.v2.rpc.RemoteCallable;
import dev.anvilcraft.lib.v2.util.UnlimitedItemStack;
import dev.dubhe.anvilcraft.api.StoragePortManager;
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
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.SoundAction;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.neoforge.transfer.ResourceHandlerUtil;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.item.CarriedSlotWrapper;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.item.PlayerInventoryWrapper;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
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
    private static final int MAX_UNDO_RECORDS = 4;
    private static final int MAX_SYNC_SLOTS = 256;
    private static final ThreadLocal<HolderLookup.@Nullable Provider> REGISTRIES = new ThreadLocal<>();
    @SuppressWarnings("unused")
    public static final StreamCodec<ByteBuf, IntList> ORDER_STREAM_CODEC = ByteBufCodecs.VAR_INT
        .apply(ByteBufCodecs.list())
        .map(IntArrayList::new, Function.identity());
    @SuppressWarnings("NullableProblems") // IDEA issue, will be unnecessary in sometime
    private static final Multimap<UUID, StorageServerStub> STUBS = ArrayListMultimap.create();

    private final Deque<Map<ItemResource, Integer>> undoRecords = new ArrayDeque<>();
    private final Map<ItemResource, Integer> undoGroup = new HashMap<>();
    private boolean undoingGroup;
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
            StorageServerStub.REGISTRIES.remove();
            throw new IllegalArgumentException("Cannot sync more than " + StorageServerStub.MAX_SYNC_SLOTS + " slots at once");
        }

        StorageView view = StorageServerStub.getView(StorageServerStub.getAndClear(), playerId, sourcePos);
        StorageServerStub stub = StorageServerStub.get(playerId, view.primary().getId());
        List<StackUpdate> updates = new ArrayList<>();
        IntOpenHashSet visited = new IntOpenHashSet(slots.size());
        for (int index : slots) {
            if (index < 0 || index >= StoragePortManager.FLUID_SLOT_BASE || !visited.add(index)) {
                continue;
            }
            updates.add(new StackUpdate(index, StorageServerStub.getStack(view, index), index < view.size() ? view.amount(index) : 0));
        }
        return new SyncResult(stub.version, view.fullness(), updates, StoragePortManager.collect(view.primary().getId()));
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static InteractionResult interact(
        UUID playerId, long sourcePos, int slot, int button, StorageInput action,
        @CallableParam(clazz = FluidStack.class, field = "OPTIONAL_STREAM_CODEC") FluidStack fluid
    ) {
        if (!action.isValid(button)) {
            StorageServerStub.REGISTRIES.remove();
            throw new IllegalArgumentException("Invalid storage interaction button: " + button);
        }

        StorageView view = StorageServerStub.getView(StorageServerStub.getAndClear(), playerId, sourcePos);
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        ItemStack carried = player.inventoryMenu.getCarried();
        boolean changed = false;
        FluidNotice notice = FluidNotice.NONE;
        if (action == StorageInput.QUICK_MOVE_TO_STORAGE) {
            changed = StorageServerStub.moveInventoryStackToStorage(player, view, slot, button == 0);
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
        } else if (action == StorageInput.FLUID_BUCKET) {
            FluidOutcome outcome = StorageServerStub.takeFluidBucket(player, view, fluid, button);
            changed = outcome.changed();
            notice = outcome.notice();
        } else if (action == StorageInput.QUICK_MOVE_FROM_STORAGE) {
            if (slot >= StoragePortManager.FLUID_SLOT_BASE) {
                FluidOutcome outcome = StorageServerStub.takeFluidBucketIntoInventory(player, view, fluid);
                changed = outcome.changed();
                notice = outcome.notice();
            } else {
                changed = StorageServerStub.moveStorageStackToInventory(player, view, slot);
            }
        } else if (!carried.isEmpty()) {
            int amount = button == 0 ? carried.getCount() : 1;
            int poured = button == 0 ? StorageServerStub.pourIntoFluidPort(player, view, carried, amount) : 0;
            if (poured > 0) {
                if (carried.isEmpty()) player.inventoryMenu.setCarried(ItemStack.EMPTY);
                changed = true;
            } else {
                try (Transaction transaction = Transaction.openRoot()) {
                    int inserted = view.insert(ItemResource.of(carried), amount, transaction);
                    if (inserted > 0) {
                        transaction.commit();
                        carried.shrink(inserted);
                        changed = true;
                    }
                }
            }
        } else if (slot >= 0 && slot < view.size() && view.amount(slot) > 0) {
            ItemResource resource = view.resource(slot);
            ItemStack itemStack = resource.toStack();
            int maxPickup = (int) Math.min(itemStack.getMaxStackSize(), view.amount(slot));
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
        return new InteractionResult(player.inventoryMenu.getCarried(), changed, 0, notice);
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static DepositResult deposit(UUID playerId, long sourcePos, boolean all, boolean pour) {
        StorageView view = StorageServerStub.getView(StorageServerStub.getAndClear(), playerId, sourcePos);
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        StorageServerStub stub = StorageServerStub.get(playerId, view.primary().getId());
        Map<ItemResource, Integer> moved = new HashMap<>();
        boolean changed = false;
        for (int slot = Inventory.SELECTION_SIZE; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty()) continue;
            if (pour && StorageServerStub.pourIntoFluidPort(player, view, stack, stack.getCount()) > 0) {
                if (stack.isEmpty()) player.getInventory().setItem(slot, ItemStack.EMPTY);
                changed = true;
                continue;
            }
            if (!all && !StorageServerStub.matchesStorageItem(view, stack)) continue;
            try (Transaction transaction = Transaction.openRoot()) {
                int inserted = view.insert(ItemResource.of(stack), stack.getCount(), transaction);
                if (inserted > 0) {
                    transaction.commit();
                    moved.merge(ItemResource.of(stack), inserted, Integer::sum);
                    stack.shrink(inserted);
                    changed = true;
                }
            }
        }
        if (changed) {
            StorageServerStub.recordUndo(stub, moved);
            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();
        }
        return new DepositResult(changed);
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static boolean quickMoveToStorage(
        UUID playerId, long sourcePos,
        @CallableParam(clazz = StorageServerStub.class, field = "ORDER_STREAM_CODEC") IntList slots
    ) {
        if (slots.isEmpty() || slots.size() > StorageServerStub.MAX_SYNC_SLOTS) {
            StorageServerStub.REGISTRIES.remove();
            throw new IllegalArgumentException("Invalid quick move slots");
        }
        StorageView view = StorageServerStub.getView(StorageServerStub.getAndClear(), playerId, sourcePos);
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        StorageServerStub stub = StorageServerStub.get(playerId, view.primary().getId());
        Map<ItemResource, Integer> moved = new HashMap<>();
        IntOpenHashSet visited = new IntOpenHashSet(slots.size());
        boolean changed = false;
        for (int slot : slots) {
            if (slot < 0 || slot >= Inventory.INVENTORY_SIZE || !visited.add(slot)) continue;
            changed |= StorageServerStub.moveInventoryStackToStorage(player, view, slot, true, moved) > 0;
        }
        if (changed) {
            StorageServerStub.recordUndo(stub, moved);
            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();
        }
        return changed;
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static boolean quickMoveFromStorage(
        UUID playerId, long sourcePos,
        @CallableParam(clazz = StorageServerStub.class, field = "ORDER_STREAM_CODEC") IntList slots
    ) {
        if (slots.isEmpty() || slots.size() > StorageServerStub.MAX_SYNC_SLOTS) {
            StorageServerStub.REGISTRIES.remove();
            throw new IllegalArgumentException("Invalid quick move slots");
        }
        StorageView view = StorageServerStub.getView(StorageServerStub.getAndClear(), playerId, sourcePos);
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        IntOpenHashSet visited = new IntOpenHashSet(slots.size());
        boolean changed = false;
        for (int slot : slots) {
            if (slot < 0 || !visited.add(slot)) continue;
            changed |= StorageServerStub.moveStorageStackToInventory(player, view, slot);
        }
        if (changed) {
            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();
        }
        return changed;
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static boolean moveSameToStorage(UUID playerId, long sourcePos, int slot, boolean pour) {
        StorageView view = StorageServerStub.getView(StorageServerStub.getAndClear(), playerId, sourcePos);
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        if (slot < 0 || slot >= Inventory.INVENTORY_SIZE) return false;
        ItemResource sample = ItemResource.of(player.getInventory().getItem(slot));
        if (sample.isEmpty()) return false;
        StorageServerStub stub = StorageServerStub.get(playerId, view.primary().getId());
        Map<ItemResource, Integer> moved = new HashMap<>();
        boolean changed = false;
        for (int index = 0; index < Inventory.INVENTORY_SIZE; index++) {
            ItemStack stack = player.getInventory().getItem(index);
            if (stack.isEmpty() || !ItemResource.of(stack).equals(sample)) continue;
            changed |= StorageServerStub.moveInventoryStackToStorage(player, view, index, pour, moved) > 0;
        }
        if (changed) {
            StorageServerStub.recordUndo(stub, moved);
            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();
        }
        return changed;
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static DepositResult undo(UUID playerId, long sourcePos) {
        StorageView view = StorageServerStub.getView(StorageServerStub.getAndClear(), playerId, sourcePos);
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        var record = StorageServerStub.get(playerId, view.primary().getId()).undoRecords.pollFirst();
        if (record == null) return new DepositResult(false);
        var inventory = PlayerInventoryWrapper.of(player);
        boolean changed = false;
        for (var entry : record.entrySet()) {
            ItemResource resource = entry.getKey();
            int limit = Math.min(entry.getValue(), StorageServerStub.getInventorySpace(player.getInventory(), resource.toStack()));
            if (limit <= 0) continue;
            try (Transaction transaction = Transaction.openRoot()) {
                int fit;
                try (Transaction simulation = Transaction.open(transaction)) {
                    fit = inventory.insert(resource, limit, simulation);
                }
                if (fit <= 0) continue;
                int extracted = view.extractByResource(resource, fit, transaction);
                if (extracted <= 0 || inventory.insert(resource, extracted, transaction) != extracted) continue;
                transaction.commit();
                changed = true;
            }
        }
        if (changed) {
            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();
        }
        return new DepositResult(changed);
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static void beginUndoGroup(UUID playerId, long sourcePos) {
        StorageView view = StorageServerStub.getView(StorageServerStub.getAndClear(), playerId, sourcePos);
        StorageServerStub stub = StorageServerStub.get(playerId, view.primary().getId());
        stub.undoGroup.clear();
        stub.undoingGroup = true;
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static void endUndoGroup(UUID playerId, long sourcePos) {
        StorageView view = StorageServerStub.getView(StorageServerStub.getAndClear(), playerId, sourcePos);
        StorageServerStub stub = StorageServerStub.get(playerId, view.primary().getId());
        if (!stub.undoingGroup) return;
        stub.undoingGroup = false;
        StorageServerStub.pushUndo(stub, stub.undoGroup);
        stub.undoGroup.clear();
    }

    private static void recordUndo(StorageServerStub stub, Map<ItemResource, Integer> moved) {
        if (stub.undoingGroup) {
            moved.forEach((resource, amount) -> stub.undoGroup.merge(resource, amount, Integer::sum));
        } else {
            StorageServerStub.pushUndo(stub, moved);
        }
    }

    private static void pushUndo(StorageServerStub stub, Map<ItemResource, Integer> moved) {
        if (moved.isEmpty()) return;
        stub.undoRecords.addFirst(new HashMap<>(moved));
        while (stub.undoRecords.size() > StorageServerStub.MAX_UNDO_RECORDS) stub.undoRecords.removeLast();
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

    private static boolean moveInventoryStackToStorage(ServerPlayer player, StorageView view, int slot, boolean pour) {
        return StorageServerStub.moveInventoryStackToStorage(player, view, slot, pour, null) > 0;
    }

    private static int moveInventoryStackToStorage(
        ServerPlayer player, StorageView view, int slot, boolean pour, @Nullable Map<ItemResource, Integer> moved
    ) {
        Inventory inventory = player.getInventory();
        if (slot < 0 || slot >= Inventory.INVENTORY_SIZE) {
            return 0;
        }
        ItemStack stack = inventory.getItem(slot);
        if (stack.isEmpty()) {
            return 0;
        }
        int poured = pour ? StorageServerStub.pourIntoFluidPort(player, view, stack, stack.getCount()) : 0;
        if (poured > 0) {
            if (stack.isEmpty()) inventory.setItem(slot, ItemStack.EMPTY);
            return poured;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            int inserted = view.insert(ItemResource.of(stack), stack.getCount(), transaction);
            if (inserted <= 0) {
                return 0;
            }
            transaction.commit();
            if (moved != null) moved.merge(ItemResource.of(stack), inserted, Integer::sum);
            stack.shrink(inserted);
            return inserted;
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
        int amount = (int) Math.min(
            Math.min(view.amount(slot), stack.getMaxStackSize()),
            StorageServerStub.getInventorySpace(player.getInventory(), stack)
        );
        if (amount <= 0) {
            return false;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            int extracted = view.extract(slot, resource, amount, transaction);
            if (extracted <= 0 || PlayerInventoryWrapper.of(player).insert(resource, extracted, transaction) != extracted) {
                return false;
            }
            transaction.commit();
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
        int amount = (int) Math.min(view.amount(slot), requested);
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

    public record StackUpdate(int index, UnlimitedItemStack stack, long count) {
        public static final StreamCodec<RegistryFriendlyByteBuf, StackUpdate> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            StackUpdate::index,
            UnlimitedItemStack.OPTIONAL_STREAM_CODEC,
            StackUpdate::stack,
            ByteBufCodecs.VAR_LONG,
            StackUpdate::count,
            StackUpdate::new
        );
    }

    /**
     * 仓储界面中的一个流体条目。
     *
     * <p>取空后条目仍需留在列表里显示 0（与物品一致），但数量为 0 的 {@link FluidStack}
     * 连流体类型都会丢失、无法在网络上传输，因此图标与数量分开携带。</p>
     *
     * @param icon   流体类型与组件（用于渲染图标、匹配与显示名），一定非空
     * @param amount 数量（mB），取空后的占位条目为 0
     */
    public record FluidEntry(FluidStack icon, int amount) {
        public static final StreamCodec<RegistryFriendlyByteBuf, FluidEntry> STREAM_CODEC = StreamCodec.composite(
            FluidStack.OPTIONAL_STREAM_CODEC,
            FluidEntry::icon,
            ByteBufCodecs.VAR_INT,
            FluidEntry::amount,
            FluidEntry::new
        );
    }

    public record SyncResult(long version, double fullness, List<StackUpdate> updates, List<FluidEntry> fluids) {
        public static final StreamCodec<RegistryFriendlyByteBuf, SyncResult> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG,
            SyncResult::version,
            ByteBufCodecs.DOUBLE,
            SyncResult::fullness,
            StackUpdate.STREAM_CODEC.apply(ByteBufCodecs.list()),
            SyncResult::updates,
            FluidEntry.STREAM_CODEC.apply(ByteBufCodecs.list()),
            SyncResult::fluids,
            SyncResult::new
        );
    }

    public enum FluidNotice {
        /** 无提示。 */
        NONE(""),
        /** 缺少空容器。 */
        BUCKET_MISSING("screen.anvilcraft.storage.fluid.bucket_missing"),
        /** 储量不足一桶。 */
        NOT_ENOUGH("screen.anvilcraft.storage.fluid.not_enough");

        public static final StreamCodec<ByteBuf, FluidNotice> STREAM_CODEC =
            StreamCodecUtil.enumStreamCodec(FluidNotice.class);

        private final String translationKey;

        FluidNotice(String translationKey) {
            this.translationKey = translationKey;
        }

        /** 提示文本；{@link #NONE} 返回空组件。 */
        public Component text() {
            return this.translationKey.isEmpty() ? Component.empty() : Component.translatable(this.translationKey);
        }
    }

    public record InteractionResult(ItemStack carried, boolean changed, int refilledSlots, FluidNotice notice) {
        public InteractionResult(ItemStack carried, boolean changed) {
            this(carried, changed, 0, FluidNotice.NONE);
        }

        public InteractionResult(ItemStack carried, boolean changed, int refilledSlots) {
            this(carried, changed, refilledSlots, FluidNotice.NONE);
        }

        public static final StreamCodec<RegistryFriendlyByteBuf, InteractionResult> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC,
            InteractionResult::carried,
            ByteBufCodecs.BOOL,
            InteractionResult::changed,
            ByteBufCodecs.VAR_INT,
            InteractionResult::refilledSlots,
            FluidNotice.STREAM_CODEC,
            InteractionResult::notice,
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
        return new UnlimitedItemStack(view.resource(index), (int) Math.min(view.amount(index), Integer.MAX_VALUE));
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
            UnlimitedItemStack unlimitedStack = new UnlimitedItemStack(resource, (int) Math.min(amount, Integer.MAX_VALUE));
            if (!StorageServerStub.matchesFilters(resource.typeHolder(), unlimitedStack, id, name, search, categories)) {
                continue;
            }
            entries.add(new OrderEntry(index, amount, id, name));
        }

        StorageServerStub.addFluidEntries(entries, view, search, requiresName, categories);
        Comparator<OrderEntry> comparator = StorageServerStub.getComparator(options);
        entries.sort(comparator);

        IntArrayList order = new IntArrayList(entries.size());
        for (OrderEntry entry : entries) {
            order.add(entry.index());
        }
        return order;
    }

    private static void addFluidEntries(
        List<OrderEntry> entries,
        StorageView view,
        String search,
        boolean requiresName,
        List<CategoryEntry> categories
    ) {
        List<FluidEntry> fluids = StoragePortManager.collect(view.primary().getId());
        for (int index = 0; index < fluids.size(); index++) {
            FluidEntry entry = fluids.get(index);
            // 与物品的 createOrder 一致：0 数量的条目不进入排序结果，
            // 因此重新排序（例如松开 Shift）后取空的流体就不再显示；
            // 按住 Shift 时由客户端保留的顺序显示 0
            if (entry.amount() <= 0) {
                continue;
            }
            if (!StorageServerStub.matchesFluidCategoryFilters(entry.icon(), categories)) {
                continue;
            }
            Identifier id = BuiltInRegistries.FLUID.getKey(entry.icon().getFluid());
            // 与 matchesFilters 的搜索判定保持一致：普通文本不在服务端过滤（服务端没有客户端
            // 语言环境），先一律放行，再由客户端 StorageScreen.applySearchFilter 按本地化名称
            // 与 id path 过滤。缺少最后一个放行分支时 matches 恒为 false，一输入普通文本
            // 流体就会整体从服务端 order 里消失，客户端那道过滤根本没机会执行。
            // '#' 前缀是物品标签搜索，流体无对应语义，故不放行（客户端在 '#' 时也不做二次过滤）。
            boolean matches = search.isEmpty()
                || search.charAt(0) == '@'
                   && id.getNamespace().toLowerCase(Locale.ROOT).contains(search.substring(1))
                || search.charAt(0) != '@' && search.charAt(0) != '#';
            if (!matches) {
                continue;
            }
            entries.add(new OrderEntry(
                StoragePortManager.FLUID_SLOT_BASE + index,
                // 按 #4792：数量排序时 1 mB 相当于 1 个物品
                entry.amount(),
                id,
                requiresName ? entry.icon().getHoverName().getString() : ""
            ));
        }
    }

    private static FluidOutcome takeFluidBucket(ServerPlayer player, StorageView view, FluidStack fluid, int button) {
        // 指针上拿着装有流体的容器：这一下是「倒进去」。倒不进去（如仓储没有可接收的
        // 端口）时继续往下走，由取出的分支判断指针是否可接收产物
        ItemStack carried = player.inventoryMenu.getCarried();
        if (!carried.isEmpty()) {
            int amount = button == 0 ? carried.getCount() : 1;
            if (StorageServerStub.pourIntoFluidPort(player, view, carried, amount) > 0) {
                if (carried.isEmpty()) {
                    player.inventoryMenu.setCarried(ItemStack.EMPTY);
                }
                return FluidOutcome.CHANGED;
            }
        }
        FilledBucket filled = StorageServerStub.fillBucketFromStorage(player, view, fluid, false);
        if (filled.stack().isEmpty()) {
            return FluidOutcome.failed(filled.notice());
        }
        // 取出的一桶流体落在鼠标指针上，与点击物品格取物一致；
        // 指针被其它物品占用（例如还剩几个空桶）时才退回背包
        if (player.inventoryMenu.getCarried().isEmpty()) {
            player.inventoryMenu.setCarried(filled.stack());
        } else if (!player.addItem(filled.stack())
            && view.insert(filled.stack().copyWithCount(1), 1) <= 0) {
            Block.popResource(player.level(), player.blockPosition(), filled.stack());
        }
        return FluidOutcome.CHANGED;
    }

    private static FluidOutcome takeFluidBucketIntoInventory(
        ServerPlayer player,
        StorageView view,
        FluidStack fluid
    ) {
        FilledBucket filled = StorageServerStub.fillBucketFromStorage(player, view, fluid, true);
        if (filled.stack().isEmpty()) {
            return FluidOutcome.failed(filled.notice());
        }
        if (!player.addItem(filled.stack()) && view.insert(filled.stack().copyWithCount(1), 1) <= 0) {
            Block.popResource(player.level(), player.blockPosition(), filled.stack());
        }
        return FluidOutcome.CHANGED;
    }

    private static void playBucketSound(ServerPlayer player, FluidStack fluid, SoundAction action) {
        SoundEvent sound = fluid.getFluidType().getSound(fluid, action);
        if (sound == null) {
            return;
        }
        player.level().playSound(
            null,
            player.getX(),
            player.getY() + 0.5,
            player.getZ(),
            sound,
            SoundSource.BLOCKS,
            1.0F,
            1.0F
        );
    }

    private static void giveEmptiedContainer(ServerPlayer player, ItemStack emptied) {
        if (!player.addItem(emptied)) {
            Block.popResource(player.level(), player.blockPosition(), emptied);
        }
    }

    private static void giveEmptiedContainerToStorage(
        @Nullable StorageView view,
        ServerPlayer player,
        ItemStack emptied
    ) {
        if (emptied.isEmpty()) {
            return;
        }
        if (view != null) {
            int inserted = view.insert(emptied.copyWithCount(1), emptied.getCount());
            if (inserted >= emptied.getCount()) {
                return;
            }
            if (inserted > 0) {
                ItemStack rest = emptied.copy();
                rest.shrink(inserted);
                StorageServerStub.giveEmptiedContainer(player, rest);
                return;
            }
        }
        StorageServerStub.giveEmptiedContainer(player, emptied);
    }

    private static boolean matchesFluidCategoryFilters(FluidStack fluid, List<CategoryEntry> categories) {
        for (CategoryEntry entry : categories) {
            if (entry.getMode() == CategoryMode.UNLIMITED) continue;
            if (entry.getMode() == CategoryMode.ALLOWLIST != entry.getCategory().testFluid(fluid)) {
                return false;
            }
        }
        return true;
    }

    private record FluidOutcome(boolean changed, FluidNotice notice) {
        private static final FluidOutcome CHANGED = new FluidOutcome(true, FluidNotice.NONE);

        private static FluidOutcome failed(FluidNotice notice) {
            return new FluidOutcome(false, notice);
        }
    }

    private record FilledBucket(ItemStack stack, FluidNotice notice) {
        private static FilledBucket failed(FluidNotice notice) {
            return new FilledBucket(ItemStack.EMPTY, notice);
        }
    }

    private static FilledBucket fillBucketFromStorage(ServerPlayer player, StorageView view, FluidStack fluid, boolean intoInventory) {
        FluidEntry entry = StoragePortManager.find(view.primary().getId(), fluid);
        if (entry == null) return FilledBucket.failed(FluidNotice.NONE);
        FluidStack target = entry.icon().copyWithAmount(FluidType.BUCKET_VOLUME);
        ItemStack filled = target.getFluidType().getBucket(target);
        if (filled.isEmpty()) return FilledBucket.failed(FluidNotice.NONE);
        ItemStack empty = StorageServerStub.emptyContainerOf(filled);
        if (empty.isEmpty()) return FilledBucket.failed(FluidNotice.NONE);
        ItemStack cursor = player.inventoryMenu.getCarried();
        if (!intoInventory && !cursor.isEmpty() && !ItemStack.isSameItemSameComponents(cursor, empty)) {
            return FilledBucket.failed(FluidNotice.NONE);
        }
        if (!StorageServerStub.hasEmptyContainer(player, view, empty)) return FilledBucket.failed(FluidNotice.BUCKET_MISSING);
        if (entry.amount() < FluidType.BUCKET_VOLUME) return FilledBucket.failed(FluidNotice.NOT_ENOUGH);
        try (Transaction transaction = Transaction.openRoot()) {
            if (!StorageServerStub.consumeEmptyContainer(player, view, empty, transaction)) {
                return FilledBucket.failed(FluidNotice.BUCKET_MISSING);
            }
            if (StoragePortManager.drain(view.primary().getId(), target, FluidType.BUCKET_VOLUME, transaction) < FluidType.BUCKET_VOLUME) {
                return FilledBucket.failed(FluidNotice.NOT_ENOUGH);
            }
            transaction.commit();
        }
        StorageServerStub.playBucketSound(player, target, SoundActions.BUCKET_FILL);
        return new FilledBucket(filled, FluidNotice.NONE);
    }

    private static ItemStack emptyContainerOf(ItemStack filled) {
        ItemStacksResourceHandler single = new ItemStacksResourceHandler(1);
        single.set(0, ItemResource.of(filled), 1);
        var handler = ItemAccess.forHandlerIndexStrict(single, 0).getCapability(Capabilities.Fluid.ITEM);
        if (handler == null) return ItemStack.EMPTY;
        try (Transaction transaction = Transaction.openRoot()) {
            for (int index = 0; index < handler.size(); index++) {
                FluidResource resource = handler.getResource(index);
                if (resource.isEmpty()) continue;
                handler.extract(resource, Integer.MAX_VALUE, transaction);
                break;
            }
            transaction.commit();
        }
        return single.getResource(0).toStack(single.getAmountAsInt(0));
    }

    private static boolean hasEmptyContainer(ServerPlayer player, StorageView view, ItemStack empty) {
        ItemStack carried = player.inventoryMenu.getCarried();
        if (!carried.isEmpty() && ItemStack.isSameItemSameComponents(carried, empty)) return true;
        for (int index = 0; index < Inventory.INVENTORY_SIZE; index++) {
            ItemStack stack = player.getInventory().getItem(index);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, empty)) return true;
        }
        ItemResource resource = ItemResource.of(empty);
        for (int index = 0; index < view.size(); index++) {
            if (view.amount(index) > 0 && view.resource(index).equals(resource)) return true;
        }
        return false;
    }

    private static boolean consumeEmptyContainer(ServerPlayer player, StorageView view, ItemStack empty, Transaction transaction) {
        ItemResource resource = ItemResource.of(empty);
        if (CarriedSlotWrapper.of(player.inventoryMenu).extract(resource, 1, transaction) > 0) return true;
        var inventory = PlayerInventoryWrapper.of(player);
        for (int index = 0; index < Inventory.INVENTORY_SIZE; index++) {
            if (inventory.extract(index, resource, 1, transaction) > 0) return true;
        }
        for (int index = 0; index < view.size(); index++) {
            if (view.amount(index) > 0 && view.resource(index).equals(resource)
                && view.extract(index, resource, 1, transaction) > 0) return true;
        }
        return false;
    }

    private static int pourIntoFluidPort(ServerPlayer player, StorageView view, ItemStack stack, int maxAmount) {
        if (stack.isEmpty() || maxAmount <= 0) return 0;
        FluidStack content = FluidUtil.getFirstStackContained(stack);
        if (content.isEmpty()) return 0;
        var acceptor = StoragePortManager.findAcceptor(view.primary().getId(), content);
        if (acceptor == null) return 0;
        int poured = 0;
        while (poured < maxAmount && !stack.isEmpty()) {
            ItemStacksResourceHandler single = new ItemStacksResourceHandler(1);
            single.set(0, ItemResource.of(stack), 1);
            var handler = ItemAccess.forHandlerIndexStrict(single, 0).getCapability(Capabilities.Fluid.ITEM);
            if (handler == null) break;
            try (Transaction transaction = Transaction.openRoot()) {
                if (ResourceHandlerUtil.moveFirst(handler, acceptor, fluid -> true, Integer.MAX_VALUE, transaction) == null) break;
                transaction.commit();
            }
            StorageServerStub.giveEmptiedContainerToStorage(view, player, single.getResource(0).toStack(single.getAmountAsInt(0)));
            stack.shrink(1);
            poured++;
        }
        if (poured > 0) StorageServerStub.playBucketSound(player, content, SoundActions.BUCKET_EMPTY);
        return poured;
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
            || search.charAt(0) != '@' && search.charAt(0) != '#';
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
                    entry.amount += items.getAmountAsLong(slot);
                }
            }
        }

        BaseStorage<?> primary() {
            return this.storages.getLast();
        }

        int size() {
            return this.entries.size();
        }

        long amount(int index) {
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

        int insert(ItemStack stack, int amount) {
            if (stack.isEmpty() || amount <= 0) return 0;
            try (Transaction transaction = Transaction.openRoot()) {
                int inserted = this.insert(ItemResource.of(stack), amount, transaction);
                transaction.commit();
                return inserted;
            }
        }

        int insert(ItemResource resource, int amount, Transaction tx) {
            int inserted = 0;
            for (int i = 0; i < this.storages.size() - 1; i++) {
                UnlimitedItemStacksResourceHandler items = this.storages.get(i).getItems();
                if (!StorageView.contains(items, resource)) continue;
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

        int extractByResource(ItemResource resource, int amount, Transaction transaction) {
            int extracted = 0;
            for (BaseStorage<?> storage : this.storages) {
                extracted += storage.getItems().extract(resource, amount - extracted, transaction);
                if (extracted == amount) break;
            }
            return extracted;
        }

        int extract(int index, ItemResource resource, int amount, Transaction tx) {
            Entry e = this.entries.get(index);
            return this.storages.get(e.storageIndex).getItems().extract(e.slot, resource, amount, tx);
        }

        private static final class Entry {
            final ItemResource resource;
            long amount;
            final int storageIndex;
            final int slot;

            Entry(ItemResource resource, long amount, int storageIndex, int slot) {
                this.resource = resource;
                this.amount = amount;
                this.storageIndex = storageIndex;
                this.slot = slot;
            }
        }
    }
}
