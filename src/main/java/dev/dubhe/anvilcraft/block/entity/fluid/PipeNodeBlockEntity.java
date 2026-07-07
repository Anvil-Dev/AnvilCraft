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
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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

        List<Exit> exits = new ArrayList<>();
        // 节点内部储罐作为节点自身高度处的一个出口
        exits.add(new Exit(pos, null, pos.getY()));

        for (Direction direction : Direction.values()) {
            PipeBlock.NodePipe value = state.getValue(PipeBlock.getPropertyForDirection(direction));
            Exit exit = resolveExit(level, pos, direction, value);
            if (exit != null) {
                exits.add(exit);
            }
        }

        if (exits.size() < 2) return;

        // 按等效高度降序：高处出口优先作为源，向低处出口排液
        exits.sort(Comparator.comparingInt((Exit e) -> e.effectiveHeight()).reversed());
        for (int i = 0; i < exits.size(); i++) {
            Exit source = exits.get(i);
            for (int j = i + 1; j < exits.size(); j++) {
                Exit target = exits.get(j);
                if (source.effectiveHeight() <= target.effectiveHeight()) {
                    continue;
                }
                AbstractPipeBlockEntity.moveFluidByEffectiveHeight(
                    level,
                    source.pos(),
                    source.direction(),
                    source.effectiveHeight(),
                    target.pos(),
                    target.direction(),
                    target.effectiveHeight()
                );
            }
        }
    }

    /**
     * 解析某方向出口对应的目标容器位置、朝向和等效高度。
     *
     * @return 该方向的出口；该方向无连接或不可达时返回 {@code null}
     */
    private static @Nullable Exit resolveExit(Level level, BlockPos pos, Direction direction, PipeBlock.NodePipe value) {
        if (value == PipeBlock.NodePipe.PIPE) {
            // 沿管道追踪至端点容器
            PipeEnd pipeEnd = AbstractPipeBlockEntity.getPipeEnd(level, pos.relative(direction), direction.getOpposite());
            if (pipeEnd == null) {
                return null;
            }
            BlockPos containerPos = pipeEnd.pos().relative(pipeEnd.direction());
            return new Exit(pipeEnd.pos(), pipeEnd.direction(), containerPos.getY() - pipeEnd.effectiveHeight());
        }
        if (value == PipeBlock.NodePipe.END) {
            BlockPos neighborPos = pos.relative(direction);
            // 端头指向泵：经泵追踪至其输出端容器
            if (level.getBlockState(neighborPos).getBlock() instanceof PumpBlock) {
                PipeEnd pumpEnd = AbstractPipeBlockEntity.getPipeEnd(level, neighborPos, direction);
                if (pumpEnd == null) {
                    return null;
                }
                BlockPos containerPos = pumpEnd.pos().relative(pumpEnd.direction());
                return new Exit(pumpEnd.pos(), pumpEnd.direction(), containerPos.getY() - pumpEnd.effectiveHeight());
            }
            // 端头直连流体容器
            return new Exit(pos, direction, neighborPos.getY());
        }
        return null;
    }

    /**
     * 节点的一个出口。
     *
     * @param pos             用于 {@link AbstractPipeBlockEntity#moveFluidByEffectiveHeight} 的当前位置
     *                        （目标容器位于 {@code pos.relative(direction)}）
     * @param direction       出口朝向；{@code null} 表示节点自身内部储罐
     * @param effectiveHeight 该出口目标容器的等效高度（已计入泵扬程修正）
     */
    record Exit(BlockPos pos, @Nullable Direction direction, int effectiveHeight) {}
}
