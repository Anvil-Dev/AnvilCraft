package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.itemhandler.IItemHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import dev.dubhe.anvilcraft.block.StoragePortBlock;
import dev.dubhe.anvilcraft.block.container.storage.HyperdimensionStorageStationBlock;
import dev.dubhe.anvilcraft.block.container.storage.ShulkerContainerBlock;
import dev.dubhe.anvilcraft.block.entity.storage.CrateBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.config.AnvilCraftServerConfig;
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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * 仓储端口方块实体。
 *
 * <p>潜影集装箱 / 超维存储站作为核心，面相邻的仓储端口可沿端口链延伸连接；
 * 整个连通组件必须恰好接触一个核心才工作（紧贴两个核心的组件不工作）。
 * 内部有 32 格缓存，可通过溜槽 / 漏斗等输入输出。</p>
 *
 * <ul>
 *   <li>未标记：缓存无类型限制，内部物品按「性能墙」限速尝试存入核心；</li>
 *   <li>已标记：只接受标记物品，并尽量在缓存中维持 1 组；不足时从核心取 1 组，
 *       超出时把多余部分存入核心，同样受性能墙限速。</li>
 * </ul>
 */
public class StoragePortBlockEntity extends BlockEntity implements IItemHandlerHolder {
    /** 缓存格数 */
    public static final int BUFFER_SLOTS = 32;
    /** 视为「外边缘」的半像素宽度：此区域左键走正常挖掘而非取出物品 */
    public static final double EDGE_SIZE = 1.0 / 32.0;
    /** 端口贴附关系重校验间隔（tick） */
    private static final int VALIDATE_INTERVAL = 20;
    /** 连通性扫描的端口访问上限，防止极端链式摆放造成性能问题 */
    private static final int CONNECTIVITY_LIMIT = 512;
    /** 双击判定的最大间隔（tick） */
    private static final long DOUBLE_CLICK_INTERVAL = 5;
    /** 长按左键时客户端取出请求的节流间隔（tick）：间隔大于该值时才会发包 */
    private static final long TAKE_OUT_HOLD_INTERVAL = 1;

    @Getter
    private final ItemStackHandler buffer = new ItemStackHandler(StoragePortBlockEntity.BUFFER_SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            ItemStack marked = StoragePortBlockEntity.this.markedItem;
            return marked.isEmpty() || ItemStack.isSameItemSameComponents(marked, stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            StoragePortBlockEntity.this.setChanged();
            if (StoragePortBlockEntity.this.level != null) {
                StoragePortBlockEntity.this.level.sendBlockUpdated(
                    getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL
                );
            }
        }
    };

    @Getter
    private ItemStack markedItem = ItemStack.EMPTY;
    /** 组件解析出的核心（潜影集装箱 / 超维存储站）主方块坐标；null 表示组件无效 */
    @Nullable
    private BlockPos coreMainPos = null;
    /** 当前是否工作（连通组件恰好接触一个有效核心） */
    @Getter
    private boolean working;
    private final Object2LongMap<UUID> lastRightClickTicks = new Object2LongOpenHashMap<>();
    private final Object2LongMap<UUID> lastTakeOutTicks = new Object2LongOpenHashMap<>();
    private int validateCountdown = 0;
    private int workCountdown = 0;

    public StoragePortBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    /**
     * 服务端主循环：周期性重校验连通关系，并按性能墙限速执行物品转移。
     */
    public void tickServer() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        if (this.validateCountdown-- <= 0) {
            this.validateCountdown = StoragePortBlockEntity.VALIDATE_INTERVAL;
            this.validateLink();
        }
        if (!this.working) {
            return;
        }
        if (this.workCountdown-- > 0) {
            return;
        }
        AnvilCraftServerConfig.StoragePort config = AnvilCraft.CONFIG.storagePort;
        this.workCountdown = config.workInterval;
        this.performTransfer(config.maxItemsPerScan);
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

    /**
     * 设置标记物品；标记变化时把缓存中旧内容尽力存入核心，并同步方块状态的 MARKED 属性。
     */
    public void setMarkedItem(ItemStack stack) {
        ItemStack mark = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        if (ItemStack.isSameItemSameComponents(this.markedItem, mark)) {
            return;
        }
        this.markedItem = mark;
        if (this.level != null && !this.level.isClientSide) {
            this.pushAllToCore();
            BlockState state = this.level.getBlockState(this.worldPosition);
            boolean shouldMark = !mark.isEmpty();
            if (state.hasProperty(StoragePortBlock.MARKED)
                && state.getValue(StoragePortBlock.MARKED) != shouldMark) {
                this.level.setBlock(this.worldPosition, state.setValue(StoragePortBlock.MARKED, shouldMark), 3);
            }
        }
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
    }

