package dev.dubhe.anvilcraft.api.fluid.network;

import dev.dubhe.anvilcraft.api.entity.IEntityCauldron;
import dev.dubhe.anvilcraft.entity.FluidTankMinecartEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
     * 储罐矿车按碰撞箱覆盖的单元接入，重复入口由管网按处理器合并。
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
        List<Result> results = findAll(level, pos, side, blockEntity);
        return results.isEmpty() ? null : results.getFirst();
    }

    /** 返回前方单元内全部实体容器，供管网保留多辆储罐矿车。 */
    public static List<Result> findAll(Level level, BlockPos pos, @Nullable Direction side) {
        return findAll(level, pos, side, level.getBlockEntity(pos));
    }

    private static List<Result> findAll(
        Level level, BlockPos pos, @Nullable Direction side, @Nullable BlockEntity blockEntity
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
            boolean cauldron = state.is(ModBlocks.FIRE_CAULDRON) || state.is(ModBlocks.MILK_CAULDRON)
                || CauldronFluidContent.getForBlock(state.getBlock()) != null;
            return List.of(new Result(blockHandler, cauldron, null));
        }

        List<Result> results = new ArrayList<>();
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, new AABB(pos), candidate -> !candidate.isRemoved()
            && (candidate instanceof FluidTankMinecartEntity || BlockPos.containing(candidate.getBoundingBox().getCenter()).equals(pos)));
        entities.sort(Comparator.comparingInt(Entity::getId));
        for (Entity entity : entities) {
            IFluidHandler handler = entity.getCapability(Capabilities.FluidHandler.ENTITY, side);
            if (handler == null) {
                continue;
            }
            boolean cauldron = entity instanceof IEntityCauldron entityCauldron
                && entityCauldron.anvilcraft$usesWholeCauldronFluidTransfers();
            results.add(new Result(handler, cauldron, entity));
        }
        return results;
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
     * <p>复用调用方已取得的方块实体，避免重复按位置查找。</p>
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

    /** 储罐矿车在管道前方一格内即可接入，其他实体保留实际接触判定。 */
    public static boolean isEntityConnectedToPipe(
        Level level, BlockPos containerPos, Direction sideToPipe, Entity entity
    ) {
        if (entity.isRemoved()
            || entity.getCapability(Capabilities.FluidHandler.ENTITY, sideToPipe) == null) {
            return false;
        }

        BlockPos pipePos = containerPos.relative(sideToPipe);
        if (!level.isLoaded(pipePos) || !FluidNetworkScanner.isPipePart(level.getBlockState(pipePos))) {
            return false;
        }
        if (entity instanceof FluidTankMinecartEntity) {
            return entity.getBoundingBox().intersects(new AABB(containerPos));
        }
        if (!BlockPos.containing(entity.getBoundingBox().getCenter()).equals(containerPos)) {
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
