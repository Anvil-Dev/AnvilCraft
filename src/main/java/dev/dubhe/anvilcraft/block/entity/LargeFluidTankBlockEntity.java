package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.fluid.IFluidResourceHandlerHolder;
import dev.dubhe.anvilcraft.api.fluid.InfinityFluidTank;
import dev.dubhe.anvilcraft.api.fluid.network.FluidNetworkManager;
import dev.dubhe.anvilcraft.block.container.LargeFluidTankBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.util.TankUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;

public class LargeFluidTankBlockEntity extends BlockEntity implements IFluidResourceHandlerHolder {
    public static final int CAPACITY = 320 * FluidType.BUCKET_VOLUME;
    public static final int BIG_CAPACITY = 12800 * FluidType.BUCKET_VOLUME;
    private static final int CHECK_INTERVAL = 100;
    protected final InfinityFluidTank tank = new InfinityFluidTank(CAPACITY, false) {
        @Override
        protected void onContentsChanged(int index, FluidStack stack) {
            setChanged();
            LargeFluidTankBlockEntity.this.sendUpdate();
        }
    };
    protected boolean bigger = false;
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

    public void tick() {
        BlockState state = getBlockState();
        if (!state.getValue(LargeFluidTankBlock.HALF).equals(Cube3x3PartHalf.MID_CENTER)) return;
        this.checkInfinity();
        setChanged();
        if (++this.tickCounter % CHECK_INTERVAL == 0 && this.level != null && !this.level.isClientSide()) {
            boolean valid = TankUtil.isMengerStructure(this.level, this.getBlockPos(), 9);
            if (this.bigger && !valid) {
                this.onUnformed();
            } else if (!this.bigger && valid) {
                this.onFormed();
            }
        }
    }

    protected void checkInfinity() {
        FluidResource resource = this.tank.getResource(0);
        int capacity = this.tank.getCapacityAsInt(0, resource);
        if (capacity != LargeFluidTankBlockEntity.BIG_CAPACITY) return;
        if (capacity - this.tank.getAmountAsInt(0) > 0) return;
        this.tank.setInfinity(true);
    }

    public void onFormed() {
        this.bigger = true;
        this.tank.setCapacity(BIG_CAPACITY);
    }

    public void onUnformed() {
        this.bigger = false;
        this.tank.setInfinity(false);
        this.tank.setCapacity(CAPACITY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("bigger", this.bigger);
        this.tank.serialize(output.child("tank"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.tank.deserialize(input.childOrEmpty("tank"));
        this.bigger = input.getBooleanOr("bigger", false);
        if (this.bigger) {
            this.onFormed();
        } else {
            this.onUnformed();
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("bigger", this.bigger);
        TagValueOutput fluidTag = TagValueOutput.createWithContext(new ProblemReporter.Collector(this.problemPath()), registries);
        this.tank.serialize(fluidTag);
        tag.put("tank", fluidTag.buildResult());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void sendUpdate() {
        if (this.level == null) return;
        this.level.sendBlockUpdated(
            this.getBlockPos(),
            this.getBlockState(),
            this.getBlockState(),
            Block.UPDATE_CLIENTS
        );
    }

    public boolean onPlayerUse(Player player, InteractionHand hand) {
        this.checkInfinity();
        try (Transaction transaction = Transaction.openRoot()) {
            boolean success = FluidUtil.interactWithFluidHandler(player, hand, this.getBlockPos(), this.tank, transaction);
            if (success) transaction.commit();
            return success;
        }
    }

    public FluidStacksResourceHandler getTank() {
        return this.tank;
    }

    public ResourceHandler<FluidResource> getFluidHandler() {
        return this.tank;
    }
}
