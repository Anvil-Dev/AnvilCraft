package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.IStoragePort;
import dev.dubhe.anvilcraft.api.StoragePortManager;
import dev.dubhe.anvilcraft.api.itemhandler.IItemResourceHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.block.logistics.storage.AbstractStoragePortBlock;
import dev.dubhe.anvilcraft.block.logistics.storage.StoragePortBlock;
import dev.dubhe.anvilcraft.item.block.StoragePortBlockItem;
import dev.dubhe.anvilcraft.item.tool.AnvilHammerItem;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import dev.dubhe.anvilcraft.saved.storage.BaseStorage;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 仓储端口保留独立的标记物品库存，连接大仓库后直接访问大仓库及其端口库存。
 * 连通组件必须恰好接触一个核心，内部库存仅在核心同种物品耗尽后被取用。
 */
public class StoragePortBlockEntity extends BlockEntity implements IItemResourceHandlerHolder, IStoragePort {
    /** 缓存格数 */
    public static final int BUFFER_SLOTS = 32;
    /** 视为「外边缘」的一像素宽度：此区域左键走正常挖掘而非取出物品 */
    public static final double EDGE_SIZE = 1.0 / 16.0;
    /** 端口贴附关系重校验间隔（tick） */
    private static final int VALIDATE_INTERVAL = 20;
    /** 双击判定的最大间隔（tick） */
    private static final long DOUBLE_CLICK_INTERVAL = 5;
    /**
     * 左键拦截的续期窗口（tick）。
     *
     * <p>按住左键期间客户端会不断重触发开始破坏事件并照发包给服务端（创造模式约每 6 tick 一次），
     * 因此窗口必须大于该间隔：只要仍在窗口内就继续拦截，取空后同一次按住不会变成挖掘。</p>
     */
    private static final long INTERCEPT_HOLD_WINDOW = 10;

    @Getter
    private final ItemStacksResourceHandler buffer = new ItemStacksResourceHandler(StoragePortBlockEntity.BUFFER_SLOTS) {
        @Override
        public boolean isValid(int slot, ItemResource resource) {
            ItemStack marked = StoragePortBlockEntity.this.markedItem;
            return marked.isEmpty() || ItemResource.of(marked).equals(resource);
        }

        @Override
        protected void onContentsChanged(int slot, ItemStack previousContents) {
            StoragePortBlockEntity.this.setChanged();
            if (StoragePortBlockEntity.this.storageId != null) {
                StorageServerStub.onContentsChanged(StoragePortBlockEntity.this.storageId);
            }
            if (StoragePortBlockEntity.this.level != null) {
                StoragePortBlockEntity.this.level.sendBlockUpdated(
                    getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL
                );
                if (!StoragePortBlockEntity.this.level.isClientSide()) {
                    StoragePortBlockEntity.this.level.updateNeighbourForOutputSignal(getBlockPos(), getBlockState().getBlock());
                }
            }
        }
    };

    @Getter
    private ItemStack markedItem = ItemStack.EMPTY;
    /** 组件解析出的核心（潜影集装箱 / 超维存储站）主方块坐标；null 表示组件无效 */
    @Nullable
    private BlockPos coreMainPos = null;
    /** 组件归属的存储 ID（登记进 {@link StoragePortManager} 用）；null 表示未接上核心 */
    @Nullable
    private UUID storageId = null;
    /** 当前是否工作（连通组件恰好接触一个有效核心） */
    @Getter
    private boolean working;
    private final Object2LongMap<UUID> lastRightClickTicks = new Object2LongOpenHashMap<>();
    /** 各玩家最近一次左键取出/拦截的 tick：按住期间持续续期，松手后窗口过期即恢复挖掘 */
    private final Object2LongMap<UUID> leftClickHoldTicks = new Object2LongOpenHashMap<>();
    private int validateCountdown = 0;
    private int workCountdown = 0;
    private final ResourceHandler<ItemResource> inventory = new PortInventory();
    private boolean availableItems;

    public StoragePortBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    /**
     * 服务端主循环：周期性重校验连通关系，并按性能墙限速执行物品转移。
     */
    public void tickServer() {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        if (this.validateCountdown-- <= 0) {
            this.validateCountdown = StoragePortBlockEntity.VALIDATE_INTERVAL;
            this.validateLink();
        }
        if (this.workCountdown-- > 0) {
            return;
        }
        var config = AnvilCraft.CONFIG.machines.storagePort;
        this.workCountdown = config.workInterval;
        this.refreshAvailableItems();
        if (this.working) {
            this.performTransfer(config.maxItemsPerScan);
        }
    }

