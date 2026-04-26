package dev.dubhe.anvilcraft.event;

import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.sliding.SlidingBlockStructureResolver;
import dev.dubhe.anvilcraft.block.sliding.ISlidingRail;
import dev.dubhe.anvilcraft.block.sliding.PoweredSlidingRailBlock;
import dev.dubhe.anvilcraft.entity.SlidingBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class NeighbourNotifyListener {
    // 类滑轨站方块上方的方块受到更新时检测是否有动力滑轨吸它
    @SubscribeEvent
    public static void onNeighbourNotify(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getLevel() instanceof Level level)) return;
        BlockPos pos = event.getPos();
        if (level.isEmptyBlock(pos)) return;
        BlockPos belowPos = pos.below();
        if (level.isEmptyBlock(belowPos)) return;
        BlockState state = level.getBlockState(pos);
        if (!PistonBaseBlock.isPushable(state, level, pos, null, true, null)) return;
        BlockState belowState = level.getBlockState(belowPos);
        if (belowState.is(ModBlockTags.SLIDING_RAIL_STOP_LIKE)) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockState railState = level.getBlockState(belowPos.relative(direction));
                if (railState.is(ModBlocks.POWERED_SLIDING_RAIL)
                    && railState.getValue(PoweredSlidingRailBlock.FACING) == direction
                    && railState.getValue(PoweredSlidingRailBlock.POWERED)) {
                    if (canMoveBlockTo(level, belowPos, state, direction)) {
                        moveBlocksAbove(level, belowPos, direction);
                    }
                }
            }
        }

    }

    public static boolean canMoveBlockTo(Level level, BlockPos pos, BlockState state, Direction moveTo) {
        BlockPos railPos = pos.relative(moveTo);
        BlockState railState = level.getBlockState(railPos);
        return Util.castSafely(railState.getBlock(), ISlidingRail.class)
            .map(rail -> rail.canMoveBlockToTop(level, railPos, railState, state, moveTo.getOpposite()))
            .orElse(false);
    }

    public static void moveBlocksAbove(Level level, BlockPos pos, Direction moveToSide) {
        SlidingBlockStructureResolver resolver = new SlidingBlockStructureResolver(level, pos.above(), moveToSide, true);
        if (!resolver.resolve()) return;
        List<Triple<BlockPos, BlockState, Optional<CompoundTag>>> toPushes = new ArrayList<>();
        List<BlockPos> toPushPoses = new ArrayList<>(resolver.getToPush());

        for (Iterator<BlockPos> iterator = toPushPoses.iterator(); iterator.hasNext(); ) {
            BlockPos toPushPos = iterator.next();
            if (toPushPos.equals(pos)) {
                iterator.remove();
                continue;
            }
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
            destroyingState.onDestroyedByPushReaction(level, destroyingPos, moveToSide, level.getFluidState(destroyingPos));
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

        SlidingBlockEntity.slid(level, pos.above(), moveToSide, toPushes);
    }
}
