package dev.dubhe.anvilcraft.block.sliding;

import dev.dubhe.anvilcraft.api.injection.block.IBlockExtension;
import dev.dubhe.anvilcraft.entity.SlidingBlockEntity;
import dev.dubhe.anvilcraft.api.sliding.SlidingBlockStructureResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Triple;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;

public interface ISlidingRail extends IBlockExtension {
    Map<BlockPos, PistonPushInfo> MOVING_PISTON_MAP = new HashMap<>();

    /**
     * 当滑动方块经过时每tick调用该方法。
     *
     * @param level  滑轨所处的世界
     * @param pos    滑轨方块位置
     * @param state  滑轨方块状态
     * @param entity 滑动方块实体
     */
    void onSlidingAbove(Level level, BlockPos pos, BlockState state, SlidingBlockEntity entity);

    Block self();

    /**
     * 当滑轨站尝试移动顶部方块到该滑轨顶部时调用该方法。<br>
     * 将在{@link Block#neighborChanged(BlockState, Level, BlockPos, Block, BlockPos, boolean) neighbourChanged()}调用。
     *
     * @param level 滑轨站所处的世界
     * @param pos   滑轨方块位置
     * @param state 滑轨方块状态
     * @param top   滑轨站顶部的方块状态
     * @param side  滑轨站相对于滑轨的方向
     *
     * @return 将要滑动的方向。若为空，则不滑动。
     */
    @SuppressWarnings("JavadocReference")
    default boolean canMoveBlockToTop(LevelReader level, BlockPos pos, BlockState state, BlockState top, Direction side) {
        return false;
    }

    static void whenOnNeighborChange(LevelReader level, BlockPos pos, BlockPos neighbor) {
        if (!level.getBlockState(neighbor).is(Blocks.MOVING_PISTON)) return;
        Direction dir = level.getBlockState(neighbor).getValue(FACING);
        if (dir.getAxis() == Direction.Axis.Y || !neighbor.equals(pos.above())) {
            MOVING_PISTON_MAP.remove(pos);
            return;
        }
        PistonPushInfo ppi = new PistonPushInfo(neighbor, dir);
        if (MOVING_PISTON_MAP.containsKey(pos)) {
            MOVING_PISTON_MAP.get(pos).fromPos = neighbor;
        } else MOVING_PISTON_MAP.put(pos, ppi);
    }

    static void whenNeighborChanged(Level level, Block block, BlockPos pos, BlockPos fromPos) {
        if (level.isClientSide) return;
        BlockState blockState = level.getBlockState(fromPos);
        if (!MOVING_PISTON_MAP.containsKey(pos)) return;
        if (blockState.is(Blocks.MOVING_PISTON)) return;
        level.scheduleTick(pos, block, 2);
    }

    static void whenTick(ServerLevel level, Block block, BlockPos pos) {
        if (!MOVING_PISTON_MAP.containsKey(pos)) return;
        if (!MOVING_PISTON_MAP.get(pos).extending && MOVING_PISTON_MAP.get(pos).isSourcePiston) {
            MOVING_PISTON_MAP.remove(pos);
            return;
        } else if (!MOVING_PISTON_MAP.get(pos).extending) {
            MOVING_PISTON_MAP.get(pos).direction = MOVING_PISTON_MAP.get(pos).direction.getOpposite();
        }
        level.blockEvent(pos, block, 0, MOVING_PISTON_MAP.get(pos).direction.get3DDataValue());
        MOVING_PISTON_MAP.remove(pos);
    }

    static boolean whenTriggerEvent(Level level, BlockPos pos, int param) {
        Direction direction = Direction.from3DDataValue(param);
        return moveBlocks(level, pos.above(), direction);
    }

    static boolean moveBlocks(Level level, BlockPos pos, Direction facing) {
        SlidingBlockStructureResolver resolver = new SlidingBlockStructureResolver(level, pos, facing, true);
        if (!resolver.resolve()) return false;
        List<Triple<BlockPos, BlockState, Optional<CompoundTag>>> toPushes = new ArrayList<>();
        List<BlockPos> toPushPoses = resolver.getToPush();

        for (BlockPos toPushPos : toPushPoses) {
            if (toPushPos.equals(pos.below())) return false;
            BlockState toPushState = level.getBlockState(toPushPos);
            if (toPushState.hasProperty(BlockStateProperties.WATERLOGGED)) {
                toPushState = toPushState.setValue(BlockStateProperties.WATERLOGGED, false);
            }
            Optional<CompoundTag> toPushEntityData = Optional.ofNullable(level.getBlockEntity(toPushPos))
                .map(entity -> entity.saveCustomOnly(level.registryAccess()));
            toPushes.add(Triple.of(toPushPos, toPushState, toPushEntityData));
        }

        List<BlockPos> toDestroys = resolver.getToDestroy();

        for (int i = toDestroys.size() - 1; i >= 0; i--) {
            BlockPos destroyingPos = toDestroys.get(i);
            BlockState destroyingState = level.getBlockState(destroyingPos);
            BlockEntity destroyingEntity = destroyingState.hasBlockEntity() ? level.getBlockEntity(destroyingPos) : null;
            Block.dropResources(destroyingState, level, destroyingPos, destroyingEntity);
            destroyingState.onDestroyedByPushReaction(level, destroyingPos, facing, level.getFluidState(destroyingPos));
        }

        BlockState air = Blocks.AIR.defaultBlockState();

        for (BlockPos toPushPos : toPushPoses) {
            level.setBlock(toPushPos, air, 0b1010010);
        }

        for (var toPushEntry : toPushes) {
            BlockPos toPushPos = toPushEntry.getLeft();
            BlockState toPushState = toPushEntry.getMiddle();
            toPushState.updateIndirectNeighbourShapes(level, toPushPos, 0b0000010);
            air.updateNeighbourShapes(level, toPushPos, 0b0000010);
            air.updateIndirectNeighbourShapes(level, toPushPos, 0b0000010);
        }

        for (var toPushEntry : toPushes) {
            level.updateNeighborsAt(toPushEntry.getLeft(), air.getBlock());
        }

        SlidingBlockEntity.slid(level, pos, facing, toPushes);
        return true;
    }

    static void stopSlidingBlock(SlidingBlockEntity entity) {
        entity.stop();
        MOVING_PISTON_MAP.remove(entity.blockPosition());
    }

    static void absorbEntity(BlockPos pos, Entity entity) {
        Vec3 blockPos = pos.getCenter();
        Vec3 entityPos = entity.position();
        Vector3f acceleration = blockPos.toVector3f()
            .sub(entityPos.toVector3f())
            .mul(0.45f)
            .div(0.98f)
            .mul(new Vector3f(1, 0, 1));
        entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.5f, 0.5f, 0.5f).add(new Vec3(acceleration)));
    }

    class PistonPushInfo {
        public BlockPos fromPos;
        public Direction direction;
        public boolean extending;
        public boolean isSourcePiston;

        public PistonPushInfo(BlockPos blockPos, Direction direction) {
            this.fromPos = blockPos;
            this.direction = direction;
            this.extending = false;
            this.isSourcePiston = false;
        }
    }
}
