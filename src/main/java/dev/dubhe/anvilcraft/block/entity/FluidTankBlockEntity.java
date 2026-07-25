package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.fluid.FluidHandlerWrapper;
import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;
import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkManager;
import dev.dubhe.anvilcraft.util.TankUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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

public class FluidTankBlockEntity extends BlockEntity implements IFluidHandlerHolder {
    public static final int BASE_CAPACITY = 16 * FluidType.BUCKET_VOLUME;
    public static final int INFINITY_THRESHOLD = 12800 * FluidType.BUCKET_VOLUME;
    private static final int CHECK_INTERVAL = 100;
    private static final String TAG_TANK = "Tank";

    private final SingleFluidTankHandler tank = new SingleFluidTankHandler(
        BASE_CAPACITY,
        INFINITY_THRESHOLD,
        this::onTankChanged
    );
    private int tickCounter;

    public FluidTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
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

    public void onFormed() {
        this.tank.setEnhanced(true);
    }

    public void onUnformed() {
        this.tank.setEnhanced(false);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FluidTankBlockEntity entity) {
        if (level.isClientSide || ++entity.tickCounter % CHECK_INTERVAL != 0) return;
        boolean valid = TankUtil.isMengerStructure(level, pos, 3);
        if (entity.tank.isEnhanced() && !valid) {
            entity.onUnformed();
        } else if (!entity.tank.isEnhanced() && valid) {
            entity.onFormed();
        }
    }

    private void onTankChanged() {
        this.setChanged();
        this.updateLightLevel();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), Block.UPDATE_ALL);
        }
    }

    private void updateLightLevel() {
        if (this.level == null) return;
        AuxiliaryLightManager manager = this.level.getAuxLightManager(this.getBlockPos());
        if (manager == null) return;

        FluidStack fluid = this.tank.getFluid();
        float fill = Math.min(1.0F, (float) this.tank.getFluidAmount() / this.tank.getCapacity());
        manager.setLightAt(
            this.getBlockPos(),
            (int) Math.ceil(fluid.getFluidType().getLightLevel(fluid) * fill)
        );
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.put(TAG_TANK, this.tank.writeToNBT(provider, new CompoundTag()));
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.tank.readFromNBT(provider, tag.getCompound(TAG_TANK));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put(TAG_TANK, this.tank.writeToNBT(registries, new CompoundTag()));
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
            tag.put(TAG_TANK, this.tank.serializeForItem(this.level.registryAccess()));
        }
    }

    public void saveToDrop(ItemStack stack, HolderLookup.Provider registries) {
        CompoundTag tag = this.saveCustomOnly(registries);
        this.removeComponentsFromTag(tag);
        tag.put(TAG_TANK, this.tank.serializeForDrop(registries));
        BlockItem.setBlockEntityData(stack, this.getType(), tag);
        stack.applyComponents(this.collectComponents());
    }

    public boolean onPlayerUse(Player player, InteractionHand hand) {
        if (this.level != null
            && FluidHandlerWrapper.tryInteractWithBottle(player, hand, this.tank, this.level, this.getBlockPos())) {
            return true;
        }
        return FluidUtil.interactWithFluidHandler(player, hand, this.tank);
    }

    public int getRedstoneSignal() {
        int amount = this.tank.getFluidAmount();
        int capacity = this.tank.getCapacity();
        int strength = amount == 0 ? 0 : amount * (Redstone.SIGNAL_MAX - 1) / capacity + 1;
        return Mth.clamp(strength, Redstone.SIGNAL_MIN, Redstone.SIGNAL_MAX);
    }

    @Override
    public IFluidHandler getFluidHandler() {
        return this.tank;
    }

    public boolean isInfinite() {
        return this.tank.isInfinite();
    }
}
