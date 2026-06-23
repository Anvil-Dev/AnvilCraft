package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.fluid.CapacityModifiableFluidHandler;
import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;
import lombok.Getter;
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
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

public class FluidTankBlockEntity extends BlockEntity implements IFluidHandlerHolder {
    public static final int CAPACITY = 16 * FluidType.BUCKET_VOLUME;
    public static final int BIG_CAPACITY = 640 * FluidType.BUCKET_VOLUME;
    @Getter
    protected final CapacityModifiableFluidHandler tank = new CapacityModifiableFluidHandler(1, FluidTankBlockEntity.CAPACITY) {
        @Override
        protected void onContentsChanged(int index, FluidStack stack) {
            setChanged();
            FluidTankBlockEntity.this.sendUpdate();
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

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("bigger", this.isBigger);
        this.tank.serialize(output.child("tank"));
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.tank.deserialize(input.childOrEmpty("tank"));
        this.isBigger = input.getBooleanOr("bigger", false);
        if (this.isBigger) {
            this.onFormed();
        } else {
            this.onUnformed();
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("bigger", this.isBigger);
        TagValueOutput valueOutput = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        this.tank.serialize(valueOutput);
        tag.store("tank", CompoundTag.CODEC, valueOutput.buildResult());
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
        return FluidUtil.interactWithFluidHandler(player, hand, worldPosition, this.tank);
    }

    @Override
    public ResourceHandler<FluidResource> getFluidHandler() {
        return this.tank;
    }
}
