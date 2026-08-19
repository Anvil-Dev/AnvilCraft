package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;
import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkManager;
import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkScanner;
import dev.dubhe.anvilcraft.api.fluid.network.FluidPipeNetwork;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.cfa.interfaces.CelestialForgingAnvilInterfaceBlock;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;

/// 锻星砧流体接口。
/// 被动模式：存储最多 4 种流体，各 80 桶，供管道和巨构读写。
/// 主动模式（红石信号激活）：模型切换到 _active，以 10 米扬程向前方泵送。
public class CelestialForgingAnvilFluidInterfaceBlockEntity extends BlockEntity
    implements IPowerConsumer, IFluidHandlerHolder {
    private static final int TANK_COUNT = 4;
    private static final int CAPACITY_PER_TANK = 80_000; /// 80 桶（以 mB 计）
    private static final int PUMP_HEADLIFT = 10; /// 10 米扬程

    @Getter
    private final FluidTank[] tanks = new FluidTank[TANK_COUNT];

    @Setter
    @Nullable
    private PowerGrid grid;
    private boolean suppressFluidSync;

    public CelestialForgingAnvilFluidInterfaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        for (int i = 0; i < TANK_COUNT; i++) {
            final int index = i;
            tanks[i] = new FluidTank(CAPACITY_PER_TANK) {
                @Override
                public boolean isFluidValid(FluidStack stack) {
                    /// 仅当此储罐已有该流体，或没有其它储罐存有该流体时才接受
                    FluidStack current = getFluid();
                    if (current.isEmpty()) {
                        for (int j = 0; j < TANK_COUNT; j++) {
                            if (j != index && tanks[j].getFluid().is(stack.getFluid())) {
                                return false;
                            }
                        }
                        return true;
                    }
                    return current.is(stack.getFluid());
                }

                @Override
                protected void onContentsChanged() {
                    CelestialForgingAnvilFluidInterfaceBlockEntity.this.setChanged();
                }
            };
        }
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

    /// 将方块实体数据同步到所有追踪的客户端。
    public void syncToClients() {
        CfaBlockEntitySync.sendToTracking(this, this.getUpdatePacket());
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (suppressFluidSync) return;
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            syncToClients();
        }
    }

    /** Drains all tanks containing the requested fluid and sends one combined update. */
    public int drainFluid(Fluid fluid) {
        int drainedAmount = 0;
        this.suppressFluidSync = true;
        try {
            for (FluidTank tank : tanks) {
                FluidStack stored = tank.getFluid();
                if (stored.isEmpty() || !stored.is(fluid)) continue;
                drainedAmount += tank.drain(stored.getAmount(), IFluidHandler.FluidAction.EXECUTE).getAmount();
            }
        } finally {
            this.suppressFluidSync = false;
        }
        if (drainedAmount > 0) this.setChanged();
        return drainedAmount;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public int getInputPower() {
        return 128; /// 128kW
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeTanks(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readTanks(tag, registries);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        writeTanks(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        readTanks(tag, registries);
    }

    private void writeTanks(CompoundTag tag, HolderLookup.Provider registries) {
        for (int i = 0; i < TANK_COUNT; i++) {
            CompoundTag tankTag = new CompoundTag();
            tanks[i].writeToNBT(registries, tankTag);
            tag.put("tank" + i, tankTag);
        }
    }

    private void readTanks(CompoundTag tag, HolderLookup.Provider registries) {
        for (int i = 0; i < TANK_COUNT; i++) {
            if (tag.contains("tank" + i)) {
                tanks[i].readFromNBT(registries, tag.getCompound("tank" + i));
            }
        }
    }

    /// 返回用于管道输入/输出的流体处理器能力。将全部 4 个储罐合并为一个处理器。
    @SuppressWarnings("unused")
    public IFluidHandler getFluidHandler() {
        return createFluidHandler(false);
    }

    /// 返回供锻星砧内部产出和状态同步使用的流体处理器。
    public IFluidHandler getInternalFluidHandler() {
        return createFluidHandler(true);
    }

    private IFluidHandler createFluidHandler(boolean allowInputWhenActive) {
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return TANK_COUNT;
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                return tanks[tank].getFluid();
            }

            @Override
            public int getTankCapacity(int tank) {
                return tanks[tank].getCapacity();
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                if (!allowInputWhenActive && isActive()) return false;
                return tanks[tank].isFluidValid(stack);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                if (!allowInputWhenActive && isActive()) return 0;
                if (resource.isEmpty()) return 0;
                /// 优先尝试已有流体的储罐，再尝试空储罐
                for (int i = 0; i < TANK_COUNT; i++) {
                    if (tanks[i].getFluid().is(resource.getFluid())) {
                        return tanks[i].fill(resource, action);
                    }
                }
                for (int i = 0; i < TANK_COUNT; i++) {
                    if (tanks[i].getFluid().isEmpty()) {
                        return tanks[i].fill(resource, action);
                    }
                }
                return 0;
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                if (resource.isEmpty()) return FluidStack.EMPTY;
                for (int i = 0; i < TANK_COUNT; i++) {
                    if (tanks[i].getFluid().is(resource.getFluid())) {
                        return tanks[i].drain(resource, action);
                    }
                }
                return FluidStack.EMPTY;
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                for (int i = 0; i < TANK_COUNT; i++) {
                    if (!tanks[i].getFluid().isEmpty()) {
                        return tanks[i].drain(maxDrain, action);
                    }
                }
                return FluidStack.EMPTY;
            }
        };
    }

    private boolean isActive() {
        BlockState state = getBlockState();
        return state.hasProperty(CelestialForgingAnvilInterfaceBlock.ACTIVE)
               && state.getValue(CelestialForgingAnvilInterfaceBlock.ACTIVE);
    }

    /// 服务器端 tick：在主动模式（红石信号激活）且有电时，以 10 米扬程向 FACING 方向的
    /// 管道网络泵送流体。前方是管道 → 扫描其网络并作为高源分配；前方是容器 → 直接推送。
    public void serverTick() {
        if (level == null || level.isClientSide()) return;
        BlockState state = getBlockState();
        if (!state.hasProperty(CelestialForgingAnvilInterfaceBlock.ACTIVE)) return;

        boolean active = state.getValue(CelestialForgingAnvilInterfaceBlock.ACTIVE);
        if (!active) return;

        /// 检查电网供电
        if (grid == null || !grid.isWorking()) return;

        Direction facing = state.getValue(CelestialForgingAnvilInterfaceBlock.FACING);
        BlockPos frontPos = getBlockPos().relative(facing);
        BlockState frontState = level.getBlockState(frontPos);

        /// 源等效高度 = 接口自身 Y + 10 米扬程
        int sourceEffectiveHeight = getBlockPos().getY() + PUMP_HEADLIFT;

        if (FluidNetworkScanner.isPipePart(frontState)) {
            /// 前方是管道 → 扫描网络，作为高等效高度的外部源向其分配
            FluidPipeNetwork network = FluidNetworkScanner.scan(level, frontPos);
            if (network == null) return;
            for (FluidTank source : tanks) {
                if (source.getFluid().isEmpty()) continue;
                network.pushFromExternalSource(source, getBlockPos(), frontPos, sourceEffectiveHeight);
            }
            return;
        }

        /// 前方是容器 → 直接推送（高度差 = 扬程 + 源Y − 目标Y）
        IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK, frontPos, facing.getOpposite());
        if (target == null) return;
        int heightDiff = sourceEffectiveHeight - frontPos.getY();
        if (heightDiff <= 0) return;
        int speed = FluidPipeNetwork.speedForHeightDiff(heightDiff);
        for (FluidTank tank : tanks) {
            FluidStack stored = tank.getFluid();
            if (stored.isEmpty()) continue;
            FluidStack toMove = stored.copyWithAmount(Math.min(speed, stored.getAmount()));
            int filled = target.fill(toMove, IFluidHandler.FluidAction.SIMULATE);
            if (filled <= 0) continue;
            FluidStack drained = tank.drain(stored.copyWithAmount(filled), IFluidHandler.FluidAction.EXECUTE);
            int actually = target.fill(drained, IFluidHandler.FluidAction.EXECUTE);
            if (actually < drained.getAmount()) {
                tank.fill(drained.copyWithAmount(drained.getAmount() - actually), IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }
}
