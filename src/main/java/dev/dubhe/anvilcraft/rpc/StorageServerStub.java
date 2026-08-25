package dev.dubhe.anvilcraft.rpc;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import dev.anvilcraft.lib.v2.rpc.CallableParam;
import dev.anvilcraft.lib.v2.rpc.IRemoteCallableValidator;
import dev.anvilcraft.lib.v2.rpc.RemoteCallable;
import dev.anvilcraft.lib.v2.util.stack.UnlimitedItemStack;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.SpaceSizeItemStacksResourceHandler;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.TypeLimitItemStacksResourceHandler;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler;
import dev.dubhe.anvilcraft.block.container.storage.CrateBlock;
import dev.dubhe.anvilcraft.block.container.storage.HyperdimensionStorageStationBlock;
import dev.dubhe.anvilcraft.block.entity.storage.CrateBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.ShulkerContainerBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.block.item.ShulkerContainerBlockItem;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.storage.ModStorageTypes;
import dev.dubhe.anvilcraft.item.property.component.TerminalBinding;
import dev.dubhe.anvilcraft.saved.setting.PlayerSetting;
import dev.dubhe.anvilcraft.saved.setting.PlayerSettings;
import dev.dubhe.anvilcraft.saved.setting.StorageSetting;
import dev.dubhe.anvilcraft.saved.setting.mode.OrderMode;
import dev.dubhe.anvilcraft.saved.setting.mode.SortMode;
import dev.dubhe.anvilcraft.saved.storage.BaseStorage;
import dev.dubhe.anvilcraft.saved.storage.HyperdimensionStorage;
import dev.dubhe.anvilcraft.saved.storage.IStorageType;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

