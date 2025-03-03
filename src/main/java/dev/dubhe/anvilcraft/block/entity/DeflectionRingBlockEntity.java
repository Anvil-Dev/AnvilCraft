package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.power.IPowerConsumer;
import dev.dubhe.anvilcraft.api.power.PowerComponentType;
import dev.dubhe.anvilcraft.api.power.PowerGrid;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2d;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class DeflectionRingBlockEntity extends BlockEntity implements IPowerConsumer {
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

    public DeflectionRingBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.DEFLECTION_RING.get(), pos, blockState);
    }

    private DeflectionRingBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static DeflectionRingBlockEntity createBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        return new DeflectionRingBlockEntity(type, pos, blockState);
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
        if (!level.getBlockState(getBlockPos()).hasProperty(DeflectionRingBlock.HALF))
            return PowerComponentType.INVALID;
        if (level.getBlockState(getBlockPos()).getValue(DeflectionRingBlock.HALF).equals(DirectionCube3x3PartHalf.MID_CENTER))
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
        return state.getValue(DeflectionRingBlock.SWITCH) == Switch.ON && !state.getValue(DeflectionRingBlock.OVERLOAD);
    }

    public void tick() {
        if (level == null) return;
        if (level.isClientSide) {
            if (isWork())
                accelerate();
        }
        if (grid == null) return;
        BlockState state = getBlockState();
        if (!state.getValue(DeflectionRingBlock.HALF).equals(DirectionCube3x3PartHalf.MID_CENTER)) return;
        if (!(state.getBlock() instanceof DeflectionRingBlock block)) return;
        if (grid.isWorking() && state.getValue(DeflectionRingBlock.OVERLOAD)) {
            block.updateState(level, getBlockPos(), DeflectionRingBlock.OVERLOAD, false, 3);
        } else if (!grid.isWorking() && !state.getValue(DeflectionRingBlock.OVERLOAD)) {
            block.updateState(level, getBlockPos(), DeflectionRingBlock.OVERLOAD, true, 3);
        }
        if (!isWork()) return;
        if (state.getValue(DeflectionRingBlock.FACING).getAxis().equals(Direction.Axis.Y))
            attractGianAnvil();
        accelerate();
    }

    public void accelerate() {
        if (level == null) return;
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, new AABB(getBlockPos()),
            entity -> (entity instanceof FallingBlockEntity fallingBlockEntity && fallingBlockEntity.getBlockState().is(BlockTags.ANVIL) && !fallingBlockEntity.getBlockState().is(ModBlockTags.NON_MAGNETIC)
                || entity instanceof Projectile)
        );
        for (Entity entity : entities) {
            Vec3 deltaMovement = entity.getDeltaMovement();
            Double speed = deltaMovement.length();
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
        boolean isFallingGiantAnvil = false;
        if (fallingGiantAnvilEntity.isPresent()) {
            if (giantAnvilPos != null && fallingGiantAnvilEntity.get().position().distanceTo(getBlockPos().getCenter()) < giantAnvilPos.getCenter().distanceTo(getBlockPos().getCenter())) {
                isFallingGiantAnvil = true;
                giantAnvilPos = BlockPos.containing(fallingGiantAnvilEntity.get().position());
            } else if (giantAnvilPos == null) {
                isFallingGiantAnvil = true;
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
        BlockPos newPos = getBlockPos().below(4);
        for (Cube3x3PartHalf part : Cube3x3PartHalf.values()) {
            if (!isFallingGiantAnvil)
                level.setBlock(giantAnvilPos.offset(part.getOffset()), Blocks.AIR.defaultBlockState(), 2);
        }
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
        return getBlockState().getValue(DeflectionRingBlock.SWITCH) == Switch.ON ? 256 : 0;
    }
}
