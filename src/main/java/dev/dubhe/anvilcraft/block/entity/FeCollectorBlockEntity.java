package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.IPowerProducer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.api.tooltip.providers.IHasAffectRange;
import dev.dubhe.anvilcraft.block.FeCollectorBlock;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

public class FeCollectorBlockEntity extends BlockEntity implements IPowerProducer, IHasAffectRange {
    public static final int MAX_ENERGY = 1_000_000;
    static final int FE_PER_TICK = 10_000;
    public static final int PRODUCE_THRESHOLD = 400_000;
    public static final int STOP_THRESHOLD = 20_000;
    static final int TRANSFER_THRESHOLD = 500_000;

    int energy;
    @Getter
    float rotation;
    @Getter
    int time;
    boolean producing;
    int outputPower;
    PowerGrid grid;
    private boolean clientSyncDirty;

    public static FeCollectorBlockEntity createBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        return new FeCollectorBlockEntity(type, pos, state);
    }

    public FeCollectorBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.FE_COLLECTOR.get(), pos, blockState);
    }

    private FeCollectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.energy = input.getIntOr("Energy", 0);
        this.producing = input.getBooleanOr("Producing", false);
        this.time = input.getIntOr("Time", 0);
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Energy", this.energy);
        output.putBoolean("Producing", this.producing);
        output.putInt("Time", this.time);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("Energy", this.energy);
        tag.putBoolean("Producing", this.producing);
        tag.putInt("Time", this.time);
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection connection, ValueInput input) {
        super.onDataPacket(connection, input);
        this.energy = input.getIntOr("Energy", 0);
        this.producing = input.getBooleanOr("Producing", false);
        this.time = input.getIntOr("Time", 0);
    }

    Direction[] getConnectedSides() {
        Direction.Axis a = getBlockState().getValue(BlockStateProperties.HORIZONTAL_AXIS);
        return a == Direction.Axis.X
            ? new Direction[]{Direction.EAST, Direction.WEST}
            : new Direction[]{Direction.NORTH, Direction.SOUTH};
    }

    Direction getOutputSide() {
        Direction.Axis a = getBlockState().getValue(BlockStateProperties.HORIZONTAL_AXIS);
        return a == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
    }

    @Nullable
    public EnergyHandler getEnergyStorage(@Nullable Direction side) {
        if (side == null) return new FeEnergyStore();
        for (Direction d : getConnectedSides()) {
            if (d == side) return new FeEnergyStore();
        }
        return null;
    }

    @SuppressWarnings("unused")
    public static void tick(Level level, BlockPos pos, BlockState state, FeCollectorBlockEntity be) {
        if (level.isClientSide()) {
            be.clientTick();
            return;
        }
        be.serverTick();
    }

    void serverTick() {
        if (level == null) return;
        BlockState state = getBlockState();
        if (state.getValue(FeCollectorBlock.POWERED) != this.producing) {
            level.setBlockAndUpdate(getBlockPos(), state.setValue(FeCollectorBlock.POWERED, this.producing));
        }
        if (this.energy > TRANSFER_THRESHOLD) {
            pushExcess();
        }
        if (clientSyncDirty && level.getGameTime() % 20 == 0) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
            clientSyncDirty = false;
        }
    }

    void pushExcess() {
        if (level == null) return;
        int excess = this.energy - TRANSFER_THRESHOLD;
        if (excess <= 0) return;
        Direction side = getOutputSide();
        EnergyHandler target = level.getCapability(
            Capabilities.Energy.BLOCK, getBlockPos().relative(side), side.getOpposite()
        );
        if (target != null) {
            try (Transaction transaction = Transaction.openRoot()) {
                int accepted = target.insert(excess, transaction);
                transaction.commit();
                if (accepted > 0) {
                    this.energy -= accepted;
                    setChanged();
                    clientSyncDirty = true;
                }
            }
        }
    }

    public void clientTick() {
        this.rotation += (float) (Math.log(this.getServerPower() + 1) * 2.5);
    }

    @Override
    public int getOutputPower() {
        return this.outputPower;
    }

    @Override
    public int getRange() {
        return 2;
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
    public void setGrid(@Nullable PowerGrid grid) {
        this.grid = grid;
    }

    @Override
    public @Nullable PowerGrid getGrid() {
        return this.grid;
    }

    @Override
    public AABB shape() {
        return AABB.ofSize(this.getBlockPos().getCenter(), 5, 5, 5);
    }

    @Override
    public void gridTick() {
        if (level == null || level.isClientSide()) return;

        if (this.energy >= PRODUCE_THRESHOLD) {
            this.producing = true;
        } else if (this.energy < STOP_THRESHOLD) {
            this.producing = false;
        }

        if (this.producing) {
            final int prev = this.outputPower;
            this.energy -= FE_PER_TICK;
            this.outputPower = (int) (FE_PER_TICK
                * (1 - AnvilCraft.CONFIG.powerConverter.powerConverterLoss)
                / AnvilCraft.CONFIG.powerConverter.powerConverterEfficiency);
            this.time++;
            setChanged();
            clientSyncDirty = true;
            if (this.outputPower != prev && this.grid != null) this.grid.markChanged();
        } else if (this.outputPower > 0) {
            this.outputPower = 0;
            if (this.grid != null) this.grid.markChanged();
        }
    }

    public int getEnergyStored() {
        return this.energy;
    }

    class FeEnergyStore implements EnergyHandler {

        @Override
        public long getAmountAsLong() {
            return energy;
        }

        @Override
        public long getCapacityAsLong() {
            return MAX_ENERGY;
        }

        @Override
        public int insert(int maxInsert, TransactionContext transaction) {
            if (energy >= MAX_ENERGY) return 0;
            int r = Math.min(MAX_ENERGY - energy, maxInsert);
            if (r > 0) {
                energy += r;
                setChanged();
                clientSyncDirty = true;
            }
            return r;
        }

        @Override
        public int extract(int maxExtract, TransactionContext transaction) {
            if (energy <= 0) return 0;
            int r = Math.min(energy, maxExtract);
            if (r > 0) {
                energy -= r;
                setChanged();
                clientSyncDirty = true;
            }
            return r;
        }
    }
}
