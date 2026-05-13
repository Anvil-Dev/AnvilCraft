package dev.dubhe.anvilcraft.block.entity;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.TriState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

@Getter
public class ActivatorSlidingRailBlockEntity extends BlockEntity {
    private TriState shouldPower = TriState.DEFAULT;

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
        data.putInt("ShouldPower", this.shouldPower.ordinal());
        return data;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("ShouldPower", this.shouldPower.ordinal());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.shouldPower = TriState.values()[input.getIntOr("ShouldPower", 0)];
    }

    public boolean shouldPower() {
        return this.shouldPower != TriState.FALSE;
    }

    public void stopPulse() {
        this.shouldPower = TriState.FALSE;
    }

    public void startPulse() {
        this.shouldPower = TriState.TRUE;
    }

    public void backToDefault() {
        this.shouldPower = TriState.DEFAULT;
    }
}
