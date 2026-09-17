package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.IStoragePort;
import dev.dubhe.anvilcraft.api.StoragePortManager;
import dev.dubhe.anvilcraft.api.fluid.FluidHandlerWrapper;
import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;
import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkManager;
import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkScanner;
import dev.dubhe.anvilcraft.block.AbstractStoragePortBlock;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;

/**
 * 仓储流体端口方块实体。
 *
 * <p>作为潜影集装箱 / 超维存储站的外部流体存储，有 {@value #CAPACITY_MB} mB（128 B）容积，
 * 只能存储单一流体（{@link FluidTank} 本身即锁定首个流体）。与仓储端口一样沿端口链延伸，
 * 两者可互相延伸连接关系。</p>
 *
 * <p>水箱独立工作：不接任何核心时仍可被桶 / 管道直接读写，也照常参与管道网络的高度偏置。
 * 与核心的连接关系只决定它归属哪个存储——即流体出现在哪个存储的界面里。</p>
 *
 * <p>拆除时流体随掉落物保留，手持门格海绵右键可清除内部流体。</p>
 */
public class StorageFluidPortBlockEntity extends BlockEntity implements IFluidHandlerHolder, IStoragePort {
    /** 流体容积（mB）：128 B */
    public static final int CAPACITY_MB = 128 * 1000;
    /** 等效高度调整上限（格），即 20 米 */
    public static final int MAX_HEIGHT_BIAS = 20;
    /** 水位下限：低于此比例开始抽入 */
    private static final double ADJUST_FILL_START = 0.5;
    /** 水位上限：抽到此比例停止抽入 */
    private static final double ADJUST_FILL_STOP = 0.65;
    /** 端口贴附关系重校验间隔（tick） */
    private static final int VALIDATE_INTERVAL = 20;
    /** 等效高度调整间隔（tick） */
    private static final int ADJUST_INTERVAL = 10;
    private static final String TAG_TANK = "Tank";

