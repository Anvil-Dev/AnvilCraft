package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;
import dev.dubhe.anvilcraft.api.fluidtank.InfinityFluidTank;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

public class CreativeFluidTankBlockEntity extends BlockEntity implements IFluidHandlerHolder {
    private final FluidTank fluidHandler = new InfinityFluidTank() {
        @Override
        protected void onContentsChanged() {
            CreativeFluidTankBlockEntity.this.setChanged();
            if (CreativeFluidTankBlockEntity.this.level != null) {
                CreativeFluidTankBlockEntity.this.level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    };

    public CreativeFluidTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        CompoundTag fluidTag = new CompoundTag();
        this.fluidHandler.writeToNBT(registries, fluidTag);
        tag.put("infinityFluid", fluidTag);
        return tag;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!this.fluidHandler.isEmpty()) {
            CompoundTag compoundTag = new CompoundTag();
            this.fluidHandler.writeToNBT(registries, compoundTag);
            tag.put("infinityFluid", compoundTag);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("infinityFluid")) {
            Tag infinityFluid = tag.get("infinityFluid");
            if (infinityFluid instanceof CompoundTag compoundTag) {
                this.fluidHandler.readFromNBT(registries, compoundTag);
            }
        }
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public IFluidHandler getFluidHandler() {
        return this.fluidHandler;
    }
}
