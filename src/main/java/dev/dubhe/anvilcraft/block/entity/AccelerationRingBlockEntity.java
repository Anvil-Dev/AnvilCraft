package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
import dev.dubhe.anvilcraft.block.AccelerationRingBlock;
import dev.dubhe.anvilcraft.block.DeflectionRingBlock;
import dev.dubhe.anvilcraft.block.GiantAnvilBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.block.state.DirectionCube3x3PartHalf;
import dev.dubhe.anvilcraft.block.state.GiantAnvilCube;
import dev.dubhe.anvilcraft.entity.FallingGiantAnvilEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.util.DistanceComparator;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

public class AccelerationRingBlockEntity extends BlockEntity implements IPowerConsumer {
    private static final HashMap<Level, HashSet<BlockPos>> LEVEL_ACCELERATION_BLOCK_MAP = new HashMap<>();
    private static final HashMap<BlockPos, AABB> ACCELERATION_AABB_MAP = new HashMap<>();
    @Getter
    @Setter
    private PowerGrid grid;

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
        if (LEVEL_ACCELERATION_BLOCK_MAP.containsKey(level)) {
            return LEVEL_ACCELERATION_BLOCK_MAP.get(level);
        } else {
            return List.of();
        }
    }

    public static AABB getAABB(BlockPos pos) {
        return ACCELERATION_AABB_MAP.get(pos);
    }

    public static void clear() {
        LEVEL_ACCELERATION_BLOCK_MAP.clear();
        ACCELERATION_AABB_MAP.clear();
    }

    private void addSelfToMap() {
        if (level == null) return;
        if (LEVEL_ACCELERATION_BLOCK_MAP.containsKey(level)) {
            LEVEL_ACCELERATION_BLOCK_MAP.get(level).add(getBlockPos());
        } else {
            HashSet<BlockPos> set = new HashSet<>();
            set.add(getBlockPos());
            LEVEL_ACCELERATION_BLOCK_MAP.put(level, set);
        }
    }

    private void removeSelfFromMap() {
        if (level == null) return;
        if (LEVEL_ACCELERATION_BLOCK_MAP.containsKey(level)) {
            LEVEL_ACCELERATION_BLOCK_MAP.get(level).remove(getBlockPos());
        }
        ACCELERATION_AABB_MAP.remove(getBlockPos());
    }

    @Override
    public @Nullable Level getCurrentLevel() {
        return this.level;
    }

    @Override
    public BlockPos getPos() {
        return getBlockPos();
    }

    @Override
    public PowerComponentType getComponentType() {
        if (this.level == null) return PowerComponentType.INVALID;
        if (!this.level.getBlockState(getBlockPos()).hasProperty(AccelerationRingBlock.HALF)) {
            return PowerComponentType.INVALID;
        }
        if (this.level.getBlockState(getBlockPos()).getValue(AccelerationRingBlock.HALF).equals(DirectionCube3x3PartHalf.MID_CENTER)) {
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
        BlockState state = getBlockState();
        return state.getValue(AccelerationRingBlock.SWITCH) == Switch.ON && !state.getValue(AccelerationRingBlock.OVERLOAD);
    }

    public void tick() {
        if (this.level == null) return;
        BlockState state = getBlockState();
        if (this.level.isClientSide()) {
            if (!state.getValue(AccelerationRingBlock.HALF).equals(DirectionCube3x3PartHalf.MID_CENTER)) return;
            if (isWork()) {
                addSelfToMap();
                accelerate();
            } else removeSelfFromMap();
        }
        if (this.grid == null) return;
        if (!state.getValue(AccelerationRingBlock.HALF).equals(DirectionCube3x3PartHalf.MID_CENTER)) return;
        if (!(state.getBlock() instanceof AccelerationRingBlock block)) return;
        if (this.grid.isWorking() && state.getValue(AccelerationRingBlock.OVERLOAD)) {
            block.updateState(this.level, getBlockPos(), AccelerationRingBlock.OVERLOAD, false, 3);
        } else if (!this.grid.isWorking() && !state.getValue(AccelerationRingBlock.OVERLOAD)) {
            block.updateState(this.level, getBlockPos(), AccelerationRingBlock.OVERLOAD, true, 3);
        }
        if (!isWork()) {
            removeSelfFromMap();
            return;
        }
        addSelfToMap();
        if (state.getValue(AccelerationRingBlock.FACING).equals(Direction.UP)) {
            attractGianAnvil();
        }
        accelerate();
    }

    public void accelerate() {
        assert this.level != null;
        Direction direction = getBlockState().getValue(AccelerationRingBlock.FACING);
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();
        BlockPos endRingPos = null;
        ArrayList<BlockPos> blockPoses = new ArrayList<>();
        checkPos.set(getBlockPos());
        boolean found = false;
        checkPos.move(direction);
        for (int i = 0; i < 14; i++) {
            checkPos.move(direction);
            BlockState checkState = this.level.getBlockState(checkPos);
            if (checkState.is(BlockTags.ANVIL) && !checkState.is(ModBlockTags.NON_MAGNETIC)) {
                blockPoses.add(checkPos.east(0));
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
            ACCELERATION_AABB_MAP.remove(getBlockPos());
            return;
        }
        BlockPos aabbStart = getBlockPos().relative(direction.getOpposite(), 1);
        BlockState deflectionCheck = level.getBlockState(getBlockPos().relative(direction.getOpposite(), 3));
        if (
            deflectionCheck.hasProperty(DeflectionRingBlock.HALF)
            && deflectionCheck.getValue(DeflectionRingBlock.HALF) == DirectionCube3x3PartHalf.MID_CENTER
            && deflectionCheck.getValue(DeflectionRingBlock.SWITCH) == IPowerComponent.Switch.ON
            && !deflectionCheck.getValue(DeflectionRingBlock.OVERLOAD)
        ) {
            aabbStart = getBlockPos().relative(direction.getOpposite(), 2);
        }
        AABB aabb = AABB.encapsulatingFullBlocks(endRingPos.relative(direction), aabbStart);
        Direction.Axis axis = direction.getAxis();
        double inflationX = axis == Direction.Axis.X ? 0 : 1;
        double inflationY = axis == Direction.Axis.Y ? 0 : 1;
        double inflationZ = axis == Direction.Axis.Z ? 0 : 1;
        ACCELERATION_AABB_MAP.put(getBlockPos(), aabb.inflate(inflationX, inflationY, inflationZ));
        for (BlockPos pos : blockPoses) {
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
            this.level.getBlockState(getBlockPos().below(2)).hasProperty(GiantAnvilBlock.HALF)
            && this.level.getBlockState(getBlockPos().below(2)).getValue(GiantAnvilBlock.HALF) == Cube3x3PartHalf.TOP_CENTER
        ) {
            return;
        }
        BlockPos giantAnvilPos = null;
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();
        checkPos.set(getBlockPos().below(2));
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
        Vector2d vector2d = new Vector2d(getBlockPos().getCenter().x, getBlockPos().getCenter().z);
        Optional<FallingGiantAnvilEntity> fallingGiantAnvilEntity = this.level.getEntitiesOfClass(
                FallingGiantAnvilEntity.class,
                new AABB(
                    getBlockPos().getX(),
                    getBlockPos().getY() - 2,
                    getBlockPos().getZ(),
                    getBlockPos().getX() + 1,
                    getBlockPos().getY() - 12,
                    getBlockPos().getZ() + 1
                )
            ).stream()
            .sorted((e1, e2) -> new DistanceComparator(getBlockPos().getCenter()).compare(e1.position(), e2.position()))
            .filter(entity -> vector2d.distance(entity.position().x, entity.position().z) <= 0.25)
            .findFirst();
        if (fallingGiantAnvilEntity.isPresent()) {
            if (
                giantAnvilPos != null
                && fallingGiantAnvilEntity.get().position().distanceTo(getBlockPos().getCenter())
                   < giantAnvilPos.getCenter().distanceTo(getBlockPos().getCenter())
            ) {
                giantAnvilPos = BlockPos.containing(fallingGiantAnvilEntity.get().position());
            } else if (giantAnvilPos == null) {
                giantAnvilPos = BlockPos.containing(fallingGiantAnvilEntity.get().position());
            }
        }
        if (giantAnvilPos == null) {
            return;
        }
        checkPos.set(giantAnvilPos);
        checkPos.move(-1, 2, -1);
        while (checkPos.getY() < getBlockPos().getY() - 1) {
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
            giantAnvilBlock.removePartsAndUpdate(level, giantAnvilPos.below());
        }
        BlockPos newPos = getBlockPos().below(4);
        for (Cube3x3PartHalf part : Cube3x3PartHalf.values()) {
            this.level.setBlockAndUpdate(
                newPos.offset(part.getOffset()), ModBlocks.GIANT_ANVIL.getDefaultState()
                    .setValue(GiantAnvilBlock.HALF, part)
                    .setValue(GiantAnvilBlock.CUBE, part.equals(Cube3x3PartHalf.MID_CENTER) ? GiantAnvilCube.CENTER : GiantAnvilCube.CORNER)
            );
        }
        fallingGiantAnvilEntity.ifPresent(Entity::kill);
    }

    @Override
    public int getInputPower() {
        return getBlockState().getValue(AccelerationRingBlock.SWITCH) == Switch.ON ? 256 : 0;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        removeSelfFromMap();
    }
}
