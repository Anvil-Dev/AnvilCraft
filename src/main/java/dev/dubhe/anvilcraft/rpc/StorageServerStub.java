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
import dev.dubhe.anvilcraft.block.entity.storage.LargeCrateBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.ShulkerContainerBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.TerminalBlockRegistry;
import dev.dubhe.anvilcraft.block.item.ShulkerContainerBlockItem;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.storage.ModStorageTypes;
import dev.dubhe.anvilcraft.item.property.component.StorageRef;
import dev.dubhe.anvilcraft.item.property.component.TerminalBinding;
import dev.dubhe.anvilcraft.saved.setting.PlayerSetting;
import dev.dubhe.anvilcraft.saved.setting.PlayerSettings;
import dev.dubhe.anvilcraft.saved.setting.StorageSetting;
import dev.dubhe.anvilcraft.saved.setting.mode.OrderMode;
import dev.dubhe.anvilcraft.saved.setting.mode.SortMode;
import dev.dubhe.anvilcraft.saved.storage.BaseStorage;
import dev.dubhe.anvilcraft.saved.storage.CraftingStorage;
import dev.dubhe.anvilcraft.saved.storage.HyperdimensionStorage;
import dev.dubhe.anvilcraft.saved.storage.IStorageType;
import dev.dubhe.anvilcraft.saved.storage.LargeCrateStorage;
import dev.dubhe.anvilcraft.saved.storage.ShulkerContainerStorage;
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
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
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
import java.util.function.BiConsumer;
import java.util.function.Function;
import javax.annotation.Nullable;

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
    public static final StreamCodec<ByteBuf, List<UUID>> UUID_LIST_STREAM_CODEC = UUIDUtil.STREAM_CODEC
        .apply(ByteBufCodecs.list());
    @SuppressWarnings("unused")
    public static final StreamCodec<RegistryFriendlyByteBuf, List<ItemStack>> ITEM_STACK_LIST_STREAM_CODEC =
        ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list());
    private static final Multimap<UUID, StorageServerStub> STUBS = ArrayListMultimap.create();
    private static final Map<UUID, Map<Long, RemoteTarget>> REMOTE_STORAGES = new HashMap<>();

    /** 本地终端自动连接大型板条箱的搜索半径（格）。 */
    private static final int LOCAL_TERMINAL_RANGE = 32;
    /** 潜影终端自动连接世界潜影集装箱的搜索半径（格）。 */
    private static final int SHULKER_TERMINAL_RANGE = 64;
    /**
     * 连续合成（Shift 点击③/④ 结果槽）单次 RPC 内最多合成的次数。
     * 单次调用在服务端线程同步执行，分块后客户端循环调用直到 {@code done}，
     * 避免几百次合成一次性阻塞服务端线程（分帧/进度由客户端循环天然实现）。
     */
    private static final int CRAFTING_TAKE_ALL_CHUNK = 64;

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
            // 越界 / 重复索引直接跳过：客户端缓存可能过期（存储被其它来源修改后
            // 条目数变化），越界槽位由客户端下次全量刷新校正，不应在此崩溃
            if (index < 0 || index >= view.size() || !visited.add(index)) {
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
    public static boolean quickMoveFromStorage(
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
        boolean changed = false;
        IntOpenHashSet visited = new IntOpenHashSet(slots.size());
        for (int slot : slots) {
            if (slot < 0 || !visited.add(slot)) {
                continue;
            }
            changed |= StorageServerStub.moveStorageStackToInventory(player, view, slot);
        }
        if (changed) {
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        }
        return changed;
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static boolean quickMoveUndo(UUID playerId, long sourcePos, int slot, int count) {
        StorageView view = StorageServerStub.getView(StorageServerStub.getAndClear(), playerId, sourcePos);
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        if (count <= 0 || slot < 0 || slot >= Inventory.INVENTORY_SIZE) {
            return false;
        }
        ItemStack stack = player.getInventory().getItem(slot);
        if (stack.isEmpty()) {
            return false;
        }
        int amount = Math.min(count, stack.getCount());
        int extracted = view.insert(stack.copyWithCount(1), amount);
        if (extracted <= 0) {
            return true;
        }
        stack.shrink(extracted);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        return true;
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
                int typeLimit = items.getTypeLimit();
                // 无限类型存储（如超维存储站）以 0 表示“无类型上限”，客户端据此渲染 ∞
                if (typeLimit == Integer.MAX_VALUE) {
                    typeLimit = 0;
                }
                return new StorageUsage(items.getTypeCount(), typeLimit, representatives);
            })
            .orElse(new StorageUsage(0, 0, List.of()));
    }

    @RemoteCallable(validator = TerminalAccessValidator.class)
    public static long openRemote(UUID playerId, UUID storageId) {
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        RemoteTarget target;
        if (storageId.equals(StorageServerStub.localTerminalId(playerId))) {
            // 本地终端：自动连接玩家 32 格内最近的一个大型板条箱
            Optional<UUID> crateId = StorageServerStub.findNearbyLargeCrate(player);
            if (crateId.isEmpty()) {
                return -1L;
            }
            target = new RemoteTarget(RemoteTarget.LARGE_CRATE, crateId.get());
        } else if (storageId.equals(StorageServerStub.shulkerTerminalId(playerId))) {
            // 潜影终端：优先连接身上槽位最靠前的潜影集装箱（无 UUID 时在打开时惰性
            // 授予，无论是否已有 UUID 都取最靠前的那个）；否则连接 64 格内最近的
            // 世界潜影集装箱
            Optional<UUID> containerId = StorageServerStub.findOrGrantFrontmostShulkerContainer(player);
            if (containerId.isPresent()) {
                target = new RemoteTarget(RemoteTarget.SHULKER_CONTAINER, containerId.get());
            } else {
                Optional<UUID> worldId = StorageServerStub.findNearbyShulkerContainer(player);
                if (worldId.isEmpty()) {
                    return -1L;
                }
                target = new RemoteTarget(RemoteTarget.SHULKER_CONTAINER, worldId.get());
            }
        } else {
            Storages.get().getOrCreate(storageId, HyperdimensionStorage.class);
            target = new RemoteTarget(RemoteTarget.HYPERDIMENSION, storageId);
        }
        Map<Long, RemoteTarget> remote = StorageServerStub.REMOTE_STORAGES.computeIfAbsent(
            playerId,
            ignored -> new HashMap<>()
        );
        long virtualPos;
        do {
            virtualPos = ThreadLocalRandom.current().nextLong();
        } while (remote.containsKey(virtualPos));
        remote.put(virtualPos, target);
        return virtualPos;
    }

    /** 仓储合成模式是否可用：主存储中同时存在工作台与切石机（按物品标签判定）。 */
    @RemoteCallable(validator = StorageAccessValidator.class)
    public static boolean craftingAvailable(UUID playerId, long sourcePos) {
        StorageView view = StorageServerStub.getView(StorageServerStub.getAndClear(), playerId, sourcePos);
        UnlimitedItemStacksResourceHandler items = view.primary().getItems();
        boolean hasWorkbench = false;
        boolean hasStonecutter = false;
        for (int i = 0; i < items.size() && (!hasWorkbench || !hasStonecutter); i++) {
            if (items.getAmountAsLong(i) <= 0) {
                continue;
            }
            ItemStack stack = items.getUnlimitedStackInSlot(i).toStack();
            if (stack.is(Tags.Items.PLAYER_WORKSTATIONS_CRAFTING_TABLES)) {
                hasWorkbench = true;
            } else if (stack.is(ModItemTags.PLAYER_WORKSTATIONS_STONECUTTERS)) {
                hasStonecutter = true;
            }
        }
        return hasWorkbench && hasStonecutter;
    }

    /**
     * 读取仓储合成面板数据（① 切石机输入、② 合成 9 宫格、切石机选中配方、上次打开模式）。
     * 世界打开读主存储的 crafting 字段，终端打开读终端物品的 crafting 数据组件。
     */
    @RemoteCallable(validator = StorageAccessValidator.class)
    public static CraftingStorage craftingGet(UUID playerId, long sourcePos) {
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        return StorageServerStub.resolveCraftingTarget(player, sourcePos).read();
    }

    /**
     * ① 切石机输入：按玩家物品栏点击语义与指针交换。
     * 左键空指针取整堆 / 右键取半堆；指针有物时同种堆叠（左键放全部、右键放 1 个），
     * 异种整个交换。仅接受能匹配切石机配方的物品。
     */
    @RemoteCallable(validator = StorageAccessValidator.class)
    public static InteractionResult craftingPutStonecutterInput(
        UUID playerId,
        long sourcePos,
        int button,
        @CallableParam(clazz = ItemStack.class, field = "OPTIONAL_STREAM_CODEC") ItemStack clientCarried
    ) {
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        StorageServerStub.CraftingTarget target = StorageServerStub.resolveCraftingTarget(player, sourcePos);
        CraftingStorage crafting = target.read();
        // 指针物品以客户端上报为准（与服务端 getCarried 一致；创造模式下服务端指针可能已过期）
        ItemStack carried = player.hasInfiniteMaterials() ? clientCarried : player.containerMenu.getCarried();
        ItemStack current = crafting.stonecutterInput();
        // 指针为空：取出①（左键整堆 / 右键半堆）
        if (carried.isEmpty()) {
            if (current.isEmpty()) {
                return new InteractionResult(ItemStack.EMPTY, false);
            }
            int amount = button == 0 ? current.getCount() : Math.ceilDiv(current.getCount(), 2);
            ItemStack taken = current.copyWithCount(amount);
            ItemStack rest = current.copy();
            rest.shrink(amount);
            target.write(crafting.withStonecutterInput(rest.isEmpty() ? ItemStack.EMPTY : rest));
            player.containerMenu.setCarried(taken);
            player.containerMenu.broadcastChanges();
            return new InteractionResult(taken, true);
        }
        // 指针有物品：仅接受能匹配切石机配方的输入
        List<RecipeHolder<StonecutterRecipe>> recipes = player.level().getRecipeManager()
            .getRecipesFor(RecipeType.STONECUTTING, new SingleRecipeInput(carried), player.level());
        if (recipes.isEmpty()) {
            return new InteractionResult(carried, false);
        }
        // 空槽 / 同种：堆叠（左键放全部、右键放 1 个，不超过最大堆叠）
        if (current.isEmpty() || ItemStack.isSameItemSameComponents(current, carried)) {
            int space = current.isEmpty() ? carried.getCount()
                : Math.min(carried.getCount(), current.getMaxStackSize() - current.getCount());
            int place = button == 0 ? space : Math.min(1, space);
            if (place <= 0) {
                return new InteractionResult(carried, false);
            }
            ItemStack newCurrent = current.copy();
            if (current.isEmpty()) {
                newCurrent = carried.copyWithCount(place);
            } else {
                newCurrent.grow(place);
            }
            target.write(crafting.withStonecutterInput(newCurrent));
            ItemStack newCarried = carried.copy();
            newCarried.shrink(place);
            player.containerMenu.setCarried(newCarried);
            player.containerMenu.broadcastChanges();
            return new InteractionResult(newCarried, true);
        }
        // 异种：整个交换（输入变化后重置选中配方）
        target.write(crafting.withStonecutterInput(carried.copy()).withStonecutterSelected(0));
        player.containerMenu.setCarried(current);
        player.containerMenu.broadcastChanges();
        return new InteractionResult(current, true);
    }

    /**
     * ② 合成 9 宫格：按玩家物品栏点击语义与指定槽交换物品。
     * 左键空指针取整堆 / 右键取半堆；指针有物时同种堆叠（左键放全部、右键放 1 个），异种整个交换。
     */
    @RemoteCallable(validator = StorageAccessValidator.class)
    public static InteractionResult craftingPutCraftingSlot(
        UUID playerId,
        long sourcePos,
        int slot,
        int button,
        @CallableParam(clazz = ItemStack.class, field = "OPTIONAL_STREAM_CODEC") ItemStack clientCarried
    ) {
        if (slot < 0 || slot >= CraftingStorage.CRAFTING_GRID_SIZE) {
            StorageServerStub.REGISTRIES.remove();
            throw new IllegalArgumentException("Invalid crafting grid slot: " + slot);
        }
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        StorageServerStub.CraftingTarget target = StorageServerStub.resolveCraftingTarget(player, sourcePos);
        CraftingStorage crafting = target.read();
        ItemStack carried = player.hasInfiniteMaterials() ? clientCarried : player.containerMenu.getCarried();
        ItemStack current = crafting.craftingInput().get(slot);
        // 指针为空：取出②槽（左键整堆 / 右键半堆）
        if (carried.isEmpty()) {
            if (current.isEmpty()) {
                return new InteractionResult(ItemStack.EMPTY, false);
            }
            int amount = button == 0 ? current.getCount() : Math.ceilDiv(current.getCount(), 2);
            ItemStack taken = current.copyWithCount(amount);
            ItemStack rest = current.copy();
            rest.shrink(amount);
            target.write(crafting.withCraftingSlot(slot, rest.isEmpty() ? ItemStack.EMPTY : rest));
            player.containerMenu.setCarried(taken);
            player.containerMenu.broadcastChanges();
            return new InteractionResult(taken, true);
        }
        // 空槽 / 同种：堆叠（左键放全部、右键放 1 个，不超过最大堆叠）
        if (current.isEmpty() || ItemStack.isSameItemSameComponents(current, carried)) {
            int space = current.isEmpty() ? carried.getCount()
                : Math.min(carried.getCount(), current.getMaxStackSize() - current.getCount());
            int place = button == 0 ? space : Math.min(1, space);
            if (place <= 0) {
                return new InteractionResult(carried, false);
            }
            ItemStack newCurrent = current.copy();
            if (current.isEmpty()) {
                newCurrent = carried.copyWithCount(place);
            } else {
                newCurrent.grow(place);
            }
            target.write(crafting.withCraftingSlot(slot, newCurrent));
            ItemStack newCarried = carried.copy();
            newCarried.shrink(place);
            player.containerMenu.setCarried(newCarried);
            player.containerMenu.broadcastChanges();
            return new InteractionResult(newCarried, true);
        }
        // 异种：整个交换
        target.write(crafting.withCraftingSlot(slot, carried.copy()));
        player.containerMenu.setCarried(current);
        player.containerMenu.broadcastChanges();
        return new InteractionResult(current, true);
    }

    /** ① 当前输入对应的切石机候选配方结果列表（用于配方选择面板）。 */
    @CallableParam(clazz = StorageServerStub.class, field = "ITEM_STACK_LIST_STREAM_CODEC")
    @RemoteCallable(validator = StorageAccessValidator.class)
    public static List<ItemStack> craftingStonecutterRecipes(UUID playerId, long sourcePos) {
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        ItemStack input = StorageServerStub.resolveCraftingTarget(player, sourcePos).read().stonecutterInput();
        if (input.isEmpty()) {
            return List.of();
        }
        return player.level().getRecipeManager()
            .getRecipesFor(RecipeType.STONECUTTING, new SingleRecipeInput(input), player.level())
            .stream()
            .map(holder -> holder.value().getResultItem(player.level().registryAccess()))
            .toList();
    }

    /** 设置① 的切石机选中配方索引。 */
    @RemoteCallable(validator = StorageAccessValidator.class)
    public static void craftingSelect(UUID playerId, long sourcePos, int index) {
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        StorageServerStub.CraftingTarget target = StorageServerStub.resolveCraftingTarget(player, sourcePos);
        target.write(target.read().withStonecutterSelected(index));
    }

    /** 记录上次关闭界面时是否为合成模式（关闭界面时调用）。 */
    @RemoteCallable(validator = StorageAccessValidator.class)
    public static void craftingSetLastOpened(UUID playerId, long sourcePos, boolean opened) {
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        StorageServerStub.CraftingTarget target = StorageServerStub.resolveCraftingTarget(player, sourcePos);
        target.write(target.read().withLastOpened(opened));
    }

    /**
     * 拖拽分配到 ①/② 输入槽与玩家背包槽（与原版物品栏拖拽一致，所有目标视为一组）。
     * 左键：把指针物品 floor 均分到各槽（余数留在指针）；右键：每槽放 1 个。
     * {@code craftingSlots} 为 ①/② 槽（0 为①，1~9 为②），
     * {@code inventorySlots} 为玩家背包在 containerMenu 中的槽位号。
     */
    @RemoteCallable(validator = StorageAccessValidator.class)
    public static InteractionResult craftingQuickCraft(
        UUID playerId,
        long sourcePos,
        int button,
        @CallableParam(clazz = StorageServerStub.class, field = "ORDER_STREAM_CODEC") IntList craftingSlots,
        @CallableParam(clazz = StorageServerStub.class, field = "ORDER_STREAM_CODEC") IntList inventorySlots,
        @CallableParam(clazz = ItemStack.class, field = "OPTIONAL_STREAM_CODEC") ItemStack clientCarried
    ) {
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        final StorageServerStub.CraftingTarget target = StorageServerStub.resolveCraftingTarget(player, sourcePos);
        ItemStack carried = player.hasInfiniteMaterials() ? clientCarried : player.containerMenu.getCarried();
        if (button != 0 && button != 1 && button != 2) {
            return new InteractionResult(carried, false);
        }
        if (carried.isEmpty() || (craftingSlots.isEmpty() && inventorySlots.isEmpty())) {
            return new InteractionResult(carried, false);
        }
        // 收集有效目标槽（去重、越界丢弃）
        List<Integer> targets = collectTargets(craftingSlots, inventorySlots, player);
        if (targets.isEmpty()) {
            return new InteractionResult(carried, false);
        }
        // 与原版一致：左键 floor 均分（余数留指针），右键每槽 1 个，中键每槽放满
        int perSlot = switch (button) {
            case 0 -> Math.floorDiv(carried.getCount(), targets.size());
            case 1 -> 1;
            default -> carried.getMaxStackSize();
        };
        CraftingStorage crafting = target.read();
        ItemStack remaining = carried.copy();
        boolean changed = false;
        for (int targetSlot : targets) {
            if (remaining.isEmpty()) {
                break;
            }
            int amount = Math.min(perSlot, remaining.getCount());
            if (amount <= 0) {
                continue;
            }
            if (targetSlot < 10) {
                // ①/② 输入槽
                ItemStack current = targetSlot == 0
                    ? crafting.stonecutterInput()
                    : crafting.craftingInput().get(targetSlot - 1);
                // ① 仅接受切石机配方输入；异种槽跳过（拖拽不交换）
                if (targetSlot == 0) {
                    List<RecipeHolder<StonecutterRecipe>> recipes = player.level().getRecipeManager()
                        .getRecipesFor(RecipeType.STONECUTTING, new SingleRecipeInput(remaining), player.level());
                    if (recipes.isEmpty()) {
                        continue;
                    }
                }
                if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, remaining)) {
                    continue;
                }
                int space = current.isEmpty() ? amount
                    : Math.min(amount, current.getMaxStackSize() - current.getCount());
                if (space <= 0) {
                    continue;
                }
                ItemStack newCurrent = current.isEmpty()
                    ? remaining.copyWithCount(space)
                    : current.copyWithCount(current.getCount() + space);
                if (targetSlot == 0) {
                    crafting = crafting.withStonecutterInput(newCurrent);
                } else {
                    crafting = crafting.withCraftingSlot(targetSlot - 1, newCurrent);
                }
                remaining.shrink(space);
                changed = true;
            } else {
                // 玩家背包槽（inventory index = targetSlot - 10）
                int invIndex = targetSlot - 10;
                ItemStack existing = player.getInventory().getItem(invIndex);
                // 与原版 canItemQuickReplace 一致：槽空或同种可堆叠才放入
                if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, remaining)) {
                    continue;
                }
                int currentCount = existing.getCount();
                int maxCount = remaining.getMaxStackSize();
                int space = Math.min(amount, maxCount - currentCount);
                if (space <= 0) {
                    continue;
                }
                player.getInventory().setItem(
                    invIndex,
                    existing.isEmpty()
                        ? remaining.copyWithCount(space)
                        : existing.copyWithCount(currentCount + space)
                );
                remaining.shrink(space);
                changed = true;
            }
        }
        if (!changed) {
            return new InteractionResult(carried, false);
        }
        target.write(crafting);
        player.getInventory().setChanged();
        player.containerMenu.setCarried(remaining);
        player.containerMenu.broadcastChanges();
        return new InteractionResult(remaining, true);
    }

    private static List<Integer> collectTargets(IntList craftingSlots, IntList inventorySlots, ServerPlayer player) {
        List<Integer> targets = new ArrayList<>();
        for (int slot : craftingSlots) {
            if (slot >= 0 && slot < 10 && !targets.contains(slot)) {
                targets.add(slot);
            }
        }
        // 背包槽：仅接受 inventory index 0~35 的背包槽
        for (int invIndex : inventorySlots) {
            if (invIndex < 0 || invIndex >= player.getInventory().items.size()) {
                continue;
            }
            if (!targets.contains(invIndex + 10)) {
                targets.add(invIndex + 10);
            }
        }
        return targets;
    }

    /**
     * 输入槽 Shift 点击：把 ①/② 槽内物品移出——先放入玩家背包（合并已有堆叠/空格），
     * 放不下再放入仓储（仅世界存储场景），仍放不下则留在槽内。绝不放到指针。
     */
    @RemoteCallable(validator = StorageAccessValidator.class)
    public static boolean craftingQuickMoveOut(UUID playerId, long sourcePos, int slot) {
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        if (slot < 0 || slot >= 10) {
            StorageServerStub.REGISTRIES.remove();
            return false;
        }
        StorageServerStub.CraftingTarget target = StorageServerStub.resolveCraftingTarget(player, sourcePos);
        CraftingStorage crafting = target.read();
        ItemStack current = slot == 0
            ? crafting.stonecutterInput()
            : crafting.craftingInput().get(slot - 1);
        if (current.isEmpty()) {
            return false;
        }
        ItemStack remaining = current.copy();
        // 1. 放入玩家背包：先合并同种堆叠，再填入空格
        for (int i = 0; i < player.getInventory().items.size() && !remaining.isEmpty(); i++) {
            ItemStack existing = player.getInventory().getItem(i);
            if (existing.isEmpty() || !existing.isStackable()
                || !ItemStack.isSameItemSameComponents(existing, remaining)) {
                continue;
            }
            int space = Math.min(remaining.getCount(), existing.getMaxStackSize() - existing.getCount());
            if (space <= 0) {
                continue;
            }
            existing.grow(space);
            remaining.shrink(space);
        }
        for (int i = 0; i < player.getInventory().items.size() && !remaining.isEmpty(); i++) {
            if (!player.getInventory().getItem(i).isEmpty()) {
                continue;
            }
            int take = Math.min(remaining.getCount(), remaining.getMaxStackSize());
            player.getInventory().setItem(i, remaining.copyWithCount(take));
            remaining.shrink(take);
        }
        // 2. 剩余放入仓储（仅世界存储场景；终端场景无仓储可放）
        if (!remaining.isEmpty() && target.view() != null) {
            int inserted = target.view().insert(remaining.copyWithCount(1), remaining.getCount());
            if (inserted > 0) {
                remaining.shrink(inserted);
            }
        }
        // 3. 剩余留在槽内
        boolean changed = remaining.getCount() != current.getCount();
        if (changed) {
            ItemStack rest = remaining.isEmpty() ? ItemStack.EMPTY : remaining;
            crafting = slot == 0
                ? crafting.withStonecutterInput(rest)
                : crafting.withCraftingSlot(slot - 1, rest);
            target.write(crafting);
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        return changed;
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static InteractionResult craftingPickupAll(
        UUID playerId,
        long sourcePos,
        int slot,
        @CallableParam(clazz = ItemStack.class, field = "OPTIONAL_STREAM_CODEC") ItemStack clientCarried
    ) {
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        StorageServerStub.CraftingTarget target = StorageServerStub.resolveCraftingTarget(player, sourcePos);
        CraftingStorage crafting = target.read();
        if (slot < 0 || slot >= 10) {
            return new InteractionResult(clientCarried, false);
        }
        ItemStack current = slot == 0
            ? crafting.stonecutterInput()
            : crafting.craftingInput().get(slot - 1);
        // 指针样本：客户端上报优先；客户端快照可能过期（首次点击的异步交互刚完成），
        // 此时以服务端当前指针为准
        ItemStack carried = !clientCarried.isEmpty()
            ? clientCarried.copy()
            : player.containerMenu.getCarried().copy();
        // 指针已有异种物品：拒绝（与双击收集语义不符）
        if (!carried.isEmpty() && !current.isEmpty()
            && !ItemStack.isSameItemSameComponents(carried, current)) {
            return new InteractionResult(clientCarried, false);
        }
        boolean changed = false;
        // 目标槽：整堆拿起并入指针
        if (!current.isEmpty()) {
            int take = Math.min(current.getCount(), carried.getMaxStackSize() - carried.getCount());
            if (take > 0) {
                if (carried.isEmpty()) {
                    carried = current.copyWithCount(take);
                } else {
                    carried.grow(take);
                }
                if (take >= current.getCount()) {
                    current = ItemStack.EMPTY;
                } else {
                    current.shrink(take);
                }
                if (slot == 0) {
                    crafting = crafting.withStonecutterInput(current);
                } else {
                    crafting = crafting.withCraftingSlot(slot - 1, current);
                }
                changed = true;
            }
        }
        // 从其它 ①② 输入槽收集同种物品（① 及其余 ② 槽）
        for (int other = 0; other < 10 && carried.getCount() < carried.getMaxStackSize(); other++) {
            if (other == slot) {
                continue;
            }
            ItemStack otherStack = other == 0
                ? crafting.stonecutterInput()
                : crafting.craftingInput().get(other - 1);
            if (otherStack.isEmpty() || !ItemStack.isSameItemSameComponents(otherStack, carried)) {
                continue;
            }
            int take = Math.min(otherStack.getCount(), carried.getMaxStackSize() - carried.getCount());
            ItemStack shrunk = otherStack.copy();
            shrunk.shrink(take);
            if (other == 0) {
                crafting = crafting.withStonecutterInput(shrunk.isEmpty() ? ItemStack.EMPTY : shrunk);
            } else {
                crafting = crafting.withCraftingSlot(other - 1, shrunk.isEmpty() ? ItemStack.EMPTY : shrunk);
            }
            carried.grow(take);
            changed = true;
        }
        // 从玩家背包收集同种物品（上限 maxStackSize）
        for (int i = 0; i < player.getInventory().items.size()
            && carried.getCount() < carried.getMaxStackSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, carried)) {
                continue;
            }
            int take = Math.min(stack.getCount(), carried.getMaxStackSize() - carried.getCount());
            if (take <= 0) {
                continue;
            }
            stack.shrink(take);
            carried.grow(take);
            changed = true;
        }
        if (!changed) {
            return new InteractionResult(clientCarried, false);
        }
        target.write(crafting);
        player.getInventory().setChanged();
        player.containerMenu.setCarried(carried);
        player.containerMenu.broadcastChanges();
        return new InteractionResult(carried, true);
    }

    /**
     * 背包槽双击补充：把 ①/② 输入槽中与指针同种的物品收集到指针
     * （背包 PICKUP_ALL 只收集背包，此调用补充输入槽部分，上限为最大堆叠）。
     */
    @RemoteCallable(validator = StorageAccessValidator.class)
    public static InteractionResult craftingPickupIntoCarried(
        UUID playerId,
        long sourcePos,
        @CallableParam(clazz = ItemStack.class, field = "OPTIONAL_STREAM_CODEC") ItemStack clientCarried
    ) {
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        StorageServerStub.CraftingTarget target = StorageServerStub.resolveCraftingTarget(player, sourcePos);
        CraftingStorage crafting = target.read();
        if (clientCarried.isEmpty()) {
            return new InteractionResult(clientCarried, false);
        }
        ItemStack carried = clientCarried.copy();
        boolean changed = false;
        for (int other = 0; other < 10 && carried.getCount() < carried.getMaxStackSize(); other++) {
            ItemStack otherStack = other == 0
                ? crafting.stonecutterInput()
                : crafting.craftingInput().get(other - 1);
            if (otherStack.isEmpty() || !ItemStack.isSameItemSameComponents(otherStack, carried)) {
                continue;
            }
            int take = Math.min(otherStack.getCount(), carried.getMaxStackSize() - carried.getCount());
            ItemStack shrunk = otherStack.copy();
            shrunk.shrink(take);
            if (other == 0) {
                crafting = crafting.withStonecutterInput(shrunk.isEmpty() ? ItemStack.EMPTY : shrunk);
            } else {
                crafting = crafting.withCraftingSlot(other - 1, shrunk.isEmpty() ? ItemStack.EMPTY : shrunk);
            }
            carried.grow(take);
            changed = true;
        }
        if (!changed) {
            return new InteractionResult(clientCarried, false);
        }
        target.write(crafting);
        player.containerMenu.setCarried(carried);
        player.containerMenu.broadcastChanges();
        return new InteractionResult(carried, true);
    }

    /**
     * 取③/④ 配方结果：消耗输入并放到指针。stonecutter=true 取③（消耗①），false 取④（消耗②）。
     * 指针已有同种且能放下的物品时合并取出；异种或放不下时不消耗输入。
     */
    @RemoteCallable(validator = StorageAccessValidator.class)
    public static InteractionResult craftingTakeResult(UUID playerId, long sourcePos, boolean stonecutter, boolean shift) {
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        StorageServerStub.CraftingTarget target = StorageServerStub.resolveCraftingTarget(player, sourcePos);
        CraftingStorage crafting = target.read();
        ItemStack carried = player.containerMenu.getCarried();
        // 先计算产物（不写入），指针不兼容时直接拒绝，避免误消耗输入
        ItemStack result = StorageServerStub.assembleCraftingResult(player, crafting, stonecutter);
        if (result == null || result.isEmpty()) {
            return new InteractionResult(carried, false);
        }
        if (shift) {
            StorageServerStub.placeCraftingResult(player, target, result);
            player.containerMenu.broadcastChanges();
            return new InteractionResult(player.containerMenu.getCarried(), true);
        }
        // 指针有物：仅当与产物同种且能放下时合并取出；否则拒绝（不消耗输入）
        if (!carried.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(carried, result)
                || carried.getCount() + result.getCount() > carried.getMaxStackSize()) {
                return new InteractionResult(carried, false);
            }
            ItemStack merged = carried.copy();
            merged.grow(result.getCount());
            StorageServerStub.consumeCraftingInput(target, crafting, stonecutter);
            player.containerMenu.setCarried(merged);
            player.containerMenu.broadcastChanges();
            return new InteractionResult(merged, true);
        }
        StorageServerStub.consumeCraftingInput(target, crafting, stonecutter);
        player.containerMenu.setCarried(result);
        player.containerMenu.broadcastChanges();
        return new InteractionResult(result, true);
    }

    /**
     * 按住 Shift 点击③/④ 结果槽：连续合成直到材料不足或产物无处可放。
     * 产物依次放入指针（同种合并）→ 背包（同种堆叠/空槽）→ 仓储。
     *
     * <p>产物完全放不下（指针异种且背包 / 仓储均无空间）时不消耗输入、不丢弃
     * 产物，立即截断——与原版「指针异种时拒绝取出」语义一致，避免凭空产出物品。</p>
     *
     * <p>产物只被部分放入（仓储剩余空间不足，部分插入后其余丢弃）时消耗输入后
     * 立即截断——继续循环只会反复「合成→部分放入→丢弃」，浪费材料且产出不可控。</p>
     *
     * <p>存在「不消耗型输入」的配方（如催化剂 / 模具类模组配方，或剩余物与输入
     * 完全相同的配方）时，消耗输入不会改变合成网格，若不终止将无限产出导致
     * 服务端 RPC 线程死循环；因此在消耗前后比对网格，无变化立即终止。</p>
     *
     * <p>单次 RPC 最多合成 {@link #CRAFTING_TAKE_ALL_CHUNK} 次；未耗尽材料时返回
     * {@code done=false}，由客户端循环调用直至 {@code done=true}，避免一次性阻塞
     * 服务端线程过久。</p>
     */
    @RemoteCallable(validator = StorageAccessValidator.class)
    public static TakeAllResult craftingTakeAll(UUID playerId, long sourcePos, boolean stonecutter) {
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        StorageServerStub.CraftingTarget target = StorageServerStub.resolveCraftingTarget(player, sourcePos);
        boolean any = false;
        int iterations = 0;
        while (iterations++ < StorageServerStub.CRAFTING_TAKE_ALL_CHUNK) {
            CraftingStorage crafting = target.read();
            ItemStack result = StorageServerStub.assembleCraftingResult(player, crafting, stonecutter);
            if (result == null || result.isEmpty()) {
                player.containerMenu.broadcastChanges();
                return new TakeAllResult(player.containerMenu.getCarried(), any, true);
            }
            PlaceResult place = StorageServerStub.placeCraftingResult(player, target, result);
            if (place == PlaceResult.NONE) {
                // 产物完全放不下（如指针异种且背包 / 仓储均无空间）：不消耗输入、
                // 不丢弃产物，立即截断——与原版「指针异种时拒绝取出」语义一致，
                // 避免凭空产出物品
                player.containerMenu.broadcastChanges();
                return new TakeAllResult(player.containerMenu.getCarried(), any, true);
            }
            if (place == PlaceResult.PARTIAL) {
                // 产物部分放入仓储（其余已在 placeCraftingResult 内丢弃）：消耗
                // 输入后截断，避免重复「合成→部分放入→丢弃」浪费材料且产出不可控
                StorageServerStub.consumeCraftingInput(target, crafting, stonecutter);
                player.containerMenu.broadcastChanges();
                return new TakeAllResult(player.containerMenu.getCarried(), true, true);
            }
            any = true;
            // 消耗输入后网格无变化（不消耗型配方）→ 继续循环只会无限产出相同产物，立即终止
            if (!StorageServerStub.consumeCraftingInput(target, crafting, stonecutter)) {
                break;
            }
        }
        // 达到分块上限：材料尚未耗尽，由客户端继续调用（done=false）
        player.containerMenu.broadcastChanges();
        return new TakeAllResult(player.containerMenu.getCarried(), any, false);
    }

    /** 计算③/④ 当前配方产物（不消耗输入）；配方无效或产物为空返回 null。 */
    @Nullable
    private static ItemStack assembleCraftingResult(
        ServerPlayer player,
        CraftingStorage crafting,
        boolean stonecutter
    ) {
        if (stonecutter) {
            ItemStack input = crafting.stonecutterInput();
            if (input.isEmpty()) {
                return null;
            }
            List<RecipeHolder<StonecutterRecipe>> recipes = player.level().getRecipeManager()
                .getRecipesFor(RecipeType.STONECUTTING, new SingleRecipeInput(input), player.level());
            if (recipes.isEmpty() || crafting.stonecutterSelected() < 0
                || crafting.stonecutterSelected() >= recipes.size()) {
                return null;
            }
            return recipes.get(crafting.stonecutterSelected()).value()
                .assemble(new SingleRecipeInput(input), player.level().registryAccess());
        }
        CraftingInput input = CraftingInput.of(3, 3, crafting.craftingInput());
        List<RecipeHolder<CraftingRecipe>> recipes = player.level().getRecipeManager()
            .getRecipesFor(RecipeType.CRAFTING, input, player.level());
        if (recipes.isEmpty()) {
            return null;
        }
        return recipes.getFirst().value().assemble(input, player.level().registryAccess());
    }

    /**
     * 把一次合成产物放入指针（同种合并）→ 背包（同种堆叠/空槽）→ 仓储。
     *
     * @return {@link PlaceResult#FULL} 全部放入；{@link PlaceResult#PARTIAL} 部分放入
     *         仓储、剩余丢弃；{@link PlaceResult#NONE} 完全放不下（不丢弃，由调用方处理）
     */
    private static PlaceResult placeCraftingResult(
        ServerPlayer player,
        StorageServerStub.CraftingTarget target,
        ItemStack result
    ) {
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) {
                inventory.setItem(i, result);
                return PlaceResult.FULL;
            }
            if (ItemStack.isSameItemSameComponents(stack, result)
                && stack.getCount() + result.getCount() <= stack.getMaxStackSize()) {
                stack.grow(result.getCount());
                return PlaceResult.FULL;
            }
        }
        ItemStack carried = player.containerMenu.getCarried();
        if (carried.isEmpty()) {
            player.containerMenu.setCarried(result);
            return PlaceResult.FULL;
        }
        if (ItemStack.isSameItemSameComponents(carried, result)
            && carried.getCount() + result.getCount() <= carried.getMaxStackSize()) {
            ItemStack merged = carried.copy();
            merged.grow(result.getCount());
            player.containerMenu.setCarried(merged);
            return PlaceResult.FULL;
        }
        StorageView view = target.view();
        if (view != null) {
            int inserted = view.insert(result.copyWithCount(1), result.getCount());
            if (inserted == result.getCount()) {
                return PlaceResult.FULL;
            }
            if (inserted > 0) {
                ItemStack rest = result.copy();
                rest.shrink(inserted);
                player.drop(rest, false);
                return PlaceResult.PARTIAL;
            }
        }
        return PlaceResult.NONE;
    }

    /** {@link #placeCraftingResult} 的放置结果分类。 */
    private enum PlaceResult {
        /** 产物全部放入（指针 / 背包 / 仓储）。 */
        FULL,
        /** 产物部分放入仓储，其余丢弃。 */
        PARTIAL,
        /** 产物完全放不下。 */
        NONE
    }

    /**
     * 消耗③/④ 对应的输入并写入合成数据。
     *
     * @return 合成网格是否发生变化；不消耗型配方（剩余物与输入相同，如催化剂 /
     *         模具）返回 {@code false}，调用方（如 {@link #craftingTakeAll}）应据此终止循环
     */
    private static boolean consumeCraftingInput(
        StorageServerStub.CraftingTarget target,
        CraftingStorage crafting,
        boolean stonecutter
    ) {
        if (stonecutter) {
            ItemStack input = crafting.stonecutterInput();
            if (input.isEmpty()) {
                return false;
            }
            ItemStack shrunk = input.copy();
            shrunk.shrink(1);
            target.write(crafting.withStonecutterInput(shrunk.isEmpty() ? ItemStack.EMPTY : shrunk));
            return true;
        }
        // 每次合成每槽只消耗 1 个（原版合成语义），剩余物品（如桶）保留在槽内
        CraftingInput input = CraftingInput.of(3, 3, crafting.craftingInput());
        List<ItemStack> remaining = target.player().level().getRecipeManager()
            .getRemainingItemsFor(RecipeType.CRAFTING, input, target.player().level());
        List<ItemStack> grid = new ArrayList<>(crafting.craftingInput());
        boolean changed = false;
        for (int i = 0; i < grid.size(); i++) {
            ItemStack current = grid.get(i);
            if (current.isEmpty()) {
                continue;
            }
            ItemStack left = i < remaining.size() ? remaining.get(i) : ItemStack.EMPTY;
            if (left.isEmpty()) {
                ItemStack shrunk = current.copy();
                shrunk.shrink(1);
                grid.set(i, shrunk.isEmpty() ? ItemStack.EMPTY : shrunk);
                changed = true;
            } else {
                // 有剩余物（桶/碗等）：原版中该槽输出剩余物而非原物，剩余物不入存储。
                // 若剩余物与原输入完全相同（催化剂/模具等不消耗型配方），网格不变化，
                // 需要据此判定消耗未发生，避免调用方无限循环。
                ItemStack replaced = left.copy();
                if (!ItemStack.isSameItemSameComponents(replaced, current)
                    || replaced.getCount() != current.getCount()) {
                    changed = true;
                }
                grid.set(i, replaced);
            }
        }
        if (changed) {
            target.write(crafting.withCraftingInput(grid));
        }
        return changed;
    }

    /**
     * 清空合成格（① 切石机输入 + ② 合成 9 宫格）：物品先放回玩家背包，
     * 背包放不下时放回存储站，返回清空后的合成数据。
     */
    private static CraftingStorage clearCrafting(
        StorageServerStub.CraftingTarget target,
        CraftingStorage crafting,
        Inventory inventory
    ) {
        ItemStack stonecutterInput = crafting.stonecutterInput();
        List<ItemStack> grid = crafting.craftingInput();
        boolean hasAny = !stonecutterInput.isEmpty()
            || grid.stream().anyMatch(stack -> !stack.isEmpty());
        if (!hasAny) {
            return crafting;
        }
        StorageView view = target.view();
        if (!stonecutterInput.isEmpty()) {
            StorageServerStub.returnToInventoryOrStorage(inventory, view, stonecutterInput);
        }
        for (ItemStack stack : grid) {
            if (!stack.isEmpty()) {
                StorageServerStub.returnToInventoryOrStorage(inventory, view, stack);
            }
        }
        List<ItemStack> emptyGrid = java.util.Collections.nCopies(CraftingStorage.CRAFTING_GRID_SIZE, ItemStack.EMPTY);
        CraftingStorage cleared = crafting
            .withStonecutterInput(ItemStack.EMPTY)
            .withCraftingInput(emptyGrid);
        target.write(cleared);
        return cleared;
    }

    /** 把物品放回玩家背包，背包放不下时放回存储站。 */
    private static void returnToInventoryOrStorage(
        Inventory inventory,
        @Nullable StorageView view,
        ItemStack stack
    ) {
        if (stack.isEmpty()) {
            return;
        }
        int remaining = StorageServerStub.giveBackToInventory(inventory, stack, stack.getCount());
        if (remaining > 0 && view != null) {
            view.insert(stack.copyWithCount(remaining), remaining);
        }
    }

    /**
     * JEI 转移：把配方输入放入 ①/② 输入槽，材料从玩家背包扣取（与 JEI 转移语义一致）。
     * stonecutter=true 时只放 ①（inputs 的第一个非空物品）；false 时把 inputs 按 9 宫格
     * 顺序放入 ②（数量不足的槽放背包中已有的量，没有则留空）。
     * 转移不触碰指针（carried）。
     */
    @RemoteCallable(validator = StorageAccessValidator.class)
    public static boolean craftingTransfer(
        UUID playerId,
        long sourcePos,
        boolean stonecutter,
        boolean maxTransfer,
        @CallableParam(clazz = StorageServerStub.class, field = "ITEM_STACK_LIST_STREAM_CODEC") List<ItemStack> inputs,
        @CallableParam(clazz = ItemStack.class, field = "OPTIONAL_STREAM_CODEC") ItemStack stonecutterResult,
        @CallableParam(clazz = StorageServerStub.class, field = "ORDER_STREAM_CODEC") IntList requestedCounts
    ) {
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        StorageServerStub.CraftingTarget target = StorageServerStub.resolveCraftingTarget(player, sourcePos);
        CraftingStorage crafting = target.read();
        Inventory inventory = player.getInventory();
        crafting = StorageServerStub.clearCrafting(target, crafting, inventory);
        if (stonecutter) {
            // ①：把背包内所有同种物品转移进①（受上限约束），不只放一个
            if (!inputs.isEmpty() && !inputs.getFirst().isEmpty()) {
                ItemStack wanted = inputs.getFirst();
                int maxStack = wanted.getMaxStackSize();
                int currentCount = crafting.stonecutterInput().getCount();
                int moved = 0;
                for (int i = 0; i < inventory.getContainerSize(); i++) {
                    ItemStack stack = inventory.getItem(i);
                    if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, wanted)) {
                        continue;
                    }
                    int take = Math.min(stack.getCount(), maxStack - currentCount - moved);
                    if (take <= 0) {
                        continue;
                    }
                    ItemStack rest = stack.copy();
                    rest.shrink(take);
                    inventory.setItem(i, rest.isEmpty() ? ItemStack.EMPTY : rest);
                    moved += take;
                }
                if (moved > 0) {
                    ItemStack newInput = crafting.stonecutterInput().copy();
                    newInput.grow(moved);
                    target.write(crafting.withStonecutterInput(newInput));
                    inventory.setChanged();
                    player.containerMenu.broadcastChanges();
                    return true;
                }
            }
            return false;
        }
        // ②：按 9 宫格顺序放入（每个槽从背包/存储找对应物品）
        // requestedCounts 由客户端用 JEI 的 RecipeTransferUtil 分配算法算出每格一组应放数量，
        // 服务端严格按份数扣料（不从背包/存储多拿），保证多槽同种材料时各槽平均分配。
        List<ItemStack> grid = new ArrayList<>(crafting.craftingInput());
        boolean changed = false;
        int rounds = maxTransfer ? Integer.MAX_VALUE : 1;
        for (int round = 0; round < rounds; round++) {
            // 预检该轮总需求：材料不足整组时放弃本轮（与 JEI 的 requireCompleteSets 一致，
            // 避免把剩余材料塞进前几个槽破坏均分）
            boolean anySlot = false;
            for (int i = 0; i < 9; i++) {
                ItemStack wanted = i < inputs.size() ? inputs.get(i) : ItemStack.EMPTY;
                if (wanted.isEmpty()) {
                    continue;
                }
                int requested = i < requestedCounts.size() ? requestedCounts.getInt(i) : 0;
                if (requested <= 0) {
                    continue;
                }
                ItemStack current = grid.get(i);
                if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, wanted)) {
                    continue;
                }
                int slotMax = current.isEmpty()
                    ? wanted.getMaxStackSize()
                    : Math.min(wanted.getMaxStackSize(), current.getMaxStackSize());
                if (current.getCount() + requested > slotMax) {
                    continue;
                }
                anySlot = true;
            }
            if (!anySlot) {
                break;
            }
            if (!StorageServerStub.hasEnoughMaterial(inventory, target.view(), grid, inputs, requestedCounts)) {
                break;
            }
            boolean anyPlaced = false;
            for (int i = 0; i < 9; i++) {
                ItemStack wanted = i < inputs.size() ? inputs.get(i) : ItemStack.EMPTY;
                if (wanted.isEmpty()) {
                    continue;
                }
                int requested = i < requestedCounts.size() ? requestedCounts.getInt(i) : 0;
                if (requested <= 0) {
                    continue;
                }
                ItemStack current = grid.get(i);
                if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, wanted)) {
                    continue;
                }
                int slotMax = current.isEmpty()
                    ? wanted.getMaxStackSize()
                    : Math.min(wanted.getMaxStackSize(), current.getMaxStackSize());
                if (current.getCount() + requested > slotMax) {
                    continue;
                }
                int placed = StorageServerStub.transferMaterialExact(
                    target, inventory, wanted, current, current.getCount() + requested, player
                );
                if (placed > 0) {
                    if (current.isEmpty()) {
                        current = wanted.copyWithCount(placed);
                    } else {
                        current = current.copy();
                        current.grow(placed);
                    }
                    grid.set(i, current);
                    changed = true;
                    anyPlaced = true;
                }
            }
            if (!anyPlaced) {
                break;
            }
        }
        if (changed) {
            target.write(crafting.withCraftingInput(grid));
        }
        return changed;
    }

    /**
     * 本轮仍可补料的各槽所需物品是否都能凑齐：按物品（同种同组件）分组统计
     * 本轮总需求，与「背包 + 存储」中该物品总可用量对比，任一物品不足则返回 false。
     */
    private static boolean hasEnoughMaterial(
        Inventory inventory,
        @Nullable StorageView view,
        List<ItemStack> grid,
        List<ItemStack> inputs,
        IntList requestedCounts
    ) {
        List<ItemStack> neededKeys = new ArrayList<>();
        List<Integer> neededCounts = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            ItemStack wanted = i < inputs.size() ? inputs.get(i) : ItemStack.EMPTY;
            if (wanted.isEmpty()) {
                continue;
            }
            int requested = i < requestedCounts.size() ? requestedCounts.getInt(i) : 0;
            if (requested <= 0) {
                continue;
            }
            ItemStack current = grid.get(i);
            if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, wanted)) {
                continue;
            }
            int slotMax = current.isEmpty()
                ? wanted.getMaxStackSize()
                : Math.min(wanted.getMaxStackSize(), current.getMaxStackSize());
            if (current.getCount() + requested > slotMax) {
                continue;
            }
            boolean merged = false;
            for (int k = 0; k < neededKeys.size(); k++) {
                if (ItemStack.isSameItemSameComponents(neededKeys.get(k), wanted)) {
                    neededCounts.set(k, neededCounts.get(k) + requested);
                    merged = true;
                    break;
                }
            }
            if (!merged) {
                neededKeys.add(wanted.copy());
                neededCounts.add(requested);
            }
        }
        for (int k = 0; k < neededKeys.size(); k++) {
            ItemStack wanted = neededKeys.get(k);
            long available = 0;
            for (int index = 0; index < inventory.getContainerSize(); index++) {
                ItemStack stack = inventory.getItem(index);
                if (ItemStack.isSameItemSameComponents(stack, wanted)) {
                    available += stack.getCount();
                }
            }
            if (view != null) {
                for (int index = 0; index < view.size(); index++) {
                    if (ItemStack.isSameItemSameComponents(view.resource(index), wanted)) {
                        available += view.amount(index);
                    }
                }
            }
            if (available < neededCounts.get(k)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 把 wanted 放入目标槽一组（requestedCount 个，含槽内已有同种物品）：
     * 从背包先取，不足再从存储补足。材料不足一组时回滚已取物品并返回 0
     * （与 JEI 的 requireCompleteSets 语义一致，避免产生部分组破坏均分）。
     */
    private static int transferMaterialExact(
        StorageServerStub.CraftingTarget target,
        Inventory inventory,
        ItemStack wanted,
        ItemStack current,
        int requestedCount,
        ServerPlayer player
    ) {
        if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, wanted)) {
            return 0;
        }
        int slotMax = current.isEmpty()
            ? wanted.getMaxStackSize()
            : Math.min(wanted.getMaxStackSize(), current.getMaxStackSize());
        if (requestedCount > slotMax) {
            requestedCount = slotMax;
        }
        int needed = requestedCount - current.getCount();
        if (needed <= 0) {
            return 0;
        }
        int moved = 0;
        int fromStorage = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, wanted)) {
                continue;
            }
            int take = Math.min(stack.getCount(), needed - moved);
            if (take <= 0) {
                continue;
            }
            ItemStack rest = stack.copy();
            rest.shrink(take);
            inventory.setItem(i, rest.isEmpty() ? ItemStack.EMPTY : rest);
            moved += take;
            if (moved >= needed) {
                break;
            }
        }
        StorageView view = target.view();
        if (moved < needed && view != null) {
            int space = needed - moved;
            for (int index = 0; index < view.size() && space > 0; index++) {
                if (
                    view.amount(index) <= 0
                    || !ItemStack.isSameItemSameComponents(view.resource(index), wanted)
                ) {
                    continue;
                }
                int extracted = view.extract(index, (int) Math.min(space, view.amount(index)));
                if (extracted <= 0) {
                    continue;
                }
                moved += extracted;
                fromStorage += extracted;
                space -= extracted;
            }
        }
        if (moved < needed) {
            // 材料不足一组：回滚已取物品（背包部分放回背包，存储部分放回存储）
            int inventoryPart = moved - fromStorage;
            if (inventoryPart > 0) {
                StorageServerStub.giveBackToInventory(inventory, wanted, inventoryPart);
            }
            if (fromStorage > 0 && view != null) {
                view.insert(wanted.copyWithCount(fromStorage), fromStorage);
            }
            return 0;
        }
        return moved;
    }

    /** 把 count 个 wanted 放回玩家背包，返回未能放入的剩余数量。 */
    private static int giveBackToInventory(Inventory inventory, ItemStack wanted, int count) {
        if (count <= 0) {
            return 0;
        }
        ItemStack toReturn = wanted.copy();
        toReturn.setCount(count);
        for (int i = 0; i < inventory.getContainerSize() && !toReturn.isEmpty(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) {
                int chunk = Math.min(toReturn.getCount(), wanted.getMaxStackSize());
                inventory.setItem(i, wanted.copyWithCount(chunk));
                toReturn.shrink(chunk);
                continue;
            }
            if (ItemStack.isSameItemSameComponents(stack, wanted)) {
                int space = stack.getMaxStackSize() - stack.getCount();
                int add = Math.min(space, toReturn.getCount());
                if (add > 0) {
                    stack.grow(add);
                    toReturn.shrink(add);
                }
            }
        }
        return toReturn.getCount();
    }

    /**
     * 从玩家背包找与 wanted 同种的物品移动到目标槽（同种堆叠或空槽放入），
     * 背包不足时从目标存储站（若有）提取补足，返回实际放置数量（0 表示没有）。
     */
    private static int transferMaterial(
        StorageServerStub.CraftingTarget target,
        Inventory inventory,
        ItemStack wanted,
        ItemStack current,
        int maxCount,
        ServerPlayer player
    ) {
        int moved = StorageServerStub.transferFromInventory(inventory, wanted, current, maxCount, player);
        if (moved >= maxCount || maxCount <= 0) {
            return moved;
        }
        if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, wanted)) {
            return moved;
        }
        StorageView view = target.view();
        if (view == null) {
            return moved;
        }
        // 背包取完后从存储补足差量（空槽或已有同种均适用）
        int space = maxCount - (current.isEmpty() ? 0 : current.getCount()) - moved;
        for (int index = 0; index < view.size() && space > 0; index++) {
            if (
                view.amount(index) <= 0
                || !ItemStack.isSameItemSameComponents(view.resource(index), wanted)
            ) {
                continue;
            }
            int extracted = view.extract(index, (int) Math.min(space, view.amount(index)));
            if (extracted <= 0) {
                continue;
            }
            moved += extracted;
            space -= extracted;
        }
        return moved;
    }

    /**
     * 从玩家背包找与 wanted 同种的物品，移动到目标槽（同种堆叠或空槽放入），
     * 返回实际放置数量；背包不足时放入背包中已有的量（0 表示没有）。
     */
    private static int transferFromInventory(
        Inventory inventory,
        ItemStack wanted,
        ItemStack current,
        int maxCount,
        ServerPlayer player
    ) {
        if (current.isEmpty()) {
            // 空槽：从背包转移所有同种物品（受 maxCount 上限约束），不只放一个
            int moved = 0;
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                ItemStack stack = inventory.getItem(i);
                if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, wanted)) {
                    continue;
                }
                int take = Math.min(stack.getCount(), maxCount - moved);
                if (take <= 0) {
                    continue;
                }
                ItemStack rest = stack.copy();
                rest.shrink(take);
                inventory.setItem(i, rest.isEmpty() ? ItemStack.EMPTY : rest);
                moved += take;
            }
            return moved;
        }
        // 已有同种：只补足到上限
        if (!ItemStack.isSameItemSameComponents(current, wanted)) {
            return 0;
        }
        int space = maxCount - current.getCount();
        if (space <= 0) {
            return 0;
        }
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, wanted)) {
                continue;
            }
            int take = Math.min(space, stack.getCount());
            if (take <= 0) {
                continue;
            }
            ItemStack rest = stack.copy();
            rest.shrink(take);
            inventory.setItem(i, rest.isEmpty() ? ItemStack.EMPTY : rest);
            return take;
        }
        return 0;
    }

    /** 合成数据读写目标：终端物品的 crafting 组件，或世界主存储的 crafting 字段。 */
    private record CraftingTarget(
        @Nullable ItemStack terminal,
        @Nullable BaseStorage<?> storage,
        @Nullable StorageView view,
        ServerPlayer player
    ) {
        CraftingStorage read() {
            if (this.terminal != null) {
                CraftingStorage crafting = this.terminal.get(ModComponents.CRAFTING);
                return crafting == null ? CraftingStorage.EMPTY : crafting;
            }
            return Objects.requireNonNull(this.storage).getCrafting();
        }

        void write(CraftingStorage crafting) {
            if (this.terminal != null) {
                this.terminal.set(ModComponents.CRAFTING, crafting);
                this.player.getInventory().setChanged();
            } else {
                Objects.requireNonNull(this.storage).setCrafting(crafting);
                Storages.get().setDirty();
            }
            this.player.containerMenu.broadcastChanges();
        }
    }

    /**
     * 解析当前界面对应的合成数据读写目标：终端打开时指向终端物品的 crafting 数据组件，
     * 世界打开时指向主存储的 crafting 字段。一次性消费调用线程的 REGISTRIES。
     */
    private static CraftingTarget resolveCraftingTarget(ServerPlayer player, long sourcePos) {
        UUID playerId = player.getGameProfile().getId();
        RemoteTarget remote = StorageServerStub.REMOTE_STORAGES.getOrDefault(playerId, Map.of()).get(sourcePos);
        if (remote != null) {
            HolderLookup.Provider registries = StorageServerStub.getAndClear();
            ItemStack terminal = StorageServerStub.findTerminalStack(player, remote.kind());
            // 终端场景：合成数据读写目标为终端物品的 crafting 组件，但转移材料
            // 需要从终端连接的目标存储补足——view 复用 getView 的终端目标解析
            StorageView view = StorageServerStub.getView(registries, playerId, sourcePos);
            return new CraftingTarget(
                terminal,
                null,
                view,
                player
            );
        }
        StorageView view = StorageServerStub.getView(StorageServerStub.getAndClear(), playerId, sourcePos);
        return new CraftingTarget(null, view.primary(), view, player);
    }

    /** 在玩家身上查找指定终端类型的物品栈（手持优先，其次物品栏）。 */
    @Nullable
    private static ItemStack findTerminalStack(ServerPlayer player, int kind) {
        Item item = switch (kind) {
            case RemoteTarget.HYPERDIMENSION -> ModItems.HYPERDIMENSION_TERMINAL.asItem();
            case RemoteTarget.LARGE_CRATE -> ModItems.LOCAL_TERMINAL.asItem();
            case RemoteTarget.SHULKER_CONTAINER -> ModItems.SHULKER_TERMINAL.asItem();
            default -> null;
        };
        if (item == null) {
            return null;
        }
        if (player.getMainHandItem().is(item)) {
            return player.getMainHandItem();
        }
        if (player.getOffhandItem().is(item)) {
            return player.getOffhandItem();
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                return stack;
            }
        }
        return null;
    }

    @CallableParam(clazz = StorageServerStub.class, field = "ORDER_STREAM_CODEC")
    @RemoteCallable(validator = StorageAccessValidator.class)
    public static IntList terminalReorder(UUID playerId, long sourcePos, String search) {
        HolderLookup.Provider registries = StorageServerStub.getAndClear();
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        // 本地 / 潜影终端超出连接范围时浮窗不应再能操作：直接返回空排序
        if (!StorageServerStub.sourceReachable(player, sourcePos)) {
            return new IntArrayList();
        }
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
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        if (!StorageServerStub.sourceReachable(player, sourcePos)) {
            StorageServerStub.getAndClear();
            return new InteractionResult(player.containerMenu.getCarried(), false);
        }
        StorageView view = StorageServerStub.getView(StorageServerStub.getAndClear(), playerId, sourcePos);
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
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        if (!StorageServerStub.sourceReachable(player, sourcePos)) {
            StorageServerStub.getAndClear();
            return new InteractionResult(player.containerMenu.getCarried(), false);
        }
        StorageView view = StorageServerStub.getView(StorageServerStub.getAndClear(), playerId, sourcePos);
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
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        if (!StorageServerStub.sourceReachable(player, sourcePos)) {
            StorageServerStub.getAndClear();
            return new InteractionResult(player.containerMenu.getCarried(), false);
        }
        StorageView view = StorageServerStub.getView(StorageServerStub.getAndClear(), playerId, sourcePos);
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
     * JEI 快速合成补库：从玩家持有的全部终端目标（超维绑定 / 本地 / 潜影）取出合成缺少的
     * 物品补入背包。每个目标按其提取时背包的实际缺口计算，总量不会超过需求。
     * 仅当玩家确实持有这些目标对应的终端时生效。
     *
     * @return 实际补入玩家背包的物品及数量（每种物品一份，数量为补入总量），
     *         供客户端在 JEI 转移失败时退回多余材料
     */
    @CallableParam(clazz = StorageServerStub.class, field = "ITEM_STACK_LIST_STREAM_CODEC")
    @RemoteCallable(validator = TerminalAccessValidator.class)
    public static List<ItemStack> terminalWithdrawToInventory(
        UUID playerId,
        @CallableParam(clazz = StorageServerStub.class, field = "UUID_LIST_STREAM_CODEC")
        List<UUID> targetIds,
        @CallableParam(clazz = StorageServerStub.class, field = "ITEM_STACK_LIST_STREAM_CODEC")
        List<ItemStack> needs
    ) {
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        if (targetIds.isEmpty()) {
            return List.of();
        }
        List<ItemStack> withdrawn = new ArrayList<>();
        for (UUID targetId : targetIds) {
            if (!StorageServerStub.terminalTargetReachable(player, targetId)) {
                continue;
            }
            List<BaseStorage<?>> storages = StorageServerStub.terminalStorages(player, targetId);
            if (storages.isEmpty()) {
                continue;
            }
            StorageServerStub.withdrawNeedsFromStorages(player, storages, needs, withdrawn);
        }
        if (!withdrawn.isEmpty()) {
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        }
        return withdrawn;
    }

    /**
     * 从终端目标存储取出排序第一的物品（供创造背包等纯客户端菜单的 BundleLike 取出）。
     * 按触发玩家的 PlayerSetting 排序（SortMode + OrderMode）取第一个。
     *
     * @return carried=取出的物品；changed=false 表示取出失败（无绑定/不可达/存储空），
     *         客户端应放回终端（vanilla fallback 语义）
     */
    @RemoteCallable(validator = CreativeTerminalAccessValidator.class)
    public static InteractionResult terminalExtractFirst(
        UUID playerId,
        UUID targetId,
        int amount,
        @CallableParam(clazz = ItemStack.class, field = "OPTIONAL_STREAM_CODEC") ItemStack terminalStack
    ) {
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        UUID playerUuid = player.getGameProfile().getId();
        // 创造模式指针物品由客户端本地管理，服务端背包/指针可能没有该终端，
        // 以客户端上报的指针终端为准校验持有关系
        if (!StorageServerStub.isBoundTerminal(terminalStack, targetId, playerUuid)) {
            return new InteractionResult(ItemStack.EMPTY, false);
        }
        if (!StorageServerStub.terminalTargetReachable(player, targetId)) {
            return new InteractionResult(ItemStack.EMPTY, false);
        }
        HolderLookup.Provider registries = StorageServerStub.getAndClear();
        StorageView view = new StorageView(StorageServerStub.terminalStorages(player, targetId), List.of());
        if (view.size() <= 0) {
            return new InteractionResult(ItemStack.EMPTY, false);
        }
        PlayerSetting setting = PlayerSettings.getSetting(registries, playerId);
        StorageSetting storage = setting.storage();
        SortOptions options = new SortOptions(storage.getSort(), storage.getOrder());
        IntList order = StorageServerStub.createOrder(view, options, "", setting.listed());
        ItemStack extracted = ItemStack.EMPTY;
        for (int i = 0; i < order.size() && extracted.isEmpty(); i++) {
            int index = order.getInt(i);
            long stackAmount = view.amount(index);
            if (stackAmount <= 0) {
                continue;
            }
            int take = (int) Math.min(amount, stackAmount);
            int got = view.extract(index, take);
            if (got > 0) {
                extracted = view.resource(index).copyWithCount(got);
            }
        }
        if (extracted.isEmpty()) {
            return new InteractionResult(ItemStack.EMPTY, false);
        }
        // 创造模式下客户端指针由本地管理，不能 broadcastChanges——会把服务端
        // carried（空）广播回客户端导致指针被清空；槽位由客户端 RPC 回调写回。
        player.getInventory().setChanged();
        return new InteractionResult(extracted, true);
    }

    /** 把物品放入终端目标存储，返回剩余（放不下的部分，供客户端放回槽位）。 */
    @CallableParam(clazz = ItemStack.class, field = "OPTIONAL_STREAM_CODEC")
    @RemoteCallable(validator = CreativeTerminalAccessValidator.class)
    public static ItemStack terminalInsertFirst(
        UUID playerId,
        UUID targetId,
        @CallableParam(clazz = ItemStack.class, field = "OPTIONAL_STREAM_CODEC") ItemStack stack,
        @CallableParam(clazz = ItemStack.class, field = "OPTIONAL_STREAM_CODEC") ItemStack terminalStack
    ) {
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        UUID playerUuid = player.getGameProfile().getId();
        // 同上：以客户端上报的指针终端为准校验持有关系
        if (!StorageServerStub.isBoundTerminal(terminalStack, targetId, playerUuid)) {
            return stack.copy();
        }
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int inserted = StorageServerStub.insertIntoTerminal(player, targetId, stack, stack.getCount());
        ItemStack remain = stack.copy();
        remain.shrink(inserted);
        if (inserted > 0) {
            // 同上：不 broadcastChanges，避免服务端空 carried 清空客户端指针
            player.getInventory().setChanged();
        }
        return remain;
    }

    /**
     * 从一组存储中按背包当前缺口提取 needs 中每种物品，补入玩家背包。
     * 实际补入的数量累加到 {@code withdrawn}（同一物品合并计数）。
     */
    private static void withdrawNeedsFromStorages(
        ServerPlayer player,
        List<BaseStorage<?>> storages,
        List<ItemStack> needs,
        List<ItemStack> withdrawn
    ) {
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
            for (BaseStorage<?> storage : storages) {
                UnlimitedItemStacksResourceHandler items = storage.getItems();
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
                        StorageServerStub.addWithdrawn(withdrawn, resource, got);
                    }
                }
                if (required <= 0) {
                    break;
                }
            }
        }
    }

    /** 把本次实际补入的数量合并进 withdrawn 列表（同一物品合并计数，防溢出）。 */
    private static void addWithdrawn(List<ItemStack> withdrawn, ItemStack resource, int got) {
        for (ItemStack existing : withdrawn) {
            if (ItemStack.isSameItemSameComponents(existing, resource)) {
                int add = Math.min(got, Integer.MAX_VALUE - existing.getCount());
                if (add > 0) {
                    existing.grow(add);
                }
                return;
            }
        }
        withdrawn.add(resource.copyWithCount(got));
    }

    /**
     * JEI 快速合成补库失败后的回退：把 {@code stacks} 中每种物品从玩家背包取回（不超过
     * 背包现有量），存入玩家绑定的终端存储。存储放不下的剩余部分保留在背包。
     * 仅当玩家确实持有这些目标对应的终端时生效。
     */
    @RemoteCallable(validator = TerminalAccessValidator.class)
    public static void terminalReturnExcess(
        UUID playerId,
        @CallableParam(clazz = StorageServerStub.class, field = "UUID_LIST_STREAM_CODEC")
        List<UUID> targetIds,
        @CallableParam(clazz = StorageServerStub.class, field = "ITEM_STACK_LIST_STREAM_CODEC")
        List<ItemStack> stacks
    ) {
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        if (targetIds.isEmpty() || stacks.isEmpty()) {
            return;
        }
        List<BaseStorage<?>> storages = new ArrayList<>();
        for (UUID targetId : targetIds) {
            if (!StorageServerStub.terminalTargetReachable(player, targetId)) {
                continue;
            }
            for (BaseStorage<?> storage : StorageServerStub.terminalStorages(player, targetId)) {
                StorageServerStub.addDistinct(storages, storage);
            }
        }
        if (storages.isEmpty()) {
            return;
        }
        boolean changed = false;
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack resource = stack.copyWithCount(1);
            int remaining = Math.min(stack.getCount(), StorageServerStub.countInInventory(player.getInventory(), resource));
            if (remaining <= 0) {
                continue;
            }
            int inserted = StorageServerStub.insertIntoStorages(storages, resource, remaining);
            if (inserted <= 0) {
                continue;
            }
            changed = true;
            // 从主物品栏移走已存入的多余数量：优先非主手槽位，最后才动主手
            int toRemove = inserted;
            for (int i = Inventory.INVENTORY_SIZE - 1; i >= 0 && toRemove > 0; i--) {
                if (i == player.getInventory().selected) {
                    continue;
                }
                ItemStack slotStack = player.getInventory().getItem(i);
                if (slotStack.isEmpty() || !ItemStack.isSameItemSameComponents(slotStack, resource)) {
                    continue;
                }
                int take = Math.min(slotStack.getCount(), toRemove);
                slotStack.shrink(take);
                toRemove -= take;
            }
            if (toRemove > 0) {
                ItemStack held = player.getInventory().getItem(player.getInventory().selected);
                if (!held.isEmpty() && ItemStack.isSameItemSameComponents(held, resource)) {
                    held.shrink(toRemove);
                }
            }
        }
        if (changed) {
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        }
    }

    /**
     * JEI 快速合成检查：返回玩家绑定存储站中每种物品的代表与总数量（去重聚合，
     * 同一物品跨多个存储/槽位合并计数），供客户端缓存判断存储站是否满足配方需求。
     * 限制返回条目数，避免超大存储站在每次刷新时全量扫描造成服务端尖峰。
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
    public static List<ItemStack> getStorageItems(UUID playerId, UUID targetId) {
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        if (!StorageServerStub.ownsBoundTerminal(player, targetId)) {
            return List.of();
        }
        if (!StorageServerStub.terminalTargetReachable(player, targetId)) {
            return List.of();
        }
        List<BaseStorage<?>> storages = StorageServerStub.terminalStorages(player, targetId);
        List<ItemStack> result = new ArrayList<>();
        for (BaseStorage<?> storage : storages) {
            UnlimitedItemStacksResourceHandler items = storage.getItems();
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
                boolean merged = false;
                for (ItemStack existing : result) {
                    if (ItemStack.isSameItemSameComponents(existing, stack)) {
                        int add = (int) Math.min(amount, Integer.MAX_VALUE - existing.getCount());
                        if (add > 0) {
                            existing.grow(add);
                        }
                        merged = true;
                        break;
                    }
                }
                if (!merged) {
                    result.add(stack.copyWithCount((int) Math.min(amount, Integer.MAX_VALUE)));
                }
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

    /** 玩家身上是否持有带指定 storageId 的存储引用物品（如从方块上拆下的潜影存储站/板条箱）。 */
    private static boolean ownsStorageRef(ServerPlayer player, UUID storageId) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            StorageRef ref = stack.get(ModComponents.STORAGE);
            if (ref != null && ref.id().isPresent() && ref.id().get().equals(storageId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean ownsBoundTerminal(ServerPlayer player, UUID storageId) {
        UUID playerId = player.getGameProfile().getId();
        if (StorageServerStub.isBoundTerminal(player.getMainHandItem(), storageId, playerId)) return true;
        if (StorageServerStub.isBoundTerminal(player.getOffhandItem(), storageId, playerId)) return true;
        boolean holdsLocal = false;
        boolean holdsShulker = false;
        // 创造模式指针物品由客户端本地管理（ItemPickerMenu 纯客户端，不进服务端背包），
        // 终端捏在指针上时服务端背包里没有它，需额外检查指针。
        if (StorageServerStub.isBoundTerminal(player.containerMenu.getCarried(), storageId, playerId)) return true;
        for (ItemStack stack : player.getInventory().items) {
            if (StorageServerStub.isBoundTerminal(stack, storageId, playerId)) return true;
            if (stack.is(ModItems.LOCAL_TERMINAL)) holdsLocal = true;
            if (stack.is(ModItems.SHULKER_TERMINAL)) holdsShulker = true;
        }
        if ((holdsLocal || player.getMainHandItem().is(ModItems.LOCAL_TERMINAL)
             || player.getOffhandItem().is(ModItems.LOCAL_TERMINAL))
            && storageId.equals(StorageServerStub.localTerminalId(playerId))) {
            return true;
        }
        return (holdsShulker
                || player.getMainHandItem().is(ModItems.SHULKER_TERMINAL)
                || player.getOffhandItem().is(ModItems.SHULKER_TERMINAL))
               && storageId.equals(StorageServerStub.shulkerTerminalId(playerId));
    }

    private static boolean isBoundTerminal(ItemStack stack, UUID storageId, UUID playerId) {
        if (stack.is(ModItems.LOCAL_TERMINAL) && storageId.equals(StorageServerStub.localTerminalId(playerId))) {
            return true;
        }
        if (stack.is(ModItems.SHULKER_TERMINAL) && storageId.equals(StorageServerStub.shulkerTerminalId(playerId))) {
            return true;
        }
        if (!stack.is(ModItems.HYPERDIMENSION_TERMINAL)) return false;
        TerminalBinding binding = stack.get(ModComponents.TERMINAL_BINDING);
        return binding != null && binding.id().isPresent() && binding.id().get().equals(storageId);
    }

    /**
     * 客户端安全（无服务端 API）的终端绑定检查：超维终端必须有绑定 ID；
     * 本地/潜影终端总是有可解析目标，返回 true。供 BundleLike 客户端预测
     * 判断"取出是否可能"（存储是否为空无法在客户端得知，由服务端决定）。
     */
    public static boolean isBoundTerminalClientSafe(ItemStack stack) {
        if (stack.is(ModItems.LOCAL_TERMINAL) || stack.is(ModItems.SHULKER_TERMINAL)) {
            return true;
        }
        if (!stack.is(ModItems.HYPERDIMENSION_TERMINAL)) {
            return false;
        }
        TerminalBinding binding = stack.get(ModComponents.TERMINAL_BINDING);
        return binding != null && binding.id().isPresent();
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean canStore(BaseStorage<?> storage, ItemStack stack) {
        Holder<IStorageType<?>> type = storage.getTypeHolder();
        if (type.is(ModStorageTypes.SHULKER_CONTAINER.getKey()) || type.is(ModStorageTypes.HYPERDIMENSION.getKey())) {
            return !(stack.getItem() instanceof ShulkerContainerBlockItem)
                   && !(stack.getItem() instanceof BlockItem blockItem
                        && (blockItem.getBlock() instanceof HyperdimensionStorageStationBlock
                            || blockItem.getBlock() instanceof ShulkerBoxBlock))
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
     * 扫描玩家背包与主/副手中的已绑定终端，收集其指向的存储（超维存储站 / 大型板条箱 / 潜影目标）。
     * 返回去重后的存储列表。本地与潜影终端仅在玩家实际持有对应终端时参与连接。
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
        if (StorageServerStub.holdsItem(player, ModItems.LOCAL_TERMINAL.asItem())) {
            StorageServerStub.findNearbyLargeCrate(player).ifPresent(id -> StorageServerStub.addDistinct(
                storages,
                Storages.get().getOrCreate(id, LargeCrateStorage.class)
            ));
        }
        if (StorageServerStub.holdsItem(player, ModItems.SHULKER_TERMINAL.asItem())) {
            for (BaseStorage<?> storage : StorageServerStub.shulkerTerminalStorages(player)) {
                StorageServerStub.addDistinct(storages, storage);
            }
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

    /** 玩家主物品栏 / 盔甲 / 副手是否持有指定物品。 */
    private static boolean holdsItem(ServerPlayer player, Item item) {
        if (player.getMainHandItem().is(item) || player.getOffhandItem().is(item)) {
            return true;
        }
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(item)) {
                return true;
            }
        }
        return false;
    }

    private static void addDistinct(List<BaseStorage<?>> storages, BaseStorage<?> storage) {
        for (BaseStorage<?> existing : storages) {
            if (existing.getId().equals(storage.getId())) {
                return;
            }
        }
        storages.add(storage);
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
            // 稀疏存储（SpaceSize / TypeLimit）的空槽在 size() 之外（增长槽），
            // 新类型必须走无槽版本 insertItem(stack)，由 handler 内部追加增长槽；
            // 按 size() 遍历找空槽会永远找不到，导致未进过存储的新物品无法放入。
            UnlimitedItemStacksResourceHandler items = storage.getItems();
            ItemStack leftover = items.insertItem(resource.copyWithCount(amount - inserted), false);
            inserted += (amount - inserted) - leftover.getCount();
            if (inserted == amount) {
                return inserted;
            }
        }
        return inserted;
    }

    /**
     * 本地终端的会话标识：按玩家 UUID 派生的合成 ID，
     * 使所有终端 RPC（打开 / JEI / 物品均衡）都能用同一标识定位本地终端连接的存储。
     */
    private static UUID localTerminalId(UUID playerId) {
        return UUID.nameUUIDFromBytes(("anvilcraft:local_terminal:" + playerId).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 潜影终端的会话标识：按玩家 UUID 派生的合成 ID。
     */
    private static UUID shulkerTerminalId(UUID playerId) {
        return UUID.nameUUIDFromBytes(("anvilcraft:shulker_terminal:" + playerId).getBytes(StandardCharsets.UTF_8));
    }

    /** 终端虚拟位置指向的目标存储。 */
    private record RemoteTarget(int kind, @Nullable UUID storageId) {
        static final int HYPERDIMENSION = 0;
        static final int LARGE_CRATE = 1;
        static final int SHULKER_CONTAINER = 2;
    }

    /** 根据终端目标标识解析本次调用应操作的存储列表。 */
    private static List<BaseStorage<?>> terminalStorages(ServerPlayer player, UUID targetId) {
        UUID playerId = player.getGameProfile().getId();
        if (targetId.equals(StorageServerStub.localTerminalId(playerId))) {
            return StorageServerStub.findNearbyLargeCrate(player)
                .map(id -> List.<BaseStorage<?>>of(Storages.get().getOrCreate(id, LargeCrateStorage.class)))
                .orElseGet(List::of);
        }
        if (targetId.equals(StorageServerStub.shulkerTerminalId(playerId))) {
            return StorageServerStub.shulkerTerminalStorages(player);
        }
        return List.of(
            Storages.get().getOrCreate(targetId, HyperdimensionStorage.class)
        );
    }

    /**
     * 从终端物品栈解析其目标存储标识：本地 / 潜影终端按玩家 UUID 派生，
     * 超维终端读取绑定存储站；非终端或未绑定时返回 {@code null}。
     */
    public static @Nullable UUID terminalTargetId(ServerPlayer player, ItemStack terminal) {
        UUID playerId = player.getGameProfile().getId();
        if (terminal.is(ModItems.LOCAL_TERMINAL)) {
            return StorageServerStub.localTerminalId(playerId);
        }
        if (terminal.is(ModItems.SHULKER_TERMINAL)) {
            // 空手右键使用终端时会惰性为身上的空潜影集装箱授予 UUID，
            // 此处仅解析会话标识（不触发授予）
            return StorageServerStub.shulkerTerminalId(playerId);
        }
        if (terminal.is(ModItems.HYPERDIMENSION_TERMINAL)) {
            TerminalBinding binding = terminal.get(ModComponents.TERMINAL_BINDING);
            return binding != null && binding.id().isPresent() ? binding.id().get() : null;
        }
        return null;
    }

    /**
     * 把物品放入终端连接的目标存储，返回实际插入数量。
     * 目标不可达（本地 / 潜影终端超出连接范围，超维终端未绑定）时返回 0。
     */
    public static int insertIntoTerminal(ServerPlayer player, UUID targetId, ItemStack stack, int amount) {
        if (stack.isEmpty() || amount <= 0) {
            return 0;
        }
        List<BaseStorage<?>> storages = StorageServerStub.terminalStorages(player, targetId);
        if (storages.isEmpty()) {
            return 0;
        }
        int inserted = StorageServerStub.insertIntoStorages(storages, stack, amount);
        if (inserted > 0) {
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        }
        return inserted;
    }

    /**
     * 从终端连接的目标存储取出一个物品（按存储顺序取第一个可取槽位）。
     * 目标不可达或存储为空时返回空栈。
     */
    public static ItemStack extractFromTerminal(ServerPlayer player, UUID targetId, int amount) {
        List<BaseStorage<?>> storages = StorageServerStub.terminalStorages(player, targetId);
        for (BaseStorage<?> storage : storages) {
            UnlimitedItemStacksResourceHandler items = storage.getItems();
            for (int slot = 0; slot < items.size(); slot++) {
                if (items.getAmountAsLong(slot) <= 0) {
                    continue;
                }
                int take = (int) Math.min(amount, items.getAmountAsLong(slot));
                ItemStack got = items.extractUnlimited(slot, take, false).toStack();
                if (!got.isEmpty()) {
                    player.getInventory().setChanged();
                    player.containerMenu.broadcastChanges();
                    return got;
                }
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * 终端目标当前是否可达：
     * <ul>
     *   <li>本地终端：32 格内仍存在大型板条箱；</li>
     *   <li>潜影终端：身上仍存在潜影集装箱，或 64 格内仍存在世界潜影集装箱；</li>
     *   <li>超维终端：绑定目标始终可达（无距离限制）。</li>
     * </ul>
     */
    private static boolean terminalTargetReachable(ServerPlayer player, UUID targetId) {
        UUID playerId = player.getGameProfile().getId();
        if (targetId.equals(StorageServerStub.localTerminalId(playerId))) {
            return StorageServerStub.findNearbyLargeCrate(player).isPresent();
        }
        if (targetId.equals(StorageServerStub.shulkerTerminalId(playerId))) {
            return !StorageServerStub.shulkerTerminalStorages(player).isEmpty();
        }
        return true;
    }

    /** 浮窗等终端操作的目标（虚拟位置）当前是否可达；真实方块路径不受限制。 */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean sourceReachable(ServerPlayer player, long sourcePos) {
        RemoteTarget target = StorageServerStub.REMOTE_STORAGES
            .getOrDefault(player.getGameProfile().getId(), Map.of())
            .get(sourcePos);
        if (target == null) {
            return true;
        }
        return switch (target.kind()) {
            case RemoteTarget.LARGE_CRATE -> StorageServerStub.findNearbyLargeCrate(player).isPresent();
            case RemoteTarget.SHULKER_CONTAINER -> !StorageServerStub.shulkerTerminalStorages(player).isEmpty();
            default -> true;
        };
    }

    /**
     * 客户端查询：本地 / 潜影终端当前是否能够连接其自动解析的目标（供“+”提示与浮窗判定）。
     */
    @RemoteCallable(validator = TerminalAccessValidator.class)
    public static boolean isTerminalReachable(UUID playerId, UUID targetId) {
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        return StorageServerStub.terminalTargetReachable(player, targetId);
    }

    /**
     * 解析潜影终端的连接目标（按优先级，不同时连接多个）：
     * <ol>
     *   <li>玩家身上槽位最靠前的潜影集装箱（仅已有 UUID 的；空集装箱只在
     *       {@link #openRemote} 右键打开时惰性授予，悬停 / 浮窗等只读路径不授予）；</li>
     *   <li>64 格内最近的世界潜影集装箱。</li>
     * </ol>
     */
    private static List<BaseStorage<?>> shulkerTerminalStorages(ServerPlayer player) {
        Optional<UUID> containerId = StorageServerStub.findBoundPlayerShulkerContainer(player);
        return containerId
            .<List<BaseStorage<?>>>map(uuid -> List.of(Storages.get().getOrCreate(uuid, ShulkerContainerStorage.class)))
            .orElseGet(() -> StorageServerStub.findNearbyShulkerContainer(player)
                .map(id -> List.<BaseStorage<?>>of(Storages.get().getOrCreate(id, ShulkerContainerStorage.class)))
                .orElseGet(List::of));
    }

    /**
     * 空占位存储的独立哨兵 ID：按玩家 UUID 派生，但与本地 / 潜影终端的合成 ID 区分，
     * 避免占位存储被误当作真实终端目标，或被 {@link Storages#getOrCreate} 写入全局
     * 注册表造成存档膨胀。
     */
    private static UUID emptyTerminalId(UUID playerId) {
        return UUID.nameUUIDFromBytes(("anvilcraft:empty_terminal:" + playerId).getBytes(StandardCharsets.UTF_8));
    }

    /** 构造终端目标不可达时的空占位存储视图（界面 / 浮窗显示为空）。 */
    private static StorageView emptyView(ServerPlayer player) {
        UUID id = StorageServerStub.emptyTerminalId(player.getGameProfile().getId());
        return new StorageView(List.of(new EmptyTerminalStorage(id)), List.of());
    }

    /** 终端目标不可达（超出范围 / 目标消失）时的占位空存储。 */
    private static final class EmptyTerminalStorage extends BaseStorage<UnlimitedItemStacksResourceHandler> {
        private EmptyTerminalStorage(UUID id) {
            super(id);
        }

        @Override
        protected UnlimitedItemStacksResourceHandler constructItemHandler(
            BiConsumer<Integer, UnlimitedItemStack> onContentsChanged
        ) {
            return new UnlimitedItemStacksResourceHandler(0) {
                @Override
                protected void onContentsChanged(int index, UnlimitedItemStack original) {
                    onContentsChanged.accept(index, original);
                }
            };
        }

        @Override
        public Holder<IStorageType<?>> getTypeHolder() {
            return ModStorageTypes.HYPERDIMENSION;
        }
    }

    /** 查找玩家 32 格内最近的大型板条箱主方块及其存储 ID（注册表优先，回退扫描补录）。 */
    private static Optional<UUID> findNearbyLargeCrate(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos mainPos = TerminalBlockRegistry.nearestLargeCrate(
            level,
            player.getX(),
            player.getY(),
            player.getZ(),
            StorageServerStub.LOCAL_TERMINAL_RANGE
        );
        if (mainPos != null && level.getBlockEntity(mainPos) instanceof LargeCrateBlockEntity be) {
            return Optional.of(StorageServerStub.ensureStorageId(be));
        }
        // 注册表缺失或条目过期：回退扫描并补录
        return StorageServerStub.scanNearestPos(player, StorageServerStub.LOCAL_TERMINAL_RANGE, LargeCrateBlockEntity.class)
            .flatMap(pos -> {
                if (!(level.getBlockEntity(pos) instanceof LargeCrateBlockEntity crate)) {
                    return Optional.empty();
                }
                TerminalBlockRegistry.registerIfApplicable(crate);
                return Optional.of(StorageServerStub.ensureStorageId(crate));
            });
    }

    /**
     * 查找玩家身上槽位最靠前的、已绑定 UUID 的潜影集装箱。
     *
     * <p>只读路径（悬停可达性、JEI 补库、物品均衡）使用：不授予 UUID，避免悬停
     * 时意外写回物品组件；空（无 UUID）集装箱需先经 {@link #openRemote} 右键打开授予。</p>
     */
    private static Optional<UUID> findBoundPlayerShulkerContainer(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!(stack.getItem() instanceof ShulkerContainerBlockItem)) {
                continue;
            }
            StorageRef ref = stack.get(ModComponents.STORAGE);
            if (ref != null
                && ref.type().is(ModStorageTypes.SHULKER_CONTAINER.getKey())
                && ref.id().isPresent()) {
                return ref.id();
            }
        }
        return Optional.empty();
    }

    /**
     * 查找玩家身上槽位最靠前的潜影集装箱，返回其存储 ID。
     *
     * <p>无论是否已有 UUID 都取槽位最靠前的那个：尚无 UUID 时在打开终端时惰性授予
     * 随机 UUID（写回物品 STORAGE 组件），保证「最靠前」语义不被有无 UUID 干扰。
     * 无可连接的集装箱时返回空。</p>
     */
    private static Optional<UUID> findOrGrantFrontmostShulkerContainer(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!(stack.getItem() instanceof ShulkerContainerBlockItem)) {
                continue;
            }
            StorageRef ref = stack.get(ModComponents.STORAGE);
            if (ref == null || !ref.type().is(ModStorageTypes.SHULKER_CONTAINER.getKey())) {
                continue;
            }
            // 槽位最靠前的集装箱无论是否已有 UUID 都作为目标；无 UUID 时授予
            if (ref.id().isPresent()) {
                return ref.id();
            }
            UUID id = UUID.randomUUID();
            stack.set(ModComponents.STORAGE, new StorageRef(ref.type(), id));
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
            return Optional.of(id);
        }
        return Optional.empty();
    }

    /** 查找玩家 64 格内最近的世界潜影集装箱主方块及其存储 ID（注册表优先，回退扫描补录）。 */
    private static Optional<UUID> findNearbyShulkerContainer(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos mainPos = TerminalBlockRegistry.nearestShulkerContainer(
            level,
            player.getX(),
            player.getY(),
            player.getZ(),
            StorageServerStub.SHULKER_TERMINAL_RANGE
        );
        if (mainPos != null && level.getBlockEntity(mainPos) instanceof ShulkerContainerBlockEntity be) {
            return Optional.of(StorageServerStub.ensureStorageId(be));
        }
        // 注册表缺失或条目过期：回退扫描并补录
        return StorageServerStub.scanNearestPos(player, StorageServerStub.SHULKER_TERMINAL_RANGE, ShulkerContainerBlockEntity.class)
            .flatMap(pos -> {
                if (!(level.getBlockEntity(pos) instanceof ShulkerContainerBlockEntity shulker)) {
                    return Optional.empty();
                }
                TerminalBlockRegistry.registerIfApplicable(shulker);
                return Optional.of(StorageServerStub.ensureStorageId(shulker));
            });
    }

    /** 扫描 AABB 内最近的目标方块实体主方块坐标（注册表未覆盖时的回退路径）。 */
    private static Optional<BlockPos> scanNearestPos(
        ServerPlayer player,
        int range,
        Class<? extends BlockEntity> type
    ) {
        Map<BlockPos, BlockEntity> mains = new HashMap<>();
        AABB area = AABB.ofSize(
            player.getEyePosition(),
            2.0 * range,
            2.0 * range,
            2.0 * range
        );
        for (BlockEntity be : StorageServerStub.blockEntitiesInAABB(player.serverLevel(), area)) {
            if (!type.isInstance(be)) {
                continue;
            }
            BlockPos mainPos = StorageServerStub.mainPartPos(be);
            mains.putIfAbsent(mainPos, player.level().getBlockEntity(mainPos));
        }
        return StorageServerStub.nearestMainPos(player, mains);
    }

    /** 收集指定 AABB 范围内已加载区块中的全部方块实体。 */
    private static List<BlockEntity> blockEntitiesInAABB(ServerLevel level, AABB area) {
        List<BlockEntity> result = new ArrayList<>();
        int minChunkX = Mth.floor(area.minX) >> 4;
        int maxChunkX = Mth.floor(area.maxX) >> 4;
        int minChunkZ = Mth.floor(area.minZ) >> 4;
        int maxChunkZ = Mth.floor(area.maxZ) >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                LevelChunk chunk = level.getChunk(chunkX, chunkZ);
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    BlockPos pos = be.getBlockPos();
                    if (area.contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) {
                        result.add(be);
                    }
                }
            }
        }
        return result;
    }

    /** 多方块方块取其主方块坐标，普通方块取自身坐标。 */
    private static BlockPos mainPartPos(BlockEntity be) {
        BlockState state = be.getBlockState();
        if (state.getBlock() instanceof AbstractMultiPartBlock<?> multipart) {
            return multipart.getMainPartPos(be.getBlockPos(), state);
        }
        return be.getBlockPos();
    }

    /** 从主方块候选集合中选出离玩家最近的一个，返回其坐标。 */
    private static Optional<BlockPos> nearestMainPos(ServerPlayer player, Map<BlockPos, BlockEntity> mains) {
        if (mains.isEmpty()) {
            return Optional.empty();
        }
        BlockPos nearest = null;
        double nearestSqr = Double.MAX_VALUE;
        double playerX = player.getX();
        double playerY = player.getY();
        double playerZ = player.getZ();
        for (BlockPos pos : mains.keySet()) {
            double sqr = pos.distToCenterSqr(playerX, playerY, playerZ);
            if (sqr < nearestSqr) {
                nearestSqr = sqr;
                nearest = pos;
            }
        }
        return Optional.ofNullable(nearest);
    }

    /** 取存储方块的存储 ID；缺失时惰性生成并持久化。 */
    private static UUID ensureStorageId(StorageBlockEntity storage) {
        UUID id = storage.getId();
        if (id == null) {
            id = UUID.randomUUID();
            storage.setId(id);
        }
        return id;
    }

    public static final class StorageUsageValidator implements IRemoteCallableValidator {
        @Override
        public boolean validate(IPayloadContext ctx, Method method, Object[] args) {
            return ctx.player() instanceof ServerPlayer player
                   && args.length >= 2
                   && args[0] instanceof UUID playerId
                   && player.getGameProfile().getId().equals(playerId)
                   && args[1] instanceof UUID storageId
                   // 仅允许查询自己持有的存储，防止凭 UUID 枚举他人存储信息：
                   // 1. 持有绑定终端指向的存储
                   // 2. 身上持有带该 storageId 的存储引用物品（潜影存储站等从方块上拆下/复制的物品）
                   && (StorageServerStub.ownsBoundTerminal(player, storageId)
                       || StorageServerStub.ownsStorageRef(player, storageId));
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
            ) {
                return false;
            }
            if (args[1] instanceof UUID storageId) {
                return StorageServerStub.ownsBoundTerminal(player, storageId);
            }
            // 多目标（JEI 多终端补库）：每个目标都必须由玩家持有的终端对应
            if (args[1] instanceof List<?> ids) {
                if (ids.isEmpty()) {
                    return false;
                }
                for (Object id : ids) {
                    if (!(id instanceof UUID storageId)
                        || !StorageServerStub.ownsBoundTerminal(player, storageId)) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
    }

    /**
     * 创造背包等纯客户端菜单的终端 BundleLike RPC 校验：
     * 仅校验玩家身份与参数形态，终端持有关系由方法体用客户端上报的指针终端
     * （terminalStack）校验——创造模式下指针物品由客户端本地管理，服务端背包
     * 与 carried 都没有该终端，TerminalAccessValidator 会误拒。
     */
    public static final class CreativeTerminalAccessValidator implements IRemoteCallableValidator {
        @Override
        public boolean validate(IPayloadContext ctx, Method method, Object[] args) {
            if (
                !(ctx.player() instanceof ServerPlayer player)
                || args.length < 3
                || !(args[0] instanceof UUID playerId)
                || !player.getGameProfile().getId().equals(playerId)
                || !(args[1] instanceof UUID storageId)
            ) {
                return false;
            }
            // args 尾部必须带客户端上报的指针终端（terminalStack），方法体内做真实持有校验
            boolean valid = args[args.length - 1] instanceof ItemStack terminalStack && !terminalStack.isEmpty();
            if (valid) {
                // 与 StorageAccessValidator 一致：方法体（terminalExtractFirst 等）需要
                // registries 构造 StorageView / 读 PlayerSetting
                StorageServerStub.REGISTRIES.set(ctx.player().registryAccess());
            }
            return valid;
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

    /**
     * {@link #craftingTakeAll} 分块合成的结果。
     *
     * @param carried 本次调用结束时的指针物品
     * @param changed 本次调用是否合成了至少一个产物
     * @param done    本次调用是否已自然终止（材料耗尽 / 产物无处可放 / 不消耗型配方）；
     *                {@code false} 表示达到分块上限，客户端应继续调用
     */
    public record TakeAllResult(ItemStack carried, boolean changed, boolean done) {
        public static final StreamCodec<RegistryFriendlyByteBuf, TakeAllResult> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC,
            TakeAllResult::carried,
            ByteBufCodecs.BOOL,
            TakeAllResult::changed,
            ByteBufCodecs.BOOL,
            TakeAllResult::done,
            TakeAllResult::new
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
            if (!StorageServerStub.matchesFilters(stack.getItemHolder(), unlimitedStack, id, search, categories)) {
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
        String search,
        List<CategoryEntry> categories
    ) {
        // 普通文本搜索不在服务端过滤：服务端无客户端语言环境，本地化名称匹配
        // 由客户端（StorageScreen.applySearchFilter）完成；服务端只处理 @ namespace
        // 与 # tag 前缀搜索。普通文本时返回 true（全部条目，客户端二次过滤）。
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
        RemoteTarget remote = StorageServerStub.REMOTE_STORAGES.getOrDefault(playerId, Map.of()).get(sourcePos);
        if (remote != null && remote.storageId() != null) {
            return switch (remote.kind()) {
                case RemoteTarget.HYPERDIMENSION -> new StorageView(
                    List.of(Storages.get().getOrCreate(remote.storageId(), HyperdimensionStorage.class)),
                    List.of()
                );
                // 本地 / 潜影终端：每次按当前状态重新解析目标（连接可能随距离 / 物品变化），
                // 目标不可达时返回空视图（界面 / 浮窗显示为空存储）
                case RemoteTarget.LARGE_CRATE -> StorageServerStub.findNearbyLargeCrate(player)
                    .map(id -> new StorageView(
                        List.of(Storages.get().getOrCreate(id, LargeCrateStorage.class)),
                        List.of()
                    ))
                    .orElseGet(() -> StorageServerStub.emptyView(player));
                case RemoteTarget.SHULKER_CONTAINER -> {
                    List<BaseStorage<?>> storages = StorageServerStub.shulkerTerminalStorages(player);
                    if (storages.isEmpty()) {
                        yield StorageServerStub.emptyView(player);
                    }
                    yield new StorageView(storages, List.of());
                }
                default -> throw new IllegalStateException("Unknown terminal target kind: " + remote.kind());
            };
        }
        BlockPos pos = BlockPos.of(sourcePos);
        BlockEntity blockEntity = player.level().getBlockEntity(pos);
        if (!(blockEntity instanceof StorageBlockEntity storage)) {
            // 无效 / 已过期的虚拟位置（客户端缓存残留或映射已被清理）：
            // 返回空视图而不是抛异常，避免 RPC 处理器崩溃
            return StorageServerStub.emptyView(player);
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
