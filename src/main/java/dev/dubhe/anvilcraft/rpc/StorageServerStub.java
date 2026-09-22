package dev.dubhe.anvilcraft.rpc;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import dev.anvilcraft.lib.v2.codec.StreamCodecUtil;
import dev.anvilcraft.lib.v2.rpc.CallableParam;
import dev.anvilcraft.lib.v2.rpc.IRemoteCallableValidator;
import dev.anvilcraft.lib.v2.rpc.RemoteCallable;
import dev.anvilcraft.lib.v2.util.UnlimitedItemStack;
import dev.dubhe.anvilcraft.api.StoragePortManager;
import dev.dubhe.anvilcraft.api.TerminalSessions;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.SpaceSizeItemStacksResourceHandler;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.TypeLimitItemStacksResourceHandler;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler;
import dev.dubhe.anvilcraft.block.container.storage.CrateBlock;
import dev.dubhe.anvilcraft.block.container.storage.HyperdimensionStorageStationBlock;
import dev.dubhe.anvilcraft.block.container.storage.ShulkerContainerBlock;
import dev.dubhe.anvilcraft.block.entity.storage.CrateBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.ShulkerContainerBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.HyperdimensionTerminalItem;
import dev.dubhe.anvilcraft.item.TerminalItem;
import dev.dubhe.anvilcraft.saved.setting.PlayerSetting;
import dev.dubhe.anvilcraft.saved.setting.PlayerSettings;
import dev.dubhe.anvilcraft.saved.setting.StorageSetting;
import dev.dubhe.anvilcraft.saved.setting.mode.OrderMode;
import dev.dubhe.anvilcraft.saved.setting.mode.SortMode;
import dev.dubhe.anvilcraft.saved.storage.BaseStorage;
import dev.dubhe.anvilcraft.saved.storage.CraftingStorage;
import dev.dubhe.anvilcraft.saved.storage.HyperdimensionStorage;
import dev.dubhe.anvilcraft.saved.storage.ShulkerContainerStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import dev.dubhe.anvilcraft.saved.storage.category.store.CategoryEntry;
import dev.dubhe.anvilcraft.saved.storage.category.store.CategoryMode;
import dev.dubhe.anvilcraft.util.ItemResourceHelper;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.CommonHooks;
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
    public static final StreamCodec<RegistryFriendlyByteBuf, List<ItemStack>> ITEM_STACK_LIST_STREAM_CODEC =
        ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list());
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
    private static final int CRAFTING_TAKE_ALL_CHUNK = 64;
    private static final Map<TakeAllKey, TakeAllSession> CRAFTING_BATCHES = new HashMap<>();

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
        return new Metadata(stub.version, stub.orderVersion, view.fullness(), view.capacity(), view.primary().getId());
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

    private record CraftingTarget(StorageView view, ServerPlayer player, @Nullable ItemStack terminal) {
        CraftingStorage read() {
            return this.terminal == null ? this.view.primary().getCrafting()
                : this.terminal.getOrDefault(ModComponents.CRAFTING, CraftingStorage.EMPTY);
        }

        void write(CraftingStorage crafting) {
            if (this.terminal == null) this.view.primary().setCrafting(crafting);
            else {
                this.terminal.set(ModComponents.CRAFTING, crafting);
                this.player.getInventory().setChanged();
            }
            this.player.inventoryMenu.broadcastChanges();
        }
    }

    private static CraftingTarget resolveCraftingTarget(ServerPlayer player, long sourcePos) {
        StorageView view = StorageServerStub.getView(StorageServerStub.getAndClear(), player.getGameProfile().id(), sourcePos);
        ItemStack terminal = TerminalSessions.contains(player.getUUID(), sourcePos) ? TerminalSessions.terminal(player, sourcePos) : null;
        return new CraftingTarget(view, player, terminal);
    }

    private static boolean carriesCraftingTerminal(CraftingTarget target) {
        return target.terminal != null && target.player.inventoryMenu.getCarried() == target.terminal;
    }

    private static ItemStack craftingInput(CraftingStorage state, int slot) {
        if (slot < 0 || slot > CraftingStorage.CRAFTING_GRID_SIZE) throw new IllegalArgumentException("Invalid crafting slot: " + slot);
        return slot == 0 ? state.stonecutterInput() : state.craftingInput().get(slot - 1);
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static InteractionResult craftingQuickCraft(
        UUID playerId, long sourcePos, int button,
        @CallableParam(clazz = StorageServerStub.class, field = "ORDER_STREAM_CODEC") IntList craftingSlots,
        @CallableParam(clazz = StorageServerStub.class, field = "ORDER_STREAM_CODEC") IntList inventorySlots,
        @CallableParam(clazz = ItemStack.class, field = "OPTIONAL_STREAM_CODEC") ItemStack clientCarried
    ) {
        ServerPlayer player = getServerPlayer(playerId);
        final CraftingTarget target = resolveCraftingTarget(player, sourcePos);
        if (carriesCraftingTerminal(target)) return new InteractionResult(player.inventoryMenu.getCarried(), false);
        cancelCraftingBatch(playerId, sourcePos);
        ItemStack carried = player.hasInfiniteMaterials() ? clientCarried : player.containerMenu.getCarried();
        if (button < 0 || button > 2 || carried.isEmpty() || button == 2 && !player.hasInfiniteMaterials()) {
            return new InteractionResult(carried, false);
        }
        if (craftingSlots.size() > MAX_SYNC_SLOTS || inventorySlots.size() > MAX_SYNC_SLOTS) {
            throw new IllegalArgumentException("Too many crafting drag targets");
        }
        IntList targets = new IntArrayList();
        IntSet visited = new IntOpenHashSet();
        for (int slot : craftingSlots) {
            if (slot >= 0 && slot <= CraftingStorage.CRAFTING_GRID_SIZE && visited.add(slot)) targets.add(slot);
        }
        for (int slot : inventorySlots) {
            if (slot >= 0 && slot < Inventory.INVENTORY_SIZE && visited.add(slot + 10)) targets.add(slot + 10);
        }
        if (targets.isEmpty()) return new InteractionResult(carried, false);
        boolean clone = button == 2;
        int perSlot = button == 0 ? carried.getCount() / targets.size() : clone ? carried.getMaxStackSize() : 1;
        ItemStack remaining = carried.copy();
        CraftingStorage crafting = target.read();
        boolean changed = false;
        boolean stoneAccepted = !player.level().recipeAccess().stonecutterRecipes().selectByInput(carried).entries().isEmpty();
        for (int targetSlot : targets) {
            if (!clone && remaining.isEmpty()) break;
            if (targetSlot == 0 && !stoneAccepted) continue;
            ItemStack current = targetSlot < 10 ? craftingInput(crafting, targetSlot)
                : player.getInventory().getItem(targetSlot - 10);
            if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, carried)) continue;
            int room = carried.getMaxStackSize() - current.getCount();
            int placed = Math.min(room, clone ? perSlot : Math.min(perSlot, remaining.getCount()));
            if (placed <= 0) continue;
            ItemStack next = carried.copyWithCount(current.getCount() + placed);
            if (targetSlot < 10) crafting = withCraftingInput(crafting, targetSlot, next);
            else player.getInventory().setItem(targetSlot - 10, next);
            if (!clone) remaining.shrink(placed);
            changed = true;
        }
        if (!changed) return new InteractionResult(carried, false);
        target.write(crafting);
        player.getInventory().setChanged();
        player.containerMenu.setCarried(remaining);
        player.containerMenu.broadcastChanges();
        return new InteractionResult(remaining, true);
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static InteractionResult craftingPickupAll(
        UUID playerId, long sourcePos, int slot,
        @CallableParam(clazz = ItemStack.class, field = "OPTIONAL_STREAM_CODEC") ItemStack clientCarried
    ) {
        ServerPlayer player = getServerPlayer(playerId);
        CraftingTarget target = resolveCraftingTarget(player, sourcePos);
        cancelCraftingBatch(playerId, sourcePos);
        ItemStack carried = player.containerMenu.getCarried();
        if (slot < 0 || slot > CraftingStorage.CRAFTING_GRID_SIZE) return new InteractionResult(carried, false);
        ItemStack current = craftingInput(target.read(), slot);
        if (!carried.isEmpty() && !current.isEmpty() && !ItemStack.isSameItemSameComponents(carried, current)) {
            return new InteractionResult(carried, false);
        }
        return collectCraftingInputs(target, slot, true);
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static InteractionResult craftingPickupIntoCarried(
        UUID playerId, long sourcePos,
        @CallableParam(clazz = ItemStack.class, field = "OPTIONAL_STREAM_CODEC") ItemStack clientCarried
    ) {
        ServerPlayer player = getServerPlayer(playerId);
        CraftingTarget target = resolveCraftingTarget(player, sourcePos);
        cancelCraftingBatch(playerId, sourcePos);
        return collectCraftingInputs(target, -1, false);
    }

    private static InteractionResult collectCraftingInputs(CraftingTarget target, int first, boolean includeInventory) {
        CraftingStorage state = target.read();
        ItemStack carried = target.player.containerMenu.getCarried();
        // 收集不创建物品，即使是创造玩家也不能用客户端旧快照替换服务端指针。
        ItemStack sample = !carried.isEmpty() ? carried : first >= 0 ? craftingInput(state, first) : ItemStack.EMPTY;
        if (sample.isEmpty()) return new InteractionResult(carried, false);
        int count = carried.getCount();
        int maximum = sample.getMaxStackSize();
        IntList order = new IntArrayList();
        if (first >= 0) order.add(first);
        for (int slot = 0; slot <= CraftingStorage.CRAFTING_GRID_SIZE; slot++) {
            if (slot != first) order.add(slot);
        }
        for (int slot : order) {
            if (count >= maximum) break;
            ItemStack current = craftingInput(state, slot);
            if (current.isEmpty() || !ItemStack.isSameItemSameComponents(current, sample)) continue;
            int take = Math.min(current.getCount(), maximum - count);
            state = withCraftingInput(state, slot, current.copyWithCount(current.getCount() - take));
            count += take;
        }
        if (includeInventory) {
            for (int slot = 0; slot < Inventory.INVENTORY_SIZE && count < maximum; slot++) {
                ItemStack current = target.player.getInventory().getItem(slot);
                if (current.isEmpty() || !ItemStack.isSameItemSameComponents(current, sample)) continue;
                int take = Math.min(current.getCount(), maximum - count);
                target.player.getInventory().setItem(slot, current.copyWithCount(current.getCount() - take));
                count += take;
            }
        }
        if (count == carried.getCount()) return new InteractionResult(carried, false);
        carried = sample.copyWithCount(count);
        target.write(state);
        target.player.getInventory().setChanged();
        target.player.containerMenu.setCarried(carried);
        target.player.containerMenu.broadcastChanges();
        return new InteractionResult(carried, true);
    }

    private static CraftingStorage withCraftingInput(CraftingStorage state, int slot, ItemStack stack) {
        return slot == 0 ? state.withStonecutterInput(stack) : state.withCraftingSlot(slot - 1, stack);
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static boolean craftingQuickMoveOut(UUID playerId, long sourcePos, int slot) {
        ServerPlayer player = getServerPlayer(playerId);
        CraftingTarget target = resolveCraftingTarget(player, sourcePos);
        cancelCraftingBatch(playerId, sourcePos);
        CraftingStorage state = target.read();
        ItemStack stack = craftingInput(state, slot);
        if (stack.isEmpty()) return false;
        int remaining = stack.getCount();
        try (Transaction transaction = Transaction.openRoot()) {
            ItemResource resource = ItemResource.of(stack);
            remaining -= PlayerInventoryWrapper.of(player).getMainSlots().insert(resource, remaining, transaction);
            if (remaining > 0) remaining -= target.view.insert(resource, remaining, transaction);
            if (remaining == stack.getCount()) return false;
            transaction.commit();
        }
        target.write(withCraftingInput(state, slot, stack.copyWithCount(remaining)));
        return true;
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static InteractionResult craftingThrowSlot(UUID playerId, long sourcePos, int slot, boolean stack) {
        ServerPlayer player = getServerPlayer(playerId);
        CraftingTarget target = resolveCraftingTarget(player, sourcePos);
        cancelCraftingBatch(playerId, sourcePos);
        ItemStack carried = player.containerMenu.getCarried();
        if (!carried.isEmpty()) return new InteractionResult(carried, false);
        CraftingStorage state = target.read();
        ItemStack item = craftingInput(state, slot);
        if (item.isEmpty()) return new InteractionResult(carried, false);
        int amount = stack ? item.getCount() : 1;
        target.write(withCraftingInput(state, slot, item.copyWithCount(item.getCount() - amount)));
        player.drop(item.copyWithCount(amount), true);
        player.swing(InteractionHand.MAIN_HAND, true);
        return new InteractionResult(carried, true);
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static InteractionResult craftingCloneSlot(UUID playerId, long sourcePos, int slot) {
        ServerPlayer player = getServerPlayer(playerId);
        CraftingTarget target = resolveCraftingTarget(player, sourcePos);
        ItemStack carried = player.containerMenu.getCarried();
        if (!player.isCreative() || !carried.isEmpty()) return new InteractionResult(carried, false);
        ItemStack item = craftingInput(target.read(), slot);
        if (item.isEmpty()) return new InteractionResult(carried, false);
        carried = item.copyWithCount(item.getMaxStackSize());
        player.containerMenu.setCarried(carried);
        player.containerMenu.broadcastChanges();
        return new InteractionResult(carried, true);
    }

    private record CraftOperation(
        Identifier recipeId, ItemStack result, CraftingStorage next, List<ItemStack> remainders, boolean consumed
    ) {
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static InteractionResult craftingTakeResult(UUID playerId, long sourcePos, boolean stonecutter, boolean shift) {
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        CraftingTarget target = StorageServerStub.resolveCraftingTarget(player, sourcePos);
        cancelCraftingBatch(playerId, sourcePos);
        CraftingStorage crafting = target.read();
        ItemStack carried = player.containerMenu.getCarried();
        CraftOperation operation = StorageServerStub.prepareCraftOperation(player, crafting, stonecutter, null);
        if (operation == null || operation.result.isEmpty()) return new InteractionResult(carried, false);
        ItemStack result = operation.result;
        int overflow = 0;
        if (!shift) {
            if (!carried.isEmpty() && (!ItemStack.isSameItemSameComponents(carried, result)
                || carried.getCount() + result.getCount() > carried.getMaxStackSize())) {
                return new InteractionResult(carried, false);
            }
            player.containerMenu.setCarried(result.copyWithCount(result.getCount() + carried.getCount()));
        } else {
            int inserted = StorageServerStub.placeCraftResult(target, result, crafting.toStorage());
            if (inserted == 0) return new InteractionResult(carried, false);
            overflow = result.getCount() - inserted;
        }
        // 先准备全部剩余物，再发放产物和写回输入；每个取出路径都只执行一次消耗。
        StorageServerStub.applyCraftOperation(target, operation, crafting.toStorage());
        if (overflow > 0) player.drop(result.copyWithCount(overflow), false);
        int refilled = StorageServerStub.refillCrafting(target, crafting);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        return new InteractionResult(player.containerMenu.getCarried(), true, refilled);
    }

    private static @Nullable CraftOperation prepareCraftOperation(
        ServerPlayer player, CraftingStorage crafting, boolean stonecutter, @Nullable Identifier lockedId
    ) {
        if (stonecutter) {
            ItemStack input = crafting.stonecutterInput();
            if (input.isEmpty()) return null;
            var choices = player.level().recipeAccess().stonecutterRecipes().selectByInput(input).entries().stream()
                .flatMap(entry -> entry.recipe().recipe().stream()).toList();
            int selected = crafting.stonecutterSelected();
            if (selected < 0 || selected >= choices.size()) return null;
            Identifier recipeId = choices.get(selected).id().identifier();
            if (lockedId != null && !lockedId.equals(recipeId)) return null;
            ItemStack result = choices.get(selected).value().assemble(new SingleRecipeInput(input));
            return new CraftOperation(recipeId, result,
                crafting.withStonecutterInput(input.copyWithCount(input.getCount() - 1)), List.of(), true);
        }
        var positioned = CraftingInput.ofPositioned(3, 3, crafting.craftingInput());
        var input = positioned.input();
        RecipeHolder<?> holder = lockedId == null
            ? player.level().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, player.level()).orElse(null)
            : player.level().getServer().getRecipeManager().recipeMap().byKey(ResourceKey.create(Registries.RECIPE, lockedId));
        if (holder == null || !(holder.value() instanceof CraftingRecipe recipe)) return null;
        if (lockedId != null && !recipe.matches(input, player.level())) return null;
        ItemStack result;
        List<ItemStack> remaining;
        final var previousPlayer = CommonHooks.getCraftingPlayer();
        CommonHooks.setCraftingPlayer(player);
        try {
            result = recipe.assemble(input);
            remaining = recipe.getRemainingItems(input);
        } finally {
            CommonHooks.setCraftingPlayer(previousPlayer);
        }
        if (result.isEmpty()) return null;
        List<ItemStack> grid = new ArrayList<>(crafting.craftingInput());
        List<ItemStack> overflow = new ArrayList<>();
        boolean consumed = false;
        for (int row = 0; row < input.height(); row++) {
            for (int column = 0; column < input.width(); column++) {
                int slot = column + positioned.left() + (row + positioned.top()) * 3;
                ItemStack current = grid.get(slot);
                if (current.isEmpty()) continue;
                ItemStack next = current.copyWithCount(current.getCount() - 1);
                int index = column + row * input.width();
                ItemStack remainder = index < remaining.size() ? remaining.get(index).copy() : ItemStack.EMPTY;
                if (!remainder.isEmpty()) {
                    if (next.isEmpty()) {
                        next = remainder;
                    } else if (ItemStack.isSameItemSameComponents(next, remainder)) {
                        next.grow(remainder.getCount());
                    } else {
                        overflow.add(remainder);
                    }
                }
                grid.set(slot, next);
                consumed |= !ItemStack.isSameItemSameComponents(current, next) || current.getCount() != next.getCount();
            }
        }
        return new CraftOperation(holder.id().identifier(), result, crafting.withCraftingInput(grid), overflow, consumed);
    }

    private static void applyCraftOperation(CraftingTarget target, CraftOperation operation, boolean storageFirst) {
        if (operation.consumed) target.write(operation.next);
        for (ItemStack remainder : operation.remainders) {
            StorageServerStub.returnCraftingStack(target, remainder, storageFirst);
        }
    }

    private record TakeAllKey(UUID player, long sourcePos) {
    }

    private record TakeAllSession(
        UUID storageId, Identifier dimension, Identifier recipeId, boolean stonecutter,
        int remaining, CraftingStorage template, CraftingStorage expected
    ) {
    }

    private static int craftBudget(ItemStack result, int multiplier) {
        long batches = Math.max(1, result.getMaxStackSize() / Math.max(1, result.getCount()));
        return (int) Math.min(Integer.MAX_VALUE, batches * Math.max(1L, multiplier));
    }

    private static boolean sameCraftingState(CraftingStorage left, CraftingStorage right) {
        if (left.autoFill() != right.autoFill() || left.toStorage() != right.toStorage()
            || left.stonecutterSelected() != right.stonecutterSelected()
            || !ItemStack.matches(left.stonecutterInput(), right.stonecutterInput())) return false;
        for (int slot = 0; slot < CraftingStorage.CRAFTING_GRID_SIZE; slot++) {
            if (!ItemStack.matches(left.craftingInput().get(slot), right.craftingInput().get(slot))) return false;
        }
        return true;
    }

    private static CraftingStorage copyCraftingState(CraftingStorage state) {
        return state.withStonecutterInput(state.stonecutterInput().copy())
            .withCraftingInput(state.craftingInput().stream().map(ItemStack::copy).toList());
    }

    private static void cancelCraftingBatch(UUID player, long sourcePos) {
        CRAFTING_BATCHES.remove(new TakeAllKey(player, sourcePos));
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static TakeAllResult craftingTakeAll(UUID playerId, long sourcePos, boolean stonecutter, int multiplier) {
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        CraftingTarget target = StorageServerStub.resolveCraftingTarget(player, sourcePos);
        TakeAllKey key = new TakeAllKey(playerId, sourcePos);
        TakeAllSession session = CRAFTING_BATCHES.remove(key);
        CraftingStorage initial = target.read();
        if (session != null && (session.stonecutter != stonecutter
            || !session.storageId.equals(target.view.primary().getId())
            || !session.dimension.equals(player.level().dimension().identifier())
            || !sameCraftingState(session.expected, initial))) {
            return new TakeAllResult(player.containerMenu.getCarried(), false, true, 0);
        }
        CraftingStorage template = session == null ? copyCraftingState(initial) : session.template;
        Identifier lockedId = session == null ? null : session.recipeId;
        int remaining = session == null ? -1 : session.remaining;
        boolean changed = false;
        int refilled = 0;
        for (int iteration = 0; iteration < CRAFTING_TAKE_ALL_CHUNK; iteration++) {
            CraftingStorage crafting = target.read();
            CraftOperation operation = prepareCraftOperation(player, crafting, stonecutter, lockedId);
            if (operation == null || operation.result.isEmpty()) {
                int filled = refillCrafting(target, template);
                refilled |= filled;
                if (filled != 0) continue;
                return finishCraftingBatch(player, changed, true, refilled);
            }
            if (lockedId == null) {
                lockedId = operation.recipeId;
                if (template.autoFill()) remaining = craftBudget(operation.result, multiplier);
            }
            int inserted = placeBatchCraftResult(target, operation.result, crafting.toStorage());
            if (inserted == 0) return finishCraftingBatch(player, changed, true, refilled);
            applyCraftOperation(target, operation, crafting.toStorage());
            changed = true;
            if (inserted < operation.result.getCount()) {
                player.drop(operation.result.copyWithCount(operation.result.getCount() - inserted), false);
                return finishCraftingBatch(player, true, true, refilled);
            }
            if (!operation.consumed) return finishCraftingBatch(player, true, true, refilled);
            // 预算结束时也恢复已耗尽的模板槽，避免桶、瓶等剩余物覆盖下一次点击的补料模板。
            refilled |= refillCrafting(target, template);
            if (remaining > 0 && --remaining == 0) return finishCraftingBatch(player, true, true, refilled);
            CraftingStorage after = target.read();
            if (stonecutter ? after.stonecutterInput().isEmpty() : after.craftingInput().stream().allMatch(ItemStack::isEmpty)) {
                return finishCraftingBatch(player, true, true, refilled);
            }
        }
        if (lockedId != null) {
            CRAFTING_BATCHES.put(key, new TakeAllSession(target.view.primary().getId(), player.level().dimension().identifier(),
                lockedId, stonecutter, remaining, template, copyCraftingState(target.read())));
        }
        return finishCraftingBatch(player, changed, false, refilled);
    }

    private static TakeAllResult finishCraftingBatch(ServerPlayer player, boolean changed, boolean done, int refilled) {
        if (changed) player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        return new TakeAllResult(player.containerMenu.getCarried(), changed, done, refilled);
    }

    private static int placeBatchCraftResult(CraftingTarget target, ItemStack result, boolean storageFirst) {
        if (storageFirst) return placeCraftResult(target, result, true);
        ItemResource resource = ItemResource.of(result);
        try (Transaction transaction = Transaction.openRoot()) {
            int remaining = result.getCount()
                - PlayerInventoryWrapper.of(target.player).getMainSlots().insert(resource, result.getCount(), transaction);
            ItemStack carried = target.player.containerMenu.getCarried();
            if (remaining > 0 && (carried.isEmpty() || ItemStack.isSameItemSameComponents(carried, result))) {
                remaining -= CarriedSlotWrapper.of(target.player.containerMenu).insert(resource, remaining, transaction);
            }
            if (remaining != 0) return 0;
            transaction.commit();
        }
        return result.getCount();
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static InteractionResult craftingThrowResult(UUID playerId, long sourcePos, boolean stonecutter, boolean stack) {
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        CraftingTarget target = StorageServerStub.resolveCraftingTarget(player, sourcePos);
        cancelCraftingBatch(playerId, sourcePos);
        if (!player.containerMenu.getCarried().isEmpty()) return new InteractionResult(player.containerMenu.getCarried(), false);
        CraftingStorage template = target.read();
        Identifier lockedId = null;
        boolean changed = false;
        int refilled = 0;
        int limit = 1;
        for (int performed = 0; performed < limit; performed++) {
            CraftOperation operation = prepareCraftOperation(player, target.read(), stonecutter, lockedId);
            if (operation == null || operation.result.isEmpty()) break;
            if (lockedId == null) {
                lockedId = operation.recipeId;
                if (stack) limit = Math.min(CRAFTING_TAKE_ALL_CHUNK, craftBudget(operation.result, 1));
            }
            player.drop(operation.result.copy(), true);
            applyCraftOperation(target, operation, template.toStorage());
            changed = true;
            if (!operation.consumed) break;
            refilled |= refillCrafting(target, template);
        }
        if (changed) {
            player.swing(InteractionHand.MAIN_HAND, true);
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        }
        return new InteractionResult(player.containerMenu.getCarried(), changed, refilled);
    }

    private static int placeCraftResult(CraftingTarget target, ItemStack result, boolean storageFirst) {
        ItemResource resource = ItemResource.of(result);
        int remaining = result.getCount();
        try (Transaction transaction = Transaction.openRoot()) {
            if (storageFirst) remaining -= target.view.insert(resource, remaining, transaction);
            remaining -= PlayerInventoryWrapper.of(target.player).getMainSlots().insert(resource, remaining, transaction);
            if (remaining > 0) {
                ItemStack carried = target.player.containerMenu.getCarried();
                if (carried.isEmpty() || ItemStack.isSameItemSameComponents(carried, result)) {
                    int room = result.getMaxStackSize() - carried.getCount();
                    if (remaining <= room) {
                        remaining -= CarriedSlotWrapper.of(target.player.containerMenu).insert(resource, remaining, transaction);
                    }
                }
            }
            if (!storageFirst && remaining > 0) remaining -= target.view.insert(resource, remaining, transaction);
            if (remaining == result.getCount()) return 0;
            transaction.commit();
        }
        return result.getCount() - remaining;
    }

    private static void returnCraftingStack(CraftingTarget target, ItemStack stack, boolean storageFirst) {
        if (stack.isEmpty()) return;
        ItemResource resource = ItemResource.of(stack);
        int remaining = stack.getCount();
        try (Transaction transaction = Transaction.openRoot()) {
            if (storageFirst) remaining -= target.view.insert(resource, remaining, transaction);
            remaining -= PlayerInventoryWrapper.of(target.player).getMainSlots().insert(resource, remaining, transaction);
            if (!storageFirst && remaining > 0) remaining -= target.view.insert(resource, remaining, transaction);
            transaction.commit();
        }
        if (remaining > 0) target.player.drop(stack.copyWithCount(remaining), false);
    }

    private static int refillCrafting(CraftingTarget target, CraftingStorage template) {
        if (!template.autoFill()) return 0;
        final CraftingStorage before = target.read();
        CraftingStorage current = before;
        int mask = 0;
        if (!template.stonecutterInput().isEmpty() && current.stonecutterInput().isEmpty()
            && StorageServerStub.takeCraftingMaterial(target, template.stonecutterInput())) {
            current = current.withStonecutterInput(template.stonecutterInput().copyWithCount(1));
            mask |= 1;
        }
        for (int slot = 0; slot < CraftingStorage.CRAFTING_GRID_SIZE; slot++) {
            ItemStack wanted = template.craftingInput().get(slot);
            if (wanted.isEmpty()) continue;
            ItemStack present = current.craftingInput().get(slot);
            if (!present.isEmpty() && !ItemStack.isSameItemSameComponents(present, wanted)) {
                StorageServerStub.returnCraftingStack(target, present, false);
                current = current.withCraftingSlot(slot, ItemStack.EMPTY);
                present = ItemStack.EMPTY;
            }
            if (present.isEmpty() && StorageServerStub.takeCraftingMaterial(target, wanted)) {
                current = current.withCraftingSlot(slot, wanted.copyWithCount(1));
                mask |= 1 << (slot + 1);
            }
        }
        if (current != before) target.write(current);
        return mask;
    }

    private static boolean takeCraftingMaterial(CraftingTarget target, ItemStack wanted) {
        try (Transaction transaction = Transaction.openRoot()) {
            if (!takeCraftingMaterial(target, wanted, transaction)) return false;
            transaction.commit();
            return true;
        }
    }

    private static boolean takeCraftingMaterial(CraftingTarget target, ItemStack wanted, Transaction parent) {
        var resource = ItemResource.of(wanted);
        try (Transaction transaction = Transaction.open(parent)) {
            if (StorageServerStub.extractCraftingMaterial(target, resource, transaction)) {
                transaction.commit();
                return true;
            }
            FluidStack fluid = FluidUtil.getFirstStackContained(wanted);
            ItemStack empty = StorageServerStub.emptyContainerOf(wanted, transaction);
            if (fluid.isEmpty() || empty.isEmpty()) return false;
            var single = new ItemStacksResourceHandler(1);
            single.set(0, ItemResource.of(empty), 1);
            var handler = ItemAccess.forHandlerIndexStrict(single, 0).getCapability(Capabilities.Fluid.ITEM);
            if (handler == null || handler.insert(FluidResource.of(fluid), fluid.getAmount(), transaction) != fluid.getAmount()
                || !single.getResource(0).equals(resource)
                || !StorageServerStub.extractCraftingMaterial(target, ItemResource.of(empty), transaction)
                || StoragePortManager.drain(target.view.primary().getId(), fluid, fluid.getAmount(), transaction) != fluid.getAmount()) {
                return false;
            }
            transaction.commit();
            return true;
        }
    }

    private static boolean extractCraftingMaterial(CraftingTarget target, ItemResource resource, Transaction transaction) {
        var inventory = PlayerInventoryWrapper.of(target.player);
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            if (target.terminal != null && target.player.getInventory().getItem(slot) == target.terminal) continue;
            if (inventory.extract(slot, resource, 1, transaction) == 1) return true;
        }
        return target.view.extractByResource(resource, 1, transaction) == 1;
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static boolean craftingTransfer(
        UUID playerId, long sourcePos, boolean stonecutter, boolean maxTransfer,
        @CallableParam(clazz = StorageServerStub.class, field = "ITEM_STACK_LIST_STREAM_CODEC") List<ItemStack> inputs,
        @CallableParam(clazz = ItemStack.class, field = "OPTIONAL_STREAM_CODEC") ItemStack stonecutterResult,
        @CallableParam(clazz = StorageServerStub.class, field = "ORDER_STREAM_CODEC") IntList requestedCounts
    ) {
        ServerPlayer player = getServerPlayer(playerId);
        final CraftingTarget target = resolveCraftingTarget(player, sourcePos);
        if (inputs.isEmpty() || inputs.size() > CraftingStorage.CRAFTING_GRID_SIZE
            || requestedCounts.size() > CraftingStorage.CRAFTING_GRID_SIZE) return false;
        int rounds = 0;
        int selected = 0;
        if (stonecutter) {
            ItemStack wanted = inputs.getFirst();
            if (wanted.isEmpty()) return false;
            var recipes = player.level().recipeAccess().stonecutterRecipes().selectByInput(wanted).entries();
            if (recipes.isEmpty()) return false;
            for (int i = 0; i < recipes.size(); i++) {
                var recipe = recipes.get(i).recipe().recipe();
                if (recipe.isPresent() && ItemStack.isSameItemSameComponents(
                    recipe.get().value().assemble(new SingleRecipeInput(wanted)), stonecutterResult)) {
                    selected = i;
                    break;
                }
            }
            rounds = maxTransfer ? wanted.getMaxStackSize() : 1;
        } else {
            for (int i = 0; i < requestedCounts.size(); i++) {
                int count = requestedCounts.getInt(i);
                ItemStack wanted = i < inputs.size() ? inputs.get(i) : ItemStack.EMPTY;
                if (count < 0 || count > (wanted.isEmpty() ? 0 : wanted.getMaxStackSize())) return false;
                if (count > 0) rounds = Math.max(rounds, maxTransfer ? wanted.getMaxStackSize() / count : 1);
            }
            if (rounds == 0) return false;
        }
        cancelCraftingBatch(playerId, sourcePos);
        CraftingStorage before = target.read();
        final boolean cleared = !before.stonecutterInput().isEmpty() || before.craftingInput().stream().anyMatch(stack -> !stack.isEmpty());
        returnCraftingStack(target, before.stonecutterInput(), true);
        for (ItemStack stack : before.craftingInput()) returnCraftingStack(target, stack, true);
        CraftingStorage current = before.withStonecutterInput(ItemStack.EMPTY)
            .withCraftingInput(java.util.Collections.nCopies(CraftingStorage.CRAFTING_GRID_SIZE, ItemStack.EMPTY));
        target.write(current);
        boolean changed = cleared;
        for (int round = 0; round < rounds; round++) {
            CraftingStorage next = transferCraftingRound(target, current, stonecutter, inputs, requestedCounts);
            if (next == current) break;
            current = next;
            changed = true;
        }
        if (stonecutter) current = current.withStonecutterSelected(selected);
        target.write(current);
        player.getInventory().setChanged();
        return changed;
    }

    private static CraftingStorage transferCraftingRound(
        CraftingTarget target, CraftingStorage current, boolean stonecutter, List<ItemStack> inputs, IntList counts
    ) {
        CraftingStorage next = current;
        // 整轮共用事务，流体与空容器也参与回滚，避免同一批容器被多个输入槽重复预占。
        try (Transaction transaction = Transaction.openRoot()) {
            for (int i = 0; i < (stonecutter ? 1 : inputs.size()); i++) {
                ItemStack wanted = inputs.get(i);
                int requested = stonecutter ? 1 : i < counts.size() ? counts.getInt(i) : 0;
                if (wanted.isEmpty() || requested == 0) continue;
                ItemStack present = stonecutter ? current.stonecutterInput() : current.craftingInput().get(i);
                if (requested > wanted.getMaxStackSize() - present.getCount()) continue;
                for (int amount = 0; amount < requested; amount++) {
                    if (!takeCraftingMaterial(target, wanted, transaction)) return current;
                }
                ItemStack placed = wanted.copyWithCount(present.getCount() + requested);
                next = stonecutter ? next.withStonecutterInput(placed) : next.withCraftingSlot(i, placed);
            }
            transaction.commit();
        }
        return next;
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static InteractionResult craftingPutStonecutterInput(
        UUID playerId,
        long sourcePos,
        int button,
        @CallableParam(clazz = ItemStack.class, field = "OPTIONAL_STREAM_CODEC") ItemStack clientCarried
    ) {
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        StorageServerStub.CraftingTarget target = StorageServerStub.resolveCraftingTarget(player, sourcePos);
        if (carriesCraftingTerminal(target)) return new InteractionResult(player.inventoryMenu.getCarried(), false);
        cancelCraftingBatch(playerId, sourcePos);
        CraftingStorage crafting = target.read();
        // 指针物品以客户端上报为准（与服务端 getCarried 一致；创造模式下服务端指针可能已过期）
        ItemStack carried = player.hasInfiniteMaterials() ? clientCarried : player.inventoryMenu.getCarried();
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
            player.inventoryMenu.setCarried(taken);
            player.inventoryMenu.broadcastChanges();
            return new InteractionResult(taken, true);
        }
        // 指针有物品：仅接受能匹配切石机配方的输入
        var recipes = player.level().recipeAccess().stonecutterRecipes().selectByInput(carried);
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
            player.inventoryMenu.setCarried(newCarried);
            player.inventoryMenu.broadcastChanges();
            return new InteractionResult(newCarried, true);
        }
        // 异种：整个交换（输入变化后重置选中配方）
        target.write(crafting.withStonecutterInput(carried.copy()).withStonecutterSelected(0));
        player.inventoryMenu.setCarried(current);
        player.inventoryMenu.broadcastChanges();
        return new InteractionResult(current, true);
    }

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
        if (carriesCraftingTerminal(target)) return new InteractionResult(player.inventoryMenu.getCarried(), false);
        cancelCraftingBatch(playerId, sourcePos);
        CraftingStorage crafting = target.read();
        ItemStack carried = player.hasInfiniteMaterials() ? clientCarried : player.inventoryMenu.getCarried();
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
            player.inventoryMenu.setCarried(taken);
            player.inventoryMenu.broadcastChanges();
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
            player.inventoryMenu.setCarried(newCarried);
            player.inventoryMenu.broadcastChanges();
            return new InteractionResult(newCarried, true);
        }
        // 异种：整个交换
        target.write(crafting.withCraftingSlot(slot, carried.copy()));
        player.inventoryMenu.setCarried(current);
        player.inventoryMenu.broadcastChanges();
        return new InteractionResult(current, true);
    }

    @CallableParam(clazz = StorageServerStub.class, field = "ITEM_STACK_LIST_STREAM_CODEC")
    @RemoteCallable(validator = StorageAccessValidator.class)
    public static List<ItemStack> craftingStonecutterRecipes(UUID playerId, long sourcePos) {
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        ItemStack input = StorageServerStub.resolveCraftingTarget(player, sourcePos).read().stonecutterInput();
        if (input.isEmpty()) {
            return List.of();
        }
        return player.level().recipeAccess().stonecutterRecipes().selectByInput(input).entries().stream()
            .flatMap(entry -> entry.recipe().recipe().stream())
            .map(holder -> holder.value().assemble(new SingleRecipeInput(input)))
            .toList();
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static boolean craftingClearToStorage(UUID playerId, long sourcePos) {
        ServerPlayer player = StorageServerStub.getServerPlayer(playerId);
        StorageServerStub.CraftingTarget target = StorageServerStub.resolveCraftingTarget(player, sourcePos);
        cancelCraftingBatch(playerId, sourcePos);
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
    public static boolean craftingAvailable(UUID playerId, long sourcePos) {
        return StorageServerStub.getView(StorageServerStub.getAndClear(), playerId, sourcePos).primary().isCraftingUnlocked();
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static boolean craftingUnlock(UUID playerId, long sourcePos) {
        return StorageServerStub.getView(StorageServerStub.getAndClear(), playerId, sourcePos).primary().unlockCrafting();
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static CraftingStorage craftingGet(UUID playerId, long sourcePos) {
        return resolveCraftingTarget(getServerPlayer(playerId), sourcePos).read();
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static void craftingSelect(UUID playerId, long sourcePos, int index) {
        CraftingTarget target = resolveCraftingTarget(getServerPlayer(playerId), sourcePos);
        cancelCraftingBatch(playerId, sourcePos);
        target.write(target.read().withStonecutterSelected(index));
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static void craftingSetOptions(UUID playerId, long sourcePos, boolean autoFill, boolean toStorage) {
        CraftingTarget target = resolveCraftingTarget(getServerPlayer(playerId), sourcePos);
        cancelCraftingBatch(playerId, sourcePos);
        target.write(target.read().withAutoFill(autoFill).withToStorage(toStorage));
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static void craftingSetLastOpened(UUID playerId, long sourcePos, boolean opened) {
        CraftingTarget target = resolveCraftingTarget(getServerPlayer(playerId), sourcePos);
        if (!opened) cancelCraftingBatch(playerId, sourcePos);
        target.write(target.read().withLastOpened(opened));
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
        CRAFTING_BATCHES.keySet().removeIf(key -> key.player.equals(playerId));
        TerminalSessions.clear(playerId);
    }

    public static void clear() {
        StorageServerStub.STUBS.clear();
        CRAFTING_BATCHES.clear();
        TerminalSessions.clear();
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
            if (TerminalSessions.contains(playerId, sourcePos)) return !TerminalSessions.terminal(player, sourcePos).isEmpty();
            BlockPos pos = BlockPos.of(sourcePos);
            if (player.level().isOutsideBuildHeight(pos) || !player.level().hasChunkAt(pos)) return false;
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

    public record Metadata(long version, long orderVersion, double fullness, Capacity capacity, UUID storageId) {
        public static final StreamCodec<ByteBuf, Metadata> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG,
            Metadata::version,
            ByteBufCodecs.VAR_LONG,
            Metadata::orderVersion,
            ByteBufCodecs.DOUBLE,
            Metadata::fullness,
            Capacity.STREAM_CODEC,
            Metadata::capacity,
            UUIDUtil.STREAM_CODEC,
            Metadata::storageId,
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

    public record TakeAllResult(ItemStack carried, boolean changed, boolean done, int refilledSlots) {
        public static final StreamCodec<RegistryFriendlyByteBuf, TakeAllResult> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC, TakeAllResult::carried,
            ByteBufCodecs.BOOL, TakeAllResult::changed,
            ByteBufCodecs.BOOL, TakeAllResult::done,
            ByteBufCodecs.VAR_INT, TakeAllResult::refilledSlots,
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
        return emptyContainerOf(filled, null);
    }

    private static ItemStack emptyContainerOf(ItemStack filled, @Nullable Transaction parent) {
        ItemStacksResourceHandler single = new ItemStacksResourceHandler(1);
        single.set(0, ItemResource.of(filled), 1);
        var handler = ItemAccess.forHandlerIndexStrict(single, 0).getCapability(Capabilities.Fluid.ITEM);
        if (handler == null) return ItemStack.EMPTY;
        try (Transaction transaction = Transaction.open(parent)) {
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
        if (TerminalSessions.contains(playerId, sourcePos)) {
            return new StorageView(List.of(TerminalSessions.storage(player, sourcePos)), List.of());
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

    @CallableParam(clazz = StorageServerStub.class, field = "ORDER_STREAM_CODEC")
    @RemoteCallable(validator = StorageAccessValidator.class)
    public static IntList terminalReorder(UUID playerId, long sourcePos, String search) {
        var registries = getAndClear();
        StorageView view = getView(registries, playerId, sourcePos);
        PlayerSetting setting = PlayerSettings.getSetting(registries, playerId);
        StorageSetting storage = setting.storage();
        return createOrder(view, new SortOptions(storage.getSort(), storage.getOrder()),
            search.strip().toLowerCase(Locale.ROOT), setting.listed());
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static InteractionResult terminalTake(
        UUID playerId, long sourcePos, int slot, int button,
        @CallableParam(clazz = ItemStack.class, field = "OPTIONAL_STREAM_CODEC") ItemStack clientCarried
    ) {
        ServerPlayer player = getServerPlayer(playerId);
        StorageView view = getView(getAndClear(), playerId, sourcePos);
        ItemStack carried = player.containerMenu.getCarried();
        boolean blocked = player.hasInfiniteMaterials() ? !clientCarried.isEmpty() : !carried.isEmpty();
        if (blocked || button < 0 || button > 1 || slot < 0 || slot >= view.size()) return new InteractionResult(carried, false);
        ItemResource resource = view.resource(slot);
        int amount = button == 0 ? resource.toStack().getMaxStackSize() : 1;
        try (Transaction transaction = Transaction.openRoot()) {
            int taken = view.extractByResource(resource, amount, transaction);
            if (taken == 0) return new InteractionResult(carried, false);
            var cursor = CarriedSlotWrapper.of(player.containerMenu);
            if (!carried.isEmpty()) cursor.extract(ItemResource.of(carried), carried.getCount(), transaction);
            if (cursor.insert(resource, taken, transaction) != taken) return new InteractionResult(carried, false);
            transaction.commit();
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        return new InteractionResult(player.containerMenu.getCarried(), true);
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static InteractionResult terminalTakeToInventory(UUID playerId, long sourcePos, int slot, int button) {
        ServerPlayer player = getServerPlayer(playerId);
        StorageView view = getView(getAndClear(), playerId, sourcePos);
        ItemStack carried = player.containerMenu.getCarried();
        if (button < 0 || button > 1 || slot < 0 || slot >= view.size()) return new InteractionResult(carried, false);
        ItemResource resource = view.resource(slot);
        int amount = button == 0 ? resource.toStack().getMaxStackSize() : 1;
        var inventory = PlayerInventoryWrapper.of(player).getMainSlots();
        try (Transaction transaction = Transaction.openRoot()) {
            int fit;
            try (Transaction simulation = Transaction.open(transaction)) {
                fit = inventory.insert(resource, amount, simulation);
            }
            if (fit == 0) return new InteractionResult(carried, false);
            int taken = view.extractByResource(resource, fit, transaction);
            if (taken == 0 || inventory.insert(resource, taken, transaction) != taken) return new InteractionResult(carried, false);
            transaction.commit();
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        return new InteractionResult(carried, true);
    }

    @RemoteCallable(validator = StorageAccessValidator.class)
    public static InteractionResult terminalInsert(
        UUID playerId, long sourcePos,
        @CallableParam(clazz = ItemStack.class, field = "OPTIONAL_STREAM_CODEC") ItemStack clientCarried
    ) {
        ServerPlayer player = getServerPlayer(playerId);
        StorageView view = getView(getAndClear(), playerId, sourcePos);
        ItemStack carried = (player.hasInfiniteMaterials() ? clientCarried : player.containerMenu.getCarried()).copy();
        if (carried.isEmpty()) return new InteractionResult(carried, false);
        int poured = pourIntoFluidPort(player, view, carried, carried.getCount());
        int inserted = poured == 0 ? view.insert(carried, carried.getCount()) : 0;
        if (poured == 0 && inserted == 0) return new InteractionResult(carried, false);
        carried.shrink(inserted);
        player.containerMenu.setCarried(carried);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        return new InteractionResult(carried, true);
    }

    private static List<BaseStorage<?>> boundStorages(ServerPlayer player) {
        List<BaseStorage<?>> storages = new ArrayList<>();
        List<ItemStack> terminals = TerminalItem.getAll(player);
        for (TerminalItem.Kind kind : new TerminalItem.Kind[]{
            TerminalItem.Kind.HYPERDIMENSION, TerminalItem.Kind.LOCAL, TerminalItem.Kind.SHULKER
        }) {
            for (ItemStack stack : terminals) {
                if (((TerminalItem) stack.getItem()).kind() != kind) continue;
                BaseStorage<?> storage = TerminalSessions.targetStorage(player, stack, false);
                if (storage != null && storages.stream().noneMatch(existing -> existing.getId().equals(storage.getId()))) {
                    storages.add(storage);
                }
            }
        }
        return storages;
    }

    public static void depositExcess(ServerPlayer player) {
        if (player.hasInfiniteMaterials()) return;
        List<BaseStorage<?>> storages = boundStorages(player);
        if (storages.isEmpty()) return;
        Map<ItemResource, Long> totals = new java.util.LinkedHashMap<>();
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.isEmpty()) totals.merge(ItemResource.of(stack), (long) stack.getCount(), Long::sum);
        }
        boolean changed = false;
        var inventory = PlayerInventoryWrapper.of(player);
        int selected = player.getInventory().getSelectedSlot();
        for (var entry : totals.entrySet()) {
            ItemResource resource = entry.getKey();
            int excess = (int) Math.min(Integer.MAX_VALUE, entry.getValue() - resource.toStack().getMaxStackSize());
            if (excess <= 0) continue;
            try (Transaction transaction = Transaction.openRoot()) {
                int remaining = insertBalanceResource(storages, resource, excess, transaction);
                if (remaining == 0) continue;
                for (int slot = Inventory.INVENTORY_SIZE - 1; slot >= 0 && remaining > 0; slot--) {
                    if (slot != selected) remaining -= inventory.extract(slot, resource, remaining, transaction);
                }
                if (remaining > 0) remaining -= inventory.extract(selected, resource, remaining, transaction);
                if (remaining != 0) continue;
                transaction.commit();
                changed = true;
            }
        }
        if (changed) {
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        }
    }

    private static int insertBalanceResource(List<BaseStorage<?>> storages, ItemResource resource, int amount, Transaction transaction) {
        int inserted = 0;
        for (BaseStorage<?> storage : storages) {
            if (!StorageView.canStore(storage, resource) || !StorageView.contains(storage.getItems(), resource)) continue;
            inserted += storage.getItems().insert(resource, amount - inserted, transaction);
            if (inserted == amount) return inserted;
        }
        for (BaseStorage<?> storage : storages) {
            if (!StorageView.canStore(storage, resource)) continue;
            inserted += storage.getItems().insert(resource, amount - inserted, transaction);
            if (inserted == amount) return inserted;
        }
        return inserted;
    }

    public static void restockHand(ServerPlayer player, ItemStack usedUpItem, int inventorySlot) {
        if (usedUpItem.isEmpty() || player.hasInfiniteMaterials() || !player.containerMenu.getCarried().isEmpty()
            || inventorySlot < 0 || inventorySlot >= Inventory.INVENTORY_SIZE && inventorySlot != 40
            || !player.getInventory().getItem(inventorySlot).isEmpty()) return;
        ItemResource resource = ItemResource.of(usedUpItem);
        int needed = usedUpItem.getMaxStackSize();
        try (Transaction transaction = Transaction.openRoot()) {
            int taken = 0;
            for (BaseStorage<?> storage : boundStorages(player)) {
                if (!StorageView.canStore(storage, resource)) continue;
                taken += storage.getItems().extract(resource, needed - taken, transaction);
                if (taken == needed) break;
            }
            if (taken == 0 || PlayerInventoryWrapper.of(player).insert(inventorySlot, resource, taken, transaction) != taken) return;
            transaction.commit();
        }
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
    }

    public static int insertIntoTerminal(ServerPlayer player, UUID targetId, ItemStack stack, int amount) {
        ItemStack terminal = TerminalSessions.findTerminal(player, targetId);
        if (terminal.isEmpty() || stack.isEmpty() || amount <= 0) return 0;
        BaseStorage<?> storage = TerminalSessions.targetStorage(player, terminal, false);
        if (storage == null) return 0;
        return new StorageView(List.of(storage), List.of()).insert(stack, Math.min(amount, stack.getCount()));
    }

    public static ItemStack extractFromTerminal(ServerPlayer player, UUID targetId, int amount, Slot targetSlot) {
        ItemStack terminal = TerminalSessions.findTerminal(player, targetId);
        if (terminal.isEmpty() || amount <= 0 || !targetSlot.isActive() || !targetSlot.allowModification(player)) return ItemStack.EMPTY;
        BaseStorage<?> storage = TerminalSessions.targetStorage(player, terminal, false);
        if (storage == null) return ItemStack.EMPTY;
        try (Transaction transaction = Transaction.openRoot()) {
            ItemStack extracted = extractTerminalItem(player, storage, amount, targetSlot, transaction);
            transaction.commit();
            return extracted;
        }
    }

    private static ItemStack extractTerminalItem(
        ServerPlayer player, BaseStorage<?> storage, int amount, Slot slot, Transaction transaction
    ) {
        var view = new StorageView(List.of(storage), List.of());
        var setting = PlayerSettings.getSetting(player.registryAccess(), player.getUUID());
        var options = new SortOptions(setting.storage().getSort(), setting.storage().getOrder());
        for (int index : createOrder(view, options, "", setting.listed())) {
            if (index < 0 || index >= view.size()) continue;
            ItemResource resource = view.resource(index);
            ItemStack stack = resource.toStack();
            if (!slot.mayPlace(stack)) continue;
            int limit = Math.min(amount, Math.min(stack.getMaxStackSize(), slot.getMaxStackSize(stack)));
            int extracted = view.extractByResource(resource, limit, transaction);
            if (extracted > 0) return resource.toStack(extracted);
        }
        return ItemStack.EMPTY;
    }

    public static final StreamCodec<ByteBuf, List<UUID>> TERMINAL_IDS_STREAM_CODEC = UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list());
    private static final int MAX_TERMINAL_TARGETS = 64;
    private static final int MAX_TERMINAL_ITEMS = 512;
    private static final int MAX_TERMINAL_SCAN = 4096;

    public record TerminalSnapshot(List<ItemStack> items, List<FluidEntry> fluids, boolean complete) {
        public static final StreamCodec<RegistryFriendlyByteBuf, TerminalSnapshot> STREAM_CODEC = StreamCodec.composite(
            ITEM_STACK_LIST_STREAM_CODEC, TerminalSnapshot::items,
            FluidEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), TerminalSnapshot::fluids,
            ByteBufCodecs.BOOL, TerminalSnapshot::complete, TerminalSnapshot::new
        );
    }

    public record TerminalRestock(List<ItemStack> withdrawn, List<ItemStack> inventoryBefore) {
        public static final StreamCodec<RegistryFriendlyByteBuf, TerminalRestock> STREAM_CODEC = StreamCodec.composite(
            ITEM_STACK_LIST_STREAM_CODEC, TerminalRestock::withdrawn,
            ITEM_STACK_LIST_STREAM_CODEC, TerminalRestock::inventoryBefore, TerminalRestock::new
        );
        public static final TerminalRestock EMPTY = new TerminalRestock(List.of(), List.of());
    }

    @RemoteCallable(validator = TerminalRecipeAccessValidator.class)
    public static TerminalSnapshot terminalSnapshot(
        UUID playerId, @CallableParam(clazz = StorageServerStub.class, field = "TERMINAL_IDS_STREAM_CODEC") List<UUID> targets
    ) {
        ServerPlayer player = getServerPlayer(playerId);
        Map<ItemResource, Long> counts = new java.util.LinkedHashMap<>();
        Map<FluidResource, FluidEntry> fluids = new java.util.LinkedHashMap<>();
        boolean complete = true;
        for (BaseStorage<?> storage : terminalRecipeStorages(player, targets)) {
            var items = storage.getItems();
            if (items.size() > MAX_TERMINAL_SCAN) complete = false;
            for (int slot = 0; slot < Math.min(items.size(), MAX_TERMINAL_SCAN); slot++) {
                ItemResource resource = items.getResource(slot);
                long amount = items.getAmountAsLong(slot);
                if (resource.isEmpty() || amount <= 0) continue;
                if (counts.size() >= MAX_TERMINAL_ITEMS && !counts.containsKey(resource)) {
                    complete = false;
                    continue;
                }
                counts.merge(resource, amount, Long::sum);
            }
            for (FluidEntry entry : StoragePortManager.collect(storage.getId())) {
                fluids.merge(FluidResource.of(entry.icon()), entry, (first, second) -> new FluidEntry(first.icon(),
                    (int) Math.min(Integer.MAX_VALUE, (long) first.amount() + second.amount())));
            }
        }
        List<ItemStack> items = counts.entrySet().stream()
            .map(entry -> entry.getKey().toStack((int) Math.min(Integer.MAX_VALUE, entry.getValue()))).toList();
        return new TerminalSnapshot(items, List.copyOf(fluids.values()), complete);
    }

    private static List<BaseStorage<?>> terminalRecipeStorages(ServerPlayer player, List<UUID> targets) {
        if (!ownsTerminalTargets(player, targets)) return List.of();
        List<BaseStorage<?>> storages = new ArrayList<>();
        for (UUID target : targets) {
            var terminal = TerminalSessions.findTerminal(player, target);
            BaseStorage<?> storage = TerminalSessions.targetStorage(player, terminal, false);
            if (storage != null && storages.stream().noneMatch(existing -> existing.getId().equals(storage.getId()))) storages.add(storage);
        }
        return storages;
    }

    /** Desired counts are final main-inventory totals, not an already-subtracted deficit. */
    @RemoteCallable(validator = TerminalRecipeAccessValidator.class)
    public static TerminalRestock terminalRestock(
        UUID playerId, int menuId,
        @CallableParam(clazz = StorageServerStub.class, field = "TERMINAL_IDS_STREAM_CODEC") List<UUID> targets,
        @CallableParam(clazz = StorageServerStub.class, field = "ITEM_STACK_LIST_STREAM_CODEC") List<ItemStack> desired
    ) {
        ServerPlayer player = getServerPlayer(playerId);
        if (player.containerMenu.containerId != menuId || desired.size() > MAX_TERMINAL_ITEMS) return TerminalRestock.EMPTY;
        List<BaseStorage<?>> storages = terminalRecipeStorages(player, targets);
        if (storages.isEmpty()) return TerminalRestock.EMPTY;
        List<ItemStack> before = new ArrayList<>();
        List<ItemStack> withdrawn = new ArrayList<>();
        for (ItemStack need : mergeTerminalCounts(player, desired, false)) {
            int have = terminalInventoryCount(player, need);
            if (have > 0) before.add(need.copyWithCount(have));
            int required = Math.max(0, need.getCount() - have);
            if (required == 0) continue;
            int filled = fillTerminalContainers(player, storages, need.copyWithCount(1), required);
            if (filled > 0) withdrawn.add(need.copyWithCount(filled));
            required -= filled;
            for (BaseStorage<?> storage : storages) {
                var items = storage.getItems();
                for (int slot = 0; slot < items.size() && required > 0; slot++) {
                    ItemResource resource = items.getResource(slot);
                    if (resource.isEmpty() || items.getAmountAsLong(slot) <= 0
                        || !sameTerminalItem(player, need, resource.toStack())) continue;
                    try (Transaction transaction = Transaction.openRoot()) {
                        var inventory = PlayerInventoryWrapper.of(player).getMainSlots();
                        int fit;
                        try (Transaction simulation = Transaction.open(transaction)) {
                            fit = inventory.insert(resource, required, simulation);
                        }
                        int taken = items.extract(slot, resource, fit, transaction);
                        if (taken == 0 || inventory.insert(resource, taken, transaction) != taken) continue;
                        transaction.commit();
                        withdrawn.add(resource.toStack(taken));
                        required -= taken;
                    }
                }
                if (required == 0) break;
            }
        }
        if (!withdrawn.isEmpty()) {
            player.getInventory().setChanged();
            player.containerMenu.broadcastChanges();
        }
        return new TerminalRestock(mergeTerminalCounts(player, withdrawn, true), List.copyOf(before));
    }

    private static boolean sameTerminalItem(ServerPlayer player, ItemStack first, ItemStack second) {
        return !first.isEmpty() && !second.isEmpty()
            && ItemResourceHelper.matchesNetworkStack(first.copyWithCount(1), second.copyWithCount(1), player.registryAccess());
    }

    private static List<ItemStack> mergeTerminalCounts(ServerPlayer player, List<ItemStack> stacks, boolean add) {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (stack.isEmpty()) continue;
            ItemStack existing = result.stream().filter(item -> sameTerminalItem(player, item, stack)).findFirst().orElse(null);
            if (existing == null) result.add(stack.copy());
            else existing.setCount(add ? (int) Math.min(Integer.MAX_VALUE, (long) existing.getCount() + stack.getCount())
                : Math.max(existing.getCount(), stack.getCount()));
        }
        return List.copyOf(result);
    }

    private static int terminalInventoryCount(ServerPlayer player, ItemStack wanted) {
        long count = 0;
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (sameTerminalItem(player, wanted, stack)) count += stack.getCount();
        }
        return (int) Math.min(Integer.MAX_VALUE, count);
    }

    private static int fillTerminalContainers(ServerPlayer player, List<BaseStorage<?>> storages, ItemStack wanted, int limit) {
        FluidStack fluid = FluidUtil.getFirstStackContained(wanted);
        if (fluid.isEmpty()) return 0;
        ItemStack empty = emptyContainerOf(wanted);
        if (empty.isEmpty()) return 0;
        var inventory = PlayerInventoryWrapper.of(player).getMainSlots();
        var view = new StorageView(storages, List.of());
        int produced = 0;
        while (produced < limit) {
            try (Transaction transaction = Transaction.openRoot()) {
                var single = new ItemStacksResourceHandler(1);
                single.set(0, ItemResource.of(empty), 1);
                var handler = ItemAccess.forHandlerIndexStrict(single, 0).getCapability(Capabilities.Fluid.ITEM);
                if (handler == null || handler.insert(FluidResource.of(fluid), fluid.getAmount(), transaction) != fluid.getAmount()
                    || !sameTerminalItem(player, wanted, single.getResource(0).toStack())) break;
                ItemResource container = ItemResource.of(empty);
                if (inventory.extract(container, 1, transaction) == 0 && view.extractByResource(container, 1, transaction) == 0) break;
                if (inventory.insert(single.getResource(0), 1, transaction) != 1) break;
                int remaining = fluid.getAmount();
                for (BaseStorage<?> storage : storages) {
                    remaining -= StoragePortManager.drain(storage.getId(), fluid, remaining, transaction);
                    if (remaining == 0) break;
                }
                if (remaining != 0) break;
                transaction.commit();
                produced++;
            }
        }
        return produced;
    }

    /** Return only the supplied surplus above the inventory count observed before restocking. */
    @RemoteCallable(validator = TerminalRecipeAccessValidator.class)
    public static boolean terminalReturnExcess(
        UUID playerId,
        @CallableParam(clazz = StorageServerStub.class, field = "TERMINAL_IDS_STREAM_CODEC") List<UUID> targets,
        TerminalRestock restock
    ) {
        ServerPlayer player = getServerPlayer(playerId);
        if (restock.withdrawn().size() > MAX_TERMINAL_ITEMS || restock.inventoryBefore().size() > MAX_TERMINAL_ITEMS) return false;
        List<BaseStorage<?>> storages = terminalRecipeStorages(player, targets);
        if (storages.isEmpty()) return false;
        boolean changed = false;
        var inventory = PlayerInventoryWrapper.of(player);
        int selected = player.getInventory().getSelectedSlot();
        for (ItemStack supplied : mergeTerminalCounts(player, restock.withdrawn(), true)) {
            int before = restock.inventoryBefore().stream().filter(item -> sameTerminalItem(player, supplied, item))
                .mapToInt(ItemStack::getCount).max().orElse(0);
            int remaining = Math.min(supplied.getCount(), Math.max(0, terminalInventoryCount(player, supplied) - before));
            for (int pass = Inventory.INVENTORY_SIZE; pass >= 0 && remaining > 0; pass--) {
                int slot = pass == 0 ? selected : pass - 1;
                if (pass != 0 && slot == selected) continue;
                ItemStack actual = player.getInventory().getItem(slot);
                if (!sameTerminalItem(player, supplied, actual)) continue;
                ItemResource resource = ItemResource.of(actual);
                try (Transaction transaction = Transaction.openRoot()) {
                    int inserted = insertBalanceResource(storages, resource, Math.min(remaining, actual.getCount()), transaction);
                    if (inserted == 0 || inventory.extract(slot, resource, inserted, transaction) != inserted) continue;
                    transaction.commit();
                    remaining -= inserted;
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

    private static boolean ownsTerminalTargets(ServerPlayer player, List<UUID> targets) {
        return !targets.isEmpty() && targets.size() <= MAX_TERMINAL_TARGETS
            && targets.stream().allMatch(target -> target != null && !TerminalSessions.findTerminal(player, target).isEmpty());
    }

    public static final class TerminalRecipeAccessValidator implements IRemoteCallableValidator {
        @Override
        public boolean validate(IPayloadContext context, Method method, Object[] args) {
            if (!(context.player() instanceof ServerPlayer player) || args.length < 2 || !(args[0] instanceof UUID playerId)
                || !playerId.equals(player.getUUID())) return false;
            int targetIndex = 1;
            if (args[1] instanceof Integer menuId) {
                if (menuId != player.containerMenu.containerId || args.length != 4) return false;
                targetIndex = 2;
            }
            if (!(args[targetIndex] instanceof List<?> targets) || targets.isEmpty() || targets.size() > MAX_TERMINAL_TARGETS) return false;
            for (Object target : targets) {
                if (!(target instanceof UUID id) || TerminalSessions.findTerminal(player, id).isEmpty()) return false;
            }
            return true;
        }
    }

    @RemoteCallable(validator = CreativeTerminalAccessValidator.class)
    public static InteractionResult creativeTerminalTransfer(
        UUID playerId, UUID targetId, int menuSlot, boolean extract,
        @CallableParam(clazz = ItemStack.class, field = "OPTIONAL_STREAM_CODEC") ItemStack expected,
        @CallableParam(clazz = ItemStack.class, field = "OPTIONAL_STREAM_CODEC") ItemStack terminalStack
    ) {
        ServerPlayer player = getServerPlayer(playerId);
        ItemStack carried = terminalStack.copy();
        if (!validCreativeTerminal(player, targetId, menuSlot, terminalStack)) return new InteractionResult(carried, false);
        Slot slot = player.inventoryMenu.getSlot(menuSlot);
        if (!slot.allowModification(player) || extract != expected.isEmpty()
            || !ItemResourceHelper.matchesNetworkStack(slot.getItem(), expected, player.registryAccess())) {
            return new InteractionResult(carried, false);
        }
        BaseStorage<?> storage = TerminalSessions.targetStorage(player, terminalStack, false);
        var inventory = PlayerInventoryWrapper.of(player);
        int inventorySlot = slot.getContainerSlot();
        boolean changed = false;
        try (Transaction transaction = Transaction.openRoot()) {
            if (extract) {
                ItemStack result = storage == null ? ItemStack.EMPTY : extractTerminalItem(player, storage, 64, slot, transaction);
                changed = !result.isEmpty();
                if (!changed) {
                    if (!slot.mayPlace(carried) || carried.getCount() > slot.getMaxStackSize(carried)) {
                        return new InteractionResult(carried, false);
                    }
                    result = carried;
                }
                if (inventory.insert(inventorySlot, ItemResource.of(result), result.getCount(), transaction) != result.getCount()) {
                    return new InteractionResult(carried, false);
                }
                if (!changed) carried = ItemStack.EMPTY;
            } else if (storage != null) {
                ItemStack actual = slot.getItem();
                ItemResource resource = ItemResource.of(actual);
                int inserted = new StorageView(List.of(storage), List.of()).insert(resource, actual.getCount(), transaction);
                if (inventory.extract(inventorySlot, resource, inserted, transaction) != inserted) {
                    return new InteractionResult(carried, false);
                }
                changed = inserted > 0;
            }
            transaction.commit();
        }
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        return new InteractionResult(carried, changed);
    }

    private static boolean validCreativeTerminal(ServerPlayer player, UUID targetId, int menuSlot, ItemStack terminalStack) {
        return player.isCreative() && player.containerMenu == player.inventoryMenu
            && menuSlot >= 0 && menuSlot < player.inventoryMenu.slots.size()
            && player.inventoryMenu.getSlot(menuSlot).container == player.getInventory()
            && player.inventoryMenu.getSlot(menuSlot).isActive() && terminalStack.getCount() == 1
            && terminalStack.getItem() instanceof TerminalItem terminal && targetId.equals(terminal.targetId(player, terminalStack));
    }

    public static final class CreativeTerminalAccessValidator implements IRemoteCallableValidator {
        @Override
        public boolean validate(IPayloadContext context, Method method, Object[] args) {
            return context.player() instanceof ServerPlayer player && args.length == 6
                && args[0] instanceof UUID playerId && playerId.equals(player.getUUID()) && args[1] instanceof UUID targetId
                && args[2] instanceof Integer menuSlot && args[3] instanceof Boolean && args[4] instanceof ItemStack
                && args[5] instanceof ItemStack terminalStack && validCreativeTerminal(player, targetId, menuSlot, terminalStack);
        }
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
                if (!canStore(this.storages.get(i), resource) || !StorageView.contains(items, resource)) continue;
                inserted += items.insert(resource, amount - inserted, tx);
                if (inserted == amount) return inserted;
            }
            UnlimitedItemStacksResourceHandler primaryItems = this.primary().getItems();
            if (canStore(this.primary(), resource)) inserted += primaryItems.insert(resource, amount - inserted, tx);
            if (inserted == amount) return inserted;
            for (int i = 0; i < this.storages.size() - 1; i++) {
                if (canStore(this.storages.get(i), resource)) {
                    inserted += this.storages.get(i).getItems().insert(resource, amount - inserted, tx);
                }
                if (inserted == amount) return inserted;
            }
            return inserted;
        }

        private static boolean canStore(BaseStorage<?> storage, ItemResource resource) {
            if (!(storage instanceof HyperdimensionStorage || storage instanceof ShulkerContainerStorage)) return true;
            Item item = resource.toStack().getItem();
            if (item instanceof HyperdimensionTerminalItem) return false;
            return !(item instanceof BlockItem block && (block.getBlock() instanceof HyperdimensionStorageStationBlock
                || block.getBlock() instanceof ShulkerContainerBlock || block.getBlock() instanceof ShulkerBoxBlock));
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
