package dev.dubhe.anvilcraft.api.laser;

import dev.dubhe.anvilcraft.block.entity.BaseLaserBlockEntity;
import dev.dubhe.anvilcraft.block.entity.RubyPrismBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlockTags;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.SortedArraySet;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;

public class LevelLaserBlockManager {
    private final Level level;
    private final HashMap<BlockPos, BaseLaserBlockEntity> laserBlockEntityMap = new HashMap<>();
    private final HashMap<Integer, SortedArraySet<Integer>> laserBlockEntityXposMap = new HashMap<>();
    private final HashMap<Integer, SortedArraySet<Integer>> laserBlockEntityYposMap = new HashMap<>();
    private final HashMap<Integer, SortedArraySet<Integer>> laserBlockEntityZposMap = new HashMap<>();
    private final HashMap<BlockPos, BlockPos> linkLaserBlockEntityMap = new HashMap<>();

    public LevelLaserBlockManager(Level level) {
        this.level = level;
    }

    private void addBlockPos(BlockPos blockPos) {
        int yposAndZposHash = Objects.hash(blockPos.getY(), blockPos.getZ());
        int xposAndZposHash = Objects.hash(blockPos.getX(), blockPos.getZ());
        int xposAndYposHash = Objects.hash(blockPos.getX(), blockPos.getY());
        SortedArraySet<Integer> xposSortedArraySet =
            laserBlockEntityXposMap.getOrDefault(yposAndZposHash, SortedArraySet.create());
        SortedArraySet<Integer> yposSortedArraySet =
            laserBlockEntityYposMap.getOrDefault(xposAndZposHash, SortedArraySet.create());
        SortedArraySet<Integer> zposSortedArraySet =
            laserBlockEntityZposMap.getOrDefault(xposAndYposHash, SortedArraySet.create());
        xposSortedArraySet.add(blockPos.getX());
        yposSortedArraySet.add(blockPos.getY());
        zposSortedArraySet.add(blockPos.getZ());
        laserBlockEntityXposMap.put(yposAndZposHash, xposSortedArraySet);
        laserBlockEntityYposMap.put(xposAndZposHash, yposSortedArraySet);
        laserBlockEntityZposMap.put(xposAndYposHash, zposSortedArraySet);
    }

    private HashMap<Direction, BlockPos> getNeighbourSameXposBlock(BlockPos blockPos) {
        int yposAndZposHash = Objects.hash(blockPos.getY(), blockPos.getZ());
        SortedArraySet<Integer> xposSortedArraySet =
            laserBlockEntityXposMap.getOrDefault(yposAndZposHash, SortedArraySet.create());
        int index = xposSortedArraySet.findIndex(blockPos.getX());
        if (index < 0) return null;
        HashMap<Direction, BlockPos> blockPosMap = new HashMap<>();
        int x = xposSortedArraySet.getInternal(index);
        if (x == blockPos.getX()) {
            if (index != xposSortedArraySet.size() - 1)
                blockPosMap.put(Direction.WEST,
                    new BlockPos(xposSortedArraySet.getInternal(index + 1),
                        blockPos.getY(), blockPos.getZ()));
            if (index != 0)
                blockPosMap.put(Direction.EAST,
                    new BlockPos(xposSortedArraySet.getInternal(index - 1),
                        blockPos.getY(), blockPos.getZ()));
        } else {
            blockPosMap.put(Direction.EAST,
                new BlockPos(xposSortedArraySet.getInternal(index),
                    blockPos.getY(), blockPos.getZ()));
            if (index != xposSortedArraySet.size() - 1)
                blockPosMap.put(Direction.WEST,
                    new BlockPos(xposSortedArraySet.getInternal(index + 1),
                        blockPos.getY(), blockPos.getZ()));
        }
        return blockPosMap;
    }