    /**
     * 把手中物品塞入缓存，一次最多 {@code maxCount} 个；扣减手中堆叠。
     */
    public void stuffFromHand(ItemStack held, int maxCount) {
        if (held.isEmpty() || maxCount <= 0) {
            return;
        }
        int before = held.getCount();
        ItemStack toInsert = held.copyWithCount(Math.min(before, maxCount));
        for (int slot = 0; slot < this.buffer.getSlots() && !toInsert.isEmpty(); slot++) {
            toInsert = this.buffer.insertItem(slot, toInsert, false);
        }
        int inserted = Math.min(before, maxCount) - toInsert.getCount();
        if (inserted > 0) {
            held.shrink(inserted);
        }
    }

    /**
     * 把玩家身上所有与标记相同种类的物品塞入缓存（缓存放不下时停止）。
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
            for (int slot = 0; slot < this.buffer.getSlots() && !toInsert.isEmpty(); slot++) {
                toInsert = this.buffer.insertItem(slot, toInsert, false);
            }
            int inserted = before - toInsert.getCount();
            if (inserted > 0) {
                stack.shrink(inserted);
                if (stack.isEmpty()) {
                    inventory.setItem(i, ItemStack.EMPTY);
                }
            } else if (toInsert.getCount() == before) {
                // 一格都塞不进说明缓存已满
                break;
            }
        }
    }

    /**
     * 从缓存取出一部分物品交给玩家；
     *
     * @param fullStack true 取出一组，false 只取 1 个
     */
    public void giveToPlayer(Player player, boolean fullStack) {
        for (int slot = 0; slot < this.buffer.getSlots(); slot++) {
            ItemStack stack = this.buffer.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            int amount = fullStack ? stack.getMaxStackSize() : 1;
            ItemStack extracted = this.buffer.extractItem(slot, amount, false);
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
     * 判断左键点击点是否落在方块外边缘的半像素（1/32）框上。
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
        for (int slot = 0; slot < this.buffer.getSlots(); slot++) {
            ItemStack stack = this.buffer.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            Block.popResource(level, pos, stack);
            this.buffer.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    /**
     * 长按左键取出的客户端发包节流：按住左键时 {@code START} 事件会每 tick 重触发，
     * 节流保证不会每 tick 都发包。只有实际发包时才会记录时间；仅在客户端使用。
     */
    public boolean onTakeOutHoldCooldown(Player player) {
        if (this.level == null) {
            return false;
        }
        long now = this.level.getGameTime();
        UUID uuid = player.getUUID();
        long last = this.lastTakeOutTicks.getLong(uuid);
        if (last != 0 && now - last <= StoragePortBlockEntity.TAKE_OUT_HOLD_INTERVAL) {
            return true;
        }
        this.lastTakeOutTicks.put(uuid, now);
        return false;
    }

    /**
     * 缓存是否为空（缓存空时左键不拦截，允许正常挖掘）。
     */
    public boolean isBufferEmpty() {
        for (int slot = 0; slot < this.buffer.getSlots(); slot++) {
            if (!this.buffer.getStackInSlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 把缓存与标记写入掉落的方块物品（拆除保留内容）。
     */
    public void saveToDrop(ItemStack stack, HolderLookup.Provider registries) {
        CompoundTag tag = this.saveCustomOnly(registries);
        BlockItem.setBlockEntityData(stack, this.getType(), tag);
        stack.applyComponents(this.collectComponents());
    }

    /**
     * 重新解析连通组件：从本端口沿面相邻的端口链延伸，
     * 收集组件接触到的核心，恰好一个核心时端口工作。
     */
    private void validateLink() {
        this.working = false;
        this.coreMainPos = null;
        if (this.level == null) {
            return;
        }
        Set<BlockPos> cores = new HashSet<>();
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.addLast(this.worldPosition);
        visited.add(this.worldPosition);
        int visitedPorts = 0;
        while (!queue.isEmpty() && visitedPorts < StoragePortBlockEntity.CONNECTIVITY_LIMIT) {
            BlockPos pos = queue.removeFirst();
            visitedPorts++;
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = pos.relative(direction);
                if (visited.contains(neighbor)) {
                    continue;
                }
                BlockState state = this.level.getBlockState(neighbor);
                Block block = state.getBlock();
                BlockPos coreMain = null;
                if (block instanceof ShulkerContainerBlock shulker) {
                    coreMain = shulker.getMainPartPos(neighbor, state);
                } else if (block instanceof HyperdimensionStorageStationBlock station) {
                    coreMain = station.getMainPartPos(neighbor, state);
                }
                if (coreMain != null) {
                    if (this.level.getBlockEntity(coreMain) instanceof StorageBlockEntity storage
                        && storage.getId() != null) {
                        cores.add(coreMain);
                    }
                    continue;
                }
                if (block instanceof StoragePortBlock
                    && this.level.getBlockEntity(neighbor) instanceof StoragePortBlockEntity) {
                    visited.add(neighbor);
                    queue.addLast(neighbor);
                }
            }
        }
        // 连通组件必须恰好接触一个核心（紧贴两个核心则整条链不工作）
        if (cores.size() != 1) {
            return;
        }
        this.coreMainPos = cores.iterator().next();
        this.working = true;
    }

    /**
     * 按性能墙限速执行一次物品转移。
     */
    private void performTransfer(int maxItemsPerScan) {
        IItemHandler core = this.getCoreHandler();
        if (core == null) {
            return;
        }
        if (this.markedItem.isEmpty()) {
            // 未标记：缓存内所有物品都尝试存入核心
            ItemHandlerUtil.exportToTarget(this.buffer, maxItemsPerScan, stack -> true, core);
        } else {
            this.balanceMarkedItem(maxItemsPerScan, core);
        }
    }

    /**
     * 已标记模式下维持缓存中标记物品的数量为 1 组。
     */
    private void balanceMarkedItem(int maxItemsPerScan, IItemHandler core) {
        ItemStack mark = this.markedItem;
        int total = this.countMarkedItem(mark);
        int target = mark.getMaxStackSize();
        if (total < target) {
            int need = Math.min(target - total, maxItemsPerScan);
            ItemStack pulled = this.extractMarkedFromCore(mark, need);
            if (pulled.isEmpty()) {
                return;
            }
            ItemStack leftover = this.insertIntoBuffer(pulled);
            if (!leftover.isEmpty()) {
                ItemHandlerHelper.insertItem(core, leftover, false);
            }
        } else if (total > target) {
            int excess = Math.min(total - target, maxItemsPerScan);
            ItemStack toPush = this.extractMarkedFromBuffer(mark, excess);
            if (toPush.isEmpty()) {
                return;
            }
            ItemStack remainder = ItemHandlerHelper.insertItem(core, toPush, false);
            if (!remainder.isEmpty()) {
                this.insertIntoBuffer(remainder);
            }
        }
    }

    private int countMarkedItem(ItemStack mark) {
        int total = 0;
        for (int slot = 0; slot < this.buffer.getSlots(); slot++) {
            ItemStack stack = this.buffer.getStackInSlot(slot);
            if (ItemStack.isSameItemSameComponents(mark, stack)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private ItemStack extractMarkedFromCore(ItemStack mark, int amount) {
        IItemHandler core = this.getCoreHandler();
        if (core == null) {
            return ItemStack.EMPTY;
        }
        for (int slot = 0; slot < core.getSlots(); slot++) {
            ItemStack stack = core.getStackInSlot(slot);
            if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(mark, stack)) {
                continue;
            }
            return core.extractItem(slot, amount, false);
        }
        return ItemStack.EMPTY;
    }

    private ItemStack extractMarkedFromBuffer(ItemStack mark, int amount) {
        for (int slot = 0; slot < this.buffer.getSlots(); slot++) {
            ItemStack stack = this.buffer.getStackInSlot(slot);
            if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(mark, stack)) {
                continue;
            }
            return this.buffer.extractItem(slot, amount, false);
        }
        return ItemStack.EMPTY;
    }

    private ItemStack insertIntoBuffer(ItemStack stack) {
        for (int slot = 0; slot < this.buffer.getSlots() && !stack.isEmpty(); slot++) {
            stack = this.buffer.insertItem(slot, stack, false);
        }
        return stack;
    }

    /**
     * 把缓存内全部物品尽力存入核心（标记切换时的一次性清理）。
     */
    private void pushAllToCore() {
        IItemHandler core = this.getCoreHandler();
        if (core == null) {
            return;
        }
        ItemHandlerUtil.exportAllToTarget(this.buffer, stack -> true, core);
    }

    @Nullable
    private IItemHandler getCoreHandler() {
        if (this.level == null || this.coreMainPos == null) {
            return null;
        }
        if (this.level.getBlockEntity(this.coreMainPos) instanceof StorageBlockEntity storage) {
            UUID id = storage.getId();
            if (id == null) {
                return null;
            }
            // 板条箱核心：先同步 dispose 方块状态，使溢出销毁在物流路径同样生效
            if (storage instanceof CrateBlockEntity crate) {
                crate.refreshDispose();
            }
            return Storages.get().getOrCreate(id, storage.getStorageType().clazz()).getItems();
        }
        return null;
    }

    @Override
    public IItemHandler getItemHandler() {
        return this.buffer;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        // 同步缓存内容，保证客户端左键取出与物流输出等操作显示一致，避免幽灵物品
        tag.put("buffer", this.buffer.serializeNBT(registries));
        if (!this.markedItem.isEmpty()) {
            tag.put("marked_item", this.markedItem.save(registries));
        }
        return tag;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!this.markedItem.isEmpty()) {
            tag.put("marked_item", this.markedItem.save(registries));
        }
        tag.put("buffer", this.buffer.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // 标签不含 marked_item 时视为已清除标记，避免客户端残留旧标记
        this.markedItem = tag.contains("marked_item")
            ? ItemStack.parseOptional(registries, tag.getCompound("marked_item"))
            : ItemStack.EMPTY;
        if (tag.contains("buffer")) {
            this.buffer.deserializeNBT(registries, tag.getCompound("buffer"));
        }
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