    @Getter
    private final FluidTank tank = new FluidTank(StorageFluidPortBlockEntity.CAPACITY_MB) {
        @Override
        protected void onContentsChanged() {
            StorageFluidPortBlockEntity.this.rememberFluid();
            StorageFluidPortBlockEntity.this.setChanged();
            if (StorageFluidPortBlockEntity.this.level != null) {
                StorageFluidPortBlockEntity.this.level.sendBlockUpdated(
                    getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL
                );
                // 流体变化同样要让仓储 UI 重同步，否则界面上的流体数量会停留在旧值
                UUID storageId = StorageFluidPortBlockEntity.this.storageId;
                if (!StorageFluidPortBlockEntity.this.level.isClientSide && storageId != null) {
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
        FluidStack current = this.tank.getFluid();
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
        return this.tank.getFluid().isEmpty() ? this.rememberedFluid : this.tank.getFluid();
    }

    /**
     * 自身等效高度的调整量（格）：负值降低高度以便进液，正值提高高度以便排液。
     *
     * <p>由内部水位自动调整，同时作用于液体与气体：管道网络用
     * {@code effectiveHeight - Y} 推导气压，故该偏置即模拟压力。</p>
     */
    @Getter
    private int heightBias;
    /**
     * 施密特触发器的锁存位：水位低于 {@link #ADJUST_FILL_START} 时置位并保持，
     * 直到抽到 {@link #ADJUST_FILL_STOP} 才复位。
     *
     * <p>复位后即使水位回落也不再重新抽入，必须再次低于下限才重新置位，
     * 这样端口只在两端动作，区间内不干预。</p>
     */
    private boolean drawing = false;
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
        if (this.level == null || this.level.isClientSide) {
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
            this.adjustHeightBias();
        }
    }

    /**
     * 按内部水位调整等效高度：水位跌破 {@link #ADJUST_FILL_START} 开始抽入，
     * 抽到 {@link #ADJUST_FILL_STOP} 停止，其余时间不干预。
     *
     * <p>只在本端口已通过管道接入管网时才调整，调整结果见 {@link #computeNextHeightBias()}。</p>
     */
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

    /**
     * 按内部水位推进施密特触发器，并把结果换算成等效高度偏置。
     *
     * <p>水位低于 {@link #ADJUST_FILL_START} 时置位开始抽入，抽到
     * {@link #ADJUST_FILL_STOP} 复位停止。置位期间压差恒为 {@code -MAX_HEIGHT_BIAS}，
     * 区间内不施加中间压差。</p>
     *
     * <p>置位与复位用两个不同阈值：停止后端口若回到中性高度，此前抽入的流体会因高度关系
     * 回落，若共用一个阈值，水位刚跌回阈值以下就会立刻再次抽入，形成反复抽放的循环。
     * 用滞回后必须一路跌破下限才重新置位。</p>
     *
     * <p>停止时把自身高度对齐到同网最高的容器，而不是回到中性高度：网络只在目标高度
     * <b>严格低于</b>源时才转移，对齐后与最高的供给方等高即互不流动，进液才真正止住；
     * 回到中性高度则仍低于上方容器，会被继续灌入。</p>
     *
     * @return 新的高度偏置（格）
     */
    private int computeNextHeightBias() {
        double fill = (double) this.tank.getFluidAmount() / this.tank.getCapacity();
        if (this.drawing) {
            // 置位期间保持抽入，直到到达停止水位才复位
            if (fill >= StorageFluidPortBlockEntity.ADJUST_FILL_STOP) {
                this.drawing = false;
            }
        } else if (fill < StorageFluidPortBlockEntity.ADJUST_FILL_START) {
            this.drawing = true;
        }
        return this.drawing ? -StorageFluidPortBlockEntity.MAX_HEIGHT_BIAS : this.alignedBias();
    }

    /**
     * 停止抽入时用于止住进液的高度偏置：在现有偏置上叠加「到最高同网容器的等效高度差」，
     * 使自身等效高度与该容器对齐。
     *
     * <p>按差值增量对齐，而不是由绝对高度回填：端点等效高度含累积扬程 phi，
     * phi 以扫描种子为零点且种子是任意选的，调用方无法得知自己的 phi。差值相减时
     * phi 自行抵消，故与种子、与多入口都无关。详见
     * {@link FluidNetworkManager#heightDeltaToHighestPeer}。</p>
     *
     * <p>取不到同网容器（未接入管网、本容器不在网内、或网内只有自己）时保持当前偏置。</p>
     *
     * @return 对齐后的偏置（格），已按 {@link #MAX_HEIGHT_BIAS} 钳制
     */
    private int alignedBias() {
        Level level = this.level;
        if (level == null) {
            return this.heightBias;
        }
        Integer delta = FluidNetworkManager.INSTANCE.heightDeltaToHighestPeer(level, this.getBlockPos());
        if (delta == null) {
            return this.heightBias;
        }
        return Math.clamp(
            this.heightBias + delta,
            -StorageFluidPortBlockEntity.MAX_HEIGHT_BIAS,
            StorageFluidPortBlockEntity.MAX_HEIGHT_BIAS
        );
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide) {
            FluidNetworkManager.INSTANCE.addContainer(this.level, this.getBlockPos());
        }
    }

    @Override
    public void setRemoved() {
        if (this.level != null && !this.level.isClientSide) {
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
        if (this.tank.isEmpty()) {
            return false;
        }
        this.tank.setFluid(FluidStack.EMPTY);
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
            && FluidHandlerWrapper.tryInteractWithBottle(player, hand, this.tank, this.level, this.getBlockPos())) {
            return true;
        }
        return FluidUtil.interactWithFluidHandler(player, hand, this.tank);
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
        return this.tank.getFluid();
    }

    /**
     * 把流体写入掉落的方块物品（拆除保留流体）。
     *
     * <p>空端口不写入方块实体数据，否则拆下来会多出一个空 tank 组件，无法与未放置过的物品堆叠。</p>
     */
    public void saveToDrop(ItemStack stack, HolderLookup.Provider registries) {
        if (this.tank.isEmpty()) {
            return;
        }
        CompoundTag tag = this.saveCustomOnly(registries);
        BlockItem.setBlockEntityData(stack, this.getType(), tag);
        stack.applyComponents(this.collectComponents());
    }

    /**
     * 物品能力形态的流体处理器，供管道等外部物流读写。
     *
     * @return 流体处理器
     */
    @Override
    public IFluidHandler getFluidHandler() {
        return this.tank;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        // 同步流体，保证客户端渲染与 UI 读取一致；为空也要写，否则客户端会残留旧流体
        tag.put(StorageFluidPortBlockEntity.TAG_TANK, this.tank.writeToNBT(registries, new CompoundTag()));
        return tag;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        // 空槽时不写流体数据：空端口被存成物品不该多出一个空 tank 组件，否则会与未放置过的端口无法堆叠
        if (!this.tank.isEmpty()) {
            tag.put(StorageFluidPortBlockEntity.TAG_TANK, this.tank.writeToNBT(registries, new CompoundTag()));
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
        if (tag.contains(StorageFluidPortBlockEntity.TAG_TANK)) {
            this.tank.readFromNBT(registries, tag.getCompound(StorageFluidPortBlockEntity.TAG_TANK));
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
