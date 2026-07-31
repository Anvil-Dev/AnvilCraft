package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.energy.IEnergyHandlerHolder;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.power.converter.BasePowerConverterBlock;
import lombok.Getter;
import lombok.Setter;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jspecify.annotations.Nullable;

public class PowerConverterBlockEntity extends BlockEntity implements IPowerConsumer, IEnergyHandlerHolder {
    @Getter
    @Setter
    private @Nullable PowerGrid grid = null;
    private int inputPower;
    private int cooldown = 0;
    int energy = 0;

    public PowerConverterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        this(type, pos, blockState, 1);
    }

    public PowerConverterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState, int inputPower) {
        super(type, pos, blockState);
        this.inputPower = inputPower;
    }

    public static PowerConverterBlockEntity createBlockEntity(
        BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        return new PowerConverterBlockEntity(type, pos, blockState);
    }

    int getMaxEnergy() {
        return this.inputPower * 10000;
    }

    @Override
    public int getInputPower() {
        return this.getBlockState().getValue(BasePowerConverterBlock.POWERED) ? 0 : this.inputPower;
    }

    @Override
    public @Nullable EnergyHandler getEnergyHandler(@Nullable Direction side) {
        if (side == null) return new PowerConverterEnergyStore();
        if (side == this.getBlockState().getValue(BasePowerConverterBlock.FACING)) return new PowerConverterEnergyStore();
        return null;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("InputPower", this.inputPower);
        output.putInt("Cooldown", this.cooldown);
        output.putInt("Energy", this.energy);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.inputPower = input.getIntOr("InputPower", 0);
        this.cooldown = input.getIntOr("Cooldown", 0);
        this.energy = input.getIntOr("Energy", 0);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("Energy", this.energy);
        tag.putInt("InputPower", this.inputPower);
        return tag;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection connection, ValueInput input) {
        this.loadAdditional(input);
    }

    public int getEnergyStored() {
        return this.energy;
    }

    public int getMaxEnergyStored() {
        return this.getMaxEnergy();
    }

    /// tick
    public void tick() {
        if (this.level != null) {
            this.flushState(this.level, this.getBlockPos());
            if (this.getBlockState().getValue(BasePowerConverterBlock.POWERED)) return;
        }
        if (this.cooldown == 0) {
            this.cooldown = AnvilCraft.CONFIG.powerConverter.powerConverterCountdown;
            if (this.getBlockState().getValue(BasePowerConverterBlock.OVERLOAD)) return;
            int amountTick = (int) (
                this.inputPower
                * AnvilCraft.CONFIG.powerConverter.powerConverterEfficiency
                * (1 - AnvilCraft.CONFIG.powerConverter.powerConverterLoss)
            );
            int amount = amountTick * AnvilCraft.CONFIG.powerConverter.powerConverterCountdown;
            this.energy = Math.min(this.energy + amount, this.getMaxEnergy());
            this.setChanged();
        } else {
            this.cooldown--;
        }
        this.pushEnergy();
        if (this.level != null && this.level.getGameTime() % 20 == 0) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
    }

    private void pushEnergy() {
        if (this.level == null || this.energy <= 0) return;
        Direction face = this.getBlockState().getValue(BasePowerConverterBlock.FACING);
        EnergyHandler target = this.level.getCapability(
            Capabilities.Energy.BLOCK,
            this.getBlockPos().relative(face),
            face.getOpposite()
        );
        if (target != null) {
            try (Transaction transaction = Transaction.openRoot()) {
                int accepted = target.insert(this.energy, transaction);
                transaction.commit();
                if (accepted > 0) {
                    this.energy -= accepted;
                    this.setChanged();
                }
            }
        }
    }

    @Override
    public @Nullable Level getCurrentLevel() {
        return this.getLevel();
    }

    @Override
    public BlockPos getPos() {
        return this.getBlockPos();
    }

    class PowerConverterEnergyStore implements EnergyHandler {

        @Override
        public long getAmountAsLong() {
            return PowerConverterBlockEntity.this.energy;
        }

        @Override
        public long getCapacityAsLong() {
            return PowerConverterBlockEntity.this.getMaxEnergy();
        }

        @Override
        public int insert(int maxInsert, TransactionContext transaction) {
            return 0;
        }

        @Override
        public int extract(int maxExtract, TransactionContext transaction) {
            if (PowerConverterBlockEntity.this.energy <= 0) return 0;
            int r = Math.min(PowerConverterBlockEntity.this.energy, maxExtract);
            if (r > 0) {
                PowerConverterBlockEntity.this.energy -= r;
                PowerConverterBlockEntity.this.setChanged();
            }
            return r;
        }
    }
}
