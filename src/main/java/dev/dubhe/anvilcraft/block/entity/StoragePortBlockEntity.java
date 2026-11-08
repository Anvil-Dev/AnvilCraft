package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.IStoragePort;
import dev.dubhe.anvilcraft.api.StoragePortManager;
import dev.dubhe.anvilcraft.api.itemhandler.IItemHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler;
import dev.dubhe.anvilcraft.block.AbstractStoragePortBlock;
import dev.dubhe.anvilcraft.block.StoragePortBlock;
import dev.dubhe.anvilcraft.block.entity.storage.CrateBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.block.item.StoragePortBlockItem;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * 仓储端口连接大仓库后直接存取共享库存，输入经过自身的 32 格缓存，已连接时按标记从仓储提取。
 */
public class StoragePortBlockEntity extends BlockEntity implements IItemHandlerHolder, IStoragePort {
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
    private final ItemStackHandler buffer = new ItemStackHandler(StoragePortBlockEntity.BUFFER_SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            ItemStack marked = StoragePortBlockEntity.this.markedItem;
            return marked.isEmpty() || ItemStack.isSameItemSameComponents(marked, stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            StoragePortBlockEntity.this.setChanged();
            StoragePortBlockEntity.this.refreshAvailableItems();
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
    private boolean linkedItemsAvailable;
    private final IItemHandler itemHandler = new IItemHandler() {
        private boolean linkedMarked() {
            return !StoragePortBlockEntity.this.markedItem.isEmpty()
                && StoragePortBlockEntity.this.getCoreHandler() != null;
        }

        @Override
        public int getSlots() {
            return BUFFER_SLOTS + (this.linkedMarked() ? 1 : 0);
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            ItemStack mark = StoragePortBlockEntity.this.markedItem;
            if (this.linkedMarked()) {
                return slot == BUFFER_SLOTS ? mark.copyWithCount((int) Math.min(Integer.MAX_VALUE,
                    StoragePortBlockEntity.this.getMarkedStorageCount())) : ItemStack.EMPTY;
            }
            if (slot < 0 || slot >= BUFFER_SLOTS) {
                return ItemStack.EMPTY;
            }
            ItemStack stack = StoragePortBlockEntity.this.buffer.getStackInSlot(slot);
            return mark.isEmpty() || ItemStack.isSameItemSameComponents(mark, stack) ? stack : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot >= 0 && slot < BUFFER_SLOTS
                ? StoragePortBlockEntity.this.buffer.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (amount <= 0) {
                return ItemStack.EMPTY;
            }
            if (this.linkedMarked()) {
                UnlimitedItemStacksResourceHandler core = StoragePortBlockEntity.this.getCoreHandler();
                return slot == BUFFER_SLOTS && core != null
                    ? core.extract(StoragePortBlockEntity.this.markedItem, amount, simulate) : ItemStack.EMPTY;
            }
            return this.getStackInSlot(slot).isEmpty() ? ItemStack.EMPTY
                : StoragePortBlockEntity.this.buffer.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return slot == BUFFER_SLOTS && this.linkedMarked() ? Integer.MAX_VALUE
                : slot >= 0 && slot < BUFFER_SLOTS ? StoragePortBlockEntity.this.buffer.getSlotLimit(slot) : 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot >= 0 && slot < BUFFER_SLOTS && StoragePortBlockEntity.this.buffer.isItemValid(slot, stack);
        }
    };

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
        if (this.workCountdown-- > 0) {
            return;
        }
        var config = AnvilCraft.CONFIG.machines.storagePort;
        this.workCountdown = config.workInterval;
        this.performTransfer(config.maxItemsPerScan);
        this.refreshAvailableItems();
    }

    private void refreshAvailableItems() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        boolean available = false;
        int slots = this.itemHandler.getSlots();
        for (int slot = 0; slot < slots; slot++) {
            if (!this.itemHandler.getStackInSlot(slot).isEmpty()) {
                available = true;
                break;
            }
        }
        if (this.linkedItemsAvailable != available) {
            this.linkedItemsAvailable = available;
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
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
                && Block.isFaceFull(neighbor.getOcclusionShape(this.level, neighborPos), direction.getOpposite()));
    }

