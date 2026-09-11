package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;
import dev.dubhe.anvilcraft.api.fluid.FluidHandlerWrapper;
import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkManager;
import dev.dubhe.anvilcraft.block.StorageFluidPortBlock;
import dev.dubhe.anvilcraft.block.entity.storage.StorageBlockEntity;
import dev.dubhe.anvilcraft.block.entity.storage.StorageFluidRegistry;
import dev.dubhe.anvilcraft.rpc.StorageServerStub;
import lombok.Getter;
import net.minecraft.core.BlockPos;
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

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 仓储流体端口方块实体。
 *
 * <p>作为潜影集装箱 / 超维存储站的外部流体存储，有 {@value #CAPACITY_MB} mB（128 B）容积，
 * 只能存储单一流体（{@link FluidTank} 本身即锁定首个流体）。与仓储端口一样沿端口链延伸，
 * 两者可互相延伸连接关系；整个连通组件必须恰好接触一个核心才工作。</p>
 *
 * <p>拆除时流体随掉落物保留，手持门格海绵右键可清除内部流体。</p>
 */
public class StorageFluidPortBlockEntity extends BlockEntity implements IFluidHandlerHolder {
    /** 流体容积（mB）：128 B */
    public static final int CAPACITY_MB = 128 * 1000;
    /** 等效高度调整上限（格），即 20 米 */
    public static final int MAX_HEIGHT_BIAS = 20;
    /** 目标水位区间下限 */
    private static final double ADJUST_FILL_LOW = 0.5;
    /** 目标水位区间上限 */
    private static final double ADJUST_FILL_HIGH = 0.75;
    /** 目标水位区间中点：区间外朝它调节 */
    private static final double ADJUST_FILL_MID = (ADJUST_FILL_LOW + ADJUST_FILL_HIGH) / 2;
    /** 每格水位偏差对应的单步调整量 */
    private static final int ADJUST_RATE = 8;
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

    /** 组件解析出的核心主方块坐标；null 表示组件无效 */
    @Nullable
    private BlockPos coreMainPos = null;
    /** 当前是否工作（连通组件恰好接触一个有效核心） */
    @Getter
    private boolean working;
    /**
     * 自身等效高度的调整量（格）：负值降低高度以便进液，正值提高高度以便排液。
     *
     * <p>由内部水位自动调整，同时作用于液体与气体：管道网络用
     * {@code effectiveHeight - Y} 推导气压，故该偏置即模拟压力。</p>
     */
    @Getter
    private int heightBias;
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
     * 按内部水位调整等效高度，把流体维持在容积的 50%~75% 之间。
     *
     * <p>低于下限时降低自身等效高度，使自身成为更低的目标、吸引高处流体流入；
     * 高于上限时提高等效高度，使其成为更高的源而向外排出；偏离中点越远调整越快。
     * 已处于目标区间时保持偏置不动：若在区间内持续向中点回拉，等效高度会来回
     * 穿越网络中其它容器，使端口在「吸入」与「排出」之间反复切换。</p>
     */
    private void adjustHeightBias() {
        int previous = this.heightBias;
        this.heightBias = this.computeNextHeightBias();
        if (this.heightBias != previous) {
            // 偏置变化需让管道网络重扫才能生效
            FluidNetworkManager.INSTANCE.markDirty(this.level);
        }
    }

    private int computeNextHeightBias() {
        if (this.tank.isEmpty()) {
            return 0;
        }
        double fill = (double) this.tank.getFluidAmount() / this.tank.getCapacity();
        // 已在目标区间内：保持当前偏置，避免反复进出
        if (fill >= StorageFluidPortBlockEntity.ADJUST_FILL_LOW
            && fill <= StorageFluidPortBlockEntity.ADJUST_FILL_HIGH) {
            return this.heightBias;
        }
        // 朝区间中点调节，靠近边界时步长自然收敛到 0，不会在边界反复横跳
        double error = StorageFluidPortBlockEntity.ADJUST_FILL_MID - fill;
        int step = (int) Math.round(Math.abs(error) * StorageFluidPortBlockEntity.ADJUST_RATE);
        if (step <= 0) {
            return this.heightBias;
        }
        // error > 0 表示需要进液（降低高度），< 0 表示需要排液（提高高度）
        int next = this.heightBias + (error < 0 ? step : -step);
        return Math.clamp(next, -StorageFluidPortBlockEntity.MAX_HEIGHT_BIAS, StorageFluidPortBlockEntity.MAX_HEIGHT_BIAS);
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
            StorageFluidRegistry.unregister(this.getBlockPos());
        }
        super.setRemoved();
    }

    /**
     * 重新解析连通组件，解析规则与仓储端口一致（两者可互相延伸）。
     */
    private void validateLink() {
        this.working = false;
        this.coreMainPos = null;
        if (this.level == null) {
            return;
        }
        BlockPos core = StoragePortBlockEntity.findSoleCore(this.level, this.worldPosition);
        if (core == null || !(this.level.getBlockEntity(core) instanceof StorageBlockEntity storage)
            || storage.getId() == null) {
            StorageFluidRegistry.unregister(this.worldPosition);
            return;
        }
        this.coreMainPos = core;
        this.working = true;
        // 自报给注册表，供仓储 UI 反查该存储可显示的流体
        if (this.level instanceof ServerLevel serverLevel) {
            StorageFluidRegistry.register(storage.getId(), serverLevel, this.worldPosition);
            this.storageId = storage.getId();
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
        // 同步流体，保证客户端渲染与 UI 读取一致
        tag.put(StorageFluidPortBlockEntity.TAG_TANK, this.tank.writeToNBT(registries, new CompoundTag()));
        return tag;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(StorageFluidPortBlockEntity.TAG_TANK, this.tank.writeToNBT(registries, new CompoundTag()));
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
