package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.fluid.IFluidResourceHandlerHolder;
import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkManager;
import dev.dubhe.anvilcraft.block.container.LargeFluidTankBlock;
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
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Redstone;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.List;

public class LargeFluidTankBlockEntity extends BlockEntity implements IFluidResourceHandlerHolder {
    public static final int BASE_CAPACITY = 320 * FluidType.BUCKET_VOLUME;
    public static final int INFINITY_THRESHOLD = 12800 * FluidType.BUCKET_VOLUME;
    private static final int CHECK_INTERVAL = 100;

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
        if (!this.isMainPart()) return;
        if (++this.tickCounter % CHECK_INTERVAL == 0 && this.level != null && !this.level.isClientSide()) {
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
        if (this.level == null) return;
        AuxiliaryLightManager manager = this.level.getAuxLightManager(this.getBlockPos());
        if (manager != null) manager.setLightAt(this.getBlockPos(), this.computeLightLevel());
    }

    private int computeLightLevel() {
        int lightLevel = 0;
        for (FluidStack stack : this.tank.copyFluids()) {
            lightLevel = Math.max(lightLevel, stack.getFluidType().getLightLevel(stack));
        }
        long renderCapacity = this.tank.isEnhanced() ? INFINITY_THRESHOLD : BASE_CAPACITY;
        double fill = Math.min(1.0, (double) this.tank.getTotalAmount() / renderCapacity);
        return (int) Math.ceil(lightLevel * fill);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.tank.serialize(output.child("Tank"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.tank.deserialize(input.childOrEmpty("Tank"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        this.tank.serialize(output);
        tag.put("Tank", output.buildResult());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public boolean onPlayerUse(Player player, InteractionHand hand) {
        try (Transaction transaction = Transaction.openRoot()) {
            boolean success = FluidUtil.interactWithFluidHandler(player, hand, this.getBlockPos(), this.getFluidHandler(), transaction);
            if (success) transaction.commit();
            return success;
        }
    }

    public int getRedstoneSignal() {
        MultiFluidTankHandler mainTank = this.getMainPart().tank;
        long amount = mainTank.getTotalAmount();
        int capacity = mainTank.isEnhanced() ? INFINITY_THRESHOLD : BASE_CAPACITY;
        int strength = amount == 0
            ? 0
            : (int) (Math.min(amount, capacity) * (Redstone.SIGNAL_MAX - 1) / capacity) + 1;
        return Mth.clamp(strength, Redstone.SIGNAL_MIN, Redstone.SIGNAL_MAX);
    }

    @Override
    public ResourceHandler<FluidResource> getFluidHandler() {
        return this.getMainPart().tank;
    }

    public boolean isMainPart() {
        return ModBlocks.LARGE_FLUID_TANK.get().isMainPart(this.getBlockState());
    }

    private LargeFluidTankBlockEntity getMainPart() {
        LargeFluidTankBlock block = ModBlocks.LARGE_FLUID_TANK.get();
        BlockPos mainPartPos = block.getMainPartPos(this.getBlockPos(), this.getBlockState());
        if (this.level == null) return this;
        BlockEntity mainPart = this.level.getBlockEntity(mainPartPos);
        return mainPart instanceof LargeFluidTankBlockEntity mainTank ? mainTank : this;
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
