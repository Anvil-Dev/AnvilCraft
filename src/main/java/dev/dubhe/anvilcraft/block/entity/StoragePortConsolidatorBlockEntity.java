package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.IStoragePort;
import dev.dubhe.anvilcraft.api.StoragePortManager;
import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.IItemHandlerHolder;
import dev.dubhe.anvilcraft.api.itemhandler.unlimited.UnlimitedItemStacksResourceHandler.ResourceKey;
import dev.dubhe.anvilcraft.block.AbstractStoragePortBlock;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * 仓储端口整合器方块实体。
 *
 * <p>沿面相邻的仓储端口收集整个端口阵列，把各端口可取库存的并集作为物品能力暴露给外部物流，
 * 并把相连的流体端口按「一个端口一格」暴露为流体能力，读写都直通该端口的储罐；
 * 相连关系只穿过端口，不跨集装箱 / 存储站，因此集装箱两侧没有直接相连的端口阵列不会互通。
 * 整合器输入始终进入各端口缓存，标记仓储输出槽仅暴露同种物品的一份数量，
 * 按槽位遍历（如 {@link ItemHandlerHelper#insertItem}）即可填满整个阵列的缓存。</p>
 *
 * <p>右键为「塞入」：单击把手持物品塞入，双击把身上对应端口标记的物品全部塞入；
 * 手持流体容器时先尝试倒入相连的、已在存放同种流体的流体端口。</p>
 */
public class StoragePortConsolidatorBlockEntity extends BlockEntity
    implements IItemHandlerHolder, IFluidHandlerHolder, IStoragePort {
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
    private final IItemHandler itemHandler = new ConsolidatedItemHandler();
    private final IFluidHandler fluidHandler = new ConsolidatedFluidHandler();
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
        if (this.level == null || this.level.isClientSide) {
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

    private List<StoragePortBlockEntity> itemPorts() {
        List<StoragePortBlockEntity> result = new ArrayList<>();
        for (int index = 0; index < this.ports.size(); index++) {
            StoragePortBlockEntity port = this.portAt(index);
            if (port != null) {
                result.add(port);
            }
        }
        return result;
    }

    private int itemSlots(StoragePortBlockEntity port, Set<ResourceKey> marks) {
        int slots = port.getItemHandler().getSlots();
        if (slots > StoragePortBlockEntity.BUFFER_SLOTS && this.storageId != null && port.isConnectedTo(this.storageId)
            && !marks.add(ResourceKey.of(port.getMarkedItem()))) {
            return StoragePortBlockEntity.BUFFER_SLOTS;
        }
        return slots;
    }

    private @Nullable PortSlot itemSlot(int slot) {
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

    @Override
    public IFluidHandler getFluidHandler() {
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

    /** 每个端口保留缓存槽，同一仓库同种标记的输出槽仅暴露一次。 */
    private class ConsolidatedItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            int slots = 0;
            Set<ResourceKey> marks = new HashSet<>();
            for (StoragePortBlockEntity port : StoragePortConsolidatorBlockEntity.this.itemPorts()) {
                slots += StoragePortConsolidatorBlockEntity.this.itemSlots(port, marks);
            }
            return slots;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            PortSlot target = StoragePortConsolidatorBlockEntity.this.itemSlot(slot);
            return target == null ? ItemStack.EMPTY : target.handler().getStackInSlot(target.slot());
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            PortSlot target = StoragePortConsolidatorBlockEntity.this.itemSlot(slot);
            return target == null ? stack : target.handler().insertItem(target.slot(), stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            PortSlot target = StoragePortConsolidatorBlockEntity.this.itemSlot(slot);
            return target == null ? ItemStack.EMPTY : target.handler().extractItem(target.slot(), amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            PortSlot target = StoragePortConsolidatorBlockEntity.this.itemSlot(slot);
            return target == null ? 0 : target.handler().getSlotLimit(target.slot());
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            PortSlot target = StoragePortConsolidatorBlockEntity.this.itemSlot(slot);
            return target != null && target.handler().isItemValid(target.slot(), stack);
        }
    }

    /**
     * 相连流体端口储罐拼成的流体能力：每个流体端口一格，读写都直通该端口的储罐。
     *
     * <p>端口的储罐只存单一流体，所以 {@code fill} 优先选择已经在存同种流体的端口，
     * 其次才是第一个空端口，避免同一种流体被拆到多个端口；{@code drain} 从第一个
     * 能取出对应流体的端口取出。</p>
     */
    private class ConsolidatedFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return StoragePortConsolidatorBlockEntity.this.fluidPorts.size();
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            StorageFluidPortBlockEntity port = StoragePortConsolidatorBlockEntity.this.fluidPortAt(tank);
            return port == null ? FluidStack.EMPTY : port.getFluid();
        }

        @Override
        public int getTankCapacity(int tank) {
            StorageFluidPortBlockEntity port = StoragePortConsolidatorBlockEntity.this.fluidPortAt(tank);
            return port == null ? 0 : port.getFluidHandler().getTankCapacity(0);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            StorageFluidPortBlockEntity port = StoragePortConsolidatorBlockEntity.this.fluidPortAt(tank);
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
                StorageFluidPortBlockEntity port = StoragePortConsolidatorBlockEntity.this.fluidPortAt(index);
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
                StorageFluidPortBlockEntity port = StoragePortConsolidatorBlockEntity.this.fluidPortAt(index);
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
                StorageFluidPortBlockEntity port = StoragePortConsolidatorBlockEntity.this.fluidPortAt(index);
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
