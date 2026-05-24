package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.IFluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class FluidTankBlockEntity extends BlockEntity implements IFluidHandlerHolder {
    public static final int CAPACITY = 16 * FluidType.BUCKET_VOLUME;
    public static final int BIG_CAPACITY = 640 * FluidType.BUCKET_VOLUME;
    protected final FluidTank tank = new FluidTank(CAPACITY) {
        @Override
        public FluidTank readFromNBT(HolderLookup.Provider lookupProvider, CompoundTag nbt) {
            FluidTank tank = super.readFromNBT(lookupProvider, nbt);
            this.onContentsChanged();
            return tank;
        }

        @Override
        protected void onContentsChanged() {
            FluidTankBlockEntity.this.updateLightLevel();
        }
    };
    protected boolean isBigger = false;

    public FluidTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public void onFormed() {
        this.isBigger = true;
        this.tank.setCapacity(BIG_CAPACITY);
    }

    public void onUnformed() {
        this.isBigger = false;
        this.tank.setCapacity(CAPACITY);
    }

    private void updateLightLevel() {
        if (this.level == null) {
            return;
        }

        BlockPos pos = this.getBlockPos();
        AuxiliaryLightManager manager = this.level.getAuxLightManager(pos);
        if (manager == null) {
            return;
        }
        manager.setLightAt(pos, this.computeLightLevel());
    }

    private int computeLightLevel() {
        FluidStack stack = this.tank.getFluid();
        float fill = (float) this.tank.getFluidAmount() / this.tank.getCapacity();
        return (int) Math.ceil(stack.getFluidType().getLightLevel(stack) * fill);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putBoolean("bigger", this.isBigger);
        CompoundTag tankNbt = tank.writeToNBT(provider, new CompoundTag());
        if (!tankNbt.isEmpty()) {
            tag.put("tank", tankNbt);
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.isBigger = tag.getBoolean("bigger");
        if (this.isBigger) {
            this.onFormed();
        } else {
            this.onUnformed();
        }
        tank.readFromNBT(provider, tag.getCompound("tank"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("bigger", this.isBigger);
        CompoundTag fluidTag = new CompoundTag();
        tank.writeToNBT(registries, fluidTag);
        tag.put("tank", fluidTag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public boolean onPlayerUse(Player player, InteractionHand hand) {
        return FluidUtil.interactWithFluidHandler(player, hand, tank);
    }

    public IFluidTank getTank() {
        return tank;
    }

    public IFluidHandler getFluidHandler() {
        return tank;
    }
}
