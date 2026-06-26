package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.cfa.interfaces.CelestialForgingAnvilInterfaceBlock;
import dev.dubhe.anvilcraft.block.entity.fluid.AbstractPipeBlockEntity;
import dev.dubhe.anvilcraft.block.fluid.PipeBlock;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
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

    /// 将方块实体数据同步到所有追踪的客户端。
    public void syncToClients() {
        if (level instanceof ServerLevel serverLevel) {
            Packet<?> packet = getUpdatePacket();
            if (packet != null) {
                for (ServerPlayer player : serverLevel.getChunkSource().chunkMap
                    .getPlayers(serverLevel.getChunkAt(worldPosition).getPos(), false)) {
                    player.connection.send(packet);
                }
            }
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            syncToClients();
        }
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
                return tanks[tank].isFluidValid(stack);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
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

    /// 服务器端 tick：在主动模式（红石信号激活）且有电时，向 FACING 方向泵送流体。前方是管道→沿管道追踪到远端再推送；前方是流体容器→直接推送；扬程 10 米，流速 50 mB/t 每米高度差。
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

        /// 确定目标：前方是管道 → 追踪到远端；否则直接用前方方块
        BlockPos targetPos;       /// 接收方的位置
        Direction targetQueryDir; /// 从接收方查询 IFluidHandler 的方向
        int pipeHeight = 0;       /// 管道沿途累计的等效高度

        if (frontState.getBlock() instanceof PipeBlock) {
            /// 从前方管道沿 facing.getOpposite() 方向追踪到管道远端
            /// getPipeEnd 的参数 direction 是"从管道哪一侧进入"，即接口连接管道的那一侧
            AbstractPipeBlockEntity.PipeEnd pipeEnd =
                AbstractPipeBlockEntity.getPipeEnd(level, frontPos, facing.getOpposite());
            if (pipeEnd == null) return;
            /// pipeEnd.direction() = 从管道末端指向接收方的方向
            targetPos = pipeEnd.pos().relative(pipeEnd.direction());
            targetQueryDir = pipeEnd.direction().getOpposite();
            pipeHeight = pipeEnd.effectiveHeight();
        } else {
            targetPos = frontPos;
            targetQueryDir = facing.getOpposite();
        }

        /// 计算有效高度差（含 10m 扬程，扣除管道累计等效高度）
        int sourceY = getBlockPos().getY();
        int targetY = targetPos.getY() - pipeHeight;
        int heightDiff = PUMP_HEADLIFT + sourceY - targetY;
        if (heightDiff <= 0) return;

        /// 复用管道系统的流体传输（自动通过 capability 查询 source / target）
        AbstractPipeBlockEntity.moveFluid(
            level,
            getBlockPos(),   /// sourcePos = 接口自身（内部储罐）
            facing,          /// sourceQueryDir（capability 忽略 side，任意方向均可）
            targetPos,       /// 接收方位置
            targetQueryDir,  /// 从接收方面向源
            heightDiff       /// 有效高度差（含扬程）
        );
    }
}