    private HashMap<Direction, BlockPos> getNeighbourSameYposBlock(BlockPos blockPos) {
        int xposAndZposHash = Objects.hash(blockPos.getX(), blockPos.getZ());
        SortedArraySet<Integer> yposSortedArraySet =
            laserBlockEntityYposMap.getOrDefault(xposAndZposHash, SortedArraySet.create());
        int index = yposSortedArraySet.findIndex(blockPos.getY());
        if (index < 0) return null;
        HashMap<Direction, BlockPos> blockPosMap = new HashMap<>();
        int y = yposSortedArraySet.getInternal(index);
        if (y == blockPos.getY()) {
            if (index != yposSortedArraySet.size() - 1)
                blockPosMap.put(
                    Direction.DOWN,
                    new BlockPos(
                        blockPos.getX(), yposSortedArraySet.getInternal(index + 1),
                        blockPos.getZ())
                );
            if (index != 0)
                blockPosMap.put(
                    Direction.UP,
                    new BlockPos(
                        blockPos.getX(), yposSortedArraySet.getInternal(index - 1),
                        blockPos.getZ())
                );
        } else {
            blockPosMap.put(
                Direction.UP,
                new BlockPos(
                    blockPos.getX(), yposSortedArraySet.getInternal(index),
                    blockPos.getZ())
            );
            if (index != yposSortedArraySet.size() - 1)
                blockPosMap.put(
                    Direction.DOWN,
                    new BlockPos(
                        blockPos.getX(), yposSortedArraySet.getInternal(index + 1),
                        blockPos.getZ())
                );
        }
        return blockPosMap;
    }

    private HashMap<Direction, BlockPos> getNeighbourSameZposBlock(BlockPos blockPos) {
        int xposAndYposHash = Objects.hash(blockPos.getX(), blockPos.getY());
        SortedArraySet<Integer> zposSortedArraySet =
            laserBlockEntityZposMap.getOrDefault(xposAndYposHash, SortedArraySet.create());
        int index = zposSortedArraySet.findIndex(blockPos.getZ());
        if (index < 0) return null;
        HashMap<Direction, BlockPos> blockPosMap = new HashMap<>();
        int z = zposSortedArraySet.getInternal(index);
        if (z == blockPos.getZ()) {
            if (index != zposSortedArraySet.size() - 1)
                blockPosMap.put(
                    Direction.NORTH,
                    new BlockPos(
                        blockPos.getX(),
                        blockPos.getY(),
                        zposSortedArraySet.getInternal(index + 1))
                );
            if (index != 0)
                blockPosMap.put(
                    Direction.SOUTH,
                    new BlockPos(
                        blockPos.getX(),
                        blockPos.getY(),
                        zposSortedArraySet.getInternal(index - 1))
                );
        } else {
            blockPosMap.put(
                Direction.SOUTH,
                new BlockPos(
                    blockPos.getX(),
                    blockPos.getY(),
                    zposSortedArraySet.getInternal(index))
            );
            if (index != zposSortedArraySet.size() - 1)
                blockPosMap.put(
                    Direction.NORTH,
                    new BlockPos(
                        blockPos.getX(),
                        blockPos.getY(),
                        zposSortedArraySet.getInternal(index + 1))
                );
        }
        return blockPosMap;
    }

    private boolean canPassThrough(Direction direction, BlockPos blockPos) {
        BlockState blockState = level.getBlockState(blockPos);
        if (blockState.is(ModBlockTags.LASE_CAN_PASS_THROUGH)) return true;
        AABB laseBoundingBox = switch (direction.getAxis()) {
            case X -> Block.box(0, 7, 7, 16, 9, 9).bounds();
            case Y -> Block.box(7, 0, 7, 9, 16, 9).bounds();
            case Z -> Block.box(7, 7, 0, 9, 9, 16).bounds();
        };
        return blockState.getShape(level, blockPos).toAabbs().stream().noneMatch(
            laseBoundingBox::intersects);
    }

