package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.BasePowerConverterBlock;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

public class PowerConverterBlockEntity extends BlockEntity implements IPowerConsumer {
    private PowerGrid grid = null;
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
        return inputPower * 10000;
    }

    public @Nullable IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        if (side == null) return new PowerConverterEnergyStore();
        if (side == getBlockState().getValue(BasePowerConverterBlock.FACING)) return new PowerConverterEnergyStore();
        return null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("InputPower", inputPower);
        tag.putInt("Cooldown", cooldown);
        tag.putInt("Energy", energy);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        inputPower = tag.getInt("InputPower");
        cooldown = tag.getInt("Cooldown");
        energy = tag.getInt("Energy");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("Energy", energy);
        tag.putInt("InputPower", inputPower);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        this.energy = tag.getInt("Energy");
        this.inputPower = tag.getInt("InputPower");
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        super.onDataPacket(connection, packet, registries);
        CompoundTag tag = packet.getTag();
        handleUpdateTag(tag, registries);
    }

    public int getEnergyStored() {
        return this.energy;
    }

    public int getMaxEnergyStored() {
        return getMaxEnergy();
    }

    /**
     * tick
     */
    public void tick() {
        if (this.level != null) {
            flushState(this.level, getBlockPos());
        }
        if (cooldown == 0) {
            cooldown = AnvilCraft.CONFIG.powerConverter.powerConverterCountdown;
            if (getBlockState().getValue(BasePowerConverterBlock.OVERLOAD)) return;
            int amountTick = (int) (inputPower
                                    * AnvilCraft.CONFIG.powerConverter.powerConverterEfficiency
                                    * (1 - AnvilCraft.CONFIG.powerConverter.powerConverterLoss)
            );
            int amount = amountTick * AnvilCraft.CONFIG.powerConverter.powerConverterCountdown;
            this.energy = Math.min(this.energy + amount, getMaxEnergy());
            setChanged();
        } else {
            cooldown--;
        }
        pushEnergy();
        if (this.level != null && level.getGameTime() % 20 == 0) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void pushEnergy() {
        if (this.level == null || this.energy <= 0) return;
        Direction face = getBlockState().getValue(BasePowerConverterBlock.FACING);
        IEnergyStorage target = level.getCapability(
            Capabilities.EnergyStorage.BLOCK,
            getBlockPos().relative(face),
            face.getOpposite()
        );
        if (target != null && target.canReceive()) {
            int accepted = target.receiveEnergy(this.energy, false);
            if (accepted > 0) {
                this.energy -= accepted;
                setChanged();
            }
        }
    }

    @Override
    public int getInputPower() {
        return inputPower;
    }

    @Override
    public Level getCurrentLevel() {
        return getLevel();
    }

    @Override
    public BlockPos getPos() {
        return getBlockPos();
    }

    @Override
    public void setGrid(@Nullable PowerGrid grid) {
        this.grid = grid;
    }

    @Override
    public @Nullable PowerGrid getGrid() {
        return grid;
    }

    class PowerConverterEnergyStore implements IEnergyStorage {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int r = Math.min(energy, maxExtract);
            if (!simulate) {
                energy -= r;
                setChanged();
            }
            return r;
        }

        @Override
        public int getEnergyStored() {
            return energy;
        }

        @Override
        public int getMaxEnergyStored() {
            return getMaxEnergy();
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    }
}