public final class StorageServerStub {
    private static final int MAX_PLAYER_STUBS = 5;
    private static final int MAX_SYNC_SLOTS = 256;
    private static final int MAX_UNDO_RECORDS = 4;
    private static final ThreadLocal<HolderLookup.Provider> REGISTRIES = new ThreadLocal<>();
    @SuppressWarnings("unused")
    public static final StreamCodec<ByteBuf, IntList> ORDER_STREAM_CODEC = ByteBufCodecs.VAR_INT
        .apply(ByteBufCodecs.list())
        .map(IntArrayList::new, Function.identity());
    @SuppressWarnings("unused")
    public static final StreamCodec<RegistryFriendlyByteBuf, List<ItemStack>> ITEM_STACK_LIST_STREAM_CODEC =
        ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list());
    private static final Multimap<UUID, StorageServerStub> STUBS = ArrayListMultimap.create();
    private static final Map<UUID, Map<Long, UUID>> REMOTE_STORAGES = new HashMap<>();

    private final UUID storageId;
    private long version;
    private long orderVersion;
    private final Map<SortOptions, IntList> orders = new HashMap<>();
    private final Deque<UndoRecord> undoRecords = new ArrayDeque<>();
    private final Map<ItemStack, Integer> undoGroup = new HashMap<>();
    private boolean undoingGroup;

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
            if (index < 0 || !visited.add(index)) {
                continue;
            }
            updates.add(new StackUpdate(index, StorageServerStub.getStack(view, index), view.amount(index)));
        }
        return new SyncResult(stub.version, view.fullness(), updates);
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static InteractionResult interact(UUID playerId, long sourcePos, int slot, int button, StorageInput action) {
        if (!action.isValid(button)) {
            StorageServerStub.REGISTRIES.remove();
            throw new IllegalArgumentException("Invalid storage interaction button: " + button);
        }

        StorageView view = StorageServerStub.getView(StorageServerStub.getAndClear(), playerId, sourcePos);
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        ItemStack carried = player.containerMenu.getCarried();
        boolean changed = false;
        if (action == StorageInput.QUICK_MOVE_TO_STORAGE) {
            changed = StorageServerStub.moveInventoryStackToStorage(player, view, slot) > 0;
        } else if (action == StorageInput.CLONE) {
            if (
                player.hasInfiniteMaterials()
                && carried.isEmpty()
                && slot >= 0
                && slot < view.size()
                && view.amount(slot) > 0
            ) {
                ItemStack stack = view.resource(slot);
                carried = stack.copyWithCount(stack.getMaxStackSize());
                player.containerMenu.setCarried(carried);
            }
        } else if (action == StorageInput.THROW) {
            changed = StorageServerStub.throwStorageStack(player, view, slot, button);
        } else if (action == StorageInput.QUICK_MOVE_FROM_STORAGE) {
            changed = StorageServerStub.moveStorageStackToInventory(player, view, slot);
        } else if (!carried.isEmpty()) {
            int amount = button == 0 ? carried.getCount() : 1;
            int inserted = view.insert(carried.copyWithCount(1), amount);
            if (inserted > 0) {
                carried.shrink(inserted);
                changed = true;
            }
        } else if (slot >= 0 && slot < view.size() && view.amount(slot) > 0) {
            ItemStack itemStack = view.resource(slot);
            int maxPickup = (int) Math.min(itemStack.getMaxStackSize(), view.amount(slot));
            int amount = button == 0 ? maxPickup : Math.ceilDiv(maxPickup, 2);
            int extracted = view.extract(slot, amount);
            if (extracted > 0) {
                carried = itemStack.copyWithCount(extracted);
                player.containerMenu.setCarried(carried);
                changed = true;
            }
        }
        if (changed) {
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        }
        return new InteractionResult(carried, changed);
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static boolean clonePut(
        UUID playerId,
        long sourcePos,
        @CallableParam(clazz = StorageServerStub.class, field = "ORDER_STREAM_CODEC") IntList slots
    ) {
        if (slots.isEmpty() || slots.size() > StorageServerStub.MAX_SYNC_SLOTS) {
            StorageServerStub.REGISTRIES.remove();
            throw new IllegalArgumentException("Invalid clone put slots");
        }
        StorageView view = StorageServerStub.getView(StorageServerStub.getAndClear(), playerId, sourcePos);
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        ItemStack carried = player.containerMenu.getCarried();
        if (!player.hasInfiniteMaterials() || carried.isEmpty()) {
            return false;
        }
        boolean changed = false;
        for (int index : slots) {
            if (index < 0 || index >= view.size()) {
                continue;
            }
            if (view.insert(carried.copyWithCount(1), carried.getMaxStackSize()) > 0) {
                changed = true;
            }
        }
        if (changed) {
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        }
        return changed;
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static boolean quickMoveToStorage(
        UUID playerId,
        long sourcePos,
        @CallableParam(clazz = StorageServerStub.class, field = "ORDER_STREAM_CODEC") IntList slots
    ) {
        if (slots.isEmpty() || slots.size() > StorageServerStub.MAX_SYNC_SLOTS) {
            StorageServerStub.REGISTRIES.remove();
            throw new IllegalArgumentException("Invalid quick move slots");
        }
        StorageView view = StorageServerStub.getView(StorageServerStub.getAndClear(), playerId, sourcePos);
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        final StorageServerStub stub = StorageServerStub.get(playerId, view.primary().getId());
        Map<ItemStack, Integer> moved = new HashMap<>();
        boolean changed = false;
        IntOpenHashSet visited = new IntOpenHashSet(slots.size());
        for (int slot : slots) {
            if (slot < 0 || !visited.add(slot)) {
                continue;
            }
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack key = stack.copyWithCount(1);
            int inserted = StorageServerStub.moveInventoryStackToStorage(player, view, slot);
            if (inserted > 0) {
                moved.merge(key, inserted, Integer::sum);
                changed = true;
            }
        }
        if (changed) {
            StorageServerStub.recordUndo(stub, moved);
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        }
        return changed;
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static boolean moveSameToStorage(UUID playerId, long sourcePos, int slot) {
        StorageView view = StorageServerStub.getView(StorageServerStub.getAndClear(), playerId, sourcePos);
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        final StorageServerStub stub = StorageServerStub.get(playerId, view.primary().getId());
        Inventory inventory = player.getInventory();
        if (slot < 0 || slot >= Inventory.INVENTORY_SIZE) {
            return false;
        }
        ItemStack sample = inventory.getItem(slot).copyWithCount(1);
        if (sample.isEmpty()) {
            return false;
        }
        Map<ItemStack, Integer> moved = new HashMap<>();
        boolean changed = false;
        for (int index = 0; index < Inventory.INVENTORY_SIZE; index++) {
            ItemStack stack = inventory.getItem(index);
            if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, sample)) {
                continue;
            }
            int inserted = view.insert(stack.copyWithCount(1), stack.getCount());
            if (inserted > 0) {
                moved.merge(stack.copyWithCount(1), inserted, Integer::sum);
                stack.shrink(inserted);
                changed = true;
            }
        }
        if (changed) {
            StorageServerStub.recordUndo(stub, moved);
            inventory.setChanged();
            player.containerMenu.broadcastChanges();
        }
        return changed;
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static DepositResult deposit(UUID playerId, long sourcePos, boolean all) {
        StorageView view = StorageServerStub.getView(StorageServerStub.getAndClear(), playerId, sourcePos);
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        final StorageServerStub stub = StorageServerStub.get(playerId, view.primary().getId());
        Map<ItemStack, Integer> moved = new HashMap<>();
        boolean changed = false;
        for (int slot = Inventory.getSelectionSize(); slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty() || !all && !StorageServerStub.matchesStorageItem(view, stack)) {
                continue;
            }
            int inserted = view.insert(stack.copyWithCount(1), stack.getCount());
            if (inserted > 0) {
                moved.merge(stack.copyWithCount(1), inserted, Integer::sum);
                stack.shrink(inserted);
                changed = true;
            }
        }
        if (changed) {
            StorageServerStub.recordUndo(stub, moved);
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        }
        return new DepositResult(changed);
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static DepositResult undo(UUID playerId, long sourcePos) {
        StorageView view = StorageServerStub.getView(StorageServerStub.getAndClear(), playerId, sourcePos);
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        StorageServerStub stub = StorageServerStub.get(playerId, view.primary().getId());
        UndoRecord record = stub.undoRecords.pollFirst();
        if (record == null) {
            return new DepositResult(false);
        }
        Inventory inventory = player.getInventory();
        int emptySlots = 0;
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            if (inventory.getItem(slot).isEmpty()) {
                emptySlots++;
            }
        }
        boolean changed = false;
        for (Map.Entry<ItemStack, Integer> entry : record.moved.entrySet()) {
            ItemStack resource = entry.getKey();
            int needed = entry.getValue();
            int maxStack = inventory.getMaxStackSize(resource);
            int stackSpace = StorageServerStub.getStackSpace(inventory, resource);
            int fit = Math.min(needed, stackSpace + emptySlots * maxStack);
            int extract = Math.min(fit, StorageServerStub.countInStorage(view, resource, fit));
            if (extract <= 0) {
                continue;
            }
            int beyondStacks = Math.max(0, extract - stackSpace);
            emptySlots -= (beyondStacks + maxStack - 1) / maxStack;
            int extracted = StorageServerStub.extractByResource(view, resource, extract);
            if (extracted > 0) {
                ItemStack returned = resource.copyWithCount(extracted);
                if (!player.addItem(returned)) {
                    view.insert(returned.copyWithCount(1), returned.getCount());
                }
                changed = true;
            }
        }
        if (changed) {
            inventory.setChanged();
            player.containerMenu.broadcastChanges();
        }
        return new DepositResult(changed);
    }

    private static int getStackSpace(Inventory inventory, ItemStack resource) {
        int space = 0;
        int maxStack = inventory.getMaxStackSize(resource);
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (ItemStack.isSameItemSameComponents(stack, resource)) {
                space += maxStack - stack.getCount();
            }
        }
        return space;
    }

    private static int countInStorage(StorageView view, ItemStack resource, int limit) {
        int count = 0;
        for (int index = 0; index < view.size() && count < limit; index++) {
            if (view.amount(index) <= 0 || !ItemStack.isSameItemSameComponents(view.resource(index), resource)) {
                continue;
            }
            count = (int) Math.min(limit, (long) count + view.amount(index));
        }
        return count;
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
        if (!stub.undoingGroup) {
            return;
        }
        stub.undoingGroup = false;
        if (!stub.undoGroup.isEmpty()) {
            StorageServerStub.pushUndo(stub, stub.undoGroup);
            stub.undoGroup.clear();
        }
    }

    @RemoteCallable(validator = StorageUsageValidator.class)
    public static StorageUsage getStorageUsage(UUID ignoredPlayerId, UUID storageId) {
        return Storages.get().get(storageId)
            .map(storage -> {
                UnlimitedItemStacksResourceHandler items = storage.getItems();
                List<ItemStack> representatives = new ArrayList<>();
                for (int index = 0; index < items.size() && representatives.size() < 9; index++) {
                    if (items.getAmountAsLong(index) <= 0) continue;
                    ItemStack stack = items.getUnlimitedStackInSlot(index).toStack().copyWithCount(1);
                    if (StorageServerStub.containsType(representatives, stack)) continue;
                    representatives.add(stack);
                }
                return new StorageUsage(items.getTypeCount(), items.getTypeLimit(), representatives);
            })
            .orElse(new StorageUsage(0, 0, List.of()));
    }

    @RemoteCallable(validator = TerminalAccessValidator.class)
    public static long openRemote(UUID playerId, UUID storageId) {
        Storages.get().getOrCreate(storageId, HyperdimensionStorage.class);
        Map<Long, UUID> remote = StorageServerStub.REMOTE_STORAGES.computeIfAbsent(
            playerId,
            ignored -> new HashMap<>()
        );
        long virtualPos;
        do {
            virtualPos = ThreadLocalRandom.current().nextLong();
        } while (remote.containsKey(virtualPos));
        remote.put(virtualPos, storageId);
        return virtualPos;
    }

    @CallableParam(clazz = StorageServerStub.class, field = "ORDER_STREAM_CODEC")
    @RemoteCallable(validator = StorageAccessValidator.class)
    public static IntList terminalReorder(UUID playerId, long sourcePos, String search) {
        HolderLookup.Provider registries = StorageServerStub.getAndClear();
        StorageView view = StorageServerStub.getView(registries, playerId, sourcePos);
        PlayerSetting setting = PlayerSettings.getSetting(registries, playerId);
        StorageSetting storage = setting.storage();
        SortOptions options = new SortOptions(storage.getSort(), storage.getOrder());
        return new IntArrayList(StorageServerStub.createOrder(
            view,
            options,
            search.strip().toLowerCase(Locale.ROOT),
            setting.listed()
        ));
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static InteractionResult terminalTake(
        UUID playerId,
        long sourcePos,
        int slot,
        int button,
        @CallableParam(clazz = ItemStack.class, field = "OPTIONAL_STREAM_CODEC") ItemStack clientCarried
    ) {
        StorageView view = StorageServerStub.getView(StorageServerStub.getAndClear(), playerId, sourcePos);
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        boolean cursorBlocked;
        if (player.hasInfiniteMaterials()) {
            // 创造模式下指针物品由客户端本地管理（ItemPickerMenu 纯客户端），服务端 carried
            // 可能已过期（例如上次取出后的残留），是否允许取出以客户端上报的指针为准。
            cursorBlocked = !clientCarried.isEmpty();
        } else {
            cursorBlocked = !player.containerMenu.getCarried().isEmpty();
        }
        if (cursorBlocked || slot < 0 || slot >= view.size() || view.amount(slot) <= 0) {
            return new InteractionResult(player.containerMenu.getCarried(), false);
        }
        ItemStack resource = view.resource(slot);
        int amount = button == 0
                     ? (int) Math.min(resource.getMaxStackSize(), view.amount(slot))
                     : 1;
        int extracted = view.extract(slot, amount);
        if (extracted > 0) {
            // 取出物品放到指针上（carried）。服务端 setCarried + broadcastChanges
            // 会通过 ContainerSynchronizer 同步到客户端当前活动菜单，无需客户端手动 setCarried；
            // 创造背包界面会忽略该广播，客户端需在 RPC 返回后手动写回指针。
            ItemStack carried = resource.copyWithCount(extracted);
            player.containerMenu.setCarried(carried);
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
            return new InteractionResult(carried, true);
        }
        return new InteractionResult(player.containerMenu.getCarried(), false);
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static InteractionResult terminalTakeToInventory(
        UUID playerId,
        long sourcePos,
        int slot,
        int button
    ) {
        StorageView view = StorageServerStub.getView(StorageServerStub.getAndClear(), playerId, sourcePos);
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        if (slot < 0 || slot >= view.size() || view.amount(slot) <= 0) {
            return new InteractionResult(player.containerMenu.getCarried(), false);
        }
        ItemStack resource = view.resource(slot);
        int amount = button == 0
                     ? (int) Math.min(resource.getMaxStackSize(), view.amount(slot))
                     : 1;
        // 可移入背包的数量 = min(目标数量, 存储数量, 背包空间)
        int space = StorageServerStub.getInventorySpace(player.getInventory(), resource);
        int target = Math.min(amount, space);
        if (target <= 0) {
            // 背包完全放不下：不移动也不取到鼠标
            return new InteractionResult(player.containerMenu.getCarried(), false);
        }
        int extracted = view.extract(slot, target);
        if (extracted <= 0) {
            return new InteractionResult(player.containerMenu.getCarried(), false);
        }
        player.getInventory().add(resource.copyWithCount(extracted));
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        return new InteractionResult(player.containerMenu.getCarried(), true);
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static InteractionResult terminalInsert(
        UUID playerId,
        long sourcePos,
        @CallableParam(clazz = ItemStack.class, field = "OPTIONAL_STREAM_CODEC") ItemStack clientCarried
    ) {
        StorageView view = StorageServerStub.getView(StorageServerStub.getAndClear(), playerId, sourcePos);
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        ItemStack carried;
        if (player.hasInfiniteMaterials()) {
            // 创造模式下指针物品由客户端本地管理（ItemPickerMenu 纯客户端，不经过服务端菜单同步），
            // 服务端 carried 可能已过期（例如取出后再从创造格子拿起别的物品），一律以客户端上报为准。
            carried = clientCarried;
        } else {
            carried = player.containerMenu.getCarried();
        }
        if (carried.isEmpty()) {
            return new InteractionResult(carried, false);
        }
        // 把指针整组放入存储；被 canStore 拒绝（如嵌套物品）时插入 0，物品保留在手中
        int inserted = view.insert(carried.copyWithCount(1), carried.getCount());
        if (inserted <= 0) {
            return new InteractionResult(carried, false);
        }
        carried.shrink(inserted);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        return new InteractionResult(carried, true);
    }

    /**
     * JEI 快速合成补库：终端持有者把合成缺少的物品从绑定存储站取出补入背包。
     * 仅当玩家确实持有指向该 storageId 的已绑定终端时生效。
     */
    @RemoteCallable(validator = TerminalAccessValidator.class)
    public static boolean terminalWithdrawToInventory(
        UUID playerId,
        UUID storageId,
        @CallableParam(clazz = StorageServerStub.class, field = "ITEM_STACK_LIST_STREAM_CODEC")
        List<ItemStack> needs
    ) {
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        if (!StorageServerStub.ownsBoundTerminal(player, storageId)) {
            return false;
        }
        Optional<HyperdimensionStorage> storageOp = Storages.get().get(storageId, HyperdimensionStorage.class);
        if (storageOp.isEmpty()) {
            return false;
        }
        HyperdimensionStorage storage = storageOp.get();
        UnlimitedItemStacksResourceHandler items = storage.getItems();
        boolean changed = false;
        for (ItemStack need : needs) {
            if (need.isEmpty()) {
                continue;
            }
            ItemStack resource = need.copyWithCount(1);
            int required = need.getCount();
            required -= StorageServerStub.countInInventory(player.getInventory(), resource);
            if (required <= 0) {
                continue;
            }
            // 每格最多取到物品上限（同种物品在背包中的总数量不超过 maxStackSize 是 JEI 的需求前提，
            // 但为防背包放不下导致 add 丢弃，按背包空间限制每次提取量）
            for (int slot = 0; slot < items.size() && required > 0; slot++) {
                if (items.getAmountAsLong(slot) <= 0) {
                    continue;
                }
                UnlimitedItemStack stored = items.getUnlimitedStackInSlot(slot);
                if (!stored.isSameItemSameComponents(resource)) {
                    continue;
                }
                int space = StorageServerStub.getInventorySpace(player.getInventory(), resource);
                int take = (int) Math.min(Math.min(required, items.getAmountAsLong(slot)), space);
                if (take <= 0) {
                    break;
                }
                int got = items.extractUnlimited(slot, take, false).getCount();
                if (got > 0) {
                    player.getInventory().add(resource.copyWithCount(got));
                    required -= got;
                    changed = true;
                }
            }
        }
        if (changed) {
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        }
        return changed;
    }

    /**
     * JEI 快速合成检查：返回玩家绑定存储站中每种物品的一份代表（去重），
     * 供客户端缓存判断存储站是否拥有配方所需物品。限制返回条目数，避免超大
     * 存储站在每次刷新时全量扫描造成服务端尖峰。
     */
    private static final int MAX_STORAGE_ITEMS = 512;
    /**
     * 单次扫描的最大槽位数。存储槽列表稀疏设计（槽位索引可能因历史删除产生空洞），
     * 仅限制结果数会允许 512 种物品分散在极大槽位范围时深扫整个列表；
     * 超过该深度后视为不再有效收集（缓存 15s 刷新，漏报仅影响 JEI "+" 可用性提示，
     * 传输阶段仍由服务端按实际缺口校验）。
     */
    private static final int MAX_STORAGE_SCAN_SLOTS = 4096;

    @CallableParam(clazz = StorageServerStub.class, field = "ITEM_STACK_LIST_STREAM_CODEC")
    @RemoteCallable(validator = TerminalAccessValidator.class)
    public static List<ItemStack> getStorageItems(UUID playerId, UUID storageId) {
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        if (!StorageServerStub.ownsBoundTerminal(player, storageId)) {
            return List.of();
        }
        Optional<HyperdimensionStorage> storageOp = Storages.get().get(storageId, HyperdimensionStorage.class);
        if (storageOp.isEmpty()) {
            return List.of();
        }
        UnlimitedItemStacksResourceHandler items = storageOp.get().getItems();
        List<ItemStack> result = new ArrayList<>();
        for (int slot = 0;
             slot < items.size()
             && slot < StorageServerStub.MAX_STORAGE_SCAN_SLOTS
             && result.size() < StorageServerStub.MAX_STORAGE_ITEMS;
             slot++) {
            long amount = items.getAmountAsLong(slot);
            if (amount <= 0) {
                continue;
            }
            ItemStack stack = items.getUnlimitedStackInSlot(slot).toStack().copyWithCount(1);
            if (stack.isEmpty()) {
                continue;
            }
            boolean duplicate = false;
            for (ItemStack existing : result) {
                if (ItemStack.isSameItemSameComponents(existing, stack)) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) {
                result.add(stack);
            }
        }
        return result;
    }

    private static int countInInventory(Inventory inventory, ItemStack resource) {
        int count = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, resource)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static boolean ownsBoundTerminal(ServerPlayer player, UUID storageId) {
        if (StorageServerStub.isBoundTerminal(player.getMainHandItem(), storageId)) return true;
        if (StorageServerStub.isBoundTerminal(player.getOffhandItem(), storageId)) return true;
        for (ItemStack stack : player.getInventory().items) {
            if (StorageServerStub.isBoundTerminal(stack, storageId)) return true;
        }
        return false;
    }

    private static boolean isBoundTerminal(ItemStack stack, UUID storageId) {
        if (!stack.is(ModItems.HYPERDIMENSION_TERMINAL)) return false;
        TerminalBinding binding = stack.get(ModComponents.TERMINAL_BINDING);
        return binding != null && binding.id().isPresent() && binding.id().get().equals(storageId);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean canStore(BaseStorage<?> storage, ItemStack stack) {
        Holder<IStorageType<?>> type = storage.getTypeHolder();
        if (type.is(ModStorageTypes.SHULKER_CONTAINER.getKey()) || type.is(ModStorageTypes.HYPERDIMENSION.getKey())) {
            return !(stack.getItem() instanceof ShulkerContainerBlockItem)
                   && !(stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof HyperdimensionStorageStationBlock)
                   && !stack.is(ModItems.HYPERDIMENSION_TERMINAL);
        }
        return true;
    }

    private static boolean containsType(List<ItemStack> types, ItemStack stack) {
        for (ItemStack type : types) {
            if (ItemStack.isSameItemSameComponents(type, stack)) return true;
        }
        return false;
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static DepositResult take(UUID playerId, long sourcePos) {
        StorageView view = StorageServerStub.getView(StorageServerStub.getAndClear(), playerId, sourcePos);
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        boolean changed = false;
        Inventory inventory = player.getInventory();
        for (int slot = Inventory.getSelectionSize(); slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            int amount = inventory.getMaxStackSize(stack) - stack.getCount();
            if (amount <= 0) {
                continue;
            }
            for (int index = 0; index < view.size() && amount > 0; index++) {
                if (
                    view.amount(index) <= 0
                    || !ItemStack.isSameItemSameComponents(view.resource(index), stack)
                ) {
                    continue;
                }
                int extracted = view.extract(index, amount);
                if (extracted > 0) {
                    stack.grow(extracted);
                    amount -= extracted;
                    changed = true;
                }
            }
        }
        if (changed) {
            inventory.setChanged();
            player.containerMenu.broadcastChanges();
        }
        return new DepositResult(changed);
    }

    private static void recordUndo(StorageServerStub stub, Map<ItemStack, Integer> moved) {
        if (moved.isEmpty()) {
            return;
        }
        if (stub.undoingGroup) {
            for (Map.Entry<ItemStack, Integer> entry : moved.entrySet()) {
                stub.undoGroup.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
            return;
        }
        StorageServerStub.pushUndo(stub, moved);
    }

    private static void pushUndo(StorageServerStub stub, Map<ItemStack, Integer> moved) {
        stub.undoRecords.addFirst(new UndoRecord(moved));
        while (stub.undoRecords.size() > StorageServerStub.MAX_UNDO_RECORDS) {
            stub.undoRecords.removeLast();
        }
    }

    private static int extractByResource(StorageView view, ItemStack resource, int amount) {
        int extracted = 0;
        for (int index = 0; index < view.size() && extracted < amount; index++) {
            if (view.amount(index) <= 0 || !ItemStack.isSameItemSameComponents(view.resource(index), resource)) {
                continue;
            }
            extracted += view.extract(index, amount - extracted);
        }
        return extracted;
    }

    private static boolean matchesStorageItem(StorageView view, ItemStack stack) {
        for (int index = 0; index < view.size(); index++) {
            if (view.amount(index) > 0 && ItemStack.isSameItemSameComponents(view.resource(index), stack)) {
                return true;
            }
        }
        return false;
    }

    private static int moveInventoryStackToStorage(
        ServerPlayer player,
        StorageView view,
        int slot
    ) {
        Inventory inventory = player.getInventory();
        if (slot < 0 || slot >= Inventory.INVENTORY_SIZE) {
            return 0;
        }
        ItemStack stack = inventory.getItem(slot);
        if (stack.isEmpty()) {
            return 0;
        }
        int inserted = view.insert(stack.copyWithCount(1), stack.getCount());
        if (inserted <= 0) {
            return 0;
        }
        stack.shrink(inserted);
        return inserted;
    }

    private static boolean moveStorageStackToInventory(
        ServerPlayer player,
        StorageView view,
        int slot
    ) {
        if (slot < 0 || slot >= view.size() || view.amount(slot) <= 0) {
            return false;
        }
        ItemStack stack = view.resource(slot);
        int amount = (int) Math.min(
            Math.min(view.amount(slot), stack.getMaxStackSize()),
            StorageServerStub.getInventorySpace(player.getInventory(), stack)
        );
        if (amount <= 0) {
            return false;
        }
        int extracted = view.extract(slot, amount);
        if (extracted <= 0) {
            return false;
        }
        player.getInventory().add(stack.copyWithCount(extracted));
        return true;
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
            !player.containerMenu.getCarried().isEmpty()
            || slot < 0
            || slot >= view.size()
            || view.amount(slot) <= 0
        ) {
            return false;
        }
        ItemStack stack = view.resource(slot);
        int stackCount = stack.getMaxStackSize();
        long requested = button == 0 ? 1 : (long) stackCount * (button == 1 ? 1 : 9);
        int amount = (int) Math.min(view.amount(slot), Math.toIntExact(requested));
        int extracted = view.extract(slot, amount);
        if (extracted <= 0) {
            return false;
        }
        int remaining = extracted;
        while (remaining > 0) {
            int dropCount = Math.min(stackCount, remaining);
            ItemStack dropped = stack.copyWithCount(dropCount);
            player.drop(dropped, true);
            remaining -= dropCount;
        }
        return true;
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
        StorageServerStub.REMOTE_STORAGES.remove(playerId);
    }

    public static void clear() {
        StorageServerStub.STUBS.clear();
        StorageServerStub.REMOTE_STORAGES.clear();
    }

    /**
     * 扫描玩家背包与主/副手中的已绑定终端，收集其指向的超维存储站。返回去重后的存储站列表。
     */
    private static List<BaseStorage<?>> boundStorages(ServerPlayer player) {
        List<BaseStorage<?>> storages = new ArrayList<>();
        for (ItemStack stack : player.getInventory().items) {
            StorageServerStub.collectBoundStorage(stack, storages);
        }
        for (ItemStack stack : player.getInventory().armor) {
            StorageServerStub.collectBoundStorage(stack, storages);
        }
        for (ItemStack stack : player.getInventory().offhand) {
            StorageServerStub.collectBoundStorage(stack, storages);
        }
        return storages;
    }

    private static void collectBoundStorage(ItemStack stack, List<BaseStorage<?>> storages) {
        if (!stack.is(ModItems.HYPERDIMENSION_TERMINAL)) {
            return;
        }
        TerminalBinding binding = stack.get(ModComponents.TERMINAL_BINDING);
        if (binding == null || binding.id().isEmpty()) {
            return;
        }
        UUID id = binding.id().get();
        for (BaseStorage<?> existing : storages) {
            if (existing.getId().equals(id)) {
                return;
            }
        }
        storages.add(Storages.get().getOrCreate(id, HyperdimensionStorage.class));
    }

    /**
     * 物品均衡：把玩家身上超过一组（满格）的多余物品自动存入已绑定的存储站。
     * 每个物品只保留一组在身上，超出部分尽量存入；返回是否发生任何变动。
     */
    public static void depositExcess(ServerPlayer player) {
        if (player.hasInfiniteMaterials()) {
            return;
        }
        List<BaseStorage<?>> storages = StorageServerStub.boundStorages(player);
        if (storages.isEmpty()) {
            return;
        }
        boolean changed = false;
        // 聚合统计：每种物品在身上的总数量，超过一组的部分即为待存入的多余量。
        // 仅统计主物品栏（0-35，含热键栏），不动盔甲与副手（功能性物品不应被自动收走）。
        List<ItemStack> representative = new ArrayList<>();
        List<Integer> totals = new ArrayList<>();
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            int idx = -1;
            for (int r = 0; r < representative.size(); r++) {
                if (ItemStack.isSameItemSameComponents(representative.get(r), stack)) {
                    idx = r;
                    break;
                }
            }
            if (idx < 0) {
                representative.add(stack.copyWithCount(1));
                totals.add(stack.getCount());
            } else {
                totals.set(idx, totals.get(idx) + stack.getCount());
            }
        }
        for (int r = 0; r < representative.size(); r++) {
            int max = representative.get(r).getMaxStackSize();
            int total = totals.get(r);
            if (total <= max) {
                continue;
            }
            int excess = total - max;
            int inserted = StorageServerStub.insertIntoStorages(storages, representative.get(r), excess);
            if (inserted <= 0) {
                continue;
            }
            changed = true;
            // 从主物品栏移走已存入的多余数量：优先非主手槽位，最后才动主手
            int remaining = inserted;
            for (int i = Inventory.INVENTORY_SIZE - 1; i >= 0 && remaining > 0; i--) {
                if (i == player.getInventory().selected) {
                    continue;
                }
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, representative.get(r))) {
                    continue;
                }
                int take = Math.min(stack.getCount(), remaining);
                stack.shrink(take);
                remaining -= take;
            }
            if (remaining > 0) {
                ItemStack held = player.getInventory().getItem(player.getInventory().selected);
                if (held.isEmpty() || !ItemStack.isSameItemSameComponents(held, representative.get(r))) {
                    // 主手不是该物品（已换手），丢弃多余无法归位则忽略（不应发生）
                    continue;
                }
                held.shrink(remaining);
            }
        }
        if (changed) {
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        }
    }

    /**
     * 物品均衡：当主/副手物品用尽时，从已绑定存储站取出同种物品补充满一组。
     * 把补满的整组物品放回指定手持槽；若存储站中没有同种物品则不做任何事。
     */
    public static void restockHand(ServerPlayer player, ItemStack usedUpItem, int inventorySlot) {
        if (usedUpItem.isEmpty() || player.hasInfiniteMaterials()) {
            return;
        }
        List<BaseStorage<?>> storages = StorageServerStub.boundStorages(player);
        if (storages.isEmpty()) {
            return;
        }
        ItemStack resource = usedUpItem.copyWithCount(1);
        int need = resource.getMaxStackSize();
        int taken = 0;
        for (BaseStorage<?> storage : storages) {
            if (!StorageServerStub.canStore(storage, resource)) {
                continue;
            }
            UnlimitedItemStacksResourceHandler items = storage.getItems();
            for (int slot = 0; slot < items.size(); slot++) {
                if (items.getAmountAsLong(slot) <= 0) {
                    continue;
                }
                UnlimitedItemStack stored = items.getUnlimitedStackInSlot(slot);
                if (!stored.isSameItemSameComponents(resource)) {
                    continue;
                }
                int take = (int) Math.min(need - taken, items.getAmountAsLong(slot));
                int got = items.extractUnlimited(slot, take, false).getCount();
                taken += got;
                if (taken == need) {
                    break;
                }
            }
            if (taken == need) {
                break;
            }
        }
        if (taken <= 0) {
            return;
        }
        ItemStack filled = resource.copyWithCount(taken);
        if (inventorySlot >= 0 && inventorySlot < player.getInventory().getContainerSize()) {
            player.getInventory().setItem(inventorySlot, filled);
        } else {
            player.drop(filled, true);
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
    }

    private static int insertIntoStorages(List<BaseStorage<?>> storages, ItemStack resource, int amount) {
        int inserted = 0;
        for (BaseStorage<?> storage : storages) {
            if (!StorageServerStub.canStore(storage, resource)) {
                continue;
            }
            UnlimitedItemStacksResourceHandler items = storage.getItems();
            for (int slot = 0; slot < items.size(); slot++) {
                if (items.getAmountAsLong(slot) > 0
                    && items.getUnlimitedStackInSlot(slot).isSameItemSameComponents(resource)) {
                    ItemStack leftover = items.insertItem(slot, resource.copyWithCount(amount - inserted), false);
                    inserted += (amount - inserted) - leftover.getCount();
                    if (inserted == amount) {
                        return inserted;
                    }
                }
            }
        }
        for (BaseStorage<?> storage : storages) {
            if (!StorageServerStub.canStore(storage, resource)) {
                continue;
            }
            UnlimitedItemStacksResourceHandler items = storage.getItems();
            for (int slot = 0; slot < items.size(); slot++) {
                if (items.getAmountAsLong(slot) <= 0) {
                    ItemStack leftover = items.insertItem(slot, resource.copyWithCount(amount - inserted), false);
                    inserted += (amount - inserted) - leftover.getCount();
                    if (inserted == amount) {
                        return inserted;
                    }
                }
            }
        }
        return inserted;
    }

    public static final class StorageUsageValidator implements IRemoteCallableValidator {
        @Override
        public boolean validate(IPayloadContext ctx, Method method, Object[] args) {
            return ctx.player() instanceof ServerPlayer player
                   && args.length >= 2
                   && args[0] instanceof UUID playerId
                   && player.getGameProfile().getId().equals(playerId)
                   && args[1] instanceof UUID storageId
                   // 仅允许查询自己持有绑定终端指向的存储，防止凭 UUID 枚举他人存储信息
                   && StorageServerStub.ownsBoundTerminal(player, storageId);
        }
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
                || !player.getGameProfile().getId().equals(playerId)
                || !(args[1] instanceof Long sourcePos)
            ) {
                return false;
            }
            if (StorageServerStub.REMOTE_STORAGES.getOrDefault(playerId, Map.of()).containsKey(sourcePos)) {
                return true;
            }
            BlockPos pos = BlockPos.of(sourcePos);
            BlockEntity blockEntity = player.level().getBlockEntity(pos);
            // 不强制要求 storage.getId() 非 null：首次访问时 getView 会惰性生成 id 并持久化，
            // 若这里拒绝会导致潜影集装箱等（放置时组件应用路径不同的多方块）永远无法存取。
            return blockEntity instanceof StorageBlockEntity storage
                   && AbstractContainerMenu.stillValid(
                ContainerLevelAccess.create(player.level(), pos),
                player,
                storage.getBlockState().getBlock()
            );
        }
    }

    public static final class TerminalAccessValidator implements IRemoteCallableValidator {
        @Override
        public boolean validate(IPayloadContext ctx, Method method, Object[] args) {
            if (
                !(ctx.player() instanceof ServerPlayer player)
                || args.length < 2
                || !(args[0] instanceof UUID playerId)
                || !player.getGameProfile().getId().equals(playerId)
                || !(args[1] instanceof UUID storageId)
            ) {
                return false;
            }
            return StorageServerStub.ownsBoundTerminal(player, storageId);
        }
    }

    public static final class StorageOpenStateValidator implements IRemoteCallableValidator {
        @Override
        public boolean validate(IPayloadContext ctx, Method method, Object[] args) {
            if (
                !(ctx.player() instanceof ServerPlayer player)
                || args.length != 3
                || !(args[0] instanceof UUID playerId)
                || !player.getGameProfile().getId().equals(playerId)
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

    public record StorageUsage(int usedTypes, int typeLimit, List<ItemStack> types) {
        public static final StreamCodec<RegistryFriendlyByteBuf, StorageUsage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            StorageUsage::usedTypes,
            ByteBufCodecs.VAR_INT,
            StorageUsage::typeLimit,
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()),
            StorageUsage::types,
            StorageUsage::new
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

    private record UndoRecord(Map<ItemStack, Integer> moved) {
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
        return this.orders.computeIfAbsent(options, ignored -> StorageServerStub.createOrder(view, options, "", categories));
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
            ItemStack stack = view.resource(index);
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            String name = requiresName ? stack.getHoverName().getString() : "";
            UnlimitedItemStack unlimitedStack = new UnlimitedItemStack(stack, (int) Math.min(amount, Integer.MAX_VALUE));
            if (!StorageServerStub.matchesFilters(stack.getItemHolder(), unlimitedStack, id, name, search, categories)) {
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
        ResourceLocation id,
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

    private static boolean matchesTag(ResourceLocation id, String search) {
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
        UUID remoteId = StorageServerStub.REMOTE_STORAGES.getOrDefault(playerId, Map.of()).get(sourcePos);
        if (remoteId != null) {
            BaseStorage<?> storage = Storages.get().getOrCreate(remoteId, HyperdimensionStorage.class);
            return new StorageView(List.of(storage), List.of());
        }
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

    private record OrderEntry(int index, long amount, ResourceLocation id, String name) {
    }

    private static final class StorageView {
        private final List<BaseStorage<?>> storages;
        private final List<Entry> entries = new ArrayList<>();

        private StorageView(List<BaseStorage<?>> storages, List<Entry> ignored) {
            this.storages = storages;
            Map<UnlimitedItemStacksResourceHandler.ResourceKey, Entry> merged = new HashMap<>();
            for (int storageIndex = 0; storageIndex < storages.size(); storageIndex++) {
                UnlimitedItemStacksResourceHandler items = storages.get(storageIndex).getItems();
                for (int slot = 0; slot < items.size(); slot++) {
                    if (items.getAmountAsLong(slot) <= 0) continue;
                    ItemStack resource = items.getUnlimitedStackInSlot(slot).toStack().copyWithCount(1);
                    UnlimitedItemStacksResourceHandler.ResourceKey key =
                        UnlimitedItemStacksResourceHandler.ResourceKey.of(resource);
                    Entry entry = merged.get(key);
                    if (entry == null) {
                        entry = new Entry(resource, 0, storageIndex, slot);
                        merged.put(key, entry);
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

        ItemStack resource(int index) {
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

        int insert(ItemStack resource, int amount) {
            if (!StorageServerStub.canStore(this.primary(), resource)) {
                return 0;
            }
            int inserted = 0;
            for (int i = 0; i < this.storages.size() - 1; i++) {
                UnlimitedItemStacksResourceHandler items = this.storages.get(i).getItems();
                if (!contains(items, resource)) continue;
                inserted += insertInto(items, resource.copyWithCount(amount - inserted));
                if (inserted == amount) return inserted;
            }
            UnlimitedItemStacksResourceHandler primaryItems = this.primary().getItems();
            inserted += insertInto(primaryItems, resource.copyWithCount(amount - inserted));
            if (inserted == amount) return inserted;
            for (int i = 0; i < this.storages.size() - 1; i++) {
                inserted += insertInto(this.storages.get(i).getItems(), resource.copyWithCount(amount - inserted));
                if (inserted == amount) return inserted;
            }
            return inserted;
        }

        private static boolean contains(UnlimitedItemStacksResourceHandler items, ItemStack resource) {
            for (int i = 0; i < items.size(); i++) {
                if (items.getAmountAsLong(i) > 0 && items.getUnlimitedStackInSlot(i).isSameItemSameComponents(resource)) return true;
            }
            return false;
        }

        int extract(int index, int amount) {
            Entry e = this.entries.get(index);
            return this.storages.get(e.storageIndex).getItems().extractUnlimited(e.slot, amount, false).getCount();
        }

        private static int insertInto(UnlimitedItemStacksResourceHandler items, ItemStack stack) {
            if (stack.isEmpty()) return 0;
            ItemStack leftover = items.insertItem(stack, false);
            return stack.getCount() - leftover.getCount();
        }

        private static final class Entry {
            final ItemStack resource;
            long amount;
            final int storageIndex;
            final int slot;

            Entry(ItemStack resource, long amount, int storageIndex, int slot) {
                this.resource = resource;
                this.amount = amount;
                this.storageIndex = storageIndex;
                this.slot = slot;
            }
        }
    }
}
