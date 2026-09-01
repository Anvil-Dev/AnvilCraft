package dev.dubhe.anvilcraft.api.fluid.network;

import dev.dubhe.anvilcraft.api.entity.IEntityCauldron;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.CauldronFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/** 查找方块单元中由方块或实体提供的流体容器。 */
public final class FluidContainerLookup {
    /**
     * 水平碰撞判定允许实体偏移约 0.001 个方块距离，同时仍判定两个面相接触。
     */
    private static final double ENTITY_PIPE_CONTACT_TOLERANCE = 1.1E-3D;

    private FluidContainerLookup() {
    }

    /**
     * 查找占据 {@code pos} 的流体端点。方块能力优先于实体能力。
     * 实体归属于其碰撞箱中心所在的方块单元，避免移动实体同时从多个相邻位置暴露能力。
     */
    public static @Nullable Result find(Level level, BlockPos pos, @Nullable Direction side) {
        return find(level, pos, side, level.getBlockEntity(pos));
    }

    private static @Nullable Result find(
        Level level,
        BlockPos pos,
        @Nullable Direction side,
        @Nullable BlockEntity blockEntity
    ) {
        BlockState state = blockEntity == null ? level.getBlockState(pos) : blockEntity.getBlockState();
        IFluidHandler blockHandler = level.getCapability(
            Capabilities.FluidHandler.BLOCK,
            pos,
            state,
            blockEntity,
            side
        );
        if (blockHandler != null) {
            boolean cauldron = CauldronFluidContent.getForBlock(state.getBlock()) != null;
            return new Result(blockHandler, cauldron, null);
        }

        Entity selected = null;
        IFluidHandler selectedHandler = null;
        for (Entity entity : level.getEntitiesOfClass(
            Entity.class,
            new AABB(pos),
            candidate -> !candidate.isRemoved()
                && BlockPos.containing(candidate.getBoundingBox().getCenter()).equals(pos)
        )) {
            IFluidHandler handler = entity.getCapability(Capabilities.FluidHandler.ENTITY, side);
            if (handler == null || selected != null && selected.getId() <= entity.getId()) {
                continue;
            }
            selected = entity;
            selectedHandler = handler;
        }
        boolean cauldron = selected instanceof IEntityCauldron entityCauldron
            && entityCauldron.anvilcraft$usesWholeCauldronFluidTransfers();
        return selectedHandler == null ? null : new Result(selectedHandler, cauldron, selected);
    }

    /**
     * 查找任意方向暴露流体能力的容器。
     *
     * <p>部分外部模组只在特定方向提供能力，不能只用 {@code side == null}
     * 判断其是否为容器；加载登记时需要检查所有方向。</p>
     */
    public static @Nullable Result findAny(Level level, BlockPos pos) {
        return findAny(level, pos, level.getBlockEntity(pos));
    }

    /**
     * 使用指定的方块实体查找任意方向的流体能力。
     *
     * <p>方块实体加载事件在实体写入区块映射前触发，因此必须使用事件提供的实体，
     * 不能再次按位置查找。</p>
     */
    public static @Nullable Result findAny(
        Level level,
        BlockPos pos,
        @Nullable BlockEntity blockEntity
    ) {
        Result result = find(level, pos, null, blockEntity);
        if (result != null) {
            return result;
        }
        for (Direction side : Direction.values()) {
            result = find(level, pos, side, blockEntity);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    /** 返回实体端点是否仍与相邻管道部件实际接触。 */
    public static boolean isEntityConnectedToPipe(
        Level level, BlockPos containerPos, Direction sideToPipe, Entity entity
    ) {
        if (entity.isRemoved()
            || !BlockPos.containing(entity.getBoundingBox().getCenter()).equals(containerPos)
            || entity.getCapability(Capabilities.FluidHandler.ENTITY, sideToPipe) == null) {
            return false;
        }

        BlockPos pipePos = containerPos.relative(sideToPipe);
        if (!level.isLoaded(pipePos)) {
            return false;
        }
        Vec3 towardPipe = Vec3.atLowerCornerOf(sideToPipe.getNormal()).scale(ENTITY_PIPE_CONTACT_TOLERANCE);
        AABB contactBox = entity.getBoundingBox().expandTowards(towardPipe);
        return level.getBlockState(pipePos).getCollisionShape(level, pipePos).toAabbs().stream()
            .map(box -> box.move(pipePos.getX(), pipePos.getY(), pipePos.getZ()))
            .anyMatch(contactBox::intersects);
    }

    /** 在单个网络端点发现的流体处理器及其转移语义。 */
    public record Result(IFluidHandler handler, boolean cauldron, @Nullable Entity entity) {
    }
}
