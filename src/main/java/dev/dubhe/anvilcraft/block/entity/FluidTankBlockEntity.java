package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;
import dev.dubhe.anvilcraft.util.TankUtil;
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
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.IFluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class FluidTankBlockEntity extends BlockEntity implements IFluidHandlerHolder {
    public static final int CAPACITY = 16;
    public static final int BIG_CAPACITY = 640;
    public static final int COOLDOWN = 200;
    protected FluidTank tank = new FluidTank(CAPACITY * FluidType.BUCKET_VOLUME);
    protected int cooldown = 0;


    public FluidTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public void serverTick() {
        if (--cooldown <= 0) {
            checkStructure();
            cooldown = COOLDOWN;
        }
        setChanged();
    }

    protected void checkStructure() {
//        int targetCapacity = TankUtil.isMengerStructure(this.getBlockPos(), 3) ? BIG_CAPACITY : CAPACITY;
//        if (tank.getCapacity() != targetCapacity) {
//            tank.setCapacity(targetCapacity);
//        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        CompoundTag tankNbt = tank.writeToNBT(provider, new CompoundTag());
        if (!tankNbt.isEmpty()) {
            tag.put("tank", tankNbt);
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        tank.readFromNBT(provider, tag.getCompound("tank"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
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
        checkStructure();
        return FluidUtil.interactWithFluidHandler(player, hand, tank);
    }

    public IFluidTank getTank() {
        return tank;
    }

    public IFluidHandler getFluidHandler() {
        return tank;
    }
}