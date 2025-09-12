package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.block.PropelPiston;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.network.UpdatePropelPistonStoredEnergyPacket;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PropelPistonBlockEntity extends BaseLaserBlockEntity {
    /**
     * 储存的能量 单位：kJ
     */
    @Getter
    private int storedEnergy = 0;
    private int delay = 0;
    private int power = 0;

    public PropelPistonBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public Direction getFacing() {
        return getBlockState().getValue(PropelPiston.FACING);
    }

    public void updateStoredEnergy(Integer energy) {
        this.storedEnergy = Math.clamp(energy, 0, 80000);
        if (level == null || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        PacketDistributor.sendToPlayersTrackingChunk(serverLevel, new ChunkPos(getBlockPos()), new UpdatePropelPistonStoredEnergyPacket(getBlockPos(), storedEnergy));
    }

    public void addEnergy(int energy) {
        updateStoredEnergy(getStoredEnergy() + energy);
    }

    @Override
    protected int getBaseLaserLevel() {
        return 0;
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        updateLaserLevel(calculateLaserLevel());
        if (changed) {
            delay = 0;
            power = laserLevel * 15;
        }
        if (!changed) {
            if (storedEnergy < 80000) {
                delay++;
                if (delay >= 20) {
                    delay = 0;
                    addEnergy(power);
                }
            }
        }
        if (getStoredEnergy() > 0) {
            level.setBlockAndUpdate(pos, state.setValue(PropelPiston.EXHAUSTED, false));
            if (!level.getBlockTicks().hasScheduledTick(pos, state.getBlock())) {
                checkCanMove(level, pos, state);
            }
        } else {
            level.setBlockAndUpdate(pos, state.setValue(PropelPiston.EXHAUSTED, true).setValue(PropelPiston.MOVING, false));
        }
        super.tick(level);
        resetState();
    }

    @Override
    public Set<Direction> getIgnoreFace() {
        Set<Direction> directions = new HashSet<>(List.of(Direction.values()));
        directions.remove(getBlockState().getValue(PropelPiston.FACING).getOpposite());
        return directions;
    }

    private void checkCanMove(Level level, BlockPos pos, BlockState state) {
        Direction direction = state.getValue(PropelPiston.FACING);
        if (state.getValue(PropelPiston.MOVING)) {
            if (new PistonStructureResolver(level, pos, direction, true).resolve()) {
                level.blockEvent(pos, state.getBlock(), 0, state.getValue(PropelPiston.FACING).get3DDataValue());
            } else {
                level.setBlockAndUpdate(pos, state.setValue(PropelPiston.MOVING, false));
            }
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.storedEnergy = tag.getInt("storedEnergy");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("storedEnergy", Math.min(this.storedEnergy, 80000));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag compound = new CompoundTag();
        compound.putLong("storedEnergy", this.storedEnergy);
        return compound;
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput componentInput) {
        Integer energy = componentInput.getOrDefault(ModComponents.STORED_ENERGY, 0);
        this.updateStoredEnergy(energy);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        components.set(ModComponents.STORED_ENERGY, this.storedEnergy);
    }
}
