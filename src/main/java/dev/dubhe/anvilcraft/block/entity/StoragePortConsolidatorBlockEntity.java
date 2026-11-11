package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.IStoragePort;
import dev.dubhe.anvilcraft.api.StoragePortManager;
import dev.dubhe.anvilcraft.api.fluid.IFluidResourceHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.IItemResourceHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.block.logistics.storage.AbstractStoragePortBlock;
import dev.dubhe.anvilcraft.item.tool.AnvilHammerItem;
import dev.dubhe.anvilcraft.saved.storage.Storages;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 仓储端口整合器方块实体。
 *
 * <p>沿面相邻的仓储端口收集整个端口阵列，把各端口缓存的并集作为物品能力暴露给外部物流，
 * 并把相连的流体端口按「一个端口一格」暴露为流体能力，读写都直通该端口的储罐；
 * 相连关系只穿过端口，不跨集装箱 / 存储站，因此集装箱两侧没有直接相连的端口阵列不会互通。
 * 标记端口的缓存排在未标记端口之前，按槽位插入只访问对应缓存槽，
 * 按槽位遍历（如 {@link ResourceHandler#insert}）即可填满整个阵列的缓存；
 * 相连端口标记物品的仓储输出槽也会并入物品能力，同一仓储的同种标记只暴露一次。
 * 未连接其他端口且直接接触唯一仓储核心时，物品能力在自身的 32 格缓存之外以只读方式
 * 暴露该仓储的全部槽位：外部只能从仓储取出物品，输入先进入缓存，再由服务端按性能墙并入仓储。</p>
 *
 * <p>右键为「塞入」：单击把手持物品塞入，双击把身上对应端口标记的物品全部塞入；
 * 手持流体容器时先尝试倒入相连的、已在存放同种流体的流体端口。</p>
 */
public class StoragePortConsolidatorBlockEntity extends BlockEntity
    implements IItemResourceHandlerHolder, IFluidResourceHandlerHolder, IStoragePort {
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
    /** 直连仓储核心时的输入缓存；未直连时不使用 */
    private final ItemStacksResourceHandler buffer = new ItemStacksResourceHandler(BUFFER_SLOTS) {
        @Override
        protected void onContentsChanged(int slot, ItemStack previousContents) {
            StoragePortConsolidatorBlockEntity.this.setChanged();
        }
    };
    private final ResourceHandler<ItemResource> itemHandler = new ConsolidatedItemHandler();
    private final ResourceHandler<FluidResource> fluidHandler = new ConsolidatedFluidHandler();
    private final Object2LongMap<UUID> lastRightClickTicks = new Object2LongOpenHashMap<>();
    /** 整条链归属的存储 ID（登记进 {@link StoragePortManager} 用）；null 表示未接上核心 */
    @Nullable
    private UUID storageId = null;
    private @Nullable BlockPos directCorePos;
    private int validateCountdown;
    private int workCountdown;

    public StoragePortConsolidatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    /**
     * 服务端主循环：周期性重校验相连的端口，并按性能墙把直连仓储的缓存并入核心。
     */
    public void tickServer() {
        if (this.level == null || this.level.isClientSide()) {
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
     */
    private void performTransfer(int limit) {
        ResourceHandler<ItemResource> core = this.getDirectStorageHandler();
        if (core == null || limit <= 0) {
            return;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            for (int slot = 0; slot < BUFFER_SLOTS && limit > 0; slot++) {
                ItemResource resource = this.buffer.getResource(slot);
                if (resource.isEmpty()) {
                    continue;
                }
                int amount = Math.min(limit, this.buffer.getAmountAsInt(slot));
                int inserted = core.insert(resource, amount, transaction);
                this.buffer.extract(slot, resource, inserted, transaction);
                limit -= inserted;
            }
            transaction.commit();
        }
    }

    /**
     * 缓存是否为空。
     */
    public boolean isBufferEmpty() {
        for (int slot = 0; slot < BUFFER_SLOTS; slot++) {
            if (this.buffer.getAmountAsLong(slot) > 0) {
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
            int amount = this.buffer.getAmountAsInt(slot);
            if (amount <= 0) {
                continue;
            }
            drops.add(this.buffer.getResource(slot).toStack(amount));
        }
    }

    /**
     * 把缓存内的物品掉落到世界中并清空缓存。
     */
    public void dropBuffer(Level level, BlockPos pos) {
        for (int slot = 0; slot < BUFFER_SLOTS; slot++) {
            int amount = this.buffer.getAmountAsInt(slot);
            if (amount <= 0) {
                continue;
            }
            Block.popResource(level, pos, this.buffer.getResource(slot).toStack(amount));
            this.buffer.set(slot, ItemResource.EMPTY, 0);
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
     * 把手中物品塞入相连端口，一次最多 {@code maxCount} 个；优先进入标记了该物品的端口。
     */
    public void stuffFromHand(ItemStack held, int maxCount) {
        if (held.isEmpty() || maxCount <= 0) {
            return;
        }
        ItemStack toInsert = held.copyWithCount(Math.min(held.getCount(), maxCount));
        ItemStack remainder = ItemHandlerUtil.insertItem(this.itemHandler, toInsert, false);
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
        if (this.stuffMatching(inventory, 0, Inventory.INVENTORY_SIZE)) {
            this.stuffMatching(inventory, Inventory.SLOT_OFFHAND, Inventory.SLOT_OFFHAND + 1);
        }
    }

    /** 对应物品已无法再插入时停止；盔甲槽不参与双击存入。 */
    private boolean stuffMatching(Inventory inventory, int start, int end) {
        for (int index = start; index < end; index++) {
            ItemStack stack = inventory.getItem(index);
            if (stack.isEmpty() || this.findMarkedPort(stack) < 0) continue;
            int before = stack.getCount();
            ItemStack remainder = ItemHandlerUtil.insertItem(this.itemHandler, stack.copy(), false);
            int inserted = before - remainder.getCount();
            if (inserted <= 0) return false;
            stack.shrink(inserted);
            if (stack.isEmpty()) inventory.setItem(index, ItemStack.EMPTY);
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
        FluidStack content = FluidUtil.getFirstStackContained(player.getItemInHand(hand));
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
        if (this.level == null || this.level.isClientSide()) {
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
        if (id != null) {
            StoragePortManager.register(id, this.level, this.worldPosition);
        }
    }

    /**
     * 直连仓储核心的物品处理器；未直连或核心不可用时为 {@code null}。
     */
    @Nullable
    public ResourceHandler<ItemResource> getDirectStorageHandler() {
        if (this.isRemoved() || this.level == null || this.level.isClientSide()
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

    @Nullable
    private StorageFluidPortBlockEntity fluidTargetAt(int index) {
        List<StorageFluidPortBlockEntity> targets = this.fluidTargets();
        return index < 0 || index >= targets.size() ? null : targets.get(index);
    }

    /**
     * 本整合器当前对外暴露的流体端口：先算面相邻直连端口，再并入同一存储名下已登记的
     * 流体端口（集装箱外接的端口即使没贴着整合器也要暴露）。同一端口只保留一次。
     */
    private List<StorageFluidPortBlockEntity> fluidTargets() {
        List<StorageFluidPortBlockEntity> targets = new ArrayList<>();
        Set<StorageFluidPortBlockEntity> seen = new HashSet<>();
        if (this.level != null) {
            for (BlockPos pos : this.fluidPorts) {
                if (!this.level.isLoaded(pos)) {
                    continue;
                }
                if (this.level.getBlockEntity(pos) instanceof StorageFluidPortBlockEntity port
                    && seen.add(port)) {
                    targets.add(port);
                }
            }
        }
        if (this.storageId != null && this.level != null && !this.level.isClientSide()) {
            for (StorageFluidPortBlockEntity port : StoragePortManager.liveFluidPorts(this.storageId)) {
                if (seen.add(port)) {
                    targets.add(port);
                }
            }
        }
        return targets;
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
    private int itemSlots(StoragePortBlockEntity port, Set<ItemResource> marks) {
        ResourceHandler<ItemResource> handler = port.getItemHandler();
        int slots = handler.size();
        if (slots > StoragePortBlockEntity.BUFFER_SLOTS
            && this.storageId != null && port.isConnectedTo(this.storageId)
            && !marks.add(ItemResource.of(port.getMarkedItem()))) {
            return StoragePortBlockEntity.BUFFER_SLOTS;
        }
        return slots;
    }

    /**
     * 把整合器的槽位映射到具体端口的物品处理器的槽位。
     */
    private @Nullable PortSlot itemSlot(int slot) {
        if (slot < 0) {
            return null;
        }
        Set<ItemResource> marks = new HashSet<>();
        for (StoragePortBlockEntity port : this.itemPorts()) {
            ResourceHandler<ItemResource> handler = port.getItemHandler();
            int slots = this.itemSlots(port, marks);
            if (slot < slots) {
                return new PortSlot(handler, slot);
            }
            slot -= slots;
        }
        return null;
    }

    private record PortSlot(ResourceHandler<ItemResource> handler, int index) {
    }

    @Override
    public ResourceHandler<ItemResource> getItemHandler() {
        return this.itemHandler;
    }

    /**
     * 直连仓储核心时的输入缓存，供 Jade 等外部预览读取。
     */
    public ResourceHandler<ItemResource> getBuffer() {
        return this.buffer;
    }

    @Override
    public ResourceHandler<FluidResource> getFluidHandler() {
        return this.fluidHandler;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.isBufferEmpty()) {
            this.buffer.serialize(output.child("buffer"));
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        ItemHandlerUtil.deserializeCompatibleStacks(this.buffer, input.childOrEmpty("buffer"));
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
    private class ConsolidatedItemHandler implements ResourceHandler<ItemResource> {
        @Override
        public int size() {
            ResourceHandler<ItemResource> storage = StoragePortConsolidatorBlockEntity.this.getDirectStorageHandler();
            if (storage != null) {
                return BUFFER_SLOTS + storage.size();
            }
            int slots = 0;
            Set<ItemResource> marks = new HashSet<>();
            for (StoragePortBlockEntity port : StoragePortConsolidatorBlockEntity.this.itemPorts()) {
                slots += StoragePortConsolidatorBlockEntity.this.itemSlots(port, marks);
            }
            return slots;
        }

        @Override
        public ItemResource getResource(int index) {
            ResourceHandler<ItemResource> storage = StoragePortConsolidatorBlockEntity.this.getDirectStorageHandler();
            if (storage != null) {
                if (index < 0) {
                    return ItemResource.EMPTY;
                }
                if (index < BUFFER_SLOTS) {
                    return StoragePortConsolidatorBlockEntity.this.buffer.getResource(index);
                }
                int storageIndex = index - BUFFER_SLOTS;
                return storageIndex < storage.size() ? storage.getResource(storageIndex) : ItemResource.EMPTY;
            }
            PortSlot target = StoragePortConsolidatorBlockEntity.this.itemSlot(index);
            return target == null ? ItemResource.EMPTY : target.handler().getResource(target.index());
        }

        @Override
        public long getAmountAsLong(int index) {
            ResourceHandler<ItemResource> storage = StoragePortConsolidatorBlockEntity.this.getDirectStorageHandler();
            if (storage != null) {
                if (index < 0) {
                    return 0;
                }
                if (index < BUFFER_SLOTS) {
                    return StoragePortConsolidatorBlockEntity.this.buffer.getAmountAsLong(index);
                }
                int storageIndex = index - BUFFER_SLOTS;
                return storageIndex < storage.size() ? storage.getAmountAsLong(storageIndex) : 0;
            }
            PortSlot target = StoragePortConsolidatorBlockEntity.this.itemSlot(index);
            return target == null ? 0 : target.handler().getAmountAsLong(target.index());
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            ResourceHandler<ItemResource> storage = StoragePortConsolidatorBlockEntity.this.getDirectStorageHandler();
            if (storage != null) {
                if (index < 0) {
                    return 0;
                }
                if (index < BUFFER_SLOTS) {
                    return StoragePortConsolidatorBlockEntity.this.buffer.getCapacityAsLong(index, resource);
                }
                int storageIndex = index - BUFFER_SLOTS;
                return storageIndex < storage.size()
                    ? storage.getCapacityAsLong(storageIndex, resource) : 0;
            }
            PortSlot target = StoragePortConsolidatorBlockEntity.this.itemSlot(index);
            return target == null ? 0 : target.handler().getCapacityAsLong(target.index(), resource);
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            ResourceHandler<ItemResource> storage = StoragePortConsolidatorBlockEntity.this.getDirectStorageHandler();
            if (storage != null) {
                return index >= 0 && index < BUFFER_SLOTS
                    && StoragePortConsolidatorBlockEntity.this.buffer.isValid(index, resource);
            }
            PortSlot target = StoragePortConsolidatorBlockEntity.this.itemSlot(index);
            return target != null && target.handler().isValid(target.index(), resource);
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            ResourceHandler<ItemResource> storage = StoragePortConsolidatorBlockEntity.this.getDirectStorageHandler();
            if (storage != null) {
                return index >= 0 && index < BUFFER_SLOTS
                    ? StoragePortConsolidatorBlockEntity.this.buffer.insert(index, resource, amount, transaction) : 0;
            }
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            PortSlot target = StoragePortConsolidatorBlockEntity.this.itemSlot(index);
            return target == null ? 0
                : target.handler().insert(target.index(), resource, amount, transaction);
        }

        @Override
        public int insert(ItemResource resource, int amount, TransactionContext transaction) {
            ResourceHandler<ItemResource> storage = StoragePortConsolidatorBlockEntity.this.getDirectStorageHandler();
            if (storage != null) {
                return StoragePortConsolidatorBlockEntity.this.buffer.insert(resource, amount, transaction);
            }
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            int inserted = 0;
            for (StoragePortBlockEntity port : StoragePortConsolidatorBlockEntity.this.itemPorts()) {
                inserted += port.getBuffer().insert(resource, amount - inserted, transaction);
                if (inserted == amount) {
                    break;
                }
            }
            return inserted;
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            ResourceHandler<ItemResource> storage = StoragePortConsolidatorBlockEntity.this.getDirectStorageHandler();
            if (storage != null) {
                if (index < 0) {
                    return 0;
                }
                if (index < BUFFER_SLOTS) {
                    return StoragePortConsolidatorBlockEntity.this.buffer
                        .extract(index, resource, amount, transaction);
                }
                int storageIndex = index - BUFFER_SLOTS;
                return storageIndex < storage.size()
                    ? storage.extract(storageIndex, resource, amount, transaction) : 0;
            }
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            PortSlot target = StoragePortConsolidatorBlockEntity.this.itemSlot(index);
            return target == null ? 0
                : target.handler().extract(target.index(), resource, amount, transaction);
        }

        @Override
        public int extract(ItemResource resource, int amount, TransactionContext transaction) {
            ResourceHandler<ItemResource> storage = StoragePortConsolidatorBlockEntity.this.getDirectStorageHandler();
            if (storage != null) {
                return storage.extract(resource, amount, transaction);
            }
            return ResourceHandler.super.extract(resource, amount, transaction);
        }
    }

    /** 每个流体端口一格；整批操作仍按源版选中一个端口，不自动跨罐分摊。 */
    private class ConsolidatedFluidHandler implements ResourceHandler<FluidResource> {
        @Override
        public int size() {
            return StoragePortConsolidatorBlockEntity.this.fluidTargets().size();
        }

        @Override
        public FluidResource getResource(int index) {
            var port = StoragePortConsolidatorBlockEntity.this.fluidTargetAt(index);
            return port == null ? FluidResource.EMPTY : port.getFluidHandler().getResource(0);
        }

        @Override
        public long getAmountAsLong(int index) {
            var port = StoragePortConsolidatorBlockEntity.this.fluidTargetAt(index);
            return port == null ? 0 : port.getFluidHandler().getAmountAsLong(0);
        }

        @Override
        public long getCapacityAsLong(int index, FluidResource resource) {
            var port = StoragePortConsolidatorBlockEntity.this.fluidTargetAt(index);
            return port == null ? 0 : port.getFluidHandler().getCapacityAsLong(0, resource);
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            var port = StoragePortConsolidatorBlockEntity.this.fluidTargetAt(index);
            if (port == null || resource.isEmpty()) return false;
            FluidResource stored = port.getFluidHandler().getResource(0);
            return (stored.isEmpty() || stored.equals(resource)) && port.getFluidHandler().isValid(0, resource);
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            var port = StoragePortConsolidatorBlockEntity.this.fluidTargetAt(index);
            return port == null ? 0 : port.getFluidHandler().insert(0, resource, amount, transaction);
        }

        @Override
        public int insert(FluidResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            var target = this.fillTarget(resource);
            return target == null ? 0 : target.getFluidHandler().insert(resource, amount, transaction);
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            var port = StoragePortConsolidatorBlockEntity.this.fluidTargetAt(index);
            return port == null ? 0 : port.getFluidHandler().extract(0, resource, amount, transaction);
        }

        @Override
        public int extract(FluidResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            for (int index = 0; index < this.size(); index++) {
                int extracted = this.extract(index, resource, amount, transaction);
                if (extracted > 0) return extracted;
            }
            return 0;
        }

        private @Nullable StorageFluidPortBlockEntity fillTarget(FluidResource resource) {
            StorageFluidPortBlockEntity empty = null;
            for (int index = 0; index < this.size(); index++) {
                var port = StoragePortConsolidatorBlockEntity.this.fluidTargetAt(index);
                if (port == null) continue;
                FluidResource stored = port.getFluidHandler().getResource(0);
                if (stored.equals(resource)) return port;
                if (stored.isEmpty() && empty == null) empty = port;
            }
            return empty;
        }
    }

}
