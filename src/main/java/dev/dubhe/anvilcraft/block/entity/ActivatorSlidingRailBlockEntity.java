package dev.dubhe.anvilcraft.block.entity;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class ActivatorSlidingRailBlockEntity extends BlockEntity {
    @Getter
    private boolean shouldPower = false;

    public ActivatorSlidingRailBlockEntity(
        BlockEntityType<?> type, BlockPos pos,
        BlockState blockState
    ) {
        super(type, pos, blockState);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag data = super.getUpdateTag(registries);
        data.putBoolean("ShouldPower", this.shouldPower);
        return data;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("ShouldPower", this.shouldPower);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.shouldPower = tag.getBoolean("ShouldPower");
    }

    public void shouldNotPower() {
        this.shouldPower = false;
    }

    public void shouldPower() {
        this.shouldPower = true;
    }
}
