package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.IStoragePort;
import dev.dubhe.anvilcraft.api.StoragePortManager;
import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.IItemHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler.ResourceKey;
import dev.dubhe.anvilcraft.block.AbstractStoragePortBlock;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * 仓储端口整合器方块实体。
 *
 * <p>沿面相邻的仓储端口收集整个端口阵列，把各端口缓存的并集作为物品能力暴露给外部物流，
 * 并把相连的流体端口按「一个端口一格」暴露为流体能力，读写都直通该端口的储罐；
 * 接上集装箱时还会并入该存储名下登记的其它流体端口，避免外接端口只对集装箱可见。
 * 物品能力的相连关系只穿过端口，不跨集装箱 / 存储站，因此集装箱两侧没有直接相连的
 * 端口阵列不会互通；流体能力则按存储归属合并，同一存储名下的端口互通。
 * 流体能力的槽位顺序按「维度 + 坐标」固定，但该槽位集合仅服务端权威：
 * 客户端只看到面相邻端口，数量可能少于服务端。
 * 标记端口的缓存排在未标记端口之前，按槽位插入只访问对应缓存槽，
 * 按槽位遍历（如 {@link ItemHandlerHelper#insertItem}）即可填满整个阵列的缓存；
 * 相连端口标记物品的仓储输出槽也会并入物品能力，同一仓储的同种标记只暴露一次。
 * 未连接其他端口且直接接触唯一仓储核心时，物品能力在自身的 32 格缓存之外以只读方式
 * 暴露该仓储的全部槽位：外部只能从仓储取出物品，输入先进入缓存，再由服务端按性能墙并入仓储。</p>
 *
 * <p>右键为「塞入」：单击把手持物品塞入，双击把身上对应端口标记的物品全部塞入；
 * 手持流体容器时先尝试倒入相连的、已在存放同种流体的流体端口。</p>
 */
public class StoragePortConsolidatorBlockEntity extends BlockEntity
    implements IItemHandlerHolder, IFluidHandlerHolder, IStoragePort {
    /** 直连仓储时的缓存格数，与仓储端口的缓存一致 */
    public static final int BUFFER_SLOTS = StoragePortBlockEntity.BUFFER_SLOTS;
    /** 相连关系重校验间隔（tick） */
    private static final int VALIDATE_INTERVAL = 20;
    /** 双击判定的最大间隔（tick） */
    private static final long DOUBLE_CLICK_INTERVAL = 5;

    /** 相连的仓储端口坐标 */
    private final List<BlockPos> ports = new ArrayList<>();
    /** 与 {@link #ports} 一一对应的端口标记，避免每次槽位映射都去取方块实体 */
    private final List<ItemStack> marks = new ArrayList<>();
    /** 相连的流体端口坐标，与 {@link ConsolidatedFluidHandler} 的储罐下标一一对应 */
    private final List<BlockPos> fluidPorts = new ArrayList<>();
    /** 暴露流体端口快照；连接或登记变化时失效，避免按索引访问每次都重建列表 */
    @Nullable
    private List<StorageFluidPortBlockEntity> fluidTargetCache;
    /** 快照对应的存储归属 */
    @Nullable
    private UUID fluidTargetCacheStorageId;
    /** 快照对应的端口登记版本 */
    private long fluidTargetCacheVersion = -1L;
    /** 快照对应的相邻端口数量 */
    private int fluidTargetCachePorts = -1;
    /** 直连仓储核心时的输入缓存；未直连时不使用 */
    private final ItemStackHandler buffer = new ItemStackHandler(BUFFER_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            StoragePortConsolidatorBlockEntity.this.setChanged();
        }
    };
    private final IItemHandler itemHandler = new ConsolidatedItemHandler();
    private final IFluidHandler fluidHandler = new ConsolidatedFluidHandler();
    private final Object2LongMap<UUID> lastRightClickTicks = new Object2LongOpenHashMap<>();
    /** 整条链归属的存储 ID（登记进 {@link StoragePortManager} 用）；null 表示未接上核心 */
    @Nullable
    private UUID storageId = null;
    @Nullable
    private BlockPos directCorePos;
    private int validateCountdown;
    private int workCountdown;

    public StoragePortConsolidatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    /**
     * 服务端主循环：周期性重校验相连的端口，并按性能墙把直连仓储的缓存并入核心。
     */
    public void tickServer() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        if (this.validateCountdown-- <= 0) {
            this.validateCountdown = StoragePortConsolidatorBlockEntity.VALIDATE_INTERVAL;
            this.refreshLinks();
        }
        if (this.workCountdown-- > 0) {
            return;
        }
        var config = AnvilCraft.CONFIG.machines.storagePort;
        this.workCountdown = config.workInterval;
        this.performTransfer(config.maxItemsPerScan);
    }

    /**
     * 把缓存内的物品并入直连的核心仓储，每次最多移动 {@code limit} 个。
     *
     * <p>仓储按物品种类接收，缓存只遍历自身槽位；未能并入的物品留在缓存里等待下一轮。</p>
     */
    private void performTransfer(int limit) {
        UnlimitedItemStacksResourceHandler core = this.getDirectStorageHandler();
        if (core == null || limit <= 0) {
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

    /**
     * 缓存是否为空。
     */
    public boolean isBufferEmpty() {
        for (int slot = 0; slot < BUFFER_SLOTS; slot++) {
            if (!this.buffer.getStackInSlot(slot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 把缓存内容加入掉落物列表。
     */
    public void addBufferDrops(List<ItemStack> drops) {
        for (int slot = 0; slot < BUFFER_SLOTS; slot++) {
            ItemStack stack = this.buffer.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                drops.add(stack.copy());
            }
        }
    }

    /**
     * 把缓存内的物品掉落到世界中并清空缓存。
     */
    public void dropBuffer(Level level, BlockPos pos) {
        for (int slot = 0; slot < BUFFER_SLOTS; slot++) {
            ItemStack stack = this.buffer.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            Block.popResource(level, pos, stack.copy());
            this.buffer.setStackInSlot(slot, ItemStack.EMPTY);
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
        return last != 0 && now - last <= StoragePortConsolidatorBlockEntity.DOUBLE_CLICK_INTERVAL;
    }

    /**
     * 右键：先尝试倒入流体，再把手持物品塞入相连端口（双击时塞入身上全部）。
     */
    @Override
    public boolean onRightClick(Player player, InteractionHand hand, List<IStoragePort> ports) {
        ItemStack stack = player.getItemInHand(hand);
        // 铁砧锤：右键交给锤子自身（长按滑动去标记、潜行拆除等），不塞物品
        if (stack.getItem() instanceof AnvilHammerItem) {
            return false;
        }
        // 先记录本次点击，避免流体交互提前返回时漏记而导致下一次点击被误判为双击
        boolean doubleClick = this.isDoubleClick(player);
        if (this.pourFluidFromHand(player, hand, ports)) {
            return true;
        }
        if (doubleClick) {
            this.stuffAllFromPlayer(player);
            return true;
        }
        if (!stack.isEmpty()) {
            this.stuffFromHand(stack, stack.getMaxStackSize());
        }
        return true;
    }

    /**
     * 把手中物品塞入相连端口，一次最多 {@code maxCount} 个。
     */
    public void stuffFromHand(ItemStack held, int maxCount) {
        if (held.isEmpty() || maxCount <= 0) {
            return;
        }
        ItemStack toInsert = held.copyWithCount(Math.min(held.getCount(), maxCount));
        ItemStack remainder = ItemHandlerHelper.insertItem(this.itemHandler, toInsert, false);
        int inserted = toInsert.getCount() - remainder.getCount();
        if (inserted > 0) {
            held.shrink(inserted);
        }
    }

    /**
     * 把玩家身上所有与相连端口标记相同的物品塞入对应端口（整个阵列都塞不进时停止）。
     *
     * <p>只处理背包与副手：双击不应该把穿在身上的盔甲也塞进端口。</p>
     */
    public void stuffAllFromPlayer(Player player) {
        Inventory inventory = player.getInventory();
        if (this.stuffMatching(inventory.items)) {
            this.stuffMatching(inventory.offhand);
        }
    }

    /**
     * 把一组物品槽里对应了端口标记的物品塞入相连端口。
     *
     * @return 是否还值得继续处理下一组，整个阵列都塞不下这种物品时为 {@code false}
     */
    private boolean stuffMatching(NonNullList<ItemStack> slots) {
        for (int index = 0; index < slots.size(); index++) {
            ItemStack stack = slots.get(index);
            if (stack.isEmpty() || this.findMarkedPort(stack) < 0) {
                continue;
            }
            int before = stack.getCount();
            ItemStack remainder = ItemHandlerHelper.insertItem(this.itemHandler, stack.copy(), false);
            int inserted = before - remainder.getCount();
            if (inserted <= 0) {
                // 一格都塞不进说明相连端口都装不下这种物品
                return false;
            }
            stack.shrink(inserted);
            if (stack.isEmpty()) {
                slots.set(index, ItemStack.EMPTY);
            }
        }
        return true;
    }

    /**
     * 手持流体容器右键：把流体倒入相连的、已在存放同种流体的流体端口。
     *
     * <p>按 #4792：只倾倒入「有相同流体」的端口；没有对应端口时返回 {@code false}，
     * 由调用方把容器作为普通物品塞入端口。因此这里<b>不</b>回退到空端口——否则空端口会把
     * 桶装流体直接吃掉，桶再也无法以物品形式入库。</p>
     *
     * <p>实际转移交给流体端口自身的交互（瓶子 / 桶与容器回写都由它处理），
     * 避免只改流体容器能力、忘了把容器写回手中而导致同一种流体可以无限倒出。</p>
     *
     * @param ports 与本整合器相连的所有其它端口
     * @return 是否发生了流体转移
     */
    private boolean pourFluidFromHand(Player player, InteractionHand hand, List<IStoragePort> ports) {
        FluidStack content = FluidUtil.getFluidContained(player.getItemInHand(hand)).orElse(FluidStack.EMPTY);
        if (content.isEmpty()) {
            return false;
        }
        for (IStoragePort port : ports) {
            if (!(port instanceof StorageFluidPortBlockEntity fluidPort)) {
                continue;
            }
            FluidStack stored = fluidPort.getFluid();
            if (stored.isEmpty() || !FluidStack.isSameFluidSameComponents(stored, content)) {
                continue;
            }
            return fluidPort.onPlayerUse(player, hand);
        }
        return false;
    }

    /**
     * 重新解析相连的方块：从整合器面相邻处开始沿端口延伸，遇到集装箱 / 存储站不穿过；
     * 一次遍历同时收集仓储端口（附标记）与流体端口，并刷新本整合器在存储名下的登记。
     */
    private void refreshLinks() {
        this.ports.clear();
        this.marks.clear();
        this.fluidPorts.clear();
        // 端口集合重算后快照必然过期
        this.invalidateFluidTargets();
        if (this.level == null) {
            return;
        }
        StoragePortManager.LinkedPorts linked = StoragePortManager.scan(this.level, this.worldPosition);
        AbstractStoragePortBlock.refreshType(this.level, this.worldPosition, linked.core());
        for (IStoragePort port : linked.ports()) {
            if (port instanceof StoragePortBlockEntity itemPort) {
                this.ports.add(itemPort.getBlockPos());
                this.marks.add(itemPort.getMarkedItem());
            } else if (port instanceof StorageFluidPortBlockEntity fluidPort) {
                this.fluidPorts.add(fluidPort.getBlockPos());
            }
        }
        BlockPos directCore = linked.ports().isEmpty() ? linked.core() : null;
        if (!Objects.equals(this.directCorePos, directCore)) {
            this.directCorePos = directCore;
            this.invalidateCapabilities();
        }
        this.updateRegistration(linked.core());
    }

    /**
     * 按扫描结果刷新登记：整条链恰好接触一个核心时登记到该存储名下，
     * 归属变化时先清旧条目，避免旧存储的查询仍看到本整合器。
     *
     * @param core 扫描出的唯一核心主方块坐标；null 表示未接上核心
     */
    private void updateRegistration(@Nullable BlockPos core) {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        UUID id = null;
        if (core != null && this.level.getBlockEntity(core) instanceof StorageBlockEntity storage) {
            id = storage.getId();
        }
        if (Objects.equals(this.storageId, id)) {
            return;
        }
        StoragePortManager.unregister(this.storageId, this.level.dimension(), this.worldPosition);
        this.storageId = id;
        // 归属改变会改变暴露的端口集合
        this.invalidateFluidTargets();
        if (id != null) {
            StoragePortManager.register(id, this.level, this.worldPosition);
        }
    }

    /**
     * 直连仓储核心的物品处理器；未直连或核心不可用时为 {@code null}。
     */
    @Nullable
    public UnlimitedItemStacksResourceHandler getDirectStorageHandler() {
        if (this.isRemoved() || this.level == null || this.level.isClientSide
            || this.directCorePos == null || !this.level.isLoaded(this.directCorePos)) {
            return null;
        }
        if (this.level.getBlockEntity(this.directCorePos) instanceof StorageBlockEntity storage) {
            UUID id = storage.getId();
            if (id != null) {
                return Storages.get().getOrCreate(id, storage.getStorageType().clazz()).getItems();
            }
        }
        return null;
    }

    @Nullable
    private StoragePortBlockEntity portAt(int index) {
        if (this.level == null || index < 0 || index >= this.ports.size()) {
            return null;
        }
        BlockPos pos = this.ports.get(index);
        if (!this.level.isLoaded(pos)) {
            return null;
        }
        BlockEntity blockEntity = this.level.getBlockEntity(pos);
        return blockEntity instanceof StoragePortBlockEntity port ? port : null;
    }

    /**
     * 本整合器当前对外暴露的流体端口：先算面相邻直连端口，再并入同一存储名下已登记的
     * 流体端口（集装箱外接的端口即使没贴着整合器也要暴露）。同一端口只保留一次。
     *
     * <p>结果按「维度 + 坐标」排序后缓存：{@code ResourceHandler} 的按索引操作会在热路径上
     * 反复取值，实时重算不仅 O(N²)，登记集合的迭代顺序还会让同一个槽位在两个调用之间
     * 指向不同端口。缓存于连接、归属或登记版本变化时失效。</p>
     */
    private List<StorageFluidPortBlockEntity> fluidTargets() {
        UUID storageId = this.storageId;
        long version = storageId == null ? -1L : StoragePortManager.itemPortVersion(storageId);
        if (this.fluidTargetCache != null
            && Objects.equals(this.fluidTargetCacheStorageId, storageId)
            && this.fluidTargetCacheVersion == version
            && this.fluidTargetCachePorts == this.fluidPorts.size()) {
            return this.fluidTargetCache;
        }
        List<PortRef> refs = new ArrayList<>();
        Set<PortRef> known = new HashSet<>();
        if (this.level != null) {
            net.minecraft.resources.ResourceKey<Level> dimension = this.level.dimension();
            for (BlockPos pos : this.fluidPorts) {
                PortRef ref = new PortRef(dimension, pos.asLong());
                if (known.add(ref)) {
                    refs.add(ref);
                }
            }
        }
        if (storageId != null && this.level != null && !this.level.isClientSide) {
            for (StorageFluidPortBlockEntity port : StoragePortManager.liveFluidPorts(storageId)) {
                if (port.getLevel() == null) {
                    continue;
                }
                PortRef ref = new PortRef(port.getLevel().dimension(), port.getBlockPos().asLong());
                if (known.add(ref)) {
                    refs.add(ref);
                }
            }
        }
        // 固定排序：同一端口在任何时候都得到同一个槽位
        refs.sort(Comparator
            .comparing((PortRef ref) -> ref.dimension().location().toString())
            .thenComparingLong(PortRef::packedPos));
        List<StorageFluidPortBlockEntity> targets = new ArrayList<>(refs.size());
        Set<StorageFluidPortBlockEntity> seen = new HashSet<>();
        for (PortRef ref : refs) {
            BlockPos pos = BlockPos.of(ref.packedPos());
            Level portLevel = this.level != null && this.level.dimension().equals(ref.dimension())
                ? this.level
                : this.serverLevelOf(ref.dimension());
            if (portLevel == null || !portLevel.isLoaded(pos)) {
                continue;
            }
            if (portLevel.getBlockEntity(pos) instanceof StorageFluidPortBlockEntity port
                && seen.add(port)) {
                targets.add(port);
            }
        }
        List<StorageFluidPortBlockEntity> snapshot = List.copyOf(targets);
        this.fluidTargetCache = snapshot;
        this.fluidTargetCacheStorageId = storageId;
        this.fluidTargetCacheVersion = version;
        this.fluidTargetCachePorts = this.fluidPorts.size();
        return snapshot;
    }

    /** 一个流体端口的身份：维度 + 打包坐标，用于去重与稳定排序。 */
    private record PortRef(net.minecraft.resources.ResourceKey<Level> dimension, long packedPos) {
    }

    @Nullable
    private Level serverLevelOf(net.minecraft.resources.ResourceKey<Level> dimension) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : server.getLevel(dimension);
    }

    /** 连接、归属或端口增删后丢弃快照。 */
    private void invalidateFluidTargets() {
        this.fluidTargetCache = null;
        this.fluidTargetCacheVersion = -1L;
        this.fluidTargetCachePorts = -1;
    }

    @Nullable
    private StorageFluidPortBlockEntity fluidTargetAt(int index) {
        List<StorageFluidPortBlockEntity> targets = this.fluidTargets();
        return index < 0 || index >= targets.size() ? null : targets.get(index);
    }

    /**
     * 第 {@code index} 个暴露的流体端口是否处于无限供给状态，供 Jade 等预览显示 ∞。
     *
     * @param index 流体槽位下标
     * @return 该槽位对应端口无限时返回 {@code true}
     */
    public boolean isFluidInfinite(int index) {
        StorageFluidPortBlockEntity port = this.fluidTargetAt(index);
        return port != null && port.isInfinite();
    }

    /**
     * 找出标记了该物品的端口下标，没有则返回 {@code -1}。
     */
    private int findMarkedPort(ItemStack stack) {
        for (int index = 0; index < this.marks.size(); index++) {
            ItemStack mark = this.marks.get(index);
            if (!mark.isEmpty() && ItemStack.isSameItemSameComponents(mark, stack)) {
                return index;
            }
        }
        return -1;
    }

    private List<StoragePortBlockEntity> itemPorts() {
        List<StoragePortBlockEntity> result = new ArrayList<>();
        for (int index = 0; index < this.ports.size(); index++) {
            StoragePortBlockEntity port = this.portAt(index);
            if (port != null) {
                result.add(port);
            }
        }
        result.sort(Comparator.comparing(port -> port.getMarkedItem().isEmpty()));
        return result;
    }

    /**
     * 某端口在本整合器中占用的槽位数：缓存槽全保留，连接本链仓储时额外暴露一个标记输出槽；
     * 同一仓储的同种标记只保留第一个端口的输出槽，避免重复展示同种物品。
     */
    private int itemSlots(StoragePortBlockEntity port, Set<ResourceKey> marks) {
        IItemHandler handler = port.getItemHandler();
        int slots = handler.getSlots();
        if (slots > StoragePortBlockEntity.BUFFER_SLOTS
            && this.storageId != null && port.isConnectedTo(this.storageId)
            && !marks.add(ResourceKey.of(port.getMarkedItem()))) {
            return StoragePortBlockEntity.BUFFER_SLOTS;
        }
        return slots;
    }

    /**
     * 把整合器的槽位映射到具体端口的物品处理器的槽位。
     */
    @Nullable
    private PortSlot itemSlot(int slot) {
        if (slot < 0) {
            return null;
        }
        Set<ResourceKey> marks = new HashSet<>();
        for (StoragePortBlockEntity port : this.itemPorts()) {
            IItemHandler handler = port.getItemHandler();
            int slots = this.itemSlots(port, marks);
            if (slot < slots) {
                return new PortSlot(handler, slot);
            }
            slot -= slots;
        }
        return null;
    }

    private record PortSlot(IItemHandler handler, int slot) {
    }

    @Override
    public IItemHandler getItemHandler() {
        return this.itemHandler;
    }

    /**
     * 直连仓储核心时的输入缓存，供 Jade 等外部预览读取。
     */
    public IItemHandler getBuffer() {
        return this.buffer;
    }

    @Override
    public IFluidHandler getFluidHandler() {
        return this.fluidHandler;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!this.isBufferEmpty()) {
            tag.put("buffer", this.buffer.serializeNBT(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("buffer")) {
            this.buffer.deserializeNBT(registries, tag.getCompound("buffer"));
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        this.refreshLinks();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (this.level != null) {
            StoragePortManager.unregister(this.storageId, this.level.dimension(), this.worldPosition);
        }
        this.storageId = null;
    }

    /**
     * 直连仓储时暴露「自身缓存 + 只读仓储」，否则映射相连端口缓存并优先向标记端口插入。
     *
     * <p>直连模式下缓存排在仓储之前，按槽位遍历插入的物流会先填满缓存；
     * 仓储槽位只允许读取与取出，写入一律拒绝，由 tick 把缓存并入仓储。</p>
     */
    private class ConsolidatedItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            IItemHandler storage = StoragePortConsolidatorBlockEntity.this.getDirectStorageHandler();
            if (storage != null) {
                return BUFFER_SLOTS + storage.getSlots();
            }
            int slots = 0;
            Set<ResourceKey> marks = new HashSet<>();
            for (StoragePortBlockEntity port : StoragePortConsolidatorBlockEntity.this.itemPorts()) {
                slots += StoragePortConsolidatorBlockEntity.this.itemSlots(port, marks);
            }
            return slots;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            IItemHandler storage = StoragePortConsolidatorBlockEntity.this.getDirectStorageHandler();
            if (storage != null) {
                if (slot < 0) {
                    return ItemStack.EMPTY;
                }
                return slot < BUFFER_SLOTS
                    ? StoragePortConsolidatorBlockEntity.this.buffer.getStackInSlot(slot)
                    : storage.getStackInSlot(slot - BUFFER_SLOTS);
            }
            PortSlot target = StoragePortConsolidatorBlockEntity.this.itemSlot(slot);
            return target == null ? ItemStack.EMPTY : target.handler().getStackInSlot(target.slot());
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            IItemHandler storage = StoragePortConsolidatorBlockEntity.this.getDirectStorageHandler();
            if (storage != null) {
                return slot >= 0 && slot < BUFFER_SLOTS
                    ? StoragePortConsolidatorBlockEntity.this.buffer.insertItem(slot, stack, simulate) : stack;
            }
            PortSlot target = StoragePortConsolidatorBlockEntity.this.itemSlot(slot);
            return target == null ? stack : target.handler().insertItem(target.slot(), stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            IItemHandler storage = StoragePortConsolidatorBlockEntity.this.getDirectStorageHandler();
            if (storage != null) {
                if (amount <= 0 || slot < 0) {
                    return ItemStack.EMPTY;
                }
                return slot < BUFFER_SLOTS
                    ? StoragePortConsolidatorBlockEntity.this.buffer.extractItem(slot, amount, simulate)
                    : storage.extractItem(slot - BUFFER_SLOTS, amount, simulate);
            }
            PortSlot target = StoragePortConsolidatorBlockEntity.this.itemSlot(slot);
            return target == null ? ItemStack.EMPTY : target.handler().extractItem(target.slot(), amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            IItemHandler storage = StoragePortConsolidatorBlockEntity.this.getDirectStorageHandler();
            if (storage != null) {
                if (slot < 0) {
                    return 0;
                }
                return slot < BUFFER_SLOTS
                    ? StoragePortConsolidatorBlockEntity.this.buffer.getSlotLimit(slot)
                    : storage.getSlotLimit(slot - BUFFER_SLOTS);
            }
            PortSlot target = StoragePortConsolidatorBlockEntity.this.itemSlot(slot);
            return target == null ? 0 : target.handler().getSlotLimit(target.slot());
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            IItemHandler storage = StoragePortConsolidatorBlockEntity.this.getDirectStorageHandler();
            if (storage != null) {
                return slot >= 0 && slot < BUFFER_SLOTS
                    && StoragePortConsolidatorBlockEntity.this.buffer.isItemValid(slot, stack);
            }
            PortSlot target = StoragePortConsolidatorBlockEntity.this.itemSlot(slot);
            return target != null && target.handler().isItemValid(target.slot(), stack);
        }
    }

    /**
     * 流体端口储罐拼成的流体能力：每个端口一格，读写都直通该端口的储罐。
     * 端口来源包括面相邻直连端口，以及当前存储名下登记的其它流体端口。
     *
     * <p>端口的储罐只存单一流体，所以 {@code fill} 优先选择已经在存同种流体的端口，
     * 其次才是第一个空端口，避免同一种流体被拆到多个端口；{@code drain} 从第一个
     * 能取出对应流体的端口取出。</p>
     */
    private class ConsolidatedFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return StoragePortConsolidatorBlockEntity.this.fluidTargets().size();
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            StorageFluidPortBlockEntity port = StoragePortConsolidatorBlockEntity.this.fluidTargetAt(tank);
            return port == null ? FluidStack.EMPTY : port.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            StorageFluidPortBlockEntity port = StoragePortConsolidatorBlockEntity.this.fluidTargetAt(tank);
            return port == null ? 0 : port.getFluidHandler().getTankCapacity(0);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            StorageFluidPortBlockEntity port = StoragePortConsolidatorBlockEntity.this.fluidTargetAt(tank);
            if (port == null || stack.isEmpty()) {
                return false;
            }
            FluidStack stored = port.getFluid();
            return (stored.isEmpty() || FluidStack.isSameFluidSameComponents(stored, stack))
                && port.getFluidHandler().isFluidValid(0, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            StorageFluidPortBlockEntity target = this.fillTarget(resource);
            return target == null ? 0 : target.getFluidHandler().fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            for (int index = 0; index < this.getTanks(); index++) {
                StorageFluidPortBlockEntity port = StoragePortConsolidatorBlockEntity.this.fluidTargetAt(index);
                if (port == null || !FluidStack.isSameFluidSameComponents(port.getFluid(), resource)) {
                    continue;
                }
                FluidStack drained = port.getFluidHandler().drain(resource, action);
                if (!drained.isEmpty()) {
                    return drained;
                }
            }
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            for (int index = 0; index < this.getTanks(); index++) {
                StorageFluidPortBlockEntity port = StoragePortConsolidatorBlockEntity.this.fluidTargetAt(index);
                if (port == null || port.getFluid().isEmpty()) {
                    continue;
                }
                FluidStack drained = port.getFluidHandler().drain(maxDrain, action);
                if (!drained.isEmpty()) {
                    return drained;
                }
            }
            return FluidStack.EMPTY;
        }

        /**
         * 选出灌入目标：优先已经在存同种流体的端口，其次第一个空端口；
         * 已存其它流体的端口只存单一流体，直接跳过。
         */
        @Nullable
        private StorageFluidPortBlockEntity fillTarget(FluidStack resource) {
            if (resource.isEmpty()) {
                return null;
            }
            StorageFluidPortBlockEntity empty = null;
            for (int index = 0; index < this.getTanks(); index++) {
                StorageFluidPortBlockEntity port = StoragePortConsolidatorBlockEntity.this.fluidTargetAt(index);
                if (port == null) {
                    continue;
                }
                FluidStack stored = port.getFluid();
                if (FluidStack.isSameFluidSameComponents(stored, resource)) {
                    return port;
                }
                if (stored.isEmpty() && empty == null) {
                    empty = port;
                }
            }
            return empty;
        }
    }
}
