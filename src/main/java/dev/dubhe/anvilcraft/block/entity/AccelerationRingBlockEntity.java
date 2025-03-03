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
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import dev.dubhe.anvilcraft.init.ModBlockTags;
import dev.dubhe.anvilcraft.init.ModBlocks;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class AccelerationRingBlockEntity extends BlockEntity implements IPowerConsumer {
    private final Comparator<Entity> ENTITY_SORTER = new Comparator<>() {
        private final Vec3 blockPosVec = getBlockPos().getCenter();

        @Override
        public int compare(Entity entity, Entity t1) {
            double d1 = entity.position().distanceTo(blockPosVec);
            double d2 = t1.position().distanceTo(blockPosVec);
            if (d1 == d2)
                return 0;
            else return d1 < d2 ? -1 : 1;
        }
    };
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


    @Override
    public Level getCurrentLevel() {
        return level;
    }

    @Override
    public @NotNull BlockPos getPos() {
        return getBlockPos();
    }

    @Override
    public @NotNull PowerComponentType getComponentType() {
        if (level == null) return PowerComponentType.INVALID;
        if (!level.getBlockState(getBlockPos()).hasProperty(AccelerationRingBlock.HALF))
            return PowerComponentType.INVALID;
        if (level.getBlockState(getBlockPos()).getValue(AccelerationRingBlock.HALF).equals(DirectionCube3x3PartHalf.MID_CENTER))
            return PowerComponentType.CONSUMER;
        else
            return PowerComponentType.INVALID;
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
        if (level == null) return;
        if (level.isClientSide) {
            if (isWork())
                accelerate();
        }
        if (grid == null) return;
        BlockState state = getBlockState();
        if (!state.getValue(AccelerationRingBlock.HALF).equals(DirectionCube3x3PartHalf.MID_CENTER)) return;
        if (!(state.getBlock() instanceof AccelerationRingBlock block)) return;
        if (grid.isWorking() && state.getValue(AccelerationRingBlock.OVERLOAD)) {
            block.updateState(level, getBlockPos(), AccelerationRingBlock.OVERLOAD, false, 3);
        } else if (!grid.isWorking() && !state.getValue(AccelerationRingBlock.OVERLOAD)) {
            block.updateState(level, getBlockPos(), AccelerationRingBlock.OVERLOAD, true, 3);
        }
        if (!isWork()) return;
        if (state.getValue(AccelerationRingBlock.FACING).equals(Direction.UP))
            attractGianAnvil();
        accelerate();
    }

    public void accelerate() {
        assert level != null;
        Direction direction = getBlockState().getValue(AccelerationRingBlock.FACING);
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();
        ArrayList<BlockPos> blockPoses = new ArrayList<>();
        checkPos.set(getBlockPos());
        boolean found = false;
        checkPos.move(direction);
        for (int i = 0; i < 8; i++) {
            checkPos.move(direction);
            BlockState checkState = level.getBlockState(checkPos);
            if (checkState.is(BlockTags.ANVIL) && !checkState.is(ModBlockTags.NON_MAGNETIC)) {
                blockPoses.add(checkPos.east(0));
            }
            if ((checkState.hasProperty(AccelerationRingBlock.HALF) && checkState.getValue(AccelerationRingBlock.HALF) == DirectionCube3x3PartHalf.MID_CENTER
                && checkState.getValue(AccelerationRingBlock.SWITCH) == IPowerComponent.Switch.ON
                && !checkState.getValue(AccelerationRingBlock.OVERLOAD)
                && checkState.getValue(AccelerationRingBlock.FACING) == direction)
                || (checkState.hasProperty(DeflectionRingBlock.HALF)
                && checkState.getValue(DeflectionRingBlock.HALF) == DirectionCube3x3PartHalf.MID_CENTER
                && checkState.getValue(DeflectionRingBlock.SWITCH) == IPowerComponent.Switch.ON
                && !checkState.getValue(DeflectionRingBlock.OVERLOAD))
            ) {
                found = true;
                break;
            }
        }
        if (!found) return;
        for (BlockPos pos : blockPoses) {
            BlockState fallState = level.getBlockState(pos);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            FallingBlockEntity.fall(level, pos, fallState).setHurtsEntities(2.0F, 40);
        }
        BlockPos end = getBlockPos().relative(direction.getOpposite(), 1);
        checkPos.move(direction, 2);
        AABB aabb = new AABB(
            checkPos.getX() + 1,
            checkPos.getY() + 1,
            checkPos.getZ() + 1,
            end.getX(),
            end.getY(),
            end.getZ()
        );
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, aabb,
            entity -> (entity instanceof FallingBlockEntity fallingBlockEntity && fallingBlockEntity.getBlockState().is(BlockTags.ANVIL) && !fallingBlockEntity.getBlockState().is(ModBlockTags.NON_MAGNETIC)
                || entity instanceof Projectile)
        );
        for (Entity entity : entities) {
            if (Math.abs(entity.getDeltaMovement().get(direction.getAxis())) > 16) {
                entity.setDeltaMovement(entity.getDeltaMovement().add(0, entity.getGravity(), 0));
                continue;
            }
            Vec3 fixMovement = getBlockPos().getCenter().subtract(
                entity instanceof FallingBlockEntity ? entity.position().add(0, 0.5, 0) : entity.position()
            );
            Vec3 deltaMovement = entity.getDeltaMovement();
            fixMovement = switch (direction.getAxis()) {
                case X -> fixMovement.multiply(0, 1, 1);
                case Y -> fixMovement.multiply(1, 0, 1);
                case Z -> fixMovement.multiply(1, 1, 0);
            };
            deltaMovement = switch (direction.getAxis()) {
                case X -> deltaMovement.multiply(1, 0, 0);
                case Y -> deltaMovement.multiply(0, 1, 0);
                case Z -> deltaMovement.multiply(0, 0, 1);
            };
            fixMovement = fixMovement.multiply(0.2, 0.2, 0.2);
            deltaMovement = deltaMovement.add(fixMovement);
            deltaMovement = deltaMovement.add(new Vec3(0.16f, 0.16f, 0.16f).multiply(Vec3.atLowerCornerOf(direction.getNormal())));
            entity.setDeltaMovement(deltaMovement);
            entity.setDeltaMovement(entity.getDeltaMovement().add(0, entity.getGravity(), 0));
        }
    }

    @SuppressWarnings("DuplicatedCode")
    public void attractGianAnvil() {
        assert level != null;
        if (level.getBlockState(getBlockPos().below(2)).hasProperty(GiantAnvilBlock.HALF) && level.getBlockState(getBlockPos().below(2)).getValue(GiantAnvilBlock.HALF) == Cube3x3PartHalf.TOP_CENTER)
            return;
        BlockPos giantAnvilPos = null;
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();
        checkPos.set(getBlockPos().below(2));
        for (int y = 0; y < 11; y++) {
            BlockState checkState = level.getBlockState(checkPos);
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
        Optional<FallingGiantAnvilEntity> fallingGiantAnvilEntity = level.getEntitiesOfClass(FallingGiantAnvilEntity.class, new AABB(
                getBlockPos().getX(),
                getBlockPos().getY() - 2,
                getBlockPos().getZ(),
                getBlockPos().getX() + 1,
                getBlockPos().getY() - 12,
                getBlockPos().getZ() + 1
            )).stream()
            .sorted(ENTITY_SORTER)
            .filter(entity -> vector2d.distance(entity.position().x, entity.position().z) <= 0.25)
            .findFirst();
        if (fallingGiantAnvilEntity.isPresent()) {
            if (giantAnvilPos != null && fallingGiantAnvilEntity.get().position().distanceTo(getBlockPos().getCenter()) < giantAnvilPos.getCenter().distanceTo(getBlockPos().getCenter())) {
                giantAnvilPos = BlockPos.containing(fallingGiantAnvilEntity.get().position());
            } else if (giantAnvilPos == null) {
                giantAnvilPos = BlockPos.containing(fallingGiantAnvilEntity.get().position());
            }
        }
        if (giantAnvilPos == null) return;
        checkPos.set(giantAnvilPos);
        checkPos.move(-1, 2, -1);
        while (checkPos.getY() < getBlockPos().getY() - 1) {
            for (int x = -1; x < 2; x++) {
                for (int z = -1; z < 2; z++) {
                    BlockState checked = level.getBlockState(checkPos);
                    if (!checked.canBeReplaced()) return;
                    checkPos.move(0, 0, 1);
                }
                checkPos.move(0, 0, -3);
                checkPos.move(1, 0, 0);
            }
            checkPos.move(-3, 1, 0);
        }
        Block block = level.getBlockState(giantAnvilPos.below()).getBlock();
        if (block instanceof GiantAnvilBlock giantAnvilBlock) {
            giantAnvilBlock.removePartsAndUpdate(level, giantAnvilPos.below());
        }
        BlockPos newPos = getBlockPos().below(4);
        for (Cube3x3PartHalf part : Cube3x3PartHalf.values()) {
            level.setBlockAndUpdate(newPos.offset(part.getOffset()), ModBlocks.GIANT_ANVIL.getDefaultState()
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
}
