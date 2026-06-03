package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.block.laser.PropelPistonBlock;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.item.property.component.StoredEnergy;
import dev.dubhe.anvilcraft.network.UpdatePropelPistonStoredEnergyPacket;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentGetter;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PropelPistonBlockEntity extends BaseLaserBlockEntity {
    /// 储存的能量 单位：kJ
    @Getter
    private int storedEnergy = 0;
    private int delay = 0;
    private int power = 0;

    public PropelPistonBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public Direction getFacing() {
        return getBlockState().getValue(PropelPistonBlock.FACING);
    }

    public void updateStoredEnergy(Integer energy) {
        this.storedEnergy = Math.clamp(energy, 0, 80000);
        if (level == null || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        PacketDistributor.sendToPlayersTrackingChunk(
            serverLevel,
            ChunkPos.containing(getBlockPos()),
            new UpdatePropelPistonStoredEnergyPacket(getBlockPos(), this.storedEnergy)
        );
    }

    public void addEnergy(int energy) {
        this.updateStoredEnergy(getStoredEnergy() + energy);
    }

    @Override
    protected int getBaseLaserLevel() {
        return 0;
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        updateLaserLevel(calculateLaserLevel());
        if (changed) {
            this.delay = 0;
            this.power = laserLevel * 15;
        }
        if (!changed) {
            if (this.storedEnergy < 80000) {
                this.delay++;
                if (this.delay >= 20) {
                    this.delay = 0;
                    this.addEnergy(this.power);
                }
            }
        }
        if (getStoredEnergy() > 0) {
            level.setBlockAndUpdate(pos, state.setValue(PropelPistonBlock.EXHAUSTED, false));
            if (!level.getBlockTicks().hasScheduledTick(pos, state.getBlock())) {
                this.checkCanMove(level, pos, state);
            }
        } else {
            level.setBlockAndUpdate(pos, state.setValue(PropelPistonBlock.EXHAUSTED, true).setValue(PropelPistonBlock.MOVING, false));
        }
        super.tick(level);
        resetState();
    }

    @Override
    public Set<Direction> getIgnoreFace() {
        Set<Direction> directions = new HashSet<>(List.of(Direction.values()));
        directions.remove(getBlockState().getValue(PropelPistonBlock.FACING).getOpposite());
        return directions;
    }

    private void checkCanMove(Level level, BlockPos pos, BlockState state) {
        Direction direction = state.getValue(PropelPistonBlock.FACING);
        if (state.getValue(PropelPistonBlock.MOVING)) {
            if (new PistonStructureResolver(level, pos, direction, true).resolve()) {
                level.blockEvent(pos, state.getBlock(), 0, state.getValue(PropelPistonBlock.FACING).get3DDataValue());
            } else {
                level.setBlockAndUpdate(pos, state.setValue(PropelPistonBlock.MOVING, false));
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.storedEnergy = input.getIntOr("storedEnergy", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("storedEnergy", Math.min(this.storedEnergy, 80000));
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
    protected void applyImplicitComponents(DataComponentGetter components) {
        Integer energy = components.getOrDefault(ModComponents.STORED_ENERGY, StoredEnergy.EMPTY).value();
        this.updateStoredEnergy(energy);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        components.set(ModComponents.STORED_ENERGY, new StoredEnergy(this.storedEnergy));
    }
}
