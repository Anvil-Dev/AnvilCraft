package dev.dubhe.anvilcraft.rpc;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
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
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockRegistry;
import dev.dubhe.anvilcraft.block.entity.storage.StorageFluidRegistry;
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
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.SoundAction;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
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
    /** Shift 连续合成会话（按玩家 × 源位置）：跨分块 RPC 保持锁定配方与本次点击剩余合成次数，防止残料漂移。 */
    private static final Map<UUID, Map<Long, TakeAllSession>> TAKE_ALL_RECIPE_LOCKS = new HashMap<>();

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
        return new SyncResult(
            stub.version,
            view.fullness(),
            updates,
            StorageFluidRegistry.collect(view.primary().getId())
        );
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static InteractionResult interact(
        UUID playerId,
        long sourcePos,
        int slot,
        int button,
        StorageInput action,
        @CallableParam(clazz = FluidStack.class, field = "OPTIONAL_STREAM_CODEC") FluidStack fluid
    ) {
        if (!action.isValid(button)) {
            StorageServerStub.REGISTRIES.remove();
            throw new IllegalArgumentException("Invalid storage interaction button: " + button);
        }

        StorageView view = StorageServerStub.getView(StorageServerStub.getAndClear(), playerId, sourcePos);
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        ItemStack carried = player.containerMenu.getCarried();
        boolean changed = false;
        FluidNotice notice = FluidNotice.NONE;
        if (action == StorageInput.QUICK_MOVE_TO_STORAGE) {
            // 同一条动作左键与右键都会用到（Shift+左键 / Shift+右键），
            // 故按实际鼠标键决定是否倾倒：左键倒液体，右键存流体桶物品
            changed = StorageServerStub.moveInventoryStackToStorage(player, view, slot, button == 0) > 0;
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
        } else if (action == StorageInput.FLUID_BUCKET) {
            StorageServerStub.FluidOutcome outcome = StorageServerStub.takeFluidBucket(player, view, fluid, button);
            changed = outcome.changed();
            notice = outcome.notice();
        } else if (action == StorageInput.QUICK_MOVE_FROM_STORAGE) {
            if (slot >= StorageFluidRegistry.FLUID_SLOT_BASE) {
                StorageServerStub.FluidOutcome outcome =
                    StorageServerStub.takeFluidBucketIntoInventory(player, view, fluid);
                changed = outcome.changed();
                notice = outcome.notice();
            } else {
                changed = StorageServerStub.moveStorageStackToInventory(player, view, slot);
            }
        } else if (!carried.isEmpty()) {
            int amount = button == 0 ? carried.getCount() : 1;
            // 桶装流体只在左键时自动倾倒；右键保持原有物品行为，
            // 让流体桶能作为普通物品存入（否则桶装流体永远无法入库）
            int poured = button == 0
                         ? StorageServerStub.pourIntoFluidPort(player, view, carried, amount)
                         : 0;
            if (poured > 0) {
                if (carried.isEmpty()) {
                    player.containerMenu.setCarried(ItemStack.EMPTY);
                }
                changed = true;
            } else {
                int inserted = view.insert(carried.copyWithCount(1), amount);
                if (inserted > 0) {
                    carried.shrink(inserted);
                    changed = true;
                }
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
        // 分支可能改动了指针物品（例如取桶时用掉了指针上的空桶），
        // 必须重新读取，否则会把动作前的旧数量回传给客户端。
        carried = player.containerMenu.getCarried();
        return new InteractionResult(carried, changed, 0, notice);
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static void returnCarriedToInventory(UUID playerId, long sourcePos) {
        StorageServerStub.getView(StorageServerStub.getAndClear(), playerId, sourcePos);
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        ItemStack carried = player.containerMenu.getCarried();
        if (carried.isEmpty()) {
            return;
        }
        if (player.getInventory().add(carried)) {
            carried = ItemStack.EMPTY;
        }
        if (!carried.isEmpty()) {
            player.drop(carried, false);
        }
        player.containerMenu.setCarried(carried);
        player.containerMenu.broadcastChanges();
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
            // 由 Shift+左键（空指针拖拽）触发，左键为流体行为，故允许倾倒
            int inserted = StorageServerStub.moveInventoryStackToStorage(player, view, slot, true);
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
    public static boolean moveSameToStorage(UUID playerId, long sourcePos, int slot, boolean pour) {
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
            // 桶装流体优先自动倾倒：属于存储动作而非入库，故不记入 moved。
            // 仅左键倾倒，右键保持物品行为，让流体桶能作为普通物品存入
            int poured = pour
                         ? StorageServerStub.pourIntoFluidPort(player, view, stack, stack.getCount())
                         : 0;
            if (poured > 0) {
                if (stack.isEmpty()) {
                    inventory.setItem(index, ItemStack.EMPTY);
                }
                changed = true;
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
    public static DepositResult deposit(UUID playerId, long sourcePos, boolean all, boolean pour) {
        StorageView view = StorageServerStub.getView(StorageServerStub.getAndClear(), playerId, sourcePos);
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        final StorageServerStub stub = StorageServerStub.get(playerId, view.primary().getId());
        Map<ItemStack, Integer> moved = new HashMap<>();
        boolean changed = false;
        for (int slot = Inventory.getSelectionSize(); slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            // 桶装流体优先自动倾倒：这是存储动作而非物品入库，故不记入 moved。
            // 仅左键倾倒，右键保持物品行为，让流体桶能作为普通物品存入
            int poured = pour
                         ? StorageServerStub.pourIntoFluidPort(player, view, stack, stack.getCount())
                         : 0;
            if (poured > 0) {
                if (stack.isEmpty()) {
                    player.getInventory().setItem(slot, ItemStack.EMPTY);
                }
                changed = true;
                continue;
            }
            if (!all && !StorageServerStub.matchesStorageItem(view, stack)) {
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

    /** 设置合成面板选项（自动补料 / 产物去向）。 */
    @RemoteCallable(validator = StorageAccessValidator.class)
    public static void craftingSetOptions(UUID playerId, long sourcePos, boolean autoFill, boolean toStorage) {
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        StorageServerStub.CraftingTarget target = StorageServerStub.resolveCraftingTarget(player, sourcePos);
        target.write(target.read().withAutoFill(autoFill).withToStorage(toStorage));
    }

    /** 清空合成栏（① 切石机输入 + ② 合成 9 格）：物品全部放入仓储。 */
    @RemoteCallable(validator = StorageAccessValidator.class)
    public static boolean craftingClearToStorage(UUID playerId, long sourcePos) {
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        StorageServerStub.CraftingTarget target = StorageServerStub.resolveCraftingTarget(player, sourcePos);
        CraftingStorage crafting = target.read();
        StorageView view = target.view();
        if (view == null) {
            return false;
        }
        ItemStack stonecutterInput = crafting.stonecutterInput();
        List<ItemStack> grid = crafting.craftingInput();
        boolean hasAny = !stonecutterInput.isEmpty() || grid.stream().anyMatch(stack -> !stack.isEmpty());
        if (!hasAny) {
            return false;
        }
        if (!stonecutterInput.isEmpty()) {
            int inserted = view.insert(stonecutterInput.copyWithCount(1), stonecutterInput.getCount());
            if (inserted < stonecutterInput.getCount()) {
                // 输入槽仅被部分放入仓储：剩余部分保留在槽内，避免丢物品。
                int rest = stonecutterInput.getCount() - inserted;
                target.write(crafting.withStonecutterInput(stonecutterInput.copyWithCount(rest)));
                return true;
            }
        }
        for (int i = 0; i < grid.size(); i++) {
            ItemStack stack = grid.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            int inserted = view.insert(stack.copyWithCount(1), stack.getCount());
            if (inserted < stack.getCount()) {
                List<ItemStack> newGrid = new ArrayList<>(grid);
                for (int j = 0; j < i; j++) {
                    newGrid.set(j, ItemStack.EMPTY);
                }
                newGrid.set(i, stack.copyWithCount(stack.getCount() - inserted));
                target.write(crafting.withStonecutterInput(ItemStack.EMPTY).withCraftingInput(newGrid));
                return true;
            }
        }
        List<ItemStack> emptyGrid = java.util.Collections.nCopies(CraftingStorage.CRAFTING_GRID_SIZE, ItemStack.EMPTY);
        target.write(crafting.withStonecutterInput(ItemStack.EMPTY).withCraftingInput(emptyGrid));
        return true;
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static void craftingSetLastOpened(UUID playerId, long sourcePos, boolean opened) {
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        StorageServerStub.CraftingTarget target = StorageServerStub.resolveCraftingTarget(player, sourcePos);
        target.write(target.read().withLastOpened(opened));
    }

    /**
     * 拖拽分配到 ①/② 输入槽与玩家背包槽（与原版物品栏拖拽一致，所有目标视为一组）。
     * 左键：把指针物品 floor 均分到各槽（余数留在指针）；右键：每槽放 1 个；
     * 中键（创造模式）：把各槽填成满堆叠，指针不消耗，即原版的「中键拖拽复制」。
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
        // 中键拖拽是创造模式专属的「复制」：每个目标槽被填成满堆叠，指针不消耗
        // （原版 getQuickCraftPlaceCount 对 type 2 返回 maxStackSize，且
        //   isValidQuickcraftType 要求 hasInfiniteMaterials）
        boolean clone = button == 2;
        if (clone && !player.hasInfiniteMaterials()) {
            return new InteractionResult(carried, false);
        }
        int perSlot = switch (button) {
            case 0 -> Math.floorDiv(carried.getCount(), targets.size());
            case 1 -> 1;
            default -> carried.getMaxStackSize();
        };
        CraftingStorage crafting = target.read();
        ItemStack remaining = carried.copy();
        boolean changed = false;
        for (int targetSlot : targets) {
            if (!clone && remaining.isEmpty()) {
                break;
            }
            int amount = clone ? perSlot : Math.min(perSlot, remaining.getCount());
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
                if (!clone) {
                    remaining.shrink(space);
                }
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
                if (!clone) {
                    remaining.shrink(space);
                }
            }
            changed = true;
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

    /**
     * ①/② 输入槽按 Q / Ctrl+Q：把槽内物品直接丢到地上。
     *
     * <p>{@code stack=false}（Q）丢 1 个，{@code stack=true}（Ctrl+Q）丢出该槽整堆，
     * 与物品栏槽位的 Q / Ctrl+Q 语义一致。</p>
     *
     * <p>指针非空时不丢出，与原版 {@code ClickType.THROW} 及仓储槽丢弃一致。</p>
     */
    @RemoteCallable(validator = StorageAccessValidator.class)
    public static InteractionResult craftingThrowSlot(UUID playerId, long sourcePos, int slot, boolean stack) {
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        if (slot < 0 || slot >= 10) {
            StorageServerStub.REGISTRIES.remove();
            return new InteractionResult(player.containerMenu.getCarried(), false);
        }
        if (!player.containerMenu.getCarried().isEmpty()) {
            return new InteractionResult(player.containerMenu.getCarried(), false);
        }
        StorageServerStub.CraftingTarget target = StorageServerStub.resolveCraftingTarget(player, sourcePos);
        CraftingStorage crafting = target.read();
        ItemStack current = slot == 0
            ? crafting.stonecutterInput()
            : crafting.craftingInput().get(slot - 1);
        if (current.isEmpty()) {
            return new InteractionResult(player.containerMenu.getCarried(), false);
        }
        int amount = stack ? current.getCount() : 1;
        ItemStack dropped = current.copyWithCount(amount);
        ItemStack rest = current.copy();
        rest.shrink(amount);
        ItemStack newSlot = rest.isEmpty() ? ItemStack.EMPTY : rest;
        target.write(slot == 0
            ? crafting.withStonecutterInput(newSlot)
            : crafting.withCraftingSlot(slot - 1, newSlot));
        player.drop(dropped, true);
        StorageServerStub.swingMainHand(player);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        return new InteractionResult(player.containerMenu.getCarried(), true);
    }

    /**
     * ①/② 输入槽中键：创造模式下把槽内物品复制一整组到指针。
     *
     * <p>与仓储槽中键的 {@code CLONE} 语义一致：仅创造模式、指针为空时生效，
     * 且槽内物品保留（复制而非取出）。</p>
     */
    @RemoteCallable(validator = StorageAccessValidator.class)
    public static InteractionResult craftingCloneSlot(UUID playerId, long sourcePos, int slot) {
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        if (slot < 0 || slot >= 10) {
            StorageServerStub.REGISTRIES.remove();
            return new InteractionResult(player.containerMenu.getCarried(), false);
        }
        if (!player.hasInfiniteMaterials() || !player.containerMenu.getCarried().isEmpty()) {
            return new InteractionResult(player.containerMenu.getCarried(), false);
        }
        CraftingStorage crafting = StorageServerStub.resolveCraftingTarget(player, sourcePos).read();
        ItemStack current = slot == 0
            ? crafting.stonecutterInput()
            : crafting.craftingInput().get(slot - 1);
        if (current.isEmpty()) {
            return new InteractionResult(player.containerMenu.getCarried(), false);
        }
        ItemStack cloned = current.copyWithCount(current.getMaxStackSize());
        player.containerMenu.setCarried(cloned);
        player.containerMenu.broadcastChanges();
        return new InteractionResult(cloned, false);
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
            if (crafting.toStorage()) {
                StorageServerStub.placeCraftingResultToStorageFirst(player, target, result);
            } else {
                StorageServerStub.placeCraftingResult(player, target, result);
            }
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
            int refilledSlots = StorageServerStub.refillAndCollectSlots(player, target, crafting);
            player.containerMenu.setCarried(merged);
            player.containerMenu.broadcastChanges();
            return new InteractionResult(merged, true, refilledSlots);
        }
        StorageServerStub.consumeCraftingInput(target, crafting, stonecutter);
        int refilledSlots = StorageServerStub.refillAndCollectSlots(player, target, crafting);
        player.containerMenu.setCarried(result);
        player.containerMenu.broadcastChanges();
        return new InteractionResult(result, true, refilledSlots);
    }

    /**
     * 连续合成的次数预算：约「一组产物」。
     *
     * <p>按产物自身的堆叠上限折算，而非固定按 64 折算：镐子等不可堆叠物品上限为 1，
     * 一次点击只合成 1 个（否则会一口气产出 64 把）；16 堆叠物品（雪球 / 鸡蛋等）
     * 按 16 折算。单次产物个数已达到或超过上限时至少合成一次。</p>
     *
     * @param result     单次合成的产物
     * @param multiplier 倍数（空格键的放大倍率）
     * @return 合成次数预算
     */
    private static int craftBudget(ItemStack result, int multiplier) {
        int perCraft = Math.max(1, result.getCount());
        int perStack = Math.max(1, result.getMaxStackSize());
        return Math.max(1, perStack / perCraft) * Math.max(1, multiplier);
    }

    /**
     * 在③/④ 结果槽按 Q / Ctrl+Q：合成并把产物直接丢到地上。
     *
     * <p>{@code stack=false}（Q）只合成一次，丢出一份产物；{@code stack=true}（Ctrl+Q）
     * 连续合成至约一组（按产物堆叠上限折算），与仓储槽 Q / Ctrl+Q 的手感一致。</p>
     *
     * <p>与其它取出路径一致：指针非空时拒绝，避免产物与指针物品混淆。</p>
     *
     * <p>开启自动填充且合成格耗尽时按需补料，使 Ctrl+Q 无需预先摆满材料；不消耗型配方
     * （剩余物与输入相同，如催化剂 / 模具）在丢出一份后立即终止，否则会无限产出。</p>
     *
     * @param stack true 为 Ctrl+Q（约一组），false 为 Q（一次）
     * @return {@code changed} 表示本次是否有产物被丢出
     */
    @RemoteCallable(validator = StorageAccessValidator.class)
    public static InteractionResult craftingThrowResult(
        UUID playerId,
        long sourcePos,
        boolean stonecutter,
        boolean stack
    ) {
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        StorageServerStub.CraftingTarget target = StorageServerStub.resolveCraftingTarget(player, sourcePos);
        if (!player.containerMenu.getCarried().isEmpty()) {
            return new InteractionResult(player.containerMenu.getCarried(), false);
        }
        boolean any = false;
        int performed = 0;
        // Ctrl+Q 的次数在首轮读取产物后按「约一组」折算；Q 固定一次
        int limit = stack ? Integer.MAX_VALUE : 1;
        CraftingStorage template = CraftingStorage.EMPTY;
        while (performed < limit) {
            CraftingStorage crafting = target.read();
            if (template == CraftingStorage.EMPTY) {
                template = crafting;
            }
            ItemStack result = StorageServerStub.assembleCraftingResult(player, crafting, stonecutter);
            if (result == null || result.isEmpty()) {
                break;
            }
            if (stack && performed == 0) {
                limit = Math.min(
                    StorageServerStub.CRAFTING_TAKE_ALL_CHUNK,
                    StorageServerStub.craftBudget(result, 1)
                );
            }
            player.drop(result.copy(), true);
            any = true;
            performed++;
            if (!StorageServerStub.consumeCraftingInput(target, crafting, stonecutter)) {
                // 不消耗型配方：已丢出一份，继续循环只会无限产出相同产物
                break;
            }
            CraftingStorage after = target.read();
            boolean exhausted = stonecutter
                ? after.stonecutterInput().isEmpty()
                : after.craftingInput().stream().allMatch(ItemStack::isEmpty);
            if (!exhausted) {
                continue;
            }
            // 合成格耗尽：自动填充时补 1 个继续，补不到或未开启则停止
            if (!template.autoFill() || StorageServerStub.refillAndCollectSlots(player, target, template) == 0) {
                break;
            }
        }
        if (any) {
            StorageServerStub.swingMainHand(player);
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        }
        return new InteractionResult(player.containerMenu.getCarried(), any);
    }

    /**
     * 按住 Shift 点击③/④ 结果槽：连续合成直到材料不足或产物无处可放。
     * 产物依次放入指针（同种合并）→ 背包（同种堆叠/空槽）→ 仓储。
     *
     * <p>产物完全放不下（指针异种且背包 / 仓储均无空间）时不消耗输入、不丢弃
     * 产物，立即截断——与原版「指针异种时拒绝取出」语义一致，避免凭空产出物品。</p>
     *
     * <p>开启自动填充时，一次点击只合成约 1 格产物量（台阶 60 个即停），连续
     * 点击再继续合成。产物去往背包时优先补满背包中已有的同种堆叠（如 60 个台阶
     * 会先补到 64），满了再放入下一个空槽，不会留下 60 60 60 的碎片堆；产物
     * 去往存储时同样按点击分批送入存储。合成格内材料不足时自动从背包 / 存储
     * 补 1 个继续，直至达到点击预算、材料枯竭或产物无处可放。</p>
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
    public static TakeAllResult craftingTakeAll(UUID playerId, long sourcePos, boolean stonecutter, int multiplier) {
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        StorageServerStub.CraftingTarget target = StorageServerStub.resolveCraftingTarget(player, sourcePos);
        boolean any = false;
        int iterations = 0;
        // 会话：同一次点击的多个分块共享锁定配方与剩余合成次数；本次点击结束后清除
        TakeAllSession session = StorageServerStub.lockedTakeAllSession(playerId, sourcePos);
        ResourceLocation lockedId = session == null ? null : session.recipeId();
        int remainingCrafts = session == null ? 0 : session.remainingCrafts();
        CraftingStorage initialCrafting = CraftingStorage.EMPTY;
        int refilledAccum = 0;
        while (iterations++ < StorageServerStub.CRAFTING_TAKE_ALL_CHUNK) {
            CraftingStorage crafting = target.read();
            if (initialCrafting == CraftingStorage.EMPTY) {
                initialCrafting = crafting;
            }
            ItemStack result;
            if (stonecutter) {
                result = StorageServerStub.assembleCraftingResult(player, crafting, true);
                // 与工作台一致：自动填充时本次点击预算 = 约一组产物 × 倍数，
                // 使 shift 点击只合成约一组而非把仓储材料一次合成光。
                // 守卫用 lockedId（随锁定本地更新）：session 是方法开头读取的局部值，
                // 同一分块内不会变化，若用它做守卫会在每次迭代重复重置预算。
                if (lockedId == null && result != null && !result.isEmpty() && initialCrafting.autoFill()) {
                    RecipeHolder<StonecutterRecipe> recipe =
                        StorageServerStub.selectedStonecutterRecipe(player, crafting);
                    if (recipe != null) {
                        lockedId = recipe.id();
                        remainingCrafts = StorageServerStub.craftBudget(result, multiplier);
                        StorageServerStub.lockTakeAllSession(playerId, sourcePos, lockedId, remainingCrafts);
                    }
                }
            } else {
                CraftingInput input = CraftingInput.of(3, 3, crafting.craftingInput());
                if (input.isEmpty()) {
                    int refilled = StorageServerStub.refillAndCollectSlots(player, target, initialCrafting);
                    refilledAccum |= refilled;
                    if (refilled != 0) {
                        continue;
                    }
                    StorageServerStub.unlockTakeAllRecipe(playerId, sourcePos);
                    player.containerMenu.broadcastChanges();
                    return new TakeAllResult(player.containerMenu.getCarried(), any, true, refilledAccum);
                }
                RecipeHolder<CraftingRecipe> locked = lockedId == null
                    ? null
                    : StorageServerStub.findTakeAllLockedRecipe(player, lockedId);
                if (locked == null) {
                    List<RecipeHolder<CraftingRecipe>> recipes = player.level().getRecipeManager()
                        .getRecipesFor(RecipeType.CRAFTING, input, player.level());
                    if (recipes.isEmpty()) {
                        int refilled = StorageServerStub.refillAndCollectSlots(player, target, initialCrafting);
                        refilledAccum |= refilled;
                        if (refilled != 0) {
                            continue;
                        }
                        StorageServerStub.unlockTakeAllRecipe(playerId, sourcePos);
                        player.containerMenu.broadcastChanges();
                        return new TakeAllResult(player.containerMenu.getCarried(), any, true, refilledAccum);
                    }
                    locked = recipes.getFirst();
                    lockedId = locked.id();
                    // 自动填充时本次点击预算 = 约一组产物 × 倍数，产物去背包或去存储均适用：
                    // 按产物堆叠上限折算，镐子等不可堆叠物品只合成 1 个，16 堆叠物品按 16 折算。
                    // 预算可能超过单次 RPC 上限（64 次），剩余次数存入会话供后续分块继续。
                    if (initialCrafting.autoFill()) {
                        ItemStack firstResult = locked.value().assemble(input, player.level().registryAccess());
                        remainingCrafts = StorageServerStub.craftBudget(firstResult, multiplier);
                    }
                    StorageServerStub.lockTakeAllSession(playerId, sourcePos, lockedId, remainingCrafts);
                } else if (!locked.value().matches(input, player.level())) {
                    // 剩余材料已不足首轮锁定的配方：自动填充时先补货重试，补不到再停止
                    int refilled = StorageServerStub.refillAndCollectSlots(player, target, initialCrafting);
                    refilledAccum |= refilled;
                    if (refilled != 0) {
                        continue;
                    }
                    StorageServerStub.unlockTakeAllRecipe(playerId, sourcePos);
                    player.containerMenu.broadcastChanges();
                    return new TakeAllResult(player.containerMenu.getCarried(), any, true, refilledAccum);
                }
                result = locked.value().assemble(input, player.level().registryAccess());
            }
            if (result == null || result.isEmpty()) {
                StorageServerStub.unlockTakeAllRecipe(playerId, sourcePos);
                player.containerMenu.broadcastChanges();
                return new TakeAllResult(player.containerMenu.getCarried(), any, true, refilledAccum);
            }
            PlaceResult place = crafting.toStorage()
                ? StorageServerStub.placeCraftingResultToStorageFirst(player, target, result)
                : StorageServerStub.placeCraftingResultToInventory(player, result);
            if (place == PlaceResult.NONE) {
                // 产物完全放不下（如指针异种且背包 / 仓储均无空间）：不消耗输入、
                // 不丢弃产物，立即截断——与原版「指针异种时拒绝取出」语义一致，
                // 避免凭空产出物品
                StorageServerStub.unlockTakeAllRecipe(playerId, sourcePos);
                player.containerMenu.broadcastChanges();
                return new TakeAllResult(player.containerMenu.getCarried(), any, true, refilledAccum);
            }
            if (place == PlaceResult.PARTIAL) {
                // 产物部分放入仓储（其余已在 placeCraftingResult 内丢弃）：消耗
                // 输入后截断，避免重复「合成→部分放入→丢弃」浪费材料且产出不可控
                StorageServerStub.unlockTakeAllRecipe(playerId, sourcePos);
                StorageServerStub.consumeCraftingInput(target, crafting, stonecutter);
                player.containerMenu.broadcastChanges();
                return new TakeAllResult(player.containerMenu.getCarried(), true, true, refilledAccum);
            }
            any = true;
            // 只消耗合成格内已有的原料合成一次
            boolean consumed = StorageServerStub.consumeCraftingInput(target, crafting, stonecutter);
            if (!consumed) {
                // 不消耗型配方（如催化剂 / 模具，或剩余物与输入完全相同）：继续
                // 循环只会无限产出相同产物，立即终止
                StorageServerStub.unlockTakeAllRecipe(playerId, sourcePos);
                player.containerMenu.broadcastChanges();
                return new TakeAllResult(player.containerMenu.getCarried(), true, true, refilledAccum);
            }
            // 自动填充：预算用尽即结束本次点击。预算耗尽前若合成格材料恰好也被抽干，
            // 先补一次料保持合成格有货，否则下一次点击会因空模板而无法补货
            if (remainingCrafts > 0) {
                remainingCrafts--;
                if (remainingCrafts <= 0) {
                    CraftingStorage afterBudget = target.read();
                    boolean emptyNow = stonecutter
                        ? afterBudget.stonecutterInput().isEmpty()
                        : afterBudget.craftingInput().stream().allMatch(ItemStack::isEmpty);
                    if (emptyNow && initialCrafting.autoFill()) {
                        int refilled = StorageServerStub.refillAndCollectSlots(player, target, initialCrafting);
                        refilledAccum |= refilled;
                    }
                    StorageServerStub.unlockTakeAllRecipe(playerId, sourcePos);
                    player.containerMenu.broadcastChanges();
                    return new TakeAllResult(player.containerMenu.getCarried(), true, true, refilledAccum);
                }
                StorageServerStub.updateTakeAllSessionRemaining(playerId, sourcePos, remainingCrafts);
            }
            // 合成格内自己的原料完全用尽后停止合成。开启自动填充时，仅当槽位
            // 完全耗尽才补 1 个供继续合成；不会在单次 shift 点击中把仓储内的
            // 同种材料全部合成完
            CraftingStorage after = target.read();
            boolean exhausted = stonecutter
                ? after.stonecutterInput().isEmpty()
                : after.craftingInput().stream().allMatch(ItemStack::isEmpty);
            if (exhausted) {
                int refilledSlots = 0;
                if (initialCrafting.autoFill()) {
                    refilledSlots = StorageServerStub.refillAndCollectSlots(player, target, initialCrafting);
                }
                refilledAccum |= refilledSlots;
                if (refilledSlots > 0) {
                    // 自动填充：合成格材料耗尽后补 1 个继续合成，产物拆批补满已有
                    // 堆叠；材料源枯竭（补不到）或产物放不下时停止
                    continue;
                }
                StorageServerStub.unlockTakeAllRecipe(playerId, sourcePos);
                player.containerMenu.broadcastChanges();
                return new TakeAllResult(player.containerMenu.getCarried(), true, true, refilledAccum);
            }
        }
        // 达到单次 RPC 分块上限（64 次合成）但预算或材料仍有余：保留会话，
        // 返回 done=false 让客户端继续下一个分块
        player.containerMenu.broadcastChanges();
        return new TakeAllResult(player.containerMenu.getCarried(), any, false, refilledAccum);
    }

    /** Shift 连续合成会话：锁定配方 + 本次点击剩余合成次数（0 表示未启用点击预算）。 */
    private record TakeAllSession(ResourceLocation recipeId, int remainingCrafts) {
    }

    /** 读取当前玩家在该源位置进行中的 Shift 连续合成会话；无会话时返回 null。 */
    @Nullable
    private static TakeAllSession lockedTakeAllSession(UUID playerId, long sourcePos) {
        return StorageServerStub.TAKE_ALL_RECIPE_LOCKS.getOrDefault(playerId, Map.of()).get(sourcePos);
    }

    /** 记录 Shift 连续合成会话，跨分块 RPC 保持。 */
    private static void lockTakeAllSession(UUID playerId, long sourcePos, ResourceLocation recipeId, int remainingCrafts) {
        StorageServerStub.TAKE_ALL_RECIPE_LOCKS.computeIfAbsent(playerId, ignored -> new HashMap<>())
            .put(sourcePos, new TakeAllSession(recipeId, remainingCrafts));
    }

    /** 更新会话剩余合成次数（配方不变）。 */
    private static void updateTakeAllSessionRemaining(UUID playerId, long sourcePos, int remainingCrafts) {
        Map<Long, TakeAllSession> sessions = StorageServerStub.TAKE_ALL_RECIPE_LOCKS.get(playerId);
        if (sessions != null) {
            TakeAllSession session = sessions.get(sourcePos);
            if (session != null) {
                sessions.put(sourcePos, new TakeAllSession(session.recipeId(), remainingCrafts));
            }
        }
    }

    /** 清除该玩家在该源位置的 Shift 连续合成会话。 */
    private static void unlockTakeAllRecipe(UUID playerId, long sourcePos) {
        Map<Long, TakeAllSession> sessions = StorageServerStub.TAKE_ALL_RECIPE_LOCKS.get(playerId);
        if (sessions != null) {
            sessions.remove(sourcePos);
            if (sessions.isEmpty()) {
                StorageServerStub.TAKE_ALL_RECIPE_LOCKS.remove(playerId);
            }
        }
    }

    /** 按配方 id 查找当前可用的合成配方持有者；不存在时返回 null。 */
    @Nullable
    @SuppressWarnings("unchecked")
    private static RecipeHolder<CraftingRecipe> findTakeAllLockedRecipe(ServerPlayer player, ResourceLocation recipeId) {
        Optional<? extends RecipeHolder<?>> holder = player.level().getRecipeManager().byKey(recipeId);
        if (holder.isEmpty() || !(holder.get().value() instanceof CraftingRecipe)) {
            return null;
        }
        return (RecipeHolder<CraftingRecipe>) holder.get();
    }

    /**
     * 在 input 当前可用的切石机配方候选列表（顺序与 {@link #craftingStonecutterRecipes} 一致，
     * 均为 {@code getRecipesFor(STONECUTTING)} 的结果）中定位 {@code stonecutterResult} 对应配方的索引。
     * JEI 转移时传入的产物可能对应该输入物品的任意候选配方（如 1 石头 → 石台阶 / 石砖…），
     * 需要把①的选中配方同步过去，③ 结果槽才会显示用户点转移的那个配方。
     *
     * @return 匹配配方的索引；产物为空 / 未匹配到时返回 0（首个候选，与手动放入输入后的默认一致）
     */
    private static int selectStonecutterResult(ServerPlayer player, ItemStack input, ItemStack stonecutterResult) {
        if (!stonecutterResult.isEmpty()) {
            List<RecipeHolder<StonecutterRecipe>> recipes = player.level().getRecipeManager()
                .getRecipesFor(RecipeType.STONECUTTING, new SingleRecipeInput(input), player.level());
            for (int i = 0; i < recipes.size(); i++) {
                ItemStack result = recipes.get(i).value().getResultItem(player.level().registryAccess());
                if (ItemStack.isSameItemSameComponents(result, stonecutterResult)) {
                    return i;
                }
            }
        }
        return 0;
    }

    /** 切石机当前选中配方的持有者；输入为空 / 无可用配方 / 选中索引越界时返回 null。 */
    @Nullable
    private static RecipeHolder<StonecutterRecipe> selectedStonecutterRecipe(
        ServerPlayer player,
        CraftingStorage crafting
    ) {
        ItemStack input = crafting.stonecutterInput();
        if (input.isEmpty()) {
            return null;
        }
        List<RecipeHolder<StonecutterRecipe>> recipes = player.level().getRecipeManager()
            .getRecipesFor(RecipeType.STONECUTTING, new SingleRecipeInput(input), player.level());
        int selected = crafting.stonecutterSelected();
        if (recipes.isEmpty() || selected < 0 || selected >= recipes.size()) {
            return null;
        }
        return recipes.get(selected);
    }

    /** 计算③/④ 当前配方产物（不消耗输入）；配方无效或产物为空返回 null。 */
    @Nullable
    private static ItemStack assembleCraftingResult(
        ServerPlayer player,
        CraftingStorage crafting,
        boolean stonecutter
    ) {
        if (stonecutter) {
            RecipeHolder<StonecutterRecipe> recipe =
                StorageServerStub.selectedStonecutterRecipe(player, crafting);
            if (recipe == null) {
                return null;
            }
            return recipe.value().assemble(
                new SingleRecipeInput(crafting.stonecutterInput()),
                player.level().registryAccess()
            );
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
     * 连续合成（自动填充 + 产物去背包）的放置：把产物拆批塞进背包，优先补满已有
     * 的同种堆叠，堆满 64 后继续放入下一个空槽，产物始终连续堆叠而不会留下
     * 60 60 60 之类的碎片；背包与指针都放不下时返回 {@link PlaceResult#NONE}。
     * 不写入仓储兜底，避免自动填充时产物无限流入存储。
     */
    private static PlaceResult placeCraftingResultToInventory(
        ServerPlayer player,
        ItemStack result
    ) {
        // 复制产物：后续 shrink / setItem 都会修改该栈，不能直接改动传入的 result
        ItemStack remaining = result.copy();
        if (remaining.isEmpty()) {
            return PlaceResult.FULL;
        }
        Inventory inventory = player.getInventory();
        int emptySlot = -1;
        // 只遍历主背包：getContainerSize() 含盔甲槽与副手槽，会把产物放进盔甲格
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            ItemStack stack = inventory.getItem(i);
            if (ItemStack.isSameItemSameComponents(stack, remaining)
                && stack.getCount() < stack.getMaxStackSize()) {
                int room = stack.getMaxStackSize() - stack.getCount();
                if (remaining.getCount() <= room) {
                    stack.grow(remaining.getCount());
                    return PlaceResult.FULL;
                }
                stack.setCount(stack.getMaxStackSize());
                remaining.shrink(room);
            } else if (stack.isEmpty() && emptySlot < 0) {
                emptySlot = i;
            }
        }
        if (emptySlot >= 0) {
            inventory.setItem(emptySlot, remaining);
            return PlaceResult.FULL;
        }
        ItemStack carried = player.containerMenu.getCarried();
        if (carried.isEmpty()) {
            player.containerMenu.setCarried(remaining);
            return PlaceResult.FULL;
        }
        if (ItemStack.isSameItemSameComponents(carried, remaining)
            && carried.getCount() + remaining.getCount() <= carried.getMaxStackSize()) {
            ItemStack merged = carried.copy();
            merged.grow(remaining.getCount());
            player.containerMenu.setCarried(merged);
            return PlaceResult.FULL;
        }
        return PlaceResult.NONE;
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
        int emptySlot = -1;
        // 只遍历主背包：getContainerSize() 含盔甲槽与副手槽，会把产物放进盔甲格
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            ItemStack stack = inventory.getItem(i);
            // 先补满已有同种堆叠（与 CraftingMenu.quickMoveStack 一致），避免新产物总是落到空槽
            // 而把背包排布拆成 60 60 60 之类的碎片
            if (ItemStack.isSameItemSameComponents(stack, result)
                && stack.getCount() < stack.getMaxStackSize()) {
                int room = stack.getMaxStackSize() - stack.getCount();
                if (result.getCount() <= room) {
                    stack.grow(result.getCount());
                    return PlaceResult.FULL;
                }
                stack.setCount(stack.getMaxStackSize());
                result.shrink(room);
            } else if (stack.isEmpty() && emptySlot < 0) {
                emptySlot = i;
            }
        }
        if (emptySlot >= 0) {
            // 所有已有同种堆叠均已补满（或不存在），把剩余产物放入首个空槽
            inventory.setItem(emptySlot, result);
            return PlaceResult.FULL;
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

    /**
     * Storage-first placement for the shift-craft destination option:
     * put the result into the attached storage first; any remainder falls
     * back to the normal inventory / carried placement path.
     */
    private static PlaceResult placeCraftingResultToStorageFirst(
        ServerPlayer player,
        StorageServerStub.CraftingTarget target,
        ItemStack result
    ) {
        StorageView view = target.view();
        if (view == null) {
            return StorageServerStub.placeCraftingResult(player, target, result);
        }
        int inserted = view.insert(result.copyWithCount(1), result.getCount());
        if (inserted == result.getCount()) {
            return PlaceResult.FULL;
        }
        if (inserted > 0) {
            ItemStack rest = result.copy();
            rest.shrink(inserted);
            return StorageServerStub.placeCraftingResult(player, target, rest);
        }
        return StorageServerStub.placeCraftingResult(player, target, result);
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
        // 每次合成每槽只消耗 1 个，剩余物按原版 ResultSlot.onTake 的规则安置
        CraftingInput input = CraftingInput.of(3, 3, crafting.craftingInput());
        List<ItemStack> remaining = target.player().level().getRecipeManager()
            .getRemainingItemsFor(RecipeType.CRAFTING, input, target.player().level());
        List<ItemStack> grid = new ArrayList<>(crafting.craftingInput());
        Inventory inventory = target.player().getInventory();
        StorageView view = target.view();
        boolean changed = false;
        for (int i = 0; i < grid.size(); i++) {
            ItemStack current = grid.get(i);
            if (current.isEmpty()) {
                continue;
            }
            ItemStack remainder = i < remaining.size() ? remaining.get(i) : ItemStack.EMPTY;
            ItemStack leftover = current.copy();
            leftover.shrink(1);
            ItemStack next;
            if (remainder.isEmpty()) {
                next = leftover.isEmpty() ? ItemStack.EMPTY : leftover;
            } else if (leftover.isEmpty()) {
                // 桶 / 碗等剩余物放回原槽位（与原版一致：槽位刚好清空时剩余物落在这里）
                next = remainder.copy();
            } else if (ItemStack.isSameItemSameComponents(leftover, remainder)) {
                // 剩余物与原料同种（催化剂 / 模具等不消耗型配方）：并入剩余量后放回，
                // 网格净变化为 0，调用方据此判定消耗未发生，避免无限产出
                next = remainder.copy();
                next.grow(leftover.getCount());
            } else {
                // 槽内还剩同类原料（如水桶还有 2 个）而剩余物不同种：原版此时把剩余物
                // 放进玩家背包，这里保留原料、剩余物交还玩家或存储
                next = leftover;
                StorageServerStub.returnCraftingRemainder(target.player(), crafting, view, remainder);
            }
            grid.set(i, next);
            // 内容或数量发生变化才算消耗。催化剂类配方净变化为 0 时返回 false，
            // 供调用方终止循环，避免无限产出
            if (!ItemStack.isSameItemSameComponents(next, current) || next.getCount() != current.getCount()) {
                changed = true;
            }
        }
        if (changed) {
            target.write(crafting.withCraftingInput(grid));
        }
        return changed;
    }

    /**
     * 归还合成剩余物（桶 / 碗等），保证其不会被吞掉。
     *
     * <p>按产物去向优先归还：产物存仓储时优先入仓储（可被端口流体再次灌装利用），
     * 否则优先回背包；首选处放不下的再放另一处。</p>
     */
    private static void returnCraftingRemainder(
        ServerPlayer player,
        CraftingStorage crafting,
        @Nullable StorageView view,
        ItemStack remainder
    ) {
        if (remainder.isEmpty()) {
            return;
        }
        if (!crafting.toStorage() || view == null) {
            StorageServerStub.returnItems(player, view, remainder, false);
            return;
        }
        int inserted = view.insert(remainder.copyWithCount(1), remainder.getCount());
        if (inserted >= remainder.getCount()) {
            return;
        }
        ItemStack rest = remainder.copy();
        rest.shrink(inserted);
        StorageServerStub.returnItems(player, view, rest, false);
    }

    /**
     * 执行一次自动补货并返回被补槽位的位掩码（bit0 为切石机输入槽，bit1~bit9 为合成格槽）。
     * 补货以 {@code template} 为模板：槽位耗尽（空）或只剩剩余物（如空桶）时，
     * 从背包 / 存储取 1 个同种物品补上，保持合成格内至少有 1 个物品可继续合成。
     */
    private static int refillAndCollectSlots(
        ServerPlayer player,
        StorageServerStub.CraftingTarget target,
        CraftingStorage template
    ) {
        CraftingStorage before = target.read();
        if (!StorageServerStub.autoRefillCrafting(player, target, template)) {
            return 0;
        }
        CraftingStorage after = target.read();
        int mask = 0;
        if (!after.stonecutterInput().isEmpty()
            && after.stonecutterInput().getCount() > before.stonecutterInput().getCount()) {
            mask |= 1;
        }
        List<ItemStack> beforeGrid = before.craftingInput();
        List<ItemStack> afterGrid = after.craftingInput();
        for (int i = 0; i < afterGrid.size(); i++) {
            if (i >= beforeGrid.size() || afterGrid.get(i).isEmpty()) {
                continue;
            }
            ItemStack afterStack = afterGrid.get(i);
            ItemStack beforeStack = beforeGrid.get(i);
            // 数量增加算补了料；内容被换成模板原料（先收走剩余物再补料，如空桶→水桶）
            // 同样算补过料，否则数量没变会被判成"没补到"而提前终止
            if (afterStack.getCount() > beforeStack.getCount()
                || !ItemStack.isSameItemSameComponents(afterStack, beforeStack)) {
                mask |= 1 << (i + 1);
            }
        }
        return mask;
    }

    /**
     * Auto-refill depleted crafting input slots when autofill is enabled.
     * The template provides the expected item per slot; a slot is refilled after it has
     * been fully consumed (empty), or after its remaining byproduct (such as an empty
     * bucket) has been handed back. Each such slot is topped up with a single item drawn
     * from the inventory first, then from the attached storage, so the crafting grid never
     * runs dry while material remains.
     */
    private static boolean autoRefillCrafting(
        ServerPlayer player,
        StorageServerStub.CraftingTarget target,
        CraftingStorage template
    ) {
        if (!template.autoFill()) {
            return false;
        }
        CraftingStorage current = target.read();
        Inventory inventory = player.getInventory();
        boolean changed = false;
        ItemStack templateStonecutter = template.stonecutterInput();
        ItemStack currentStonecutter = current.stonecutterInput();
        if (!templateStonecutter.isEmpty() && currentStonecutter.isEmpty()) {
            // 切石机输入完全耗尽才补 1 个，保持可继续合成
            int want = 1;
            int moved = StorageServerStub.transferMaterial(
                target,
                inventory,
                templateStonecutter,
                want
            );
            if (moved > 0) {
                current = current.withStonecutterInput(templateStonecutter.copyWithCount(moved));
                changed = true;
            }
        }
        List<ItemStack> templateGrid = template.craftingInput();
        List<ItemStack> currentGrid = current.craftingInput();
        for (int i = 0; i < templateGrid.size(); i++) {
            ItemStack templateStack = templateGrid.get(i);
            if (templateStack.isEmpty()) {
                continue;
            }
            ItemStack currentStack = i < currentGrid.size() ? currentGrid.get(i) : ItemStack.EMPTY;
            // 槽内是剩余物（如空桶）而非模板原料：先收走再补料。剩余物本身要按原版留在
            // 槽内，但槽位被它占着会补不进料，连续合成随即中断，故补料前先转交玩家 / 存储
            if (!currentStack.isEmpty()
                && !ItemStack.isSameItemSameComponents(currentStack, templateStack)) {
                StorageServerStub.returnItems(player, target.view(), currentStack, false);
                current = current.withCraftingSlot(i, ItemStack.EMPTY);
                currentStack = ItemStack.EMPTY;
                changed = true;
            }
            // 仅在槽位完全耗尽（空）时补 1 个，保持合成格内至少有 1 个物品；
            // 数量减少但未用尽（如 64→63）不补，避免边合成边补货消耗大量材料
            if (!currentStack.isEmpty()) {
                continue;
            }
            int want = 1;
            int moved = StorageServerStub.transferMaterial(
                target,
                inventory,
                templateStack,
                want
            );
            if (moved > 0) {
                current = current.withCraftingSlot(i, templateStack.copyWithCount(moved));
                changed = true;
            }
        }
        if (changed) {
            target.write(current);
        }
        return changed;
    }

    /**
     * 清空合成格（① 切石机输入 + ② 合成 9 宫格）：物品先送入存储站，
     * 存储放不下时回退到玩家背包，返回清空后的合成数据。
     */
    private static CraftingStorage clearCrafting(
        StorageServerStub.CraftingTarget target,
        CraftingStorage crafting
    ) {
        ItemStack stonecutterInput = crafting.stonecutterInput();
        List<ItemStack> grid = crafting.craftingInput();
        boolean hasAny = !stonecutterInput.isEmpty()
            || grid.stream().anyMatch(stack -> !stack.isEmpty());
        if (!hasAny) {
            return crafting;
        }
        StorageView view = target.view();
        ServerPlayer player = target.player();
        if (!stonecutterInput.isEmpty()) {
            StorageServerStub.returnItems(player, view, stonecutterInput, true);
        }
        for (ItemStack stack : grid) {
            if (!stack.isEmpty()) {
                StorageServerStub.returnItems(player, view, stack, true);
            }
        }
        List<ItemStack> emptyGrid = java.util.Collections.nCopies(CraftingStorage.CRAFTING_GRID_SIZE, ItemStack.EMPTY);
        CraftingStorage cleared = crafting
            .withStonecutterInput(ItemStack.EMPTY)
            .withCraftingInput(emptyGrid);
        target.write(cleared);
        return cleared;
    }

    /**
     * 归还物品的三级去向：主去处 → 次去处 → 掉落世界。
     *
     * <p>最后一级是必需的兜底：两处都放不下时若直接丢弃余量，物品就被凭空吞掉。
     * 主去处由 {@code storageFirst} 决定，次去处为另一处。</p>
     *
     * @param storageFirst 主去处是否为存储站；{@code false} 表示背包优先
     */
    private static void returnItems(
        ServerPlayer player,
        @Nullable StorageView view,
        ItemStack stack,
        boolean storageFirst
    ) {
        if (stack.isEmpty()) {
            return;
        }
        Inventory inventory = player.getInventory();
        int remaining = stack.getCount();
        if (storageFirst && view != null) {
            remaining -= view.insert(stack.copyWithCount(remaining), remaining);
        }
        if (remaining > 0) {
            remaining = StorageServerStub.giveBackToInventory(inventory, stack, remaining);
        }
        if (remaining > 0 && !storageFirst && view != null) {
            remaining -= view.insert(stack.copyWithCount(remaining), remaining);
        }
        if (remaining > 0) {
            Block.popResource(player.level(), player.blockPosition(), stack.copyWithCount(remaining));
        }
    }

    /**
     * JEI 转移：把配方输入放入 ①/② 输入槽，材料从玩家背包 + 存储扣取（与 JEI 转移语义一致）。
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
        crafting = StorageServerStub.clearCrafting(target, crafting);
        if (stonecutter) {
            // ①：默认只放 1 个（够出一次产物）；maxTransfer（Shift 点击）才把背包 + 存储中
            // 的同种材料全部转进①（受物品上限约束）。与 ② 合成格的 JEI 转移语义一致：
            // 材料可来自存储（板条箱/终端存储），而不仅限于背包。
            if (inputs.isEmpty() || inputs.getFirst().isEmpty()) {
                return false;
            }
            ItemStack wanted = inputs.getFirst();
            int targetCount = maxTransfer ? wanted.getMaxStackSize() : 1;
            int moved = StorageServerStub.transferMaterial(
                target, inventory, wanted, targetCount
            );
            if (moved <= 0) {
                return false;
            }
            int selected = StorageServerStub.selectStonecutterResult(player, wanted, stonecutterResult);
            target.write(crafting
                .withStonecutterInput(wanted.copyWithCount(moved))
                .withStonecutterSelected(selected));
            inventory.setChanged();
            player.containerMenu.broadcastChanges();
            return true;
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
                    target, inventory, wanted, current, current.getCount() + requested
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
            // 只统计主背包：与 transferMaterialExact 的取用范围一致，否则「够用」判定会与实际扣料不符
            for (int index = 0; index < Inventory.INVENTORY_SIZE; index++) {
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
            // 桶装流体：空容器 + 端口流体可现场盛装的量同样要计入，
            // 否则预检会判定流体桶不足而直接放弃本轮，转移看起来毫无反应
            available += StorageServerStub.countProducibleContainers(view, inventory, wanted);
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
        int requestedCount
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
        int fromFluid = 0;
        // 只从主背包取料：getContainerSize() 含盔甲槽，会把身上装备当材料扣掉
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
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
            // 现成物品取完仍不足时，才用空容器 + 端口流体现场盛装；
            // 盛装量从 needed 扣减后一并计入，保证与真实物品合计仍可凑满一组
            int produced = StorageServerStub.produceFilledContainer(
                target.player(), target.view(), wanted, needed - moved
            );
            if (produced > 0) {
                moved += produced;
                fromFluid += produced;
            }
        }
        if (moved < needed) {
            // 材料不足一组：回滚已取物品（背包部分放回背包，存储部分放回存储）
            int inventoryPart = moved - fromStorage - fromFluid;
            if (inventoryPart > 0) {
                // 背包放不下的余量转入存储，最终仍放不下则掉落世界，避免回滚途中吞物品
                StorageServerStub.returnItems(
                    target.player(), view, wanted.copyWithCount(inventoryPart), false
                );
            }
            // 不变式：fromStorage 只在上方 view != null 的取存储分支内累加，
            // 故 fromStorage > 0 蕴含 view 非空，此处无需再判空。
            if (fromStorage > 0) {
                StorageServerStub.returnItems(target.player(), view, wanted.copyWithCount(fromStorage), true);
            }
            // 现场盛装出的部分退回存储为物品（等量于消耗的空容器 + 流体，不会凭空增减）
            if (fromFluid > 0) {
                StorageServerStub.returnItems(target.player(), view, wanted.copyWithCount(fromFluid), true);
            }
            return 0;
        }
        return moved;
    }

    /**
     * 尝试用「空容器 + 流体端口中的同种流体」现场盛装出装有流体的容器。
     *
     * <p>用于 JEI 填充合成：配方要求流体桶时，只要有空桶并连着存有该流体的端口，
     * 就现场装桶，而不必预先存有成品桶。空桶可来自玩家背包或存储。</p>
     *
     * @param player    玩家（用于取空容器与兜底归还）
     * @param view      当前存储视图
     * @param wanted    目标物品（需为装有流体的容器）
     * @param needed    需要数量
     * @return 实际盛装出的数量
     */
    private static int produceFilledContainer(
        ServerPlayer player,
        @Nullable StorageView view,
        ItemStack wanted,
        int needed
    ) {
        if (view == null || needed <= 0) {
            return 0;
        }
        FluidStack content = StorageServerStub.fluidContentOf(wanted);
        if (content.isEmpty()) {
            return 0;
        }
        ItemStack emptyContainer = StorageServerStub.emptyContainerOf(wanted);
        if (emptyContainer.isEmpty()) {
            return 0;
        }
        Inventory inventory = player.getInventory();
        int perUnit = content.getAmount();
        int count = Math.min(needed, StorageServerStub.countProducibleContainers(view, inventory, wanted));
        if (count <= 0) {
            return 0;
        }
        UUID storageId = view.primary().getId();
        // 两步都先模拟确认可行，再动手：流体抽得出、空容器扣得到。
        // 不先模拟就 EXECUTE，任何一步中途失败都只能回滚，回滚疏漏就会凭空增减物品
        if (StorageFluidRegistry.drain(storageId, content, perUnit * count, true) < perUnit * count) {
            return 0;
        }
        if (!StorageServerStub.hasEnoughContainers(inventory, view, emptyContainer, count)) {
            return 0;
        }
        // 先扣空容器再抽流体：容器扣取失败时流体尚未改动，只需把已扣的容器还回去；
        // 反过来先抽流体的话，失败回滚就得处理「流体灌回」，复杂度与风险都更高
        int removed = 0;
        while (removed < count && StorageServerStub.consumeOne(inventory, view, emptyContainer)) {
            removed++;
        }
        if (removed < count) {
            StorageServerStub.giveBackContainers(player, view, emptyContainer, removed);
            return 0;
        }
        int drained = StorageFluidRegistry.drain(storageId, content, perUnit * count);
        if (drained < perUnit * count) {
            // 模拟通过后实际抽取仍不足（并发改动）：容器全数还回，已抽出的流体灌回
            StorageServerStub.giveBackContainers(player, view, emptyContainer, count);
            StorageServerStub.refillFluid(storageId, content, drained);
            return 0;
        }
        return count;
    }

    /**
     * 判断玩家背包 + 该存储中合计是否有足够数量的某种空容器。
     *
     * <p>与 {@link #consumeOne} 的取用范围一致，只在两者都统计到的前提下才允许实际扣取。</p>
     */
    private static boolean hasEnoughContainers(
        @Nullable Inventory inventory,
        @Nullable StorageView view,
        ItemStack resource,
        int count
    ) {
        long available = 0;
        if (inventory != null) {
            available += StorageServerStub.countInInventory(inventory, resource);
        }
        if (view != null) {
            available += StorageServerStub.countInView(view, resource);
        }
        return available >= count;
    }

    /**
     * 非破坏性判断是否存在可用空容器（鼠标指针 / 背包 / 存储本体）。
     *
     * <p>取用范围与 {@link #consumeEmptyContainer} 保持一致；提示优先级需要在不消耗容器的
     * 前提下先判定「有没有桶」，故单独提供。</p>
     */
    private static boolean hasEmptyContainer(ServerPlayer player, StorageView view, ItemStack emptyContainer) {
        ItemStack carried = player.containerMenu.getCarried();
        if (!carried.isEmpty() && ItemStack.isSameItemSameComponents(carried, emptyContainer)) {
            return true;
        }
        return StorageServerStub.hasEnoughContainers(player.getInventory(), view, emptyContainer, 1);
    }

    /** 把 count 个容器还回玩家背包，放不下的再放回该存储，最终仍放不下则掉落世界。 */
    private static void giveBackContainers(
        ServerPlayer player,
        @Nullable StorageView view,
        ItemStack resource,
        int count
    ) {
        StorageServerStub.returnItems(player, view, resource.copyWithCount(Math.max(0, count)), false);
    }

    /**
     * 把流体灌回该存储的端口；端口装不下时按实际容量尽力而为。
     *
     * <p>灌回的是先前抽出、因后续步骤失败而必须归还的流体，其原端口可能已空，
     * 故用允许空端口的 {@link StorageFluidRegistry#findRefillTarget}。</p>
     */
    private static void refillFluid(UUID storageId, FluidStack fluid, int amountMb) {
        int remaining = amountMb;
        while (remaining > 0) {
            IFluidHandler acceptor = StorageFluidRegistry.findRefillTarget(storageId, fluid);
            if (acceptor == null) {
                return;
            }
            int filled = acceptor.fill(fluid.copyWithAmount(remaining), IFluidHandler.FluidAction.EXECUTE);
            if (filled <= 0) {
                return;
            }
            remaining -= filled;
        }
    }

    /**
     * 不改动任何状态地算出「空容器 + 端口流体」最多能盛装出多少个 wanted。
     *
     * <p>供取材预检使用：不把可盛装的量算进去，预检会认为流体桶不足而直接放弃转移。</p>
     *
     * @param view      当前存储视图；可为 null
     * @param inventory 玩家背包；可为 null
     * @param wanted    目标物品（需为装有流体的容器）
     * @return 可盛装数量；不具备条件时返回 0
     */
    private static int countProducibleContainers(
        @Nullable StorageView view,
        @Nullable Inventory inventory,
        ItemStack wanted
    ) {
        if (view == null) {
            return 0;
        }
        FluidStack content = StorageServerStub.fluidContentOf(wanted);
        if (content.isEmpty()) {
            return 0;
        }
        ItemStack emptyContainer = StorageServerStub.emptyContainerOf(wanted);
        if (emptyContainer.isEmpty()) {
            return 0;
        }
        long containers = StorageServerStub.countInView(view, emptyContainer);
        if (inventory != null) {
            containers += StorageServerStub.countInInventory(inventory, emptyContainer);
        }
        if (containers <= 0) {
            return 0;
        }
        int availableFluid = StorageServerStub.countFluidInStorage(view, content);
        return (int) Math.min(containers, availableFluid / content.getAmount());
    }

    /** 该储存在端口中存放的指定流体总量（mB）。 */
    private static int countFluidInStorage(StorageView view, FluidStack fluid) {
        for (FluidEntry entry : StorageFluidRegistry.collect(view.primary().getId())) {
            if (entry.amount() > 0 && FluidStack.isSameFluidSameComponents(entry.icon(), fluid)) {
                return entry.amount();
            }
        }
        return 0;
    }

    /** 从玩家背包（优先）或存储中取走一个 resource；取到返回 true。 */
    private static boolean consumeOne(
        @Nullable Inventory inventory,
        @Nullable StorageView view,
        ItemStack resource
    ) {
        if (inventory != null) {
            for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
                ItemStack stack = inventory.getItem(i);
                if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, resource)) {
                    continue;
                }
                stack.shrink(1);
                if (stack.isEmpty()) {
                    inventory.setItem(i, ItemStack.EMPTY);
                }
                return true;
            }
        }
        if (view != null) {
            for (int index = 0; index < view.size(); index++) {
                if (view.amount(index) > 0
                    && ItemStack.isSameItemSameComponents(view.resource(index), resource)
                    && view.extract(index, 1) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 装有流体的容器的内容物；非流体容器或空容器返回空。 */
    private static FluidStack fluidContentOf(ItemStack filled) {
        IFluidHandlerItem handler = FluidUtil.getFluidHandler(filled.copyWithCount(1)).orElse(null);
        return handler == null
            ? FluidStack.EMPTY
            : handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
    }

    /** 统计视图中与给定物品匹配的总量。 */
    private static int countInView(StorageView view, ItemStack resource) {
        long total = 0;
        for (int index = 0; index < view.size(); index++) {
            if (view.amount(index) > 0 && ItemStack.isSameItemSameComponents(view.resource(index), resource)) {
                total += view.amount(index);
            }
        }
        return (int) Math.min(total, Integer.MAX_VALUE);
    }

    /** 把 count 个 wanted 放回玩家背包，返回未能放入的剩余数量。 */
    private static int giveBackToInventory(Inventory inventory, ItemStack wanted, int count) {
        if (count <= 0) {
            return 0;
        }
        ItemStack toReturn = wanted.copy();
        toReturn.setCount(count);
        // 只遍历主背包（含快捷栏）：getContainerSize() 还含 4 个盔甲槽与副手槽，
        // 原版 Inventory#add 也只走 items（见 getFreeSlot / getSlotWithRemainingSpace），
        // 否则物品会被塞进盔甲槽或与身上装备堆叠
        for (int i = 0; i < Inventory.INVENTORY_SIZE && !toReturn.isEmpty(); i++) {
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
        CraftingTarget target,
        Inventory inventory,
        ItemStack wanted,
        int maxCount
    ) {
        int moved = StorageServerStub.transferFromInventory(inventory, wanted, maxCount);
        if (moved >= maxCount || maxCount <= 0) {
            return moved;
        }
        if (!ItemStack.EMPTY.isEmpty() && !ItemStack.isSameItemSameComponents(ItemStack.EMPTY, wanted)) {
            return moved;
        }
        StorageView view = target.view();
        if (view == null) {
            return moved;
        }
        // 背包取完后从存储补足差量（空槽或已有同种均适用）
        int space = maxCount - (ItemStack.EMPTY.isEmpty() ? 0 : ItemStack.EMPTY.getCount()) - moved;
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
        // 现成物品取完仍不足时，才用空容器 + 端口流体现场盛装
        if (moved < maxCount) {
            moved += StorageServerStub.produceFilledContainer(target.player(), view, wanted, maxCount - moved);
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
        int maxCount
    ) {
        if (ItemStack.EMPTY.isEmpty()) {
            // 空槽：从背包转移所有同种物品（受 maxCount 上限约束），不只放一个
            int moved = 0;
            for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
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
        if (!ItemStack.isSameItemSameComponents(ItemStack.EMPTY, wanted)) {
            return 0;
        }
        int space = maxCount - ItemStack.EMPTY.getCount();
        if (space <= 0) {
            return 0;
        }
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
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
        // 普通文本搜索由客户端（TerminalRemoteOverlay）在获得全量内容缓存后按本地化
        // 名称二次过滤；服务端只处理 @ namespace 与 # tag 前缀（与 reorder 一致）
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
        // 桶装流体优先自动倾倒进能接收它的端口，空容器回背包
        int poured = StorageServerStub.pourIntoFluidPort(player, view, carried, carried.getCount());
        if (poured > 0) {
            player.containerMenu.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
            return new InteractionResult(player.containerMenu.getCarried(), true);
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
        HolderLookup.Provider registries = player.level().registryAccess();
        StorageView view = new StorageView(StorageServerStub.terminalStorages(player, targetId), List.of());
        if (view.size() <= 0) {
            return new InteractionResult(ItemStack.EMPTY, false);
        }
        PlayerSetting setting = PlayerSettings.getSetting(registries, playerId);
        StorageSetting storage = setting.storage();
        SortOptions options = new SortOptions(storage.getSort(), storage.getOrder());
        // 取物路径只遍历真实物品槽位：流体伪槽位拿去索引 StorageView 会越界
        IntList order = StorageServerStub.createItemOrder(view, options, setting.listed());
        ItemStack extracted = ItemStack.EMPTY;
        for (int i = 0; i < order.size() && extracted.isEmpty(); i++) {
            int index = order.getInt(i);
            long stackAmount = view.amount(index);
            if (stackAmount <= 0) {
                continue;
            }
            int take = (int) Math.min(Math.min(amount, view.resource(index).getMaxStackSize()), stackAmount);
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
            // 桶装流体：JEI 填充合成按桶识别配方时，用存储中的空桶 + 端口流体现场盛装
            int produced = StorageServerStub.produceFilledContainer(
                player,
                new StorageServerStub.StorageView(storages, List.of()),
                resource,
                required
            );
            if (produced > 0) {
                player.getInventory().add(resource.copyWithCount(produced));
                StorageServerStub.addWithdrawn(withdrawn, resource, produced);
                required -= produced;
                if (required <= 0) {
                    continue;
                }
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
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
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
        stub.undoRecords.addFirst(new UndoRecord(new HashMap<>(moved)));
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
        int slot,
        boolean pour
    ) {
        Inventory inventory = player.getInventory();
        if (slot < 0 || slot >= Inventory.INVENTORY_SIZE) {
            return 0;
        }
        ItemStack stack = inventory.getItem(slot);
        if (stack.isEmpty()) {
            return 0;
        }
        // 桶装流体优先自动倾倒，空容器留在背包。仅左键倾倒，
        // 右键保持物品行为，让流体桶能作为普通物品存入
        int poured = pour
                     ? StorageServerStub.pourIntoFluidPort(player, view, stack, stack.getCount())
                     : 0;
        if (poured > 0) {
            if (stack.isEmpty()) {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
            return poured;
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

    /**
     * 让玩家播放主手摆动动画。
     *
     * <p>{@code Player#drop} 只在客户端摆动（{@code level().isClientSide} 分支），而丢弃走的是
     * 服务端 RPC，因此不会自动播放手部动画。这里显式摆动：{@code updateSelf=true} 使动画包
     * 既广播给追踪玩家、也发给玩家自己，本地第一人称才会摆手。</p>
     */
    private static void swingMainHand(ServerPlayer player) {
        player.swing(InteractionHand.MAIN_HAND, true);
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
        StorageServerStub.swingMainHand(player);
        return true;
    }

    public static void onContentsChanged(UUID storageId) {
        StorageBlockRegistry.notifyContentsChanged(storageId);
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
        StorageServerStub.TAKE_ALL_RECIPE_LOCKS.remove(playerId);
    }

    public static void clear() {
        StorageServerStub.STUBS.clear();
        StorageServerStub.REMOTE_STORAGES.clear();
        StorageServerStub.TAKE_ALL_RECIPE_LOCKS.clear();
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
     * 从终端连接的目标存储取出一个物品，与创造模式 {@link #terminalExtractFirst} 一致，
     * 按玩家绑定的存储界面排序（SortMode + OrderMode）取第一个可取槽位。
     * 目标不可达或存储为空时返回空栈。
     */
    public static ItemStack extractFromTerminal(ServerPlayer player, UUID targetId, int amount) {
        HolderLookup.Provider registries = player.level().registryAccess();
        StorageView view = new StorageView(StorageServerStub.terminalStorages(player, targetId), List.of());
        if (view.size() <= 0) {
            return ItemStack.EMPTY;
        }
        PlayerSetting setting = PlayerSettings.getSetting(registries, player.getGameProfile().getId());
        StorageSetting storage = setting.storage();
        SortOptions options = new SortOptions(storage.getSort(), storage.getOrder());
        // 取物路径只遍历真实物品槽位：流体伪槽位拿去索引 StorageView 会越界
        IntList order = StorageServerStub.createItemOrder(view, options, setting.listed());
        for (int i = 0; i < order.size(); i++) {
            int index = order.getInt(i);
            long stackAmount = view.amount(index);
            if (stackAmount <= 0) {
                continue;
            }
            int take = (int) Math.min(Math.min(amount, view.resource(index).getMaxStackSize()), stackAmount);
            int got = view.extract(index, take);
            if (got > 0) {
                ItemStack extracted = view.resource(index).copyWithCount(got);
                player.getInventory().setChanged();
                player.containerMenu.broadcastChanges();
                return extracted;
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
                || !(args[1] instanceof UUID)
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

    public record SyncResult(
        long version,
        double fullness,
        List<StackUpdate> updates,
        List<FluidEntry> fluids
    ) {
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

    /**
     * 流体交互失败的原因，随 {@link InteractionResult} 回传由仓储界面渲染成浮层提示。
     *
     * <p>不用 {@code displayClientMessage}：那是动作栏消息，会被打开的仓储界面盖住，
     * 玩家看不到任何反馈。</p>
     */
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

    /**
     * {@link #craftingTakeAll} 分块合成的结果。
     *
     * @param carried       本次调用结束时的指针物品
     * @param changed       本次调用是否合成了至少一个产物
     * @param done          本次调用是否已自然终止（材料耗尽 / 产物无处可放 / 不消耗型配方）；
     *                       {@code false} 表示达到分块上限，客户端应继续调用
     * @param refilledSlots 补货位掩码：bit0 为切石机输入槽，bit1~bit9 为合成格 9 槽；0 表示未补货
     */
    public record TakeAllResult(ItemStack carried, boolean changed, boolean done, int refilledSlots) {

        public static final StreamCodec<RegistryFriendlyByteBuf, TakeAllResult> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC,
            TakeAllResult::carried,
            ByteBufCodecs.BOOL,
            TakeAllResult::changed,
            ByteBufCodecs.BOOL,
            TakeAllResult::done,
            ByteBufCodecs.VAR_INT,
            TakeAllResult::refilledSlots,
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

    /** 含流体伪槽位的排序结果，供仓储 / 终端界面渲染使用。 */
    private static IntList createOrder(
        StorageView view,
        SortOptions options,
        String search,
        List<CategoryEntry> categories
    ) {
        return StorageServerStub.createOrder(view, options, search, categories, true);
    }

    /**
     * 汇总排序结果；是否并入流体伪槽位由调用方决定。
     *
     * @param includeFluids 是否并入流体伪槽位；仅供界面渲染传 {@code true}，取物路径传 {@code false}
     */
    private static IntList createOrder(
        StorageView view,
        SortOptions options,
        String search,
        List<CategoryEntry> categories,
        boolean includeFluids
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

        // 流体按 1 mB = 1 个物品折算成等量物品数参与排序，并按分类逐条过滤
        // （流体分类判定流体、命名空间分类按流体命名空间判定，其余分类默认不匹配流体）
        if (includeFluids) {
            StorageServerStub.addFluidEntries(entries, view, search, requiresName, categories);
        }
        entries.sort(StorageServerStub.getComparator(options));

        IntArrayList order = new IntArrayList(entries.size());
        for (OrderEntry entry : entries) {
            order.add(entry.index());
        }
        return order;
    }

    /**
     * 只含真实物品槽位的排序结果，供服务端取出路径使用。
     *
     * <p>流体伪槽位编号自 {@link StorageFluidRegistry#FLUID_SLOT_BASE} 起，远大于真实槽位数，
     * 而取出路径会拿排序结果直接索引 {@link StorageView}，混入伪槽位必然越界。需要取物的
     * 调用方一律用本方法，从源头避免「每个消费者都得记得过滤」这一隐患。</p>
     */
    private static IntList createItemOrder(
        StorageView view,
        SortOptions options,
        List<CategoryEntry> categories
    ) {
        return StorageServerStub.createOrder(view, options, "", categories, false);
    }

    /**
     * 把已连接端口的流体折算为排序条目追加进列表。
     *
     * <p>折算规则（见 #4792）：按数量排序时 <b>1 mB 相当于 1 个物品</b>，
     * 故直接以 mB 数值参与比较。</p>
     *
     * @param entries      物品排序条目（会被就地追加）
     * @param view         当前存储视图
     * @param search       搜索词
     * @param requiresName 是否需要名称（名称排序或普通文本搜索）
     * @param categories   玩家列出的分类（含模式，UNLIMITED 不过滤）
     */
    private static void addFluidEntries(
        List<OrderEntry> entries,
        StorageView view,
        String search,
        boolean requiresName,
        List<CategoryEntry> categories
    ) {
        List<FluidEntry> fluids = StorageFluidRegistry.collect(view.primary().getId());
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
            ResourceLocation id = BuiltInRegistries.FLUID.getKey(entry.icon().getFluid());
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
                StorageFluidRegistry.FLUID_SLOT_BASE + index,
                // 按 #4792：数量排序时 1 mB 相当于 1 个物品
                entry.amount(),
                id,
                requiresName ? entry.icon().getHoverName().getString() : ""
            ));
        }
    }

    /**
     * 左键点击流体格：指针上拿着流体容器时倒进去，否则用空桶装出一桶该流体。
     *
     * <p>流体格是双向的：空桶装、满桶倒。指针被非空容器物品占用且倒不进去时不做任何改动，
     * 避免无谓消耗背包 / 存储里的空桶。右键不走这里，而走原有物品行为。</p>
     *
     * @param player 玩家
     * @param view   当前存储视图
     * @param fluid  被点击的流体（客户端随点击上报，按身份匹配而非下标）
     * @param button 鼠标键（0 左键整叠，1 右键 1 个），与物品格的放入语义一致
     * @return 是否发生改动，以及失败原因（供界面提示）
     */
    private static FluidOutcome takeFluidBucket(ServerPlayer player, StorageView view, FluidStack fluid, int button) {
        // 指针上拿着装有流体的容器：这一下是「倒进去」。倒不进去（如仓储没有可接收的
        // 端口）时继续往下走，由取出的分支判断指针是否可接收产物
        ItemStack carried = player.containerMenu.getCarried();
        if (!carried.isEmpty()) {
            int amount = button == 0 ? carried.getCount() : 1;
            if (StorageServerStub.pourIntoFluidPort(player, view, carried, amount) > 0) {
                if (carried.isEmpty()) {
                    player.containerMenu.setCarried(ItemStack.EMPTY);
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
        if (player.containerMenu.getCarried().isEmpty()) {
            player.containerMenu.setCarried(filled.stack());
        } else if (!player.addItem(filled.stack())
            && view.insert(filled.stack().copyWithCount(1), 1) <= 0) {
            Block.popResource(player.level(), player.blockPosition(), filled.stack());
        }
        return FluidOutcome.CHANGED;
    }

    /**
     * Shift 点击流体格：装出一桶并直接放进玩家背包，不经过鼠标指针。
     *
     * @param player 玩家
     * @param view   当前存储视图
     * @param fluid  被点击的流体（客户端随点击上报）
     * @return 是否发生改动，以及失败原因（供界面提示）
     */
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

    /** 一次流体交互的结果：是否改动 + 失败原因（由界面渲染成浮层提示）。 */
    private record FluidOutcome(boolean changed, FluidNotice notice) {
        private static final FluidOutcome CHANGED = new FluidOutcome(true, FluidNotice.NONE);

        private static FluidOutcome failed(FluidNotice notice) {
            return new FluidOutcome(false, notice);
        }
    }

    /** 装桶结果：成功时给出成品桶，失败时给出原因。 */
    private record FilledBucket(ItemStack stack, FluidNotice notice) {
        private static FilledBucket failed(FluidNotice notice) {
            return new FilledBucket(ItemStack.EMPTY, notice);
        }
    }

    /**
     * 用空桶从流体格装出一桶流体，空桶可取自指针、背包或存储本体。
     *
     * <p>只负责消耗空桶与抽取流体；成品桶的去向由调用方决定。</p>
     *
     * <p>按流体身份而非槽位下标定位：客户端点击与服务端处理之间列表可能变化
     * （端口被拆、区块卸载、新流体接入），按下标会取到另一种流体。</p>
     *
     * @param player        玩家
     * @param view          当前存储视图
     * @param fluid         被点击的流体；为空表示客户端未上报流体身份
     * @param intoInventory 产物是否进背包（Shift 路径）。为 {@code false} 时产物要落在指针上，
     *                      因此指针必须为空或正拿着所需空桶
     * @return 装出的一桶流体；失败时返回空（并按需给出提示）
     */
    private static FilledBucket fillBucketFromStorage(
        ServerPlayer player,
        StorageView view,
        FluidStack fluid,
        boolean intoInventory
    ) {
        FluidEntry entry = StorageFluidRegistry.find(view.primary().getId(), fluid);
        if (entry == null) {
            return FilledBucket.failed(FluidNotice.NONE);
        }
        FluidStack target = entry.icon().copyWithAmount(FluidType.BUCKET_VOLUME);
        ItemStack filled = FluidUtil.getFilledBucket(target);
        if (filled.isEmpty()) {
            return FilledBucket.failed(FluidNotice.NONE);
        }
        ItemStack emptyContainer = StorageServerStub.emptyContainerOf(filled);
        if (emptyContainer.isEmpty()) {
            return FilledBucket.failed(FluidNotice.NONE);
        }
        // 指针路径：产物要落在指针上，指针被别的物品占着（例如已拿着一桶水）就无处安置，
        // 此时不执行取水，避免无谓消耗背包/存储里的空桶。此判断先于下面的提示，
        // 否则指针被占用时还会给出与本意无关的「储量不足」提示
        if (!intoInventory) {
            ItemStack cursor = player.containerMenu.getCarried();
            if (!cursor.isEmpty() && !ItemStack.isSameItemSameComponents(cursor, emptyContainer)) {
                return FilledBucket.failed(FluidNotice.NONE);
            }
        }
        // 提示优先级：先判「有没有空桶」，再判「储量够不够」。
        // 例如只剩 250 mB 且身上没桶时，玩家更该看到「需要空桶」而非「流体不足一桶」。
        // 这里用非破坏性判定，避免为了报错而先把容器消耗掉再回滚
        if (!StorageServerStub.hasEmptyContainer(player, view, emptyContainer)) {
            return FilledBucket.failed(FluidNotice.BUCKET_MISSING);
        }
        if (entry.amount() < FluidType.BUCKET_VOLUME) {
            return FilledBucket.failed(FluidNotice.NOT_ENOUGH);
        }
        // 空容器可以来自鼠标指针、玩家背包或存储本体：仓储界面里三者都应可用
        if (!StorageServerStub.consumeEmptyContainer(player, view, emptyContainer)) {
            // 判定与实际取用之间容器被消耗（并发）：仍按缺桶提示
            return FilledBucket.failed(FluidNotice.BUCKET_MISSING);
        }
        // 确认能真正抽出，否则把空容器还回去，避免凭空吞桶
        if (StorageFluidRegistry.drain(view.primary().getId(), target, FluidType.BUCKET_VOLUME)
            < FluidType.BUCKET_VOLUME) {
            StorageServerStub.giveEmptiedContainer(player, emptyContainer);
            return FilledBucket.failed(FluidNotice.NOT_ENOUGH);
        }
        // 取水音效：倾倒由 FluidUtil 自行播放，取出的这条路径需自行补上
        StorageServerStub.playBucketSound(player, target, SoundActions.BUCKET_FILL);
        return new FilledBucket(filled, FluidNotice.NONE);
    }

    /**
     * 反推装有流体的容器对应的空容器（把内容物倒空后剩下的物品）。
     *
     * <p>用容器自身的流体能力推导，因此不限于原版桶。</p>
     *
     * @param filled 装有流体的容器
     * @return 空容器；无法推导时返回空
     */
    private static ItemStack emptyContainerOf(ItemStack filled) {
        IFluidHandlerItem handler = FluidUtil.getFluidHandler(filled.copyWithCount(1)).orElse(null);
        if (handler == null) {
            return ItemStack.EMPTY;
        }
        handler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
        return handler.getContainer();
    }

    /**
     * 从鼠标指针、玩家背包或存储本体中取走一个指定的空容器。
     *
     * @param player         玩家
     * @param view           当前存储视图
     * @param emptyContainer 需要的空容器
     * @return 是否成功取走
     */
    private static boolean consumeEmptyContainer(ServerPlayer player, StorageView view, ItemStack emptyContainer) {
        // 鼠标指针：优先使用玩家正拿着的容器
        ItemStack carried = player.containerMenu.getCarried();
        if (!carried.isEmpty() && ItemStack.isSameItemSameComponents(carried, emptyContainer)) {
            carried.shrink(1);
            player.containerMenu.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
            return true;
        }
        Inventory inventory = player.getInventory();
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, emptyContainer)) {
                continue;
            }
            stack.shrink(1);
            if (stack.isEmpty()) {
                inventory.setItem(i, ItemStack.EMPTY);
            }
            return true;
        }
        for (int i = 0; i < view.size(); i++) {
            if (view.amount(i) <= 0 || !ItemStack.isSameItemSameComponents(view.resource(i), emptyContainer)) {
                continue;
            }
            if (view.extract(i, 1) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * 把桶装流体自动倾倒进仓储的流体端口，一次可倒多桶。
     *
     * <p>只有存在能接收该流体的端口（同种流体或空端口）时才会倾倒；
     * 否则返回 0，由调用方按普通物品存入桶。空容器直接交还玩家。</p>
     *
     * @param player    玩家
     * @param view      当前存储视图
     * @param stack     待倾倒的桶装流体（会被就地扣减）
     * @param maxAmount 本次最多倾倒的桶数
     * @return 实际倾倒的桶数
     */
    private static int pourIntoFluidPort(
        ServerPlayer player,
        StorageView view,
        ItemStack stack,
        int maxAmount
    ) {
        if (stack.isEmpty() || maxAmount <= 0) {
            return 0;
        }
        FluidStack content = FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY);
        if (content.isEmpty()) {
            return 0;
        }
        IFluidHandler acceptor = StorageFluidRegistry.findAcceptor(view.primary().getId(), content);
        if (acceptor == null) {
            return 0;
        }
        int poured = 0;
        while (poured < maxAmount && !stack.isEmpty()) {
            ItemStack single = stack.copyWithCount(1);
            // 先模拟一次，确认端口确实装得下再真正倾倒。
            // 传 null 抑制 FluidUtil 的逐桶音效，改为整批只播一次，避免批量操作时声音叠加
            if (!FluidUtil.tryEmptyContainer(single, acceptor, Integer.MAX_VALUE, null, false).isSuccess()) {
                break;
            }
            FluidActionResult result = FluidUtil.tryEmptyContainer(single, acceptor, Integer.MAX_VALUE, null, true);
            if (!result.isSuccess()) {
                break;
            }
            StorageServerStub.giveEmptiedContainerToStorage(view, player, result.getResult());
            stack.shrink(1);
            poured++;
        }
        if (poured > 0) {
            StorageServerStub.playBucketSound(player, content, SoundActions.BUCKET_EMPTY);
        }
        return poured;
    }

    /**
     * 按流体类型播放桶的倒空/装满音效；取不到该流体的音效时不播放。
     *
     * <p>位置与参数与 {@code FluidUtil} 保持一致。</p>
     *
     * @param player 玩家
     * @param fluid  涉及的流体
     * @param action 音效动作（{@link SoundActions#BUCKET_FILL} 或 {@link SoundActions#BUCKET_EMPTY}）
     */
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

    /**
     * 把倾倒后剩下的空容器交还玩家；背包放不下时掉落在脚下。
     *
     * <p>用于取出失败等需要把容器退回原主人的场景。倒入后的空容器走
     * {@link #giveEmptiedContainerToStorage}，优先入仓储。</p>
     */
    private static void giveEmptiedContainer(ServerPlayer player, ItemStack emptied) {
        if (!player.addItem(emptied)) {
            Block.popResource(player.level(), player.blockPosition(), emptied);
        }
    }

    /**
     * 倾倒后交还空容器：优先存入仓储，仓储放不下再回退玩家背包，最后掉落在脚下。
     *
     * <p>倒入的流体已进仓储，空桶留在背包会占格子；直接入库可与仓储里的流体配套
     * （空桶就位后即可再次盛装）。仓储装满时不强塞，回退到玩家背包。</p>
     *
     * @param view    当前存储视图；为 null 时直接走玩家背包
     * @param player  玩家
     * @param emptied 倒空后的容器
     */
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

    /**
     * 流体条目是否通过分类过滤，判定方式与 {@link #matchesFilters} 对物品的一致。
     *
     * @param fluid      待判定的流体
     * @param categories 玩家列出的分类
     * @return 通过时返回 true
     */
    private static boolean matchesFluidCategoryFilters(FluidStack fluid, List<CategoryEntry> categories) {
        for (CategoryEntry entry : categories) {
            if (entry.getMode() == CategoryMode.UNLIMITED) continue;
            if (entry.getMode() == CategoryMode.ALLOWLIST != entry.getCategory().testFluid(fluid)) {
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
        // 板条箱：先把 dispose 方块状态同步到存储处理器，保证 GUI / RPC 插入
        // 与方块状态一致（相邻虚空物质时溢出部分被销毁）；需在 getOrCreate 之后，
        // 首次创建的存储处理器默认 dispose=false
        if (storage instanceof CrateBlockEntity crate) {
            crate.refreshDispose();
        }
        String search = PlayerSettings.getSetting(registries, playerId).storage().getSearchContent().strip();
        if (search.isEmpty() || !(storage instanceof CrateBlockEntity)) {
            return new StorageView(List.of(primary), List.of());
        }
        List<BaseStorage<?>> storages = new ArrayList<>();
        for (CrateBlockEntity crate : CrateBlock.getNearbyCrates(player.level(), pos)) {
            if (crate.getId() != null) {
                Storages.get().get(crate.getId()).ifPresent(stored -> {
                    // 邻居板条箱并入搜索视图：其存储若已存在，插入会落到该 handler，
                    // 这里顺带同步 dispose（storage 未创建则不加入视图，无需刷新）
                    crate.refreshDispose();
                    storages.add(stored);
                });
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
