package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.block.LensBlock;
import dev.dubhe.anvilcraft.block.state.LensType;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypes;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.util.BreakBlockUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayList;
import java.util.List;

public class LensBlockEntity extends BaseLaserBlockEntity {
    private boolean enabled = false;
    private Direction emittingDirection = Direction.NORTH;

    public LensBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    public Direction getFacing() {
        if (irradiateBlockPos != null) {
            Direction.Axis axis = getBlockState().getValue(LensBlock.AXIS);
            int diff = switch (axis) {
                case X -> irradiateBlockPos.getX() - getBlockPos().getX();
                case Y -> irradiateBlockPos.getY() - getBlockPos().getY();
                case Z -> irradiateBlockPos.getZ() - getBlockPos().getZ();
            };
            Direction.AxisDirection axisDir = diff > 0
                ? Direction.AxisDirection.POSITIVE
                : Direction.AxisDirection.NEGATIVE;
            return Direction.fromAxisAndDirection(axis, axisDir);
        }
        return Direction.fromAxisAndDirection(
            getBlockState().getValue(LensBlock.AXIS),
            Direction.AxisDirection.POSITIVE
        );
    }

    @Override
    protected int getBaseLaserLevel() {
        return 0;
    }

    @Override
    public void onIrradiated(BaseLaserBlockEntity source) {
        if (determineEmissionDirection(source)) {
            super.onIrradiated(source);
            this.enabled = true;
        }
    }

    private boolean determineEmissionDirection(BaseLaserBlockEntity source) {
        Direction.Axis axis = getBlockState().getValue(LensBlock.AXIS);
        BlockPos sourcePos = source.getBlockPos();
        BlockPos myPos = getBlockPos();
        boolean aligned = switch (axis) {
            case X -> sourcePos.getY() == myPos.getY() && sourcePos.getZ() == myPos.getZ();
            case Y -> sourcePos.getX() == myPos.getX() && sourcePos.getZ() == myPos.getZ();
            case Z -> sourcePos.getX() == myPos.getX() && sourcePos.getY() == myPos.getY();
        };
        if (!aligned) return false;

        int travel = switch (axis) {
            case X -> myPos.getX() - sourcePos.getX();
            case Y -> myPos.getY() - sourcePos.getY();
            case Z -> myPos.getZ() - sourcePos.getZ();
        };
        Direction.AxisDirection axisDir = travel > 0
            ? Direction.AxisDirection.POSITIVE
            : Direction.AxisDirection.NEGATIVE;
        this.emittingDirection = Direction.fromAxisAndDirection(axis, axisDir);
        return true;
    }

    @Override
    public void tick(Level level) {
        if (enabled) {
            emitLaser(emittingDirection);
        }
        super.tick(level);
        if (laserLevel == 0) enabled = false;
        resetState();
    }

    @Override
    public void deliverItem(List<ItemStack> drops, Direction direction, BlockPos sourceBlockPos) {
        if (!irradiateSelfLaserBlockSet.isEmpty()) {
            BaseLaserBlockEntity upstream = irradiateSelfLaserBlockSet.iterator().next();
            upstream.deliverItem(drops, direction, sourceBlockPos);
            return;
        }
        super.deliverItem(drops, direction, sourceBlockPos);
    }

    private List<ItemStack> getFrostDrops(ServerLevel serverLevel) {
        List<ItemStack> drops = new ArrayList<>();
        if (serverLevel.random.nextFloat() <= 0.25f) {
            drops.add(new ItemStack(ModItems.EXP_GEM.get()));
        }
        return drops;
    }

    private BlockPos scanIrradiateBlockPos(int expectedLength, Direction direction, BlockPos originPos) {
        for (int length = 1; length <= expectedLength; length++) {
            if (!this.canPassThrough(direction, originPos.relative(direction, length))) {
                return originPos.relative(direction, length);
            }
        }
        return originPos.relative(direction, expectedLength);
    }

    @Override
    public void emitLaser(Direction direction) {
        if (this.level == null) return;
        BlockPos tempIrradiateBlockPos = this.scanIrradiateBlockPos(
            this.maxTransmissionDistance, direction, this.getBlockPos()
        );
        if (!tempIrradiateBlockPos.equals(this.irradiateBlockPos)) {
            if (this.irradiateBlockPos != null) {
                BlockEntity oldBe = this.level.getBlockEntity(this.irradiateBlockPos);
                if (oldBe instanceof BaseLaserBlockEntity lastIrradiatedLaserBlockEntity) {
                    lastIrradiatedLaserBlockEntity.onCancelingIrradiation(this);
                }
            }
        }
        if (
            this.level.getBlockEntity(tempIrradiateBlockPos) instanceof BaseLaserBlockEntity irradiatedLaserBlockEntity
            && !this.isInIrradiateSelfLaserBlockSet(irradiatedLaserBlockEntity)
        ) {
            if (!irradiatedLaserBlockEntity.getIgnoreFace().contains(direction)) {
                this.level.updateNeighborsAt(tempIrradiateBlockPos, getBlockState().getBlock());
                irradiatedLaserBlockEntity.onIrradiated(this);
            }
        }
        this.updateIrradiateBlockPos(tempIrradiateBlockPos);

        if (!(this.level instanceof ServerLevel serverLevel)) return;
        this.updateLaserLevel(this.calculateLaserLevel());
        int hurt = Math.min(16, this.laserLevel - 4);
        if (hurt > 0) {
            Vec3 startPos = this.getBlockPos()
                .relative(direction)
                .getCenter()
                .add(-0.0625, -0.0625, -0.0625);
            AABB trackBoundingBox = new AABB(
                startPos,
                this.irradiateBlockPos.relative(direction.getOpposite())
                    .getCenter()
                    .add(0.0625, 0.0625, 0.0625)
            );
            this.level.getEntities(
                EntityTypeTest.forClass(LivingEntity.class),
                trackBoundingBox,
                Entity::isAlive
            ).forEach(livingEntity ->
                livingEntity.hurt(
                    ModDamageTypes.laser(this.level),
                    hurt
                )
            );
        }
        BlockState irradiateBlock = this.level.getBlockState(this.irradiateBlockPos);
        int cooldown = COOLDOWNS[Math.clamp(this.laserLevel / 4, 0, 4)];
        if (this.tickCount >= cooldown) {
            this.tickCount = 0;
            LensType lensType = getBlockState().getValue(LensBlock.TYPE);
            boolean isOreTarget = irradiateBlock.is(Tags.Blocks.ORES);
            boolean isLensSpecialTarget = lensType != LensType.NONE
                && (irradiateBlock.is(ModBlocks.VOID_STONE) || irradiateBlock.is(ModBlocks.EARTH_CORE_SHARD_ORE));
            if (isOreTarget || isLensSpecialTarget) {
                List<ItemStack> drops = switch (lensType) {
                    case ROYAL -> BreakBlockUtil.dropSilkTouch(serverLevel, this.irradiateBlockPos);
                    case FROST -> getFrostDrops(serverLevel);
                    case EMBER -> BreakBlockUtil.dropSmelt(serverLevel, this.irradiateBlockPos, 2);
                    default -> BreakBlockUtil.drop(serverLevel, this.irradiateBlockPos);
                };
                this.deliverItem(drops, direction, this.irradiateBlockPos);
            }
        }
    }
}
