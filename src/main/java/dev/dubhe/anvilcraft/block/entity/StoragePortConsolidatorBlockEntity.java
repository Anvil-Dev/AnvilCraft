package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.IStoragePort;
import dev.dubhe.anvilcraft.api.StoragePortManager;
import dev.dubhe.anvilcraft.api.fluid.IFluidResourceHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.IItemResourceHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.ItemHandlerUtil;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.block.logistics.storage.AbstractStoragePortBlock;
import dev.dubhe.anvilcraft.item.tool.AnvilHammerItem;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 仓储端口整合器方块实体。
 *
 * <p>沿面相邻的仓储端口收集整个端口阵列，把各端口缓存的并集作为物品能力暴露给外部物流，
 * 并把相连的流体端口按「一个端口一格」暴露为流体能力，读写都直通该端口的储罐；
 * 相连关系只穿过端口，不跨集装箱 / 存储站，因此集装箱两侧没有直接相连的端口阵列不会互通。
 * 整合器自身没有容积空间：输入物品直接进入相连端口，优先进入标记了对应物品的端口，
 * 其余情况只进入调用方指定槽位所属的端口，与仓储端口自身的缓存语义一致，
 * 按槽位遍历（如 {@link ResourceHandler#insert}）即可填满整个阵列。</p>
 *
 * <p>右键为「塞入」：单击把手持物品塞入，双击把身上对应端口标记的物品全部塞入；
 * 手持流体容器时先尝试倒入相连的、已在存放同种流体的流体端口。</p>
 */
public class StoragePortConsolidatorBlockEntity extends BlockEntity
    implements IItemResourceHandlerHolder, IFluidResourceHandlerHolder, IStoragePort {
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
    private final ResourceHandler<ItemResource> itemHandler = new ConsolidatedItemHandler();
    private final ResourceHandler<FluidResource> fluidHandler = new ConsolidatedFluidHandler();
    private final Object2LongMap<UUID> lastRightClickTicks = new Object2LongOpenHashMap<>();
    /** 整条链归属的存储 ID（登记进 {@link StoragePortManager} 用）；null 表示未接上核心 */
    @Nullable
    private UUID storageId = null;
    private int validateCountdown;

    public StoragePortConsolidatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    /**
     * 服务端主循环：周期性重校验相连的端口。
     */
    public void tickServer() {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        if (this.validateCountdown-- > 0) {
            return;
        }
        this.validateCountdown = StoragePortConsolidatorBlockEntity.VALIDATE_INTERVAL;
        this.refreshLinks();
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
    private StorageFluidPortBlockEntity fluidPortAt(int index) {
        if (this.level == null || index < 0 || index >= this.fluidPorts.size()) {
            return null;
        }
        BlockPos pos = this.fluidPorts.get(index);
        if (!this.level.isLoaded(pos)) {
            return null;
        }
        BlockEntity blockEntity = this.level.getBlockEntity(pos);
        return blockEntity instanceof StorageFluidPortBlockEntity port ? port : null;
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

    private static int portIndex(int slot) {
        return slot < 0 ? -1 : slot / StoragePortBlockEntity.BUFFER_SLOTS;
    }

    private static int bufferSlot(int slot) {
        return slot % StoragePortBlockEntity.BUFFER_SLOTS;
    }

    @Override
    public ResourceHandler<ItemResource> getItemHandler() {
        return this.itemHandler;
    }

    @Override
    public ResourceHandler<FluidResource> getFluidHandler() {
        return this.fluidHandler;
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

    /** 相连端口缓存按 32 格映射；插入沿用标记端口优先的规则。 */
    private class ConsolidatedItemHandler implements ResourceHandler<ItemResource> {
        @Override
        public int size() {
            return StoragePortConsolidatorBlockEntity.this.ports.size() * StoragePortBlockEntity.BUFFER_SLOTS;
        }

        @Override
        public ItemResource getResource(int index) {
            var port = StoragePortConsolidatorBlockEntity.this.portAt(portIndex(index));
            return port == null ? ItemResource.EMPTY : port.getBuffer().getResource(bufferSlot(index));
        }

        @Override
        public long getAmountAsLong(int index) {
            var port = StoragePortConsolidatorBlockEntity.this.portAt(portIndex(index));
            return port == null ? 0 : port.getBuffer().getAmountAsLong(bufferSlot(index));
        }

        @Override
        public long getCapacityAsLong(int index, ItemResource resource) {
            var port = StoragePortConsolidatorBlockEntity.this.portAt(portIndex(index));
            return port == null ? 0 : port.getBuffer().getCapacityAsLong(bufferSlot(index), resource);
        }

        @Override
        public boolean isValid(int index, ItemResource resource) {
            var port = StoragePortConsolidatorBlockEntity.this.portAt(portIndex(index));
            return port != null && port.getBuffer().isValid(bufferSlot(index), resource);
        }

        @Override
        public int insert(int index, ItemResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            int inserted = this.insertMarked(resource, amount, transaction);
            if (inserted == amount) return inserted;
            var port = StoragePortConsolidatorBlockEntity.this.portAt(portIndex(index));
            return inserted + (port == null ? 0 : port.getBuffer().insert(resource, amount - inserted, transaction));
        }

        @Override
        public int insert(ItemResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            int inserted = this.insertMarked(resource, amount, transaction);
            for (int index = 0; index < StoragePortConsolidatorBlockEntity.this.ports.size() && inserted < amount; index++) {
                var port = StoragePortConsolidatorBlockEntity.this.portAt(index);
                if (port != null) inserted += port.getBuffer().insert(resource, amount - inserted, transaction);
            }
            return inserted;
        }

        private int insertMarked(ItemResource resource, int amount, TransactionContext transaction) {
            int marked = StoragePortConsolidatorBlockEntity.this.findMarkedPort(resource.toStack());
            var port = StoragePortConsolidatorBlockEntity.this.portAt(marked);
            return port == null ? 0 : port.getBuffer().insert(resource, amount, transaction);
        }

        @Override
        public int extract(int index, ItemResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            var port = StoragePortConsolidatorBlockEntity.this.portAt(portIndex(index));
            return port == null ? 0 : port.getBuffer().extract(bufferSlot(index), resource, amount, transaction);
        }
    }

    /** 每个流体端口一格；整批操作仍按源版选中一个端口，不自动跨罐分摊。 */
    private class ConsolidatedFluidHandler implements ResourceHandler<FluidResource> {
        @Override
        public int size() {
            return StoragePortConsolidatorBlockEntity.this.fluidPorts.size();
        }

        @Override
        public FluidResource getResource(int index) {
            var port = StoragePortConsolidatorBlockEntity.this.fluidPortAt(index);
            return port == null ? FluidResource.EMPTY : port.getFluidHandler().getResource(0);
        }

        @Override
        public long getAmountAsLong(int index) {
            var port = StoragePortConsolidatorBlockEntity.this.fluidPortAt(index);
            return port == null ? 0 : port.getFluidHandler().getAmountAsLong(0);
        }

        @Override
        public long getCapacityAsLong(int index, FluidResource resource) {
            var port = StoragePortConsolidatorBlockEntity.this.fluidPortAt(index);
            return port == null ? 0 : port.getFluidHandler().getCapacityAsLong(0, resource);
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            var port = StoragePortConsolidatorBlockEntity.this.fluidPortAt(index);
            if (port == null || resource.isEmpty()) return false;
            FluidResource stored = port.getFluidHandler().getResource(0);
            return (stored.isEmpty() || stored.equals(resource)) && port.getFluidHandler().isValid(0, resource);
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
            var port = StoragePortConsolidatorBlockEntity.this.fluidPortAt(index);
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
            var port = StoragePortConsolidatorBlockEntity.this.fluidPortAt(index);
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
                var port = StoragePortConsolidatorBlockEntity.this.fluidPortAt(index);
                if (port == null) continue;
                FluidResource stored = port.getFluidHandler().getResource(0);
                if (stored.equals(resource)) return port;
                if (stored.isEmpty() && empty == null) empty = port;
            }
            return empty;
        }
    }

}
