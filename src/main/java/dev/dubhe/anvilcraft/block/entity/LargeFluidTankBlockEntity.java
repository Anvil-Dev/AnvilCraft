package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.fluid.FluidHandlerWrapper;
import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;
import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkManager;
import dev.dubhe.anvilcraft.block.LargeFluidTankBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.util.TankUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Redstone;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.List;

public class LargeFluidTankBlockEntity extends BlockEntity implements IFluidHandlerHolder {
    public static final int BASE_CAPACITY = 512 * FluidType.BUCKET_VOLUME;
    public static final int INFINITY_THRESHOLD = 12800 * FluidType.BUCKET_VOLUME;
    private static final int CHECK_INTERVAL = 100;
    private static final String TAG_TANK = "Tank";

    private final MultiFluidTankHandler tank = new MultiFluidTankHandler(
        BASE_CAPACITY,
        INFINITY_THRESHOLD,
        this::onTankChanged
    );
    private int tickCounter;

    public LargeFluidTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && !this.level.isClientSide()) {
            FluidNetworkManager.INSTANCE.addContainer(this.level, this.getBlockPos());
        }
    }

    @Override
    public void setRemoved() {
        if (this.level != null && !this.level.isClientSide()) {
            FluidNetworkManager.INSTANCE.removeContainer(this.level, this.getBlockPos());
        }
        super.setRemoved();
    }

    private void onTankChanged() {
        this.setChangedForAllParts();
        this.updateLightLevel();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
    }

    private void setChangedForAllParts() {
        if (this.level == null) return;
        BlockPos pos = this.getBlockPos();
        BlockState state = this.getBlockState();
        if (!(state.getBlock() instanceof LargeFluidTankBlock block)) return;
        Vec3i baseOffset = block.getOffset(state);
        for (Cube3x3PartHalf part : block.getParts()) {
            BlockEntity blockEntity = this.level.getBlockEntity(pos.subtract(baseOffset).offset(part.getOffset()));
            if (blockEntity != null) blockEntity.setChanged();
        }
    }

    public void tick() {
        BlockState state = getBlockState();
        if (!state.getValue(LargeFluidTankBlock.HALF).equals(Cube3x3PartHalf.MID_CENTER)) return;
        if (++this.tickCounter % CHECK_INTERVAL == 0 && this.level != null && !this.level.isClientSide) {
            boolean valid = TankUtil.isMengerStructure(this.level, this.getBlockPos(), 9);
            if (this.tank.isEnhanced() && !valid) {
                this.onUnformed();
            } else if (!this.tank.isEnhanced() && valid) {
                this.onFormed();
            }
        }
    }

    public void onFormed() {
        this.tank.setEnhanced(true);
    }

    public void onUnformed() {
        this.tank.setEnhanced(false);
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
        int lightLevel = 0;
        for (FluidStack stack : this.tank.copyFluids()) {
            lightLevel = Math.max(lightLevel, stack.getFluidType().getLightLevel(stack));
        }
        long renderCapacity = this.tank.isEnhanced() ? INFINITY_THRESHOLD : BASE_CAPACITY;
        double fill = Math.min(1, (double) this.tank.getTotalAmount() / renderCapacity);
        return (int) Math.ceil(lightLevel * fill);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.put(TAG_TANK, this.tank.serializeNBT(provider));
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.tank.deserializeNBT(provider, tag.getCompound(TAG_TANK));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put(TAG_TANK, this.tank.serializeNBT(registries));
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void removeComponentsFromTag(CompoundTag tag) {
        super.removeComponentsFromTag(tag);
        if (this.level != null) {
            tag.put(TAG_TANK, this.getMainPart().tank.serializeForItem(this.level.registryAccess()));
        }
    }

    public void saveToDrop(ItemStack stack, HolderLookup.Provider registries) {
        CompoundTag tag = this.saveCustomOnly(registries);
        this.removeComponentsFromTag(tag);
        tag.put(TAG_TANK, this.getMainPart().tank.serializeForDrop(registries));
        BlockItem.setBlockEntityData(stack, this.getType(), tag);
        stack.applyComponents(this.collectComponents());
    }

    public boolean onPlayerUse(Player player, InteractionHand hand) {
        if (this.level != null
            && FluidHandlerWrapper.tryInteractWithBottle(
                player,
                hand,
                this.getFluidHandler(),
                this.level,
                this.getBlockPos()
            )) {
            return true;
        }
        return FluidUtil.interactWithFluidHandler(player, hand, this.getFluidHandler());
    }

    public int getRedstoneSignal() {
        MultiFluidTankHandler tank = this.getMainPart().tank;
        long amount = tank.getTotalAmount();
        int capacity = tank.isEnhanced() ? INFINITY_THRESHOLD : BASE_CAPACITY;
        int strength = amount == 0 ? 0 : (int) (Math.min(amount, capacity)
            * (Redstone.SIGNAL_MAX - 1) / capacity) + 1;
        strength = Mth.clamp(strength, Redstone.SIGNAL_MIN, Redstone.SIGNAL_MAX);
        return strength;
    }

    @Override
    public IFluidHandler getFluidHandler() {
        return this.getMainPart().tank;
    }

    public boolean isMainPart() {
        LargeFluidTankBlock block = ModBlocks.LARGE_FLUID_TANK.get();
        return block.isMainPart(this.getBlockState());
    }

    private LargeFluidTankBlockEntity getMainPart() {
        LargeFluidTankBlock block = ModBlocks.LARGE_FLUID_TANK.get();
        BlockPos mainPartPos = block.getMainPartPos(this.getBlockPos(), this.getBlockState());
        if (this.getLevel() == null) return this;
        BlockEntity mainPart = this.getLevel().getBlockEntity(mainPartPos);
        return mainPart instanceof LargeFluidTankBlockEntity mainPart1 ? mainPart1 : this;
    }

    public boolean isEnhanced() {
        return this.getMainPart().tank.isEnhanced();
    }

    public boolean isInfinite(FluidStack fluid) {
        return this.getMainPart().tank.isInfinite(fluid);
    }

    public List<FluidStack> getStoredFluids() {
        return this.getMainPart().tank.copyFluids();
    }
}