    public void registerPrism(Direction direction, BlockPos blockPos, RubyPrismBlockEntity rubyPrismBlockEntity) {
        laserBlockEntityMap.put(blockPos, rubyPrismBlockEntity);
        addBlockPos(blockPos);
        HashMap<Direction, BlockPos> xposSet = getNeighbourSameXposBlock(blockPos);
        HashMap<Direction, BlockPos> yposSet = getNeighbourSameYposBlock(blockPos);
        HashMap<Direction, BlockPos> zposSet = getNeighbourSameZposBlock(blockPos);
        HashMap<Direction, BlockPos> directionBlockPosHashMap = new HashMap<>();
        if (xposSet != null) directionBlockPosHashMap.putAll(xposSet);
        if (yposSet != null) directionBlockPosHashMap.putAll(yposSet);
        if (zposSet != null) directionBlockPosHashMap.putAll(zposSet);
        if (directionBlockPosHashMap.containsKey(direction)) {
            linkLaserBlockEntityMap.put(blockPos, directionBlockPosHashMap.get(direction));
        }
        for (Map.Entry<Direction, BlockPos> entry : directionBlockPosHashMap.entrySet()) {
            if (this.level.getBlockState(entry.getValue()).hasProperty(BlockStateProperties.FACING)
                && entry.getKey() == this.level.getBlockState(entry.getValue()).getValue(
                BlockStateProperties.FACING).getOpposite()) {
                linkLaserBlockEntityMap.put(entry.getValue(), blockPos);
            }
        }
    }

    public void unregister(BlockPos blockPos) {
        laserBlockEntityMap.remove(blockPos);
        linkLaserBlockEntityMap.remove(blockPos);
        int yposAndZposHash = Objects.hash(blockPos.getY(), blockPos.getZ());
        int xposAndZposHash = Objects.hash(blockPos.getX(), blockPos.getZ());
        int xposAndYposHash = Objects.hash(blockPos.getX(), blockPos.getY());
        SortedArraySet<Integer> xposSortedArraySet =
            laserBlockEntityXposMap.getOrDefault(yposAndZposHash, SortedArraySet.create());
        SortedArraySet<Integer> yposSortedArraySet =
            laserBlockEntityYposMap.getOrDefault(xposAndZposHash, SortedArraySet.create());
        SortedArraySet<Integer> zposSortedArraySet =
            laserBlockEntityZposMap.getOrDefault(xposAndYposHash, SortedArraySet.create());
        xposSortedArraySet.remove(blockPos.getX());
        yposSortedArraySet.remove(blockPos.getY());
        zposSortedArraySet.remove(blockPos.getZ());
        if (xposSortedArraySet.isEmpty()) laserBlockEntityXposMap.remove(yposAndZposHash);
        else laserBlockEntityXposMap.put(yposAndZposHash, xposSortedArraySet);
        if (yposSortedArraySet.isEmpty()) laserBlockEntityYposMap.remove(xposAndZposHash);
        else laserBlockEntityYposMap.put(xposAndZposHash, yposSortedArraySet);
        if (zposSortedArraySet.isEmpty()) laserBlockEntityZposMap.remove(xposAndYposHash);
        else laserBlockEntityZposMap.put(xposAndYposHash, zposSortedArraySet);
    }

    public BaseLaserBlockEntity getIrradiateLaserBlockEntity(BlockPos blockPos) {
        if (!linkLaserBlockEntityMap.containsKey(blockPos)) return null;
        return laserBlockEntityMap.get(linkLaserBlockEntityMap.get(blockPos));
    }

    public BlockPos getIrradiateBlockPos(int expectedLength, Direction direction, BlockPos originPos) {
        for (int length = 1; length <= expectedLength; length++) {
            if (canPassThrough(direction, originPos.relative(direction, length)))
                return originPos.relative(direction, length);
        }
        return  originPos.relative(direction, expectedLength);
    }
}
