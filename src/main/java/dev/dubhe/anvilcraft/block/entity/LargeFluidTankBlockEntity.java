package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;
import dev.dubhe.anvilcraft.api.fluidtank.InfinityFluidTank;
import dev.dubhe.anvilcraft.block.LargeFluidTankBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
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

public class LargeFluidTankBlockEntity extends BlockEntity implements IFluidHandlerHolder {
    public static final int CAPACITY = 320 * FluidType.BUCKET_VOLUME;
    public static final int BIG_CAPACITY = 12800 * FluidType.BUCKET_VOLUME;
    protected final InfinityFluidTank tank = new InfinityFluidTank(CAPACITY, false) {
        @Override
        public FluidTank readFromNBT(HolderLookup.Provider lookupProvider, CompoundTag nbt) {
            FluidTank tank = super.readFromNBT(lookupProvider, nbt);
            this.onContentsChanged();
            return tank;
        }

        @Override
        protected void onContentsChanged() {
            LargeFluidTankBlockEntity.this.setChanged();
            LargeFluidTankBlockEntity.this.updateLightLevel();
            LargeFluidTankBlockEntity.this.updateBlock();
        }
    };
    protected boolean isBigger = false;

    public LargeFluidTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    private void updateBlock() {
        if (this.level != null) {
            this.level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    public void tick() {
        BlockState state = getBlockState();
        if (!state.getValue(LargeFluidTankBlock.HALF).equals(Cube3x3PartHalf.MID_CENTER)) return;
        this.checkInfinity();
        setChanged();
    }

    protected void checkInfinity() {
        if (this.tank.getCapacity() == LargeFluidTankBlockEntity.BIG_CAPACITY && this.tank.getSpace() <= 0) this.tank.setInfinity(true);
    }

    public void onFormed() {
        this.isBigger = true;
        this.tank.setCapacity(BIG_CAPACITY);
    }

    public void onUnformed() {
        this.isBigger = false;
        this.tank.setInfinity(false);
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
        FluidType type = stack.getFluidType();
        if (this.tank.isInfinity()) {
            return type.getLightLevel(stack);
        }
        float fill = (float) this.tank.getFluidAmount() / this.tank.getCapacity();
        return (int) Math.ceil(type.getLightLevel(stack) * fill);
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
        this.checkInfinity();
        return FluidUtil.interactWithFluidHandler(player, hand, this.getFluidHandler());
    }

    public IFluidTank getTank() {
        return this.getMainPart().tank;
    }

    public IFluidHandler getFluidHandler() {
        return this.getMainPart().tank;
    }

    public boolean isMainPart() {
        LargeFluidTankBlock block = ModBlocks.LARGE_FLUID_TANK.get();
        return block.isMainPart(this.getBlockState());
    }

    public LargeFluidTankBlockEntity getMainPart() {
        LargeFluidTankBlock block = ModBlocks.LARGE_FLUID_TANK.get();
        BlockPos mainPartPos = block.getMainPartPos(this.getBlockPos(), this.getBlockState());
        if (this.getLevel() == null) return this;
        BlockEntity mainPart = this.getLevel().getBlockEntity(mainPartPos);
        return mainPart instanceof LargeFluidTankBlockEntity mainPart1 ? mainPart1 : this;
    }
}
