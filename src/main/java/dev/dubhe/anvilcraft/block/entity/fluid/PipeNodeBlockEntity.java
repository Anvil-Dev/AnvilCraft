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
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 管道节点的 BlockEntity，持有内部 {@link FluidTank} 并负责 per-tick 流体分发。
 *
 * <h3>流体存储</h3>
 * 内部 FluidTank 容量为 {@value #CAPACITY}（4 Bucket）。内容变化时自动发送
 * 客户端同步包和邻居更新。
 *
 * <h3>Per-tick 逻辑</h3>
 * 检测六个方向的出口，沿管道/泵追踪到各自的实际目标容器及其等效高度，
 * 再将节点内部储罐一并视为节点自身高度处的一个出口，统一按高度从高到低
 * 在各出口之间搬运流体（高处出口流向低处出口，流速由高度差决定）。
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
     * Per-tick 流体分发逻辑。
     *
     * <p>检测六个方向的出口（PIPE 沿管追踪、END 直连容器/经泵追踪），解析出每个出口
     * 对应的实际目标容器及其等效高度；再把节点内部储罐作为节点自身高度处的一个出口，
     * 统一收集后按等效高度从高到低，在每一对「高 → 低」出口之间搬运流体。
     */
    public static void tick(Level level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof PipeNodeBlock)) {
            return;
        }

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

        if (exits.size() < 2) {
            return;
        }

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
    record Exit(BlockPos pos, @Nullable Direction direction, int effectiveHeight) {
    }
}
