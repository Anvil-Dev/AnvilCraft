package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.fluid.IFluidResourceHandlerHolder;
import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkManager;
import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkScanner;
import dev.dubhe.anvilcraft.api.fluid.network.FluidPipeNetwork;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.cfa.interfaces.CelestialForgingAnvilInterfaceBlock;
import dev.dubhe.anvilcraft.block.entity.fluid.AbstractPipeBlockEntity;
import dev.dubhe.anvilcraft.block.fluid.PipeBlock;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.Level;
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
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

/**
 * 锻星砧流体接口。
 * 最多存储 4 种流体，每种容量为 80 桶；工作时消耗 128 kW，并支持管道输入输出。
 */
public class CelestialForgingAnvilFluidInterfaceBlockEntity extends BlockEntity implements IPowerConsumer, IFluidResourceHandlerHolder {
    private static final int TANK_COUNT = 4;
    private static final int CAPACITY_PER_TANK = 80_000; // 80 桶，以 mB 计
    private static final int PUMP_HEADLIFT = 10; // 10 米扬程

    @Getter
    private final FluidStacksResourceHandler tank;
    private final ResourceHandler<FluidResource> externalTank;

    @Setter
    @Nullable
    private PowerGrid grid;

    public CelestialForgingAnvilFluidInterfaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        this.tank = new FluidStacksResourceHandler(
            CelestialForgingAnvilFluidInterfaceBlockEntity.TANK_COUNT,
            CelestialForgingAnvilFluidInterfaceBlockEntity.CAPACITY_PER_TANK
        ) {
            @Override
            public boolean isValid(int index, FluidResource resource) {
                if (resource.isEmpty()) return false;
                FluidStack currentStack = this.getStackFrom(this.getResource(index), this.getAmountAsInt(index));
                if (!currentStack.isEmpty() && currentStack.is(resource.getFluid())) return true;
                if (currentStack.isEmpty()) {
                    for (int j = 0; j < CelestialForgingAnvilFluidInterfaceBlockEntity.TANK_COUNT; j++) {
                        if (j != index) {
                            FluidStack otherStack = this.getStackFrom(this.getResource(j), this.getAmountAsInt(j));
                            if (!otherStack.isEmpty() && otherStack.is(resource.getFluid())) {
                                return false;
                            }
                        }
                    }
                    return true;
                }
                return false;
            }

            @Override
            protected void onContentsChanged(int index, FluidStack previousContents) {
                CelestialForgingAnvilFluidInterfaceBlockEntity.this.setChanged();
            }
        };
        this.externalTank = new ResourceHandler<>() {
            @Override
            public int size() {
                return CelestialForgingAnvilFluidInterfaceBlockEntity.this.tank.size();
            }

            @Override
            public FluidResource getResource(int index) {
                return CelestialForgingAnvilFluidInterfaceBlockEntity.this.tank.getResource(index);
            }

            @Override
            public long getAmountAsLong(int index) {
                return CelestialForgingAnvilFluidInterfaceBlockEntity.this.tank.getAmountAsLong(index);
            }

            @Override
            public long getCapacityAsLong(int index, FluidResource resource) {
                return CelestialForgingAnvilFluidInterfaceBlockEntity.this.tank.getCapacityAsLong(index, resource);
            }

            @Override
            public boolean isValid(int index, FluidResource resource) {
                return !CelestialForgingAnvilFluidInterfaceBlockEntity.this.isActive()
                    && CelestialForgingAnvilFluidInterfaceBlockEntity.this.tank.isValid(index, resource);
            }

            @Override
            public int insert(
                int index, FluidResource resource, int amount, TransactionContext transaction
            ) {
                if (CelestialForgingAnvilFluidInterfaceBlockEntity.this.isActive()) return 0;
                return CelestialForgingAnvilFluidInterfaceBlockEntity.this.tank.insert(
                    index, resource, amount, transaction
                );
            }

            @Override
            public int insert(FluidResource resource, int amount, TransactionContext transaction) {
                if (CelestialForgingAnvilFluidInterfaceBlockEntity.this.isActive()) return 0;
                return CelestialForgingAnvilFluidInterfaceBlockEntity.this.tank.insert(
                    resource, amount, transaction
                );
            }

            @Override
            public int extract(
                int index, FluidResource resource, int amount, TransactionContext transaction
            ) {
                return CelestialForgingAnvilFluidInterfaceBlockEntity.this.tank.extract(
                    index, resource, amount, transaction
                );
            }

            @Override
            public int extract(FluidResource resource, int amount, TransactionContext transaction) {
                return CelestialForgingAnvilFluidInterfaceBlockEntity.this.tank.extract(
                    resource, amount, transaction
                );
            }
        };
    }

    public CelestialForgingAnvilFluidInterfaceBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.CELESTIAL_FORGING_ANVIL_FLUID_INTERFACE.get(), pos, blockState);
    }

    public static CelestialForgingAnvilFluidInterfaceBlockEntity createBlockEntity(
        BlockEntityType<?> type, BlockPos pos, BlockState state
    ) {
        return new CelestialForgingAnvilFluidInterfaceBlockEntity(type, pos, state);
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
        }
        super.setRemoved();
    }

    public void syncToClients() {
        CfaBlockEntitySync.sendToTracking(this, this.getUpdatePacket());
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
            this.syncToClients();
        }
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public int getInputPower() {
        return 128;
    }

    @Override
    public @Nullable Level getCurrentLevel() {
        return this.level;
    }

    @Override
    public BlockPos getPos() {
        return this.getBlockPos();
    }

    @Override
    public @Nullable PowerGrid getGrid() {
        return this.grid;
    }

    @Override
    public PowerComponentType getComponentType() {
        return IPowerConsumer.super.getComponentType();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.tank.serialize(output.child("tank"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.tank.deserialize(input.childOrEmpty("tank"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        TagValueOutput fluidTag = TagValueOutput.createWithContext(
            new ProblemReporter.Collector(this.problemPath()), registries);
        this.tank.serialize(fluidTag);
        tag.put("tank", fluidTag.buildResult());
        return tag;
    }

    /**
     * 返回供外部管道访问的流体处理器。
     */
    @SuppressWarnings("unused")
    public ResourceHandler<FluidResource> getFluidHandler() {
        return this.externalTank;
    }

    /**
     * 返回供锻星砧内部产出和状态同步使用的流体处理器。
     */
    public ResourceHandler<FluidResource> getInternalFluidHandler() {
        return this.tank;
    }

    private boolean isActive() {
        BlockState state = this.getBlockState();
        return state.hasProperty(CelestialForgingAnvilInterfaceBlock.ACTIVE)
            && state.getValue(CelestialForgingAnvilInterfaceBlock.ACTIVE);
    }

    /// 服务器端 tick：在主动模式（铁砧锤切换的 ACTIVE）且有电时，向 FACING 方向泵送流体。
    /// 前方是管道→沿管道追踪到远端再推送；前方是流体容器→直接推送；
    /// 扬程 10 米，流速随高度差放大（复用管道系统的 moveFluid）。
    public void serverTick() {
        if (this.level == null || this.level.isClientSide()) return;
        BlockState state = this.getBlockState();
        if (!state.hasProperty(CelestialForgingAnvilInterfaceBlock.ACTIVE)) return;

        boolean active = state.getValue(CelestialForgingAnvilInterfaceBlock.ACTIVE);
        if (!active) return;

        // 检查电网供电
        if (this.grid == null || !this.grid.isWorking()) return;

        Direction facing = state.getValue(CelestialForgingAnvilInterfaceBlock.FACING);
        BlockPos frontPos = this.getBlockPos().relative(facing);
        BlockState frontState = this.level.getBlockState(frontPos);
        int sourceEffectiveHeight = this.getBlockPos().getY() + CelestialForgingAnvilFluidInterfaceBlockEntity.PUMP_HEADLIFT;

        if (FluidNetworkScanner.isPipePart(frontState)) {
            FluidPipeNetwork network = FluidNetworkScanner.scan(this.level, frontPos);
            if (network != null) {
                network.pushFromExternalSource(this.tank, this.getBlockPos(), frontPos, sourceEffectiveHeight);
            }
            return;
        }

        // 确定目标：前方是管道 → 追踪到远端；否则直接用前方方块
        BlockPos targetPos;       // 接收方的位置
        Direction targetQueryDir; // 从接收方查询 IFluidHandler 的方向
        int pipeHeight = 0;       // 管道沿途累计的等效高度

        if (frontState.getBlock() instanceof PipeBlock) {
            // 从前方管道沿 facing.getOpposite() 方向追踪到管道远端。
            // getPipeEnd 的参数 direction 是"从管道哪一侧进入"，即接口连接管道的那一侧。
            AbstractPipeBlockEntity.PipeEnd pipeEnd =
                AbstractPipeBlockEntity.getPipeEnd(this.level, frontPos, facing.getOpposite());
            if (pipeEnd == null) return;
            // pipeEnd.direction() = 从管道末端指向接收方的方向
            targetPos = pipeEnd.pos().relative(pipeEnd.direction());
            targetQueryDir = pipeEnd.direction().getOpposite();
            pipeHeight = pipeEnd.effectiveHeight();
        } else {
            targetPos = frontPos;
            targetQueryDir = facing.getOpposite();
        }

        // 计算有效高度差（含 10m 扬程，扣除管道累计等效高度）
        int sourceY = this.getBlockPos().getY();
        int targetY = targetPos.getY() - pipeHeight;
        int heightDiff = CelestialForgingAnvilFluidInterfaceBlockEntity.PUMP_HEADLIFT + sourceY - targetY;
        if (heightDiff <= 0) return;

        // 复用管道系统的流体传输：源端为接口自身（内部储罐，通过 Fluid.BLOCK 能力查询）。
        AbstractPipeBlockEntity.moveFluid(
            this.level,
            this.getBlockPos(),   // sourcePos = 接口自身（内部储罐）
            facing,          // sourceQueryDir（能力忽略 side，任意方向均可）
            targetPos,       // 接收方位置
            targetQueryDir,  // 从接收方面向源
            heightDiff       // 有效高度差（含扬程）
        );
    }
}