    /**
     * 设置输出标记；缓存内容不随标记改变。
     */
    public void setMarkedItem(ItemStack stack) {
        ItemStack mark = StoragePortBlockEntity.createMarker(stack);
        if (ItemStack.isSameItemSameComponents(this.markedItem, mark)) {
            return;
        }
        this.markedItem = mark;
        if (this.level != null && !this.level.isClientSide) {
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
     * 把手中物品存入连接的仓库或内部容器，一次最多 {@code maxCount} 个；扣减手中堆叠。
     */
    public void stuffFromHand(ItemStack held, int maxCount) {
        if (held.isEmpty() || maxCount <= 0) {
            return;
        }
        int before = held.getCount();
        ItemStack toInsert = held.copyWithCount(Math.min(before, maxCount));
        for (int slot = 0; slot < this.itemHandler.getSlots() && !toInsert.isEmpty(); slot++) {
            toInsert = this.itemHandler.insertItem(slot, toInsert, false);
        }
        int inserted = Math.min(before, maxCount) - toInsert.getCount();
        if (inserted > 0) {
            held.shrink(inserted);
        }
    }

    /**
     * 把玩家身上所有与标记相同种类的物品存入连接的仓库或内部容器。
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
            for (int slot = 0; slot < this.itemHandler.getSlots() && !toInsert.isEmpty(); slot++) {
                toInsert = this.itemHandler.insertItem(slot, toInsert, false);
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
     * 从连接的仓库或内部容器取出物品交给玩家；
     *
     * @param fullStack true 取出一组，false 只取 1 个
     */
    public void giveToPlayer(Player player, boolean fullStack) {
        for (int slot = 0; slot < this.itemHandler.getSlots(); slot++) {
            ItemStack stack = this.itemHandler.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            int amount = fullStack ? stack.getMaxStackSize() : 1;
            ItemStack extracted = this.itemHandler.extractItem(slot, amount, false);
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
     * 右键将手持物品存入缓存；首次设置标记，双击存入背包中的同种物品。
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
        this.stuffFromHand(stack, stack.getMaxStackSize());
        return true;
    }

    /**
     * 是否拦截左键：存在可取出的标记物品时取出物品；一旦开始取出，该玩家按住左键期间持续拦截，
     * 避免取空后同一次按住被判定成挖掘（创造模式下会直接打掉方块）。
     * 命中模型外边缘的半像素框架、或空端口且该玩家未在按住取出时不拦截，允许正常挖掘。
     */
    @Override
    public boolean interceptsLeftClick(Player player, @Nullable BlockHitResult hit) {
        this.refreshAvailableItems();
        if (!this.isLeftClickHeld(player) && !this.linkedItemsAvailable) {
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
     * 缓存是否为空（缓存空且未在按住取出时，左键不拦截，允许正常挖掘）。
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
     *
     * <p>缓存为空且无标记时不写入方块实体数据：否则空端口拆下来会多出一个
     * 内容为 {@code {buffer: {Size: 32, Items: []}}} 的组件，无法与未放置过的物品堆叠。</p>
     */
    public void saveToDrop(ItemStack stack, HolderLookup.Provider registries) {
        if (this.isBufferEmpty() && this.markedItem.isEmpty()) {
            return;
        }
        CompoundTag tag = this.saveCustomOnly(registries);
        BlockItem.setBlockEntityData(stack, this.getType(), tag);
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
        UUID id = core != null && this.level.getBlockEntity(core) instanceof StorageBlockEntity storage
            ? storage.getId() : null;
        boolean changed = !java.util.Objects.equals(this.storageId, id);
        if (changed) {
            StoragePortManager.unregister(this.storageId, this.level.dimension(), this.worldPosition);
            this.storageId = id;
            if (id != null) {
                StoragePortManager.register(id, this.level, this.worldPosition);
            }
        }
        this.coreMainPos = core;
        AbstractStoragePortBlock.refreshType(this.level, this.worldPosition, core);
        this.working = id != null;
        if (changed && this.working) {
            this.performTransfer(Integer.MAX_VALUE);
        }

    }

    /** 缓存合并只遍历本端口槽位，仓储按物品索引接收。 */
    private void performTransfer(int limit) {
        UnlimitedItemStacksResourceHandler core = this.getCoreHandler();
        if (core == null) {
            return;
        }
        for (int slot = 0; slot < BUFFER_SLOTS && limit > 0; slot++) {
            ItemStack stack = this.buffer.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            int requested = Math.min(limit, stack.getCount());
            int inserted = requested - core.insertItem(stack.copyWithCount(requested), false).getCount();
            this.buffer.extractItem(slot, inserted, false);
            limit -= inserted;
        }
    }

    /** 检查端口是否仍连接到指定存储，供共享库存排除已失效端口。 */
    public boolean isConnectedTo(UUID id) {
        return !this.isRemoved() && id.equals(this.storageId) && this.getCoreHandler() != null;
    }

    @Nullable
    private UnlimitedItemStacksResourceHandler getCoreHandler() {
        if (this.level == null || this.level.isClientSide || this.isRemoved() || this.coreMainPos == null
            || !this.level.isLoaded(this.coreMainPos)) {
            return null;
        }
        if (this.level.getBlockEntity(this.coreMainPos) instanceof StorageBlockEntity storage) {
            UUID id = storage.getId();
            if (id == null || !id.equals(this.storageId)) {
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
        return this.itemHandler;
    }

    /** 仓储数量只查询标记物品，缓存通过独立的固定槽位展示。 */
    public long getMarkedStorageCount() {
        if (this.markedItem.isEmpty()) {
            return 0;
        }
        UnlimitedItemStacksResourceHandler core = this.getCoreHandler();
        return core == null ? 0 : core.count(this.markedItem);
    }

    public long getStoredCount() {
        if (this.getCoreHandler() != null) {
            return this.getMarkedStorageCount();
        }
        long count = 0;
        for (int slot = 0; slot < BUFFER_SLOTS; slot++) {
            ItemStack stack = this.buffer.getStackInSlot(slot);
            if (ItemStack.isSameItemSameComponents(stack, this.markedItem)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        // 同步缓存内容，保证客户端左键取出与物流输出等操作显示一致，避免幽灵物品；为空也要写，否则客户端会残留旧内容
        tag.put("buffer", this.buffer.serializeNBT(registries));
        tag.putBoolean("linked_items_available", this.linkedItemsAvailable);
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
        // 缓存为空时不写 handler 数据：空端口被存成物品（掉落、存档数据打包等）不该多出
        // 内容为 {buffer: {Size: 32}} 的组件，否则会与未放置过的端口无法堆叠
        if (!this.isBufferEmpty()) {
            tag.put("buffer", this.buffer.serializeNBT(registries));
        }
    }

    @Override
    public void saveToItem(ItemStack stack, HolderLookup.Provider registries) {
        // 通用的方块实体打包成物品入口同样走带守卫的掉落逻辑
        this.saveToDrop(stack, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.linkedItemsAvailable = tag.getBoolean("linked_items_available");
        // 标签不含 marked_item 时视为已清除标记，避免客户端残留旧标记
        this.markedItem = tag.contains("marked_item")
            ? StoragePortBlockEntity.createMarker(ItemStack.parseOptional(registries, tag.getCompound("marked_item")))
            : ItemStack.EMPTY;
        if (tag.contains("buffer")) {
            this.buffer.deserializeNBT(registries, tag.getCompound("buffer"));
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
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
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