    /**
     * 记录一次右键并判断是否为双击（距上次右键不超过 {@link #DOUBLE_CLICK_INTERVAL} tick）。
     */
    public boolean isDoubleClick(Player player) {
        if (this.level == null) {
            return false;
        }
        long now = this.level.getGameTime();
        UUID uuid = player.getUUID();
        long last = this.lastRightClickTicks.getLong(uuid);
        this.lastRightClickTicks.put(uuid, now);
        return last != 0 && now - last <= StoragePortBlockEntity.DOUBLE_CLICK_INTERVAL;
    }

    public static ItemStack createMarker(ItemStack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        return stack.getItem() instanceof StoragePortBlockItem ? new ItemStack(stack.getItem()) : stack.copyWithCount(1);
    }

    public boolean isMarkedFaceVisible(Direction direction) {
        if (this.level == null) return false;
        BlockPos neighborPos = this.worldPosition.relative(direction);
        BlockState neighbor = this.level.getBlockState(neighborPos);
        return !(neighbor.getBlock() instanceof StoragePortBlock)
            && !(neighbor.canOcclude()
                && Block.isFaceFull(neighbor.getOcclusionShape(), direction.getOpposite()));
    }

    /**
     * 设置标记物品；已连接时先把内部库存完整存入核心，容量不足则保留原标记。
     */
    public void setMarkedItem(ItemStack stack) {
        ItemStack mark = StoragePortBlockEntity.createMarker(stack);
        if (ItemStack.isSameItemSameComponents(this.markedItem, mark)) {
            return;
        }
        if (this.level != null && !this.level.isClientSide() && !this.pushAllToCore()) {
            return;
        }
        this.markedItem = mark;
        if (this.level != null && !this.level.isClientSide()) {
            BlockState state = this.level.getBlockState(this.worldPosition);
            boolean shouldMark = !mark.isEmpty();
            if (state.hasProperty(StoragePortBlock.MARKED)
                && state.getValue(StoragePortBlock.MARKED) != shouldMark) {
                this.level.setBlock(this.worldPosition, state.setValue(StoragePortBlock.MARKED, shouldMark), 3);
            }
        }
        this.setChanged();
        this.refreshAvailableItems();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
    }

    /**
     * 把手中物品存入端口，一次最多 {@code maxCount} 个；扣减手中堆叠。
     */
    public void stuffFromHand(ItemStack held, int maxCount) {
        if (held.isEmpty() || maxCount <= 0) {
            return;
        }
        int before = held.getCount();
        ItemStack toInsert = held.copyWithCount(Math.min(before, maxCount));
        toInsert = ItemHandlerUtil.insertItem(this.inventory, toInsert, false);
        int inserted = Math.min(before, maxCount) - toInsert.getCount();
        if (inserted > 0) {
            held.shrink(inserted);
            this.refreshAvailableItems();
        }
    }

