package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.power.ring.AccelerationRingBlock;
import dev.dubhe.anvilcraft.block.power.ring.DeflectionRingBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.block.state.DirectionCube3x3PartHalf;
import dev.dubhe.anvilcraft.block.state.GiantAnvilCube;
import dev.dubhe.anvilcraft.block.workstation.GiantAnvilBlock;
import dev.dubhe.anvilcraft.entity.FallingGiantAnvilEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class AccelerationRingBlockEntity extends BlockEntity implements IPowerConsumer {
    private static final HashMap<Level, AccelerationIndex> LEVEL_ACCELERATION_INDEX = new HashMap<>();
    @Getter
    @Setter
    private @Nullable PowerGrid grid;

    public AccelerationRingBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.ACCELERATION_RING.get(), pos, blockState);
    }

    private AccelerationRingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static AccelerationRingBlockEntity createBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        return new AccelerationRingBlockEntity(type, pos, blockState);
    }

    public static Iterable<BlockPos> getAllBlocks(Level level) {
        AccelerationIndex index = AccelerationRingBlockEntity.LEVEL_ACCELERATION_INDEX.get(level);
        return index == null ? List.of() : index.positions;
    }

    public static @Nullable AABB getAABB(Level level, BlockPos pos) {
        AccelerationIndex index = AccelerationRingBlockEntity.LEVEL_ACCELERATION_INDEX.get(level);
        return index == null ? null : index.areas.get(pos);
    }

    public static Iterable<BlockPos> getBlocksAt(Level level, Vec3 pos) {
        AccelerationIndex index = AccelerationRingBlockEntity.LEVEL_ACCELERATION_INDEX.get(level);
        return index == null ? List.of() : index.getBlocksAt(pos);
    }

    public static Iterable<BlockPos> getBlocksAlongMovement(Level level, Vec3 start, Vec3 movement) {
        AccelerationIndex index = AccelerationRingBlockEntity.LEVEL_ACCELERATION_INDEX.get(level);
        return index == null ? List.of() : index.getBlocksAlongMovement(start, movement);
    }

    public static Iterable<BlockPos> getRingsAlongMovement(Level level, Vec3 start, Vec3 movement) {
        AccelerationIndex index = AccelerationRingBlockEntity.LEVEL_ACCELERATION_INDEX.get(level);
        return index == null ? List.of() : index.getRingsAlongMovement(start, movement);
    }

    public static void clear(Level level) {
        AccelerationRingBlockEntity.LEVEL_ACCELERATION_INDEX.remove(level);
    }

    private void addSelfToMap() {
        if (this.level == null) return;
        AccelerationRingBlockEntity.LEVEL_ACCELERATION_INDEX
            .computeIfAbsent(this.level, ignored -> new AccelerationIndex())
            .add(this.getBlockPos());
    }

    private void removeSelfFromMap() {
        if (this.level == null) return;
        AccelerationIndex index = AccelerationRingBlockEntity.LEVEL_ACCELERATION_INDEX.get(this.level);
        if (index == null) return;
        index.remove(this.getBlockPos());
        if (index.positions.isEmpty()) AccelerationRingBlockEntity.LEVEL_ACCELERATION_INDEX.remove(this.level);
    }

    private void removeAccelerationArea() {
        if (this.level == null) return;
        AccelerationIndex index = AccelerationRingBlockEntity.LEVEL_ACCELERATION_INDEX.get(this.level);
        if (index != null) index.removeArea(this.getBlockPos());
    }

    private void updateAccelerationArea(AABB area) {
        if (this.level == null) return;
        AccelerationRingBlockEntity.LEVEL_ACCELERATION_INDEX.computeIfAbsent(this.level, ignored -> new AccelerationIndex())
            .updateArea(this.getBlockPos(), area);
    }

    @Override
    public @Nullable Level getCurrentLevel() {
        return this.level;
    }

    @Override
    public BlockPos getPos() {
        return this.getBlockPos();
    }

    @Override
    public PowerComponentType getComponentType() {
        if (this.level == null) return PowerComponentType.INVALID;
        if (!this.level.getBlockState(this.getBlockPos()).hasProperty(AccelerationRingBlock.HALF)) {
            return PowerComponentType.INVALID;
        }
        if (this.level.getBlockState(this.getBlockPos()).getValue(AccelerationRingBlock.HALF).equals(DirectionCube3x3PartHalf.MID_CENTER)) {
            return PowerComponentType.CONSUMER;
        } else {
            return PowerComponentType.INVALID;
        }
    }

    @Override
    public int getRange() {
        return 1;
    }

    public boolean isWork() {
        BlockState state = this.getBlockState();
        return state.getValue(AccelerationRingBlock.SWITCH) == Switch.ON && !state.getValue(AccelerationRingBlock.OVERLOAD);
    }

    public void tick() {
        if (this.level == null) return;
        BlockState state = this.getBlockState();
        if (this.level.isClientSide()) {
            if (!state.getValue(AccelerationRingBlock.HALF).equals(DirectionCube3x3PartHalf.MID_CENTER)) return;
            if (this.isWork()) {
                this.addSelfToMap();
                this.accelerate();
            } else this.removeSelfFromMap();
        }
        if (this.grid == null) return;
        if (!state.getValue(AccelerationRingBlock.HALF).equals(DirectionCube3x3PartHalf.MID_CENTER)) return;
        if (!(state.getBlock() instanceof AccelerationRingBlock block)) return;
        if (this.grid.isWorking() && state.getValue(AccelerationRingBlock.OVERLOAD)) {
            block.updateState(this.level, this.getBlockPos(), AccelerationRingBlock.OVERLOAD, false, 3);
        } else if (!this.grid.isWorking() && !state.getValue(AccelerationRingBlock.OVERLOAD)) {
            block.updateState(this.level, this.getBlockPos(), AccelerationRingBlock.OVERLOAD, true, 3);
        }
        if (!this.isWork()) {
            this.removeSelfFromMap();
            return;
        }
        this.addSelfToMap();
        if (state.getValue(AccelerationRingBlock.FACING).equals(Direction.UP)) {
            this.attractGianAnvil();
        }
        this.accelerate();
    }

    public void accelerate() {
        assert this.level != null;
        Direction direction = this.getBlockState().getValue(AccelerationRingBlock.FACING);
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();
        BlockPos endRingPos = null;
        ArrayList<BlockPos> blockPositions = null;
        checkPos.set(this.getBlockPos());
        boolean found = false;
        checkPos.move(direction);
        for (int i = 0; i < 14; i++) {
            checkPos.move(direction);
            BlockState checkState = this.level.getBlockState(checkPos);
            if (!this.level.isClientSide() && checkState.is(BlockTags.ANVIL) && !checkState.is(ModBlockTags.NON_MAGNETIC)) {
                if (blockPositions == null) blockPositions = new ArrayList<>();
                blockPositions.add(checkPos.immutable());
            }
            if (
                checkState.hasProperty(AccelerationRingBlock.HALF)
                && checkState.getValue(AccelerationRingBlock.HALF) == DirectionCube3x3PartHalf.MID_CENTER
                && checkState.getValue(AccelerationRingBlock.SWITCH) == IPowerComponent.Switch.ON
                && !checkState.getValue(AccelerationRingBlock.OVERLOAD)
                && checkState.getValue(AccelerationRingBlock.FACING) == direction
            ) {
                found = true;
                endRingPos = checkPos.immutable();
                break;
            }
        }
        if (!found) {
            this.removeAccelerationArea();
            return;
        }
        BlockPos aabbStart = this.getBlockPos().relative(direction.getOpposite(), 1);
        BlockState deflectionCheck = this.level.getBlockState(this.getBlockPos().relative(direction.getOpposite(), 3));
        if (
            deflectionCheck.hasProperty(DeflectionRingBlock.HALF)
            && deflectionCheck.getValue(DeflectionRingBlock.HALF) == DirectionCube3x3PartHalf.MID_CENTER
            && deflectionCheck.getValue(DeflectionRingBlock.SWITCH) == IPowerComponent.Switch.ON
            && !deflectionCheck.getValue(DeflectionRingBlock.OVERLOAD)
        ) {
            aabbStart = this.getBlockPos().relative(direction.getOpposite(), 2);
        }
        AABB aabb = AABB.encapsulatingFullBlocks(endRingPos.relative(direction), aabbStart);
        this.updateAccelerationArea(aabb);
        if (this.level.isClientSide() || blockPositions == null) return;
        for (BlockPos pos : blockPositions) {
            BlockState fallState = this.level.getBlockState(pos);
            this.level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            FallingBlockEntity fallingEntity = FallingBlockEntity.fall(this.level, pos, fallState);
            if (fallState.getBlock() instanceof FallingBlock fallingBlock) {
                fallingBlock.falling(fallingEntity);
            }
        }
    }

    @SuppressWarnings("DuplicatedCode")
    public void attractGianAnvil() {
        assert this.level != null;
        if (
            this.level.getBlockState(this.getBlockPos().below(2)).hasProperty(GiantAnvilBlock.HALF)
            && this.level.getBlockState(this.getBlockPos().below(2)).getValue(GiantAnvilBlock.HALF) == Cube3x3PartHalf.TOP_CENTER
        ) {
            return;
        }
        BlockPos giantAnvilPos = null;
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();
        checkPos.set(this.getBlockPos().below(2));
        for (int y = 0; y < 11; y++) {
            BlockState checkState = this.level.getBlockState(checkPos);
            if (!checkState.hasProperty(GiantAnvilBlock.HALF)) {
                checkPos.move(Direction.DOWN);
                continue;
            }
            Cube3x3PartHalf cube3x3PartHalf = checkState.getValue(GiantAnvilBlock.HALF);
            if (cube3x3PartHalf == Cube3x3PartHalf.MID_CENTER) {
                giantAnvilPos = checkPos.west(0);
                break;
            }
            checkPos.move(Direction.DOWN);
        }
        Vec3 ringCenter = this.getBlockPos().getCenter();
        FallingGiantAnvilEntity fallingGiantAnvilEntity = null;
        double nearestDistanceSqr = Double.POSITIVE_INFINITY;
        for (FallingGiantAnvilEntity entity : this.level.getEntitiesOfClass(
            FallingGiantAnvilEntity.class,
            new AABB(
                this.getBlockPos().getX(),
                this.getBlockPos().getY() - 2,
                this.getBlockPos().getZ(),
                this.getBlockPos().getX() + 1,
                this.getBlockPos().getY() - 12,
                this.getBlockPos().getZ() + 1
            )
        )) {
            double offsetX = entity.getX() - ringCenter.x;
            double offsetZ = entity.getZ() - ringCenter.z;
            if (offsetX * offsetX + offsetZ * offsetZ > 0.25 * 0.25) continue;
            double distanceSqr = entity.position().distanceToSqr(ringCenter);
            if (distanceSqr >= nearestDistanceSqr) continue;
            fallingGiantAnvilEntity = entity;
            nearestDistanceSqr = distanceSqr;
        }
        if (fallingGiantAnvilEntity != null) {
            if (
                giantAnvilPos != null
                && nearestDistanceSqr < giantAnvilPos.getCenter().distanceToSqr(ringCenter)
            ) {
                giantAnvilPos = BlockPos.containing(fallingGiantAnvilEntity.position());
            } else if (giantAnvilPos == null) {
                giantAnvilPos = BlockPos.containing(fallingGiantAnvilEntity.position());
            }
        }
        if (giantAnvilPos == null) {
            return;
        }
        checkPos.set(giantAnvilPos);
        checkPos.move(-1, 2, -1);
        while (checkPos.getY() < this.getBlockPos().getY() - 1) {
            for (int x = -1; x < 2; x++) {
                for (int z = -1; z < 2; z++) {
                    BlockState checked = this.level.getBlockState(checkPos);
                    if (!checked.canBeReplaced()) {
                        return;
                    }
                    checkPos.move(0, 0, 1);
                }
                checkPos.move(0, 0, -3);
                checkPos.move(1, 0, 0);
            }
            checkPos.move(-3, 1, 0);
        }
        Block block = this.level.getBlockState(giantAnvilPos.below()).getBlock();
        if (block instanceof GiantAnvilBlock giantAnvilBlock) {
            giantAnvilBlock.removePartsAndUpdate(this.level, giantAnvilPos.below());
        }
        BlockPos newPos = this.getBlockPos().below(4);
        for (Cube3x3PartHalf part : Cube3x3PartHalf.values()) {
            this.level.setBlockAndUpdate(
                newPos.offset(part.getOffset()), ModBlocks.GIANT_ANVIL.getDefaultState()
                    .setValue(GiantAnvilBlock.HALF, part)
                    .setValue(GiantAnvilBlock.CUBE, part.equals(Cube3x3PartHalf.MID_CENTER) ? GiantAnvilCube.CENTER : GiantAnvilCube.CORNER)
            );
        }
        if (fallingGiantAnvilEntity != null) fallingGiantAnvilEntity.discard();
    }

    @Override
    public int getInputPower() {
        return this.getBlockState().getValue(AccelerationRingBlock.SWITCH) == Switch.ON ? 256 : 0;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        this.removeSelfFromMap();
    }

    private static final class AccelerationIndex {
        private final HashSet<BlockPos> positions = new HashSet<>();
        private final HashMap<BlockPos, AABB> areas = new HashMap<>();
        private final HashMap<BlockPos, LongArrayList> areaSections = new HashMap<>();
        private final Long2ObjectOpenHashMap<HashSet<BlockPos>> bySection = new Long2ObjectOpenHashMap<>();
        private final Long2ObjectOpenHashMap<HashSet<BlockPos>> ringsBySection = new Long2ObjectOpenHashMap<>();

        private void add(BlockPos pos) {
            BlockPos immutablePos = pos.immutable();
            if (!this.positions.add(immutablePos)) return;
            this.ringsBySection.computeIfAbsent(SectionPos.asLong(immutablePos), ignored -> new HashSet<>())
                .add(immutablePos);
        }

        private void remove(BlockPos pos) {
            if (this.positions.remove(pos)) {
                long sectionKey = SectionPos.asLong(pos);
                if (this.ringsBySection.containsKey(sectionKey)) {
                    HashSet<BlockPos> sectionPositions = this.ringsBySection.get(sectionKey);
                    sectionPositions.remove(pos);
                    if (sectionPositions.isEmpty()) this.ringsBySection.remove(sectionKey);
                }
            }
            this.removeArea(pos);
        }

        private void updateArea(BlockPos pos, AABB area) {
            AABB previous = this.areas.get(pos);
            if (area.equals(previous)) return;
            this.removeArea(pos);

            BlockPos immutablePos = pos.immutable();
            this.areas.put(immutablePos, area);
            int minSectionX = SectionPos.blockToSectionCoord(area.minX);
            int minSectionY = SectionPos.blockToSectionCoord(area.minY);
            int minSectionZ = SectionPos.blockToSectionCoord(area.minZ);
            int maxSectionX = SectionPos.blockToSectionCoord(Math.nextDown(area.maxX));
            int maxSectionY = SectionPos.blockToSectionCoord(Math.nextDown(area.maxY));
            int maxSectionZ = SectionPos.blockToSectionCoord(Math.nextDown(area.maxZ));
            int sectionCount = (maxSectionX - minSectionX + 1)
                               * (maxSectionY - minSectionY + 1)
                               * (maxSectionZ - minSectionZ + 1);
            LongArrayList sections = new LongArrayList(sectionCount);
            for (int sectionX = minSectionX; sectionX <= maxSectionX; sectionX++) {
                for (int sectionY = minSectionY; sectionY <= maxSectionY; sectionY++) {
                    for (int sectionZ = minSectionZ; sectionZ <= maxSectionZ; sectionZ++) {
                        long sectionKey = SectionPos.asLong(sectionX, sectionY, sectionZ);
                        sections.add(sectionKey);
                        this.bySection.computeIfAbsent(sectionKey, ignored -> new HashSet<>()).add(immutablePos);
                    }
                }
            }
            this.areaSections.put(immutablePos, sections);
        }

        private void removeArea(BlockPos pos) {
            this.areas.remove(pos);
            LongArrayList sections = this.areaSections.remove(pos);
            if (sections == null) return;
            for (int i = 0; i < sections.size(); i++) {
                long sectionKey = sections.getLong(i);
                if (!this.bySection.containsKey(sectionKey)) continue;
                HashSet<BlockPos> sectionPositions = this.bySection.get(sectionKey);
                sectionPositions.remove(pos);
                if (sectionPositions.isEmpty()) this.bySection.remove(sectionKey);
            }
        }

        private Iterable<BlockPos> getBlocksAt(Vec3 pos) {
            long sectionKey = SectionPos.asLong(
                SectionPos.blockToSectionCoord(pos.x),
                SectionPos.blockToSectionCoord(pos.y),
                SectionPos.blockToSectionCoord(pos.z)
            );
            return this.bySection.containsKey(sectionKey) ? this.bySection.get(sectionKey) : List.of();
        }

        private Iterable<BlockPos> getBlocksAlongMovement(Vec3 start, Vec3 movement) {
            return AccelerationIndex.getPositionsAlongMovement(start, movement, this.bySection);
        }

        private Iterable<BlockPos> getRingsAlongMovement(Vec3 start, Vec3 movement) {
            return AccelerationIndex.getPositionsAlongMovement(start, movement, this.ringsBySection);
        }

        private static Iterable<BlockPos> getPositionsAlongMovement(
            Vec3 start,
            Vec3 movement,
            Long2ObjectOpenHashMap<HashSet<BlockPos>> sectionIndex
        ) {
            double movementSqr = movement.lengthSqr();
            if (!Double.isFinite(movementSqr)) return List.of();
            Vec3 end = start.add(movement);
            int sectionX = SectionPos.blockToSectionCoord(start.x);
            int sectionY = SectionPos.blockToSectionCoord(start.y);
            int sectionZ = SectionPos.blockToSectionCoord(start.z);
            int endSectionX = SectionPos.blockToSectionCoord(end.x);
            int endSectionY = SectionPos.blockToSectionCoord(end.y);
            int endSectionZ = SectionPos.blockToSectionCoord(end.z);
            if (sectionX == endSectionX && sectionY == endSectionY && sectionZ == endSectionZ) {
                long sectionKey = SectionPos.asLong(sectionX, sectionY, sectionZ);
                return sectionIndex.containsKey(sectionKey) ? sectionIndex.get(sectionKey) : List.of();
            }

            int stepX = Double.compare(movement.x, 0.0);
            int stepY = Double.compare(movement.y, 0.0);
            int stepZ = Double.compare(movement.z, 0.0);
            double nextBoundaryX = SectionPos.sectionToBlockCoord(sectionX + (stepX > 0 ? 1 : 0));
            double nextBoundaryY = SectionPos.sectionToBlockCoord(sectionY + (stepY > 0 ? 1 : 0));
            double nextBoundaryZ = SectionPos.sectionToBlockCoord(sectionZ + (stepZ > 0 ? 1 : 0));
            double nextSectionProgressX = stepX == 0
                                          ? Double.POSITIVE_INFINITY
                                          : (nextBoundaryX - start.x) / movement.x;
            double nextSectionProgressY = stepY == 0
                                          ? Double.POSITIVE_INFINITY
                                          : (nextBoundaryY - start.y) / movement.y;
            double nextSectionProgressZ = stepZ == 0
                                          ? Double.POSITIVE_INFINITY
                                          : (nextBoundaryZ - start.z) / movement.z;
            double sectionProgressStepX = stepX == 0
                                          ? Double.POSITIVE_INFINITY
                                          : 16.0 / Math.abs(movement.x);
            double sectionProgressStepY = stepY == 0
                                          ? Double.POSITIVE_INFINITY
                                          : 16.0 / Math.abs(movement.y);
            double sectionProgressStepZ = stepZ == 0
                                          ? Double.POSITIVE_INFINITY
                                          : 16.0 / Math.abs(movement.z);
            int remainingSections = Math.abs(endSectionX - sectionX)
                                    + Math.abs(endSectionY - sectionY)
                                    + Math.abs(endSectionZ - sectionZ)
                                    + 1;
            HashSet<BlockPos> candidates = new HashSet<>();
            while (remainingSections-- > 0) {
                long sectionKey = SectionPos.asLong(sectionX, sectionY, sectionZ);
                if (sectionIndex.containsKey(sectionKey)) candidates.addAll(sectionIndex.get(sectionKey));
                if (sectionX == endSectionX && sectionY == endSectionY && sectionZ == endSectionZ) break;

                if (nextSectionProgressX <= nextSectionProgressY
                    && nextSectionProgressX <= nextSectionProgressZ) {
                    sectionX += stepX;
                    nextSectionProgressX += sectionProgressStepX;
                } else if (nextSectionProgressY <= nextSectionProgressZ) {
                    sectionY += stepY;
                    nextSectionProgressY += sectionProgressStepY;
                } else {
                    sectionZ += stepZ;
                    nextSectionProgressZ += sectionProgressStepZ;
                }
            }
            return candidates;
        }
    }
}
