package dev.dubhe.anvilcraft.block.entity.fluid;

import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

@Getter
public class PipeNodeBlockEntity extends AbstractPipeBlockEntity implements IFluidHandlerHolder {
    public static final int CAPACITY = FluidType.BUCKET_VOLUME;
    private final FluidTank fluidHandler = new FluidTank(PipeNodeBlockEntity.CAPACITY) {
        @Override
        protected void onContentsChanged() {
            PipeNodeBlockEntity.this.setChanged();
            PipeNodeBlockEntity.this.sendUpdate();
            PipeNodeBlockEntity.this.sendNeighbourUpdate();
        }

        @Override
        public FluidTank readFromNBT(HolderLookup.Provider lookupProvider, CompoundTag nbt) {
            FluidTank tank = super.readFromNBT(lookupProvider, nbt);
            this.onContentsChanged();
            return tank;
        }
    };

    protected PipeNodeBlockEntity(
        BlockEntityType<PipeNodeBlockEntity> type,
        BlockPos pos,
        BlockState blockState
    ) {
        super(type, pos, blockState);
    }

    public static PipeNodeBlockEntity create(BlockEntityType<PipeNodeBlockEntity> type, BlockPos pos, BlockState blockState) {
        return new PipeNodeBlockEntity(type, pos, blockState);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag tankNbt = this.fluidHandler.writeToNBT(registries, new CompoundTag());
        if (!tankNbt.isEmpty()) {
            tag.put("Fluid", tankNbt);
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.fluidHandler.readFromNBT(registries, tag.getCompound("Fluid"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        CompoundTag tankNbt = this.fluidHandler.writeToNBT(registries, new CompoundTag());
        if (!tankNbt.isEmpty()) {
            tag.put("Fluid", tankNbt);
        }
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
