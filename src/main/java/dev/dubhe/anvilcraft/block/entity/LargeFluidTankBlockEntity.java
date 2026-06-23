package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;
import dev.dubhe.anvilcraft.api.fluid.InfinityFluidTank;
import dev.dubhe.anvilcraft.block.container.LargeFluidTankBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
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

public class LargeFluidTankBlockEntity extends BlockEntity implements IFluidHandlerHolder {
    public static final int CAPACITY = 320 * FluidType.BUCKET_VOLUME;
    public static final int BIG_CAPACITY = 12800 * FluidType.BUCKET_VOLUME;
    protected final InfinityFluidTank tank = new InfinityFluidTank(CAPACITY, false) {
        @Override
        protected void onContentsChanged(int index, FluidStack stack) {
            setChanged();
            LargeFluidTankBlockEntity.this.sendUpdate();
        }
    };
    protected boolean bigger = false;

    public LargeFluidTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public void tick() {
        BlockState state = getBlockState();
        if (!state.getValue(LargeFluidTankBlock.HALF).equals(Cube3x3PartHalf.MID_CENTER)) return;
        this.checkInfinity();
        setChanged();
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
        return FluidUtil.interactWithFluidHandler(player, hand, this.getBlockPos(), this.getFluidHandler());
    }

    public FluidStacksResourceHandler getTank() {
        return this.tank;
    }

    public ResourceHandler<FluidResource> getFluidHandler() {
        return this.tank;
    }
}
