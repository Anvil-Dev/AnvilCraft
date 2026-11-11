package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.IStoragePort;
import dev.dubhe.anvilcraft.api.StoragePortManager;
import dev.dubhe.anvilcraft.api.fluid.FluidBottleInteraction;
import dev.dubhe.anvilcraft.api.fluid.IFluidResourceHandlerHolder;
import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkManager;
import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkScanner;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.block.logistics.storage.AbstractStoragePortBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 仓储流体端口方块实体。
 *
 * <p>作为潜影集装箱 / 超维存储站的外部流体存储，有 {@value #CAPACITY_MB} mB（128 B）容积，
 * 只能存储单一流体（{@link FluidStacksResourceHandler} 本身即锁定首个流体）。与仓储端口一样沿端口链延伸，
 * 两者可互相延伸连接关系。</p>
 *
 * <p>水箱独立工作：不接任何核心时仍可被桶 / 管道直接读写，也照常参与管道网络的高度偏置。
 * 与核心的连接关系只决定它归属哪个存储——即流体出现在哪个存储的界面里。</p>
 *
 * <p>拆除时流体随掉落物保留，手持门格海绵右键可清除内部流体。</p>
 */
public class StorageFluidPortBlockEntity extends BlockEntity implements IFluidResourceHandlerHolder, IStoragePort {
    /** 流体容积（mB）：128 B */
    public static final int CAPACITY_MB = 128 * 1000;
    /** 等效高度调整上限（格），即 20 米 */
    public static final int MAX_HEIGHT_BIAS = 20;
    private static final double ADJUST_FILL_START = 0.5;
    private static final double ADJUST_DRAIN_START = 0.75;
    private static final double ADJUST_STOP = 0.65;
    /** 端口贴附关系重校验间隔（tick） */
    private static final int VALIDATE_INTERVAL = 20;
    /** 等效高度调整间隔（tick） */
    private static final int ADJUST_INTERVAL = 10;
    private static final String TAG_TANK = "Tank";

    @Getter
    private final FluidStacksResourceHandler tank = new FluidStacksResourceHandler(1, StorageFluidPortBlockEntity.CAPACITY_MB) {
        @Override
        protected int getCapacity(int index, FluidResource resource) {
            return StorageFluidPortBlockEntity.this.infinite
                ? Integer.MAX_VALUE
                : super.getCapacity(index, resource);
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            if (StorageFluidPortBlockEntity.this.infinite && this.matchesStored(index, resource)) {
                return amount;
            }
            return super.extract(index, resource, amount, transaction);
        }

        @Override
        public int extract(FluidResource resource, int amount, TransactionContext transaction) {
            if (StorageFluidPortBlockEntity.this.infinite && this.matchesStored(0, resource)) {
                return amount;
            }
            return super.extract(resource, amount, transaction);
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            if (StorageFluidPortBlockEntity.this.infinite) {
                return this.matchesStored(index, resource) ? amount : 0;
            }
            return super.insert(index, resource, amount, transaction);
        }

        @Override
        public int insert(FluidResource resource, int amount, TransactionContext transaction) {
            if (StorageFluidPortBlockEntity.this.infinite) {
                return this.matchesStored(0, resource) ? amount : 0;
            }
            return super.insert(resource, amount, transaction);
        }

        private boolean matchesStored(int index, FluidResource resource) {
            FluidResource stored = this.getResource(index);
            return !stored.isEmpty() && stored.equals(resource);
        }

        @Override
        protected void onContentsChanged(int index, FluidStack previousContents) {
            StorageFluidPortBlockEntity.this.rememberFluid();
            StorageFluidPortBlockEntity.this.setChanged();
            if (StorageFluidPortBlockEntity.this.level != null) {
                StorageFluidPortBlockEntity.this.level.sendBlockUpdated(
                    getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL
                );
                if (!StorageFluidPortBlockEntity.this.level.isClientSide()) {
                    StorageFluidPortBlockEntity.this.level.updateNeighbourForOutputSignal(getBlockPos(), getBlockState().getBlock());
                }
                // 流体变化同样要让仓储 UI 重同步，否则界面上的流体数量会停留在旧值
                UUID storageId = StorageFluidPortBlockEntity.this.storageId;
                if (!StorageFluidPortBlockEntity.this.level.isClientSide() && storageId != null) {
                    StorageServerStub.onContentsChanged(storageId);
                }
            }
        }
    };

    /**
     * 最后一次盛放的流体。
     *
     * <p>取空后 {@code getFluid()} 会变成空栈、连流体类型都读不到，导致 UI 里的条目直接消失；
     * 保留它可以让条目像物品一样留在列表里显示 0。仅运行时记忆，不写入 NBT，
     * 因此重载后与物品一致地不再显示已清空的条目。</p>
     */
    private FluidStack rememberedFluid = FluidStack.EMPTY;

    /** 记录当前流体类型，供取空后继续占位显示。 */
    private void rememberFluid() {
        FluidStack current = this.getFluid();
        if (!current.isEmpty()) {
            this.rememberedFluid = current.copy();
        }
    }

    /**
     * 取空后仍可用于占位的流体类型。
     *
     * @return 最后盛放过的流体；从未装过流体时为空
     */
    public FluidStack getRememberedFluid() {
        return this.getFluid().isEmpty() ? this.rememberedFluid : this.getFluid();
    }

    /**
     * 自身等效高度的调整量（格）：负值降低高度以便进液，正值提高高度以便排液。
     *
     * <p>由内部水位自动调整，同时作用于液体与气体：管道网络用
     * {@code effectiveHeight - Y} 推导气压，故该偏置即模拟压力。</p>
     */
    @Getter
    private int heightBias;
    /** 抽入与排出状态保持到目标水位，避免在启动阈值附近反复换向。 */
    private boolean drawing;
    private boolean draining;
    /** 是否因通过管道接入无限储罐而处于无限存量状态；断开后重算，不持久化 */
    private boolean infinite;
    /** 连接到的存储 ID；未连接时为 null，用于把流体变化通知给仓储 UI */
    @Nullable
    private UUID storageId = null;
    private int validateCountdown = 0;
    private int adjustCountdown = 0;

    public StorageFluidPortBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    /**
     * 服务端主循环：周期性重校验连通关系。
     */
    public void tickServer() {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        if (this.validateCountdown-- <= 0) {
            this.validateCountdown = StorageFluidPortBlockEntity.VALIDATE_INTERVAL;
            // 存档载入的存量不会触发 onContentsChanged，这里补记一次，
            // 否则载入后直接取空会没有可供占位显示的流体类型
            this.rememberFluid();
            this.validateLink();
        }
        if (this.adjustCountdown-- <= 0) {
            this.adjustCountdown = StorageFluidPortBlockEntity.ADJUST_INTERVAL;
            this.refreshInfinite();
            this.adjustHeightBias();
        }
    }

    /**
     * 重算无限状态：同网内存在正在无限供给同一流体的储罐时变为无限；
     * 断开后恢复为普通满存量端口。
     */
    private void refreshInfinite() {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        FluidResource current = this.tank.getResource(0);
        FluidStack source = FluidNetworkManager.INSTANCE.findInfiniteFluid(
            this.level, this.getBlockPos(), current);
        if (source.isEmpty()) {
            this.setInfinite(false, FluidStack.EMPTY);
            return;
        }
        if (!current.isEmpty() && !current.equals(FluidResource.of(source))) {
            this.setInfinite(false, FluidStack.EMPTY);
            return;
        }
        this.setInfinite(true, source);
    }

    /**
     * 切换无限状态；变为无限时把储量拉满并记录流体种类，恢复有限时保留满存量。
     */
    private void setInfinite(boolean value, FluidStack source) {
        if (this.infinite == value
            && (!value || this.tank.getResource(0).equals(FluidResource.of(source)))) {
            return;
        }
        this.infinite = value;
        FluidResource resource = value ? FluidResource.of(source) : this.tank.getResource(0);
        if (!resource.isEmpty()) {
            this.tank.set(0, resource, value ? Integer.MAX_VALUE : StorageFluidPortBlockEntity.CAPACITY_MB);
        }
        this.setChanged();
        if (this.level != null) {
            BlockState state = this.getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_ALL);
            if (!this.level.isClientSide() && this.storageId != null) {
                StorageServerStub.onContentsChanged(this.storageId);
            }
        }
    }

    /** 当前是否因接入无限储罐而无限供给。 */
    public boolean isInfinite() {
        return this.infinite;
    }

    /** 水位越过启动阈值后持续抽入或排出，到目标水位时停止。 */
    private void adjustHeightBias() {
        // 规格前置条件：仅在「通过管道连接了其他流体储存方块」时才调整。
        // 没有相邻管道时本端口不属于任何管网，偏置不会被读取（见 FluidNetworkScanner#heightBiasAt），
        // 调整纯属空转；且偏置每次变化都会 markDirty，触发整世界的管网重建。
        if (!this.hasAdjacentPipe()) {
            return;
        }
        int previous = this.heightBias;
        this.heightBias = this.computeNextHeightBias();
        if (this.heightBias != previous) {
            // 偏置变化需让管道网络重扫才能生效
            if (this.level != null) {
                FluidNetworkManager.INSTANCE.markDirty(this.level);
            }
        }
    }

    /** 是否至少有一面相邻管道部件（即已接入管网，等效高度调整才有意义）。 */
    private boolean hasAdjacentPipe() {
        Level level = this.level;
        if (level == null) {
            return false;
        }
        BlockPos pos = this.getBlockPos();
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            if (level.isLoaded(neighbor) && FluidNetworkScanner.isPipePart(level.getBlockState(neighbor))) {
                return true;
            }
        }
        return false;
    }

    private int computeNextHeightBias() {
        double fill = (double) this.tank.getAmountAsInt(0) / StorageFluidPortBlockEntity.CAPACITY_MB;
        if (this.drawing) {
            if (fill >= StorageFluidPortBlockEntity.ADJUST_STOP) {
                this.drawing = false;
            }
        } else if (this.draining) {
            if (fill <= StorageFluidPortBlockEntity.ADJUST_STOP) {
                this.draining = false;
            }
        } else if (fill < StorageFluidPortBlockEntity.ADJUST_FILL_START) {
            this.drawing = true;
        } else if (fill > StorageFluidPortBlockEntity.ADJUST_DRAIN_START) {
            this.draining = true;
        }
        if (this.drawing) {
            return -StorageFluidPortBlockEntity.MAX_HEIGHT_BIAS;
        }
        if (this.draining) {
            return StorageFluidPortBlockEntity.MAX_HEIGHT_BIAS;
        }
        return this.alignedBias();
    }

    /** 停止时对齐同网最高容器，消除残留高度差导致的继续流入或回流。 */
    private int alignedBias() {
        Level level = this.level;
        if (level == null) {
            return this.heightBias;
        }
        Integer delta = FluidNetworkManager.INSTANCE.heightDeltaToHighestPeer(level, this.getBlockPos());
        if (delta == null) {
            return this.heightBias;
        }
        return Math.clamp(this.heightBias + delta,
            -StorageFluidPortBlockEntity.MAX_HEIGHT_BIAS, StorageFluidPortBlockEntity.MAX_HEIGHT_BIAS);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide()) {
            FluidNetworkManager.INSTANCE.addContainer(this.level, this.getBlockPos());
        }
    }

    @Override
    public void setRemoved() {
        if (this.level != null && !this.level.isClientSide()) {
            FluidNetworkManager.INSTANCE.removeContainer(this.level, this.getBlockPos());
            UUID id = this.storageId;
            StoragePortManager.unregister(id, this.level.dimension(), this.getBlockPos());
            // 端口消失同样改变归属：不清缓存的话，其伪槽位会以空格子形式残留在界面上
            if (id != null) {
                StorageServerStub.onContentsChanged(id);
            }
        }
        super.setRemoved();
    }

    /**
     * 重新解析连通组件，解析规则与仓储端口一致（两者可互相延伸）。
     *
     * <p>只有当连通组件恰好接触一个有效核心时，才把本端口登记到该存储名下；登记只影响
     * 「归属哪个存储的 UI」，端口自身的水箱始终独立可用（可被桶、管道直接读写）。</p>
     */
    private void validateLink() {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return;
        }
        UUID previousId = this.storageId;
        // 先清掉旧存储名下的登记再重新登记：端口可能从 A 存储改挂到 B 存储（链路重排），
        // 若不清旧条目，A 的 UI 仍会显示本端口的流体、drain(A) 还会抽走属于 B 的流体
        StoragePortManager.unregister(previousId, serverLevel.dimension(), this.worldPosition);
        this.storageId = null;

        BlockPos core = StoragePortManager.findSoleCore(this.level, this.worldPosition);
        AbstractStoragePortBlock.refreshType(this.level, this.worldPosition, core);
        UUID id = null;
        if (core != null
            && this.level.getBlockEntity(core) instanceof StorageBlockEntity storage) {
            id = storage.getId();
        }
        this.storageId = id;
        if (id != null) {
            // 自报给注册表，供仓储 UI 反查该存储可显示的流体
            StoragePortManager.register(id, serverLevel, this.worldPosition);
        }
        // 归属变化要清掉相关存储的排序缓存：伪槽位编号取自 collect() 的下标，缓存里仍留着
        // 旧归属时的流体条目，新接上的端口流体要等到下次内容变化才出现，拆掉的端口
        // 还会残留成空格子（点击后发出空流体，服务端静默不动）。
        // 旧归属与新归属都要通知：由 A 改挂到 B 时 A 的界面也要移除本端口的流体。
        if (!Objects.equals(previousId, id)) {
            if (previousId != null) {
                StorageServerStub.onContentsChanged(previousId);
            }
            if (id != null) {
                StorageServerStub.onContentsChanged(id);
            }
        }
    }

    /**
     * 清空内部流体（手持门格海绵右键时调用）。
     *
     * @return 是否实际清除了流体
     */
    public boolean clearFluid() {
        if (this.getFluid().isEmpty()) {
            return false;
        }
        this.tank.set(0, FluidResource.EMPTY, 0);
        return true;
    }

    /**
     * 玩家手持容器右键：先尝试瓶子，再按普通流体容器（桶等）交互。
     *
     * @param player 玩家
     * @param hand   交互手
     * @return 是否发生了流体交换
     */
    public boolean onPlayerUse(Player player, InteractionHand hand) {
        if (this.level != null
            && FluidBottleInteraction.tryInteract(player, hand, this.tank, this.level, this.getBlockPos())) {
            return true;
        }
        try (Transaction transaction = Transaction.openRoot()) {
            boolean success = FluidUtil.interactWithFluidHandler(player, hand, this.getBlockPos(), this.tank, transaction);
            if (success) transaction.commit();
            return success;
        }
    }

    /**
     * 右键：手持门格海绵清除内部流体，其余手持容器先瓶子后桶，与储罐一致。
     */
    @Override
    public boolean onRightClick(Player player, InteractionHand hand, List<IStoragePort> ports) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(ModBlocks.MENGER_SPONGE.asItem())) {
            if (this.clearFluid() && this.level != null && !this.level.isClientSide()) {
                BlockState state = this.getBlockState();
                this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_ALL);
            }
            return true;
        }
        return this.onPlayerUse(player, hand);
    }

    /**
     * 内部流体，供仓储 UI 等手段读取。
     *
     * @return 当前流体，可能为空
     */
    public FluidStack getFluid() {
        return this.tank.getResource(0).toStack(this.tank.getAmountAsInt(0));
    }

    /** 存档与同步用的流体：无限状态只在运行时生效，落盘数量按真实容量截断。 */
    private FluidStack storedFluid() {
        FluidStack fluid = this.getFluid();
        if (fluid.getAmount() > StorageFluidPortBlockEntity.CAPACITY_MB) {
            fluid.setAmount(StorageFluidPortBlockEntity.CAPACITY_MB);
        }
        return fluid;
    }

    /**
     * 把流体写入掉落的方块物品（拆除保留流体）。
     *
     * <p>空端口不写入方块实体数据，否则拆下来会多出一个空 tank 组件，无法与未放置过的物品堆叠。</p>
     */
    public void saveToDrop(ItemStack stack, HolderLookup.Provider registries) {
        if (this.getFluid().isEmpty()) {
            return;
        }
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        this.saveAdditional(output);
        BlockItem.setBlockEntityData(stack, this.getType(), output);
        stack.applyComponents(this.collectComponents());
    }

    /**
     * 物品能力形态的流体处理器，供管道等外部物流读写。
     *
     * @return 流体处理器
     */
    @Override
    public ResourceHandler<FluidResource> getFluidHandler() {
        return this.tank;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        ValueOutput tank = output.child(TAG_TANK);
        FluidStack fluid = this.storedFluid();
        if (!fluid.isEmpty()) tank.store("Fluid", FluidStack.CODEC, fluid);
        return output.buildResult();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        FluidStack fluid = this.storedFluid();
        if (!fluid.isEmpty()) output.child(TAG_TANK).store("Fluid", FluidStack.CODEC, fluid);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        FluidStack fluid = input.childOrEmpty(TAG_TANK).read("Fluid", FluidStack.OPTIONAL_CODEC).orElse(FluidStack.EMPTY);
        this.infinite = false;
        this.tank.set(0, FluidResource.of(fluid),
            Math.min(fluid.getAmount(), StorageFluidPortBlockEntity.CAPACITY_MB));
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
