package dev.dubhe.anvilcraft.block.entity;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Laser interface for the Celestial Forging Anvil.
 * Receives laser beams and reports their level via the anvil hammer HUD.
 * No power consumption.
 */
public class CelestialForgingAnvilLaserInterfaceBlockEntity extends BlockEntity {
    @Getter
    private int receivedLaserLevel = 0;
    @Getter
    private boolean laserValid = false;
    @Getter
    private int requiredLaserLevel = 0;

    public CelestialForgingAnvilLaserInterfaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    /**
     * Sync block entity data to all tracking clients.
     */
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

    public void onLaserReceived(int level) {
        this.receivedLaserLevel = level;
        this.laserValid = (requiredLaserLevel > 0 && level >= requiredLaserLevel);
        this.setChanged();
    }

    public void setRequiredLaserLevel(int level) {
        this.requiredLaserLevel = level;
        this.laserValid = (receivedLaserLevel > 0 && receivedLaserLevel >= level);
        this.setChanged();
    }

    public void resetLaser() {
        this.receivedLaserLevel = 0;
        this.laserValid = false;
        this.setChanged();
    }

    @Override
    protected void saveAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeLaserData(tag);
    }

    @Override
    protected void loadAdditional(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        readLaserData(tag);
    }

    @Override
    public @NotNull CompoundTag getUpdateTag(@NotNull HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        writeLaserData(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(@NotNull CompoundTag tag, @NotNull HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        readLaserData(tag);
    }

    private void writeLaserData(CompoundTag tag) {
        tag.putInt("receivedLaserLevel", receivedLaserLevel);
        tag.putInt("requiredLaserLevel", requiredLaserLevel);
        tag.putBoolean("laserValid", laserValid);
    }

    private void readLaserData(CompoundTag tag) {
        this.receivedLaserLevel = tag.getInt("receivedLaserLevel");
        this.requiredLaserLevel = tag.getInt("requiredLaserLevel");
        this.laserValid = tag.getBoolean("laserValid");
    }
}
