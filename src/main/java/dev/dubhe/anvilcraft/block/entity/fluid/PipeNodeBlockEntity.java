package dev.dubhe.anvilcraft.block.entity.fluid;

import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;
import dev.dubhe.anvilcraft.block.fluid.PipeBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeNodeBlock;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

@Getter
public class PipeNodeBlockEntity extends AbstractPipeBlockEntity implements IFluidHandlerHolder {
    public static final int CAPACITY = FluidType.BUCKET_VOLUME * 4;
    private final FluidTank fluidHandler = new FluidTank(PipeNodeBlockEntity.CAPACITY) {
        @Override
        protected void onContentsChanged() {
            PipeNodeBlockEntity.this.setChanged();
            PipeNodeBlockEntity.this.sendUpdate();
            PipeNodeBlockEntity.this.sendNeighbourUpdate();
        }

        @Override
        public FluidTank readFromNBT(HolderLookup.Provider lookupProvider, CompoundTag nbt) {
            FluidTank tank = super.readFromNBT(lookupProvider, nbt);
            this.onContentsChanged();
            return tank;
        }
    };

    protected PipeNodeBlockEntity(
        BlockEntityType<PipeNodeBlockEntity> type,
        BlockPos pos,
        BlockState blockState
    ) {
        super(type, pos, blockState);
    }

    public static PipeNodeBlockEntity create(BlockEntityType<PipeNodeBlockEntity> type, BlockPos pos, BlockState blockState) {
        return new PipeNodeBlockEntity(type, pos, blockState);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag tankNbt = this.fluidHandler.writeToNBT(registries, new CompoundTag());
        if (!tankNbt.isEmpty()) {
            tag.put("Fluid", tankNbt);
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.fluidHandler.readFromNBT(registries, tag.getCompound("Fluid"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        CompoundTag tankNbt = this.fluidHandler.writeToNBT(registries, new CompoundTag());
        if (!tankNbt.isEmpty()) {
            tag.put("Fluid", tankNbt);
        }
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public static void tick(Level level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof PipeNodeBlock)) {
            return;
        }
        Set<EndAndDirection> pipeEnds = new TreeSet<>(Comparator.comparingInt(e -> -e.end().pos().getY()));
        for (Direction direction : Direction.values()) {
            EnumProperty<PipeBlock.NodePipe> property = PipeBlock.getPropertyForDirection(direction);
            PipeBlock.NodePipe value = state.getValue(property);
            if (value.equals(PipeBlock.NodePipe.END) && direction.equals(Direction.UP)) {
                AbstractPipeBlockEntity.moveFluidWithHeightCheck(
                    level,
                    pos,
                    Direction.UP,
                    pos.relative(Direction.UP),
                    Direction.DOWN
                );
            }
            if (value.equals(PipeBlock.NodePipe.END) && direction.equals(Direction.DOWN)) {
                AbstractPipeBlockEntity.moveFluidWithHeightCheck(
                    level,
                    pos.relative(Direction.DOWN),
                    Direction.UP,
                    pos,
                    Direction.DOWN
                );
            }
            if (!value.equals(PipeBlock.NodePipe.PIPE)) {
                continue;
            }
            PipeEnd pipeEnd = AbstractPipeBlockEntity.getPipeEnd(level, pos.relative(direction), direction.getOpposite());
            if (pipeEnd == null) continue;
            pipeEnds.add(new EndAndDirection(pipeEnd, direction));
        }
        if (pipeEnds.isEmpty()) {
            return;
        }
        for (EndAndDirection endAndDirection : pipeEnds) {
            AbstractPipeBlockEntity.moveFluidWithHeightCheck(
                level,
                pos.relative(endAndDirection.direction()),
                endAndDirection.direction().getOpposite(),
                endAndDirection.end().pos(),
                endAndDirection.end().direction()
            );
        }
    }

    record EndAndDirection(PipeEnd end, Direction direction) {
    }
}
