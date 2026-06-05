package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.block.FeCollectorBlock;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

public class FeCollectorBlockEntity extends BlockEntity {
    private static final int MAX_ENERGY = 1_000_000; // 1 MFE

    private int energy = 0;
    @Getter
    private float rotation = 0;

    public static FeCollectorBlockEntity createBlockEntity(
        BlockEntityType<?> type, BlockPos pos, BlockState blockState
    ) {
        return new FeCollectorBlockEntity(type, pos, blockState);
    }

    public FeCollectorBlockEntity(BlockPos pos, BlockState blockState) {
        this(ModBlockEntities.FE_COLLECTOR.get(), pos, blockState);
    }

    private FeCollectorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.energy = tag.getInt("Energy");
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Energy", this.energy);
    }

    /**
     * 检查是否是左右两侧接口（可输入输出FE的面）
     */
    private boolean isSideConnected(@Nullable Direction side) {
        if (side == null) return false;
        Direction.Axis axis = getBlockState().getValue(BlockStateProperties.HORIZONTAL_AXIS);
        // axis=x (rotation=0°): 模型支柱在 EAST/WEST 面
        // axis=z (rotation=90°): 模型支柱旋转到 NORTH/SOUTH 面
        return axis == Direction.Axis.X
            ? side == Direction.EAST || side == Direction.WEST
            : side == Direction.NORTH || side == Direction.SOUTH;
    }

    /**
     * 获取指定面的FE能量存储
     */
    @Nullable
    public IEnergyStorage getEnergyStorage(@Nullable Direction side) {
        if (side != null && !isSideConnected(side)) {
            return null;
        }
        return new FeEnergyStorage();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FeCollectorBlockEntity be) {
        if (level == null) return;
        if (level.isClientSide()) {
            be.clientTick();
            return;
        }
        // 根据能量更新POWERED状态
        boolean powered = state.getValue(FeCollectorBlock.POWERED);
        boolean hasEnergy = be.energy > 0;
        if (powered != hasEnergy) {
            level.setBlockAndUpdate(pos, state.setValue(FeCollectorBlock.POWERED, hasEnergy));
        }
    }

    /**
     * 获取当前存储的能量（用于渲染）
     */
    public int getEnergyStored() {
        return this.energy;
    }

    public void clientTick() {
        this.rotation += (float) (Math.log(Math.max(this.energy, 0) + 1) * 2.5);
    }

    /**
     * 内部FE能量存储实现
     */
    private class FeEnergyStorage implements IEnergyStorage {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (!canReceive()) return 0;
            int energyReceived = Math.min(MAX_ENERGY - energy, maxReceive);
            if (!simulate) {
                energy += energyReceived;
                setChanged();
            }
            return energyReceived;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            if (!canExtract()) return 0;
            int energyExtracted = Math.min(energy, maxExtract);
            if (!simulate) {
                energy -= energyExtracted;
                setChanged();
            }
            return energyExtracted;
        }

        @Override
        public int getEnergyStored() {
            return energy;
        }

        @Override
        public int getMaxEnergyStored() {
            return MAX_ENERGY;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return energy < MAX_ENERGY;
        }
    }
}
