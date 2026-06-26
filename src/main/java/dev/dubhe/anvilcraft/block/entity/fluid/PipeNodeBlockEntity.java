package dev.dubhe.anvilcraft.block.entity.fluid;

import dev.dubhe.anvilcraft.api.fluid.CapacityModifiableFluidHandler;
import dev.dubhe.anvilcraft.api.fluid.IFluidResourceHandlerHolder;
import dev.dubhe.anvilcraft.block.fluid.PipeBlock;
import dev.dubhe.anvilcraft.block.fluid.PipeNodeBlock;
import dev.dubhe.anvilcraft.block.fluid.PumpBlock;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

/**
 * 管道节点的 BlockEntity，持有内部 FluidTank（4 Bucket）并负责 per-tick 流体分发。
 * 按等效高度降序分发到各 PipeEnd。
 *
 * <p>26.1 版本使用 {@link CapacityModifiableFluidHandler} 替代旧的 FluidTank。
 */
@Getter
public class PipeNodeBlockEntity extends AbstractPipeBlockEntity implements IFluidResourceHandlerHolder {

    public static final int CAPACITY = FluidType.BUCKET_VOLUME * 4;

    private final CapacityModifiableFluidHandler fluidHandler = new CapacityModifiableFluidHandler(1, PipeNodeBlockEntity.CAPACITY) {
        @Override
        protected void onContentsChanged(int index, net.neoforged.neoforge.fluids.FluidStack stack) {
            PipeNodeBlockEntity.this.setChanged();
            PipeNodeBlockEntity.this.sendUpdate();
            PipeNodeBlockEntity.this.sendNeighbourUpdate();
        }
    };

    protected PipeNodeBlockEntity(BlockEntityType<PipeNodeBlockEntity> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static PipeNodeBlockEntity create(BlockEntityType<PipeNodeBlockEntity> type, BlockPos pos, BlockState blockState) {
        return new PipeNodeBlockEntity(type, pos, blockState);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        this.fluidHandler.serialize(output);
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.fluidHandler.deserialize(input);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        TagValueOutput valueOutput = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        this.fluidHandler.serialize(valueOutput);
        tag.store("Fluid", CompoundTag.CODEC, valueOutput.buildResult());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public ResourceHandler<FluidResource> getFluidHandler() {
        return this.fluidHandler;
    }

    public static void tick(Level level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof PipeNodeBlock)) return;
        Set<EndAndDirection> pipeEnds = new TreeSet<>(Comparator.comparingInt(e -> -e.effectiveHeight()));

        for (Direction direction : Direction.values()) {
            EnumProperty<PipeBlock.NodePipe> property = PipeBlock.getPropertyForDirection(direction);
            PipeBlock.NodePipe value = state.getValue(property);

            if (value.equals(PipeBlock.NodePipe.END) && direction == Direction.UP) {
                BlockPos neighborPos = pos.relative(Direction.UP);
                if (level.getBlockState(neighborPos).getBlock() instanceof PumpBlock) {
                    PipeEnd pumpEnd = AbstractPipeBlockEntity.getPipeEnd(level, neighborPos, Direction.UP);
                    if (pumpEnd != null) {
                        AbstractPipeBlockEntity.moveFluidWithHeightCheck(
                            level, pos, Direction.UP, pumpEnd.pos(), pumpEnd.direction(), pumpEnd.effectiveHeight());
                    }
                } else {
                    AbstractPipeBlockEntity.moveFluidWithHeightCheck(
                        level, pos, Direction.UP, pos.relative(Direction.UP), Direction.DOWN, 0);
                }
            }
            if (value.equals(PipeBlock.NodePipe.END) && direction == Direction.DOWN) {
                BlockPos neighborPos = pos.relative(Direction.DOWN);
                if (level.getBlockState(neighborPos).getBlock() instanceof PumpBlock) {
                    PipeEnd pumpEnd = AbstractPipeBlockEntity.getPipeEnd(level, neighborPos, Direction.DOWN);
                    if (pumpEnd != null) {
                        AbstractPipeBlockEntity.moveFluidWithHeightCheck(
                            level, pos.relative(Direction.DOWN),
                            Direction.UP, pumpEnd.pos(), pumpEnd.direction(), pumpEnd.effectiveHeight());
                    }
                } else {
                    AbstractPipeBlockEntity.moveFluidWithHeightCheck(
                        level, pos.relative(Direction.DOWN), Direction.UP, pos, Direction.DOWN, 0);
                }
            }

            if (!value.equals(PipeBlock.NodePipe.PIPE)) continue;
            PipeEnd pipeEnd = AbstractPipeBlockEntity.getPipeEnd(level, pos.relative(direction), direction.getOpposite());
            if (pipeEnd == null) continue;
            pipeEnds.add(new EndAndDirection(pipeEnd, direction, pipeEnd.effectiveHeight()));
        }

        if (pipeEnds.isEmpty()) return;
        for (EndAndDirection endAndDirection : pipeEnds) {
            AbstractPipeBlockEntity.moveFluidWithHeightCheck(
                level, pos.relative(endAndDirection.direction()),
                endAndDirection.direction().getOpposite(),
                endAndDirection.end().pos(), endAndDirection.end().direction(),
                endAndDirection.effectiveHeight());
        }
    }

    record EndAndDirection(PipeEnd end, Direction direction, int effectiveHeight) {}
}
