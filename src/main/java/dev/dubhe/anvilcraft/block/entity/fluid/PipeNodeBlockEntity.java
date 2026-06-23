package dev.dubhe.anvilcraft.block.entity.fluid;

import dev.dubhe.anvilcraft.api.fluid.IFluidHandlerHolder;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

/**
 * 管道节点的 BlockEntity，持有内部 {@link FluidTank} 并负责 per-tick 流体分发。
 *
 * <h3>流体存储</h3>
 * 内部 FluidTank 容量为 {@value #CAPACITY}（4 Bucket）。内容变化时自动发送
 * 客户端同步包和邻居更新。
 *
 * <h3>Per-tick 逻辑</h3>
 * <ol>
 *   <li>END + UP 方向：向上方排液</li>
 *   <li>END + DOWN 方向：向下方排液</li>
 *   <li>所有 PIPE 方向：收集 PipeEnd，按等效高度降序排序后逐一分发</li>
 * </ol>
 */
@Getter
public class PipeNodeBlockEntity extends AbstractPipeBlockEntity implements IFluidHandlerHolder {

    /**
     * 内部 FluidTank 容量：4 Bucket（4000 mB）
     */
    public static final int CAPACITY = FluidType.BUCKET_VOLUME * 4;

    /**
     * 节点内部流体储罐。内容变化时自动发送客户端更新和邻居更新。
     */
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

    protected PipeNodeBlockEntity(BlockEntityType<PipeNodeBlockEntity> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static PipeNodeBlockEntity create(BlockEntityType<PipeNodeBlockEntity> type, BlockPos pos, BlockState blockState) {
        return new PipeNodeBlockEntity(type, pos, blockState);
    }

    // ---- NBT 持久化（FluidTank + heightBonus） ----

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

    // ---- Per-tick 流体分发 ----

    /**
     * Per-tick 流体分发逻辑。使用等效高度排序 PipeEnd（高优先），
     * 再依次向各终点排液。
     */
    public static void tick(Level level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof PipeNodeBlock)) {
            return;
        }

        // 按等效高度降序排列 PipeEnd
        Set<EndAndDirection> pipeEnds = new TreeSet<>(Comparator.comparingInt(e -> -e.end().effectiveHeight()));

        for (Direction direction : Direction.values()) {
            EnumProperty<PipeBlock.NodePipe> property = PipeBlock.getPropertyForDirection(direction);
            PipeBlock.NodePipe value = state.getValue(property);

            // END + UP：向上方排液（若端头指向泵则透传追踪）
            if (value.equals(PipeBlock.NodePipe.END) && direction.equals(Direction.UP)) {
                BlockPos neighborPos = pos.relative(Direction.UP);
                if (level.getBlockState(neighborPos).getBlock() instanceof PumpBlock) {
                    PipeEnd pumpEnd = AbstractPipeBlockEntity.getPipeEnd(level, neighborPos, Direction.UP);
                    if (pumpEnd != null) {
                        AbstractPipeBlockEntity.moveFluidWithHeightCheck(
                            level,
                            pos,
                            Direction.UP,
                            pumpEnd.pos(),
                            pumpEnd.direction(),
                            pumpEnd.effectiveHeight()
                        );
                    }
                } else {
                    AbstractPipeBlockEntity.moveFluidWithHeightCheck(
                        level,
                        pos,
                        Direction.UP,
                        pos.relative(Direction.UP),
                        Direction.DOWN,
                        0
                    );
                }
            }
            // END + DOWN：向下方排液（若端头指向泵则透传追踪）
            if (value.equals(PipeBlock.NodePipe.END) && direction.equals(Direction.DOWN)) {
                BlockPos neighborPos = pos.relative(Direction.DOWN);
                if (level.getBlockState(neighborPos).getBlock() instanceof PumpBlock) {
                    PipeEnd pumpEnd = AbstractPipeBlockEntity.getPipeEnd(level, neighborPos, Direction.DOWN);
                    if (pumpEnd != null) {
                        AbstractPipeBlockEntity.moveFluidWithHeightCheck(
                            level,
                            pos.relative(Direction.DOWN),
                            Direction.UP,
                            pumpEnd.pos(),
                            pumpEnd.direction(),
                            pumpEnd.effectiveHeight()
                        );
                    }
                } else {
                    AbstractPipeBlockEntity.moveFluidWithHeightCheck(
                        level,
                        pos.relative(Direction.DOWN),
                        Direction.UP,
                        pos,
                        Direction.DOWN,
                        0
                    );
                }
            }

            if (!value.equals(PipeBlock.NodePipe.PIPE)) {
                continue;
            }

            // PIPE 方向：追踪 PipeEnd（沿途累加各段的 heightBonus）
            PipeEnd pipeEnd = AbstractPipeBlockEntity.getPipeEnd(level, pos.relative(direction), direction.getOpposite());
            if (pipeEnd == null) {
                continue;
            }
            pipeEnds.add(new EndAndDirection(pipeEnd, direction, pipeEnd.effectiveHeight()));
        }

        if (pipeEnds.isEmpty()) {
            return;
        }

        // 按等效高度降序分发流体
        for (EndAndDirection endAndDirection : pipeEnds) {
            AbstractPipeBlockEntity.moveFluidWithHeightCheck(
                level,
                pos.relative(endAndDirection.direction()),
                endAndDirection.direction().getOpposite(),
                endAndDirection.end().pos(),
                endAndDirection.end().direction(),
                endAndDirection.effectiveHeight()
            );
        }
    }

    /**
     * PipeEnd + 方向对
     */
    record EndAndDirection(PipeEnd end, Direction direction, int effectiveHeight) {
    }
}