    /**
     * 把玩家身上所有与标记相同种类的物品存入端口。
     */
    public void stuffAllFromPlayer(Player player) {
        ItemStack mark = this.markedItem;
        if (mark.isEmpty()) {
            return;
        }
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(mark, stack)) {
                continue;
            }
            int before = stack.getCount();
            ItemStack toInsert = stack.copy();
            toInsert = ItemHandlerUtil.insertItem(this.inventory, toInsert, false);
            int inserted = before - toInsert.getCount();
            if (inserted > 0) {
                stack.shrink(inserted);
                if (stack.isEmpty()) {
                    inventory.setItem(i, ItemStack.EMPTY);
                }
            } else if (toInsert.getCount() == before) {
                break;
            }
        }
        this.refreshAvailableItems();
    }

    /**
     * 按大仓库优先顺序取出物品交给玩家；
     *
     * @param fullStack true 取出一组，false 只取 1 个
     */
    public void giveToPlayer(Player player, boolean fullStack) {
        for (int slot = 0; slot < this.inventory.size(); slot++) {
            ItemStack stack = stackAt(this.inventory, slot);
            if (stack.isEmpty()) {
                continue;
            }
            int amount = fullStack ? stack.getMaxStackSize() : 1;
            ItemStack extracted = extractAt(this.inventory, slot, amount);
            this.refreshAvailableItems();
            if (extracted.isEmpty()) {
                return;
            }
            if (this.level != null && !player.addItem(extracted)) {
                Block.popResource(this.level, BlockPos.containing(player.position()), extracted);
            }
            return;
        }
    }

    /**
     * 左键取出缓存内物品：shift 取一组，否则只取 1 个。
     */
    @Override
    public boolean onLeftClick(Player player, List<IStoragePort> ports) {
        this.giveToPlayer(player, player.isShiftKeyDown());
        // 取出即说明玩家正在左键该端口：立刻续期拦截窗口。客户端是先发取出包、后发开始破坏包
        // （事件在发送包的预测 lambda 内触发），服务端处理紧随其后的开始破坏包时缓存已空，
        // 没有这一步的话创造模式下同一次点击就会把端口打掉。
        this.armLeftClickHold(player);
        return true;
    }

    /**
     * 右键：未标记时把手中物品标记并塞入，已标记时按标记塞入，双击塞入身上全部。
     */
    @Override
    public boolean onRightClick(Player player, InteractionHand hand, List<IStoragePort> ports) {
        ItemStack stack = player.getItemInHand(hand);
        // 铁砧锤：普通右键不调整任何状态（去标记需长按右键并滑动，见客户端手势）
        if (stack.getItem() instanceof AnvilHammerItem) {
            return false;
        }
        boolean doubleClick = this.isDoubleClick(player);
        ItemStack mark = this.markedItem;
        if (mark.isEmpty()) {
            // 未标记：手持物品右键 → 标记并塞入最多一组
            if (!stack.isEmpty()) {
                this.setMarkedItem(stack);
                this.stuffFromHand(stack, stack.getMaxStackSize());
            }
            return true;
        }
        if (stack.isEmpty()) {
            // 已标记 + 空手：单击不做任何事（取出走左键）；双击塞入身上全部
            // （第一次点击已把手上的物品塞入并完成标记，故第二次点击时手可能已空）
            if (doubleClick) {
                this.stuffAllFromPlayer(player);
            }
            return true;
        }
        if (ItemStack.isSameItemSameComponents(mark, stack)) {
            // 对应物品：单击塞入最多一组，双击塞入身上全部
            if (doubleClick) {
                this.stuffAllFromPlayer(player);
            } else {
                this.stuffFromHand(stack, stack.getMaxStackSize());
            }
            return true;
        }
        // 不对应物品：不替换标记
        return true;
    }

    /**
     * 是否拦截左键：缓存非空时取出物品；一旦开始取出，该玩家按住左键期间持续拦截，
     * 避免取空后同一次按住被判定成挖掘（创造模式下会直接打掉方块）。
     * 命中模型外边缘的半像素框架、或空端口且该玩家未在按住取出时不拦截，允许正常挖掘。
     */
    @Override
    public boolean interceptsLeftClick(Player player, @Nullable BlockHitResult hit) {
        if (!this.isLeftClickHeld(player) && !this.hasAvailableItems()) {
            // 空端口且该玩家没有正在进行的取出：不拦截，允许正常挖掘
            return false;
        }
        if (
            hit != null
            && hit.getBlockPos().equals(this.worldPosition)
            && StoragePortBlockEntity.isEdgeHit(hit)
        ) {
            // 命中模型外边缘的一像素框架：走挖掘，不取出
            return false;
        }
        this.armLeftClickHold(player);
        return true;
    }

    /**
     * 标记该玩家正在按住左键取出：续期拦截窗口，见 {@link StoragePortBlockEntity#INTERCEPT_HOLD_WINDOW}。
     */
    private void armLeftClickHold(Player player) {
        if (this.level != null) {
            this.leftClickHoldTicks.put(player.getUUID(), this.level.getGameTime());
        }
    }

    /**
     * 该玩家上一次左键拦截是否仍在续期窗口内。
     *
     * <p>按住左键时客户端会不断重触发事件，每次判定为拦截都会续期，
     * 因此只要手没松就一直拦截；松手后事件停止，窗口过期即恢复挖掘。</p>
     */
    private boolean isLeftClickHeld(Player player) {
        if (this.level == null) {
            return false;
        }
        long last = this.leftClickHoldTicks.getLong(player.getUUID());
        return last != 0
            && this.level.getGameTime() - last <= StoragePortBlockEntity.INTERCEPT_HOLD_WINDOW;
    }

    /**
     * 判断左键点击点是否落在方块外边缘的一像素（1/16）框上。
     *
     * <p>模型外侧是一圈细边框，点击该区域应走正常挖掘逻辑而非取出物品。</p>
     */
    public static boolean isEdgeHit(@Nullable BlockHitResult hit) {
        if (hit == null) {
            return false;
        }
        Vec3 location = hit.getLocation();
        BlockPos pos = hit.getBlockPos();
        double fx = location.x - pos.getX();
        double fy = location.y - pos.getY();
        double fz = location.z - pos.getZ();
        return switch (hit.getDirection().getAxis()) {
            case X -> StoragePortBlockEntity.isEdgeCoordinate(fy) || StoragePortBlockEntity.isEdgeCoordinate(fz);
            case Y -> StoragePortBlockEntity.isEdgeCoordinate(fx) || StoragePortBlockEntity.isEdgeCoordinate(fz);
            case Z -> StoragePortBlockEntity.isEdgeCoordinate(fx) || StoragePortBlockEntity.isEdgeCoordinate(fy);
        };
    }

    private static boolean isEdgeCoordinate(double value) {
        return value <= StoragePortBlockEntity.EDGE_SIZE || value >= 1.0 - StoragePortBlockEntity.EDGE_SIZE;
    }

    /**
     * 掉落缓存内全部物品（仅用于异常路径）。
     */
    public void dropContents(Level level, BlockPos pos) {
        for (int slot = 0; slot < this.buffer.size(); slot++) {
            ItemStack stack = stackAt(this.buffer, slot);
            if (stack.isEmpty()) {
                continue;
            }
            Block.popResource(level, pos, stack);
            this.buffer.set(slot, ItemResource.EMPTY, 0);
        }
    }

    /**
     * 缓存是否为空（缓存空且未在按住取出时，左键不拦截，允许正常挖掘）。
     */
    public boolean isBufferEmpty() {
        for (int slot = 0; slot < this.buffer.size(); slot++) {
            if (!stackAt(this.buffer, slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 把缓存与标记写入掉落的方块物品（拆除保留内容）。
     *
     * <p>缓存为空且无标记时不写入方块实体数据：否则空端口拆下来会多出一个
     * 内容为 {@code {buffer: {Size: 32, Items: []}}} 的组件，无法与未放置过的物品堆叠。</p>
     */
    public void saveToDrop(ItemStack stack, HolderLookup.Provider registries) {
        if (this.isBufferEmpty() && this.markedItem.isEmpty()) {
            return;
        }
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        this.saveAdditional(output);
        BlockItem.setBlockEntityData(stack, this.getType(), output);
        stack.applyComponents(this.collectComponents());
    }

    /**
     * 重新解析连通组件：从本端口沿面相邻的端口链延伸，
     * 收集组件接触到的核心，恰好一个核心时端口工作。
     */
    private void validateLink() {
        if (this.level == null) {
            return;
        }
        BlockPos core = StoragePortManager.findSoleCore(this.level, this.worldPosition);
        UUID nextId = core != null && this.level.getBlockEntity(core) instanceof StorageBlockEntity storage
            ? storage.getId() : null;
        if (!Objects.equals(this.storageId, nextId)) {
            StoragePortManager.unregister(this.storageId, this.level.dimension(), this.worldPosition);
        }
        this.coreMainPos = core;
        this.storageId = nextId;
        this.working = nextId != null;
        AbstractStoragePortBlock.refreshType(this.level, this.worldPosition, core);
        if (nextId != null) {
            StoragePortManager.register(nextId, this.level, this.worldPosition);
        }
    }

    /** 未标记时将已有内部库存限速存入核心。 */
    private void performTransfer(int maxItemsPerScan) {
        BaseStorage<?> core = this.getCoreStorage();
        if (core != null && this.markedItem.isEmpty()) {
            ItemHandlerUtil.exportToTarget(this.buffer, maxItemsPerScan, (resource, amount) -> true, core.getItems());
        }
    }

    private boolean pushAllToCore() {
        BaseStorage<?> core = this.getCoreStorage();
        if (core == null) {
            return true;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            for (int slot = 0; slot < this.buffer.size(); slot++) {
                ItemResource resource = this.buffer.getResource(slot);
                int amount = this.buffer.getAmountAsInt(slot);
                if (resource.isEmpty() || amount == 0) {
                    continue;
                }
                if (core.getItems().insert(resource, amount, transaction) != amount) {
                    return false;
                }
                this.buffer.extract(slot, resource, amount, transaction);
            }
            transaction.commit();
        }
        return true;
    }

    private @Nullable BaseStorage<?> getCoreStorage() {
        if (this.level == null || this.level.isClientSide() || this.isRemoved() || this.coreMainPos == null
            || !this.level.isLoaded(this.worldPosition) || !this.level.isLoaded(this.coreMainPos)) {
            return null;
        }
        if (this.level.getBlockEntity(this.coreMainPos) instanceof StorageBlockEntity storage
            && storage.getId() != null && storage.getId().equals(this.storageId)) {
            return Storages.get().getOrCreate(storage.getId(), storage.getStorageType().clazz());
        }
        return null;
    }

    /** 判断端口仍属于指定大仓库，避免旧能力句柄访问已经断开的内部库存。 */
    public boolean isConnectedTo(UUID id) {
        return !this.isRemoved() && this.working && id.equals(this.storageId) && this.level != null
            && this.level.isLoaded(this.worldPosition) && this.getCoreStorage() != null;
    }

    private void refreshAvailableItems() {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        boolean available = this.hasAvailableItems();
        if (available != this.availableItems) {
            this.availableItems = available;
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private boolean hasAvailableItems() {
        ItemResource mark = ItemResource.of(this.markedItem);
        if (mark.isEmpty()) {
            return false;
        }
        if (this.level != null && this.level.isClientSide() && this.availableItems) {
            return true;
        }
        BaseStorage<?> core = this.getCoreStorage();
        ResourceHandler<ItemResource> target = core == null ? this.buffer : core.getAvailableItems();
        for (int slot = 0; slot < target.size(); slot++) {
            if (target.getAmountAsLong(slot) > 0 && target.getResource(slot).equals(mark)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ResourceHandler<ItemResource> getItemHandler() {
        return this.inventory;
    }

    private final class PortInventory implements ResourceHandler<ItemResource> {
        private ResourceHandler<ItemResource> target() {
            BaseStorage<?> storage = StoragePortBlockEntity.this.getCoreStorage();
            return storage == null ? StoragePortBlockEntity.this.buffer : storage.getAvailableItems();
        }

        private boolean linkedMarked() {
            return !StoragePortBlockEntity.this.markedItem.isEmpty()
                && StoragePortBlockEntity.this.getCoreStorage() != null;
        }

        @Override
        public int size() {
            return this.linkedMarked() ? 1 : this.target().size();
        }

        @Override
        public ItemResource getResource(int index) {
            ItemResource marked = ItemResource.of(StoragePortBlockEntity.this.markedItem);
            if (marked.isEmpty()) {
                return ItemResource.EMPTY;
            }
            if (this.linkedMarked()) {
                return index == 0 ? marked : ItemResource.EMPTY;
            }
            ResourceHandler<ItemResource> target = this.target();
            return index >= 0 && index < target.size() && target.getResource(index).equals(marked) ? marked : ItemResource.EMPTY;
        }

        @Override
        public long getAmountAsLong(int index) {
            if (this.getResource(index).isEmpty()) {
                return 0;
            }
            ResourceHandler<ItemResource> target = this.target();
            if (!this.linkedMarked()) {
                return index >= 0 && index < target.size() ? target.getAmountAsLong(index) : 0;
            }
            if (index != 0) {
                return 0;
            }
            ItemResource marked = ItemResource.of(StoragePortBlockEntity.this.markedItem);
            long amount = 0;
            for (int slot = 0; slot < target.size(); slot++) {
                if (target.getResource(slot).equals(marked)) {
                    amount += target.getAmountAsLong(slot);
                }
            }
            return amount;
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            if (!resource.isEmpty() && !this.isValid(index, resource)) {
                return 0;
            }
            if (this.linkedMarked()) {
                return index == 0 ? Integer.MAX_VALUE : 0;
            }
            ResourceHandler<ItemResource> target = this.target();
            return index >= 0 && index < target.size() ? target.getCapacityAsLong(index, resource) : 0;
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            ItemStack marked = StoragePortBlockEntity.this.markedItem;
            if (!marked.isEmpty() && !ItemResource.of(marked).equals(resource)) {
                return false;
            }
            if (this.linkedMarked()) {
                BaseStorage<?> storage = StoragePortBlockEntity.this.getCoreStorage();
                return index == 0 && storage != null && storage.getItems().isValid(0, resource);
            }
            ResourceHandler<ItemResource> target = this.target();
            return index >= 0 && index < target.size() && target.isValid(index, resource);
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            if (!this.isValid(index, resource)) {
                return 0;
            }
            return this.linkedMarked() ? this.target().insert(resource, amount, transaction)
                : this.target().insert(index, resource, amount, transaction);
        }

        @Override
        public int insert(ItemResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            ItemStack marked = StoragePortBlockEntity.this.markedItem;
            if (!marked.isEmpty() && !ItemResource.of(marked).equals(resource)) {
                return 0;
            }
            return this.target().insert(resource, amount, transaction);
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            if (!this.getResource(index).equals(resource)) {
                return 0;
            }
            return this.linkedMarked() ? this.target().extract(resource, amount, transaction)
                : this.target().extract(index, resource, amount, transaction);
        }
    }

    /** 当前标记物品的总量，包括关联核心及端口缓存，保留超过普通物品堆的计数。 */
    public long getStoredCount() {
        if (this.markedItem.isEmpty()) {
            return 0;
        }
        BaseStorage<?> storage = this.getCoreStorage();
        ResourceHandler<ItemResource> handler = storage == null ? this.buffer : storage.getAvailableItems();
        ItemResource marker = ItemResource.of(this.markedItem);
        long amount = 0;
        for (int slot = 0; slot < handler.size(); slot++) {
            if (handler.getResource(slot).equals(marker)) {
                amount += handler.getAmountAsLong(slot);
            }
        }
        return amount;
    }

    /** 供库存提示读取完整存量，不应用于自动化提取的标记判定。 */
    public ResourceHandler<ItemResource> getItemHandlerForDisplay() {
        BaseStorage<?> core = this.getCoreStorage();
        return core == null ? this.buffer : core.getAvailableItems();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        this.saveAdditional(output);
        this.buffer.serialize(output.child("buffer"));
        output.putBoolean("available_items", this.hasAvailableItems());
        return output.buildResult();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.markedItem.isEmpty()) output.store("marked_item", ItemStack.CODEC, this.markedItem);
        if (!this.isBufferEmpty()) this.buffer.serialize(output.child("buffer"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.markedItem = createMarker(input.read("marked_item", ItemStack.CODEC).orElse(ItemStack.EMPTY));
        ItemHandlerUtil.deserializeCompatibleStacks(this.buffer, input.childOrEmpty("buffer"));
        this.availableItems = input.getBooleanOr("available_items", false);
    }

    private static ItemStack stackAt(ResourceHandler<ItemResource> handler, int slot) {
        return handler.getResource(slot).toStack(handler.getAmountAsInt(slot));
    }

    private static ItemStack extractAt(ResourceHandler<ItemResource> handler, int slot, int amount) {
        ItemResource resource = handler.getResource(slot);
        if (resource.isEmpty()) return ItemStack.EMPTY;
        try (Transaction transaction = Transaction.openRoot()) {
            int extracted = handler.extract(slot, resource, amount, transaction);
            transaction.commit();
            return resource.toStack(extracted);
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (this.level != null) {
            StoragePortManager.unregister(this.storageId, this.level.dimension(), this.worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level != null) {
            StoragePortManager.unregister(this.storageId, this.level.dimension(), this.worldPosition);
        }
        this.storageId = null;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // 放置带有存档数据的方块物品时，让方块状态的 MARKED 与数据一致
        if (this.level != null && this.level.getBlockState(this.worldPosition).hasProperty(StoragePortBlock.MARKED)) {
            boolean shouldMark = !this.markedItem.isEmpty();
            BlockState state = this.level.getBlockState(this.worldPosition);
            if (state.getValue(StoragePortBlock.MARKED) != shouldMark) {
                this.level.setBlock(this.worldPosition, state.setValue(StoragePortBlock.MARKED, shouldMark), 3);
            }
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
