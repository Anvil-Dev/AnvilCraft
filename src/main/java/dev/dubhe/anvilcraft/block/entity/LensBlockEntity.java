package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.api.laser.LaserComponentMap;
import dev.dubhe.anvilcraft.api.laser.LaserComponentTypes;
import dev.dubhe.anvilcraft.api.laser.LaserMiningComponent;
import dev.dubhe.anvilcraft.block.LensBlock;
import dev.dubhe.anvilcraft.block.state.LensType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

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
    protected void configureLaserComponents(LaserComponentMap components) {
        LensType type = getBlockState().getValue(LensBlock.TYPE);
        components.put(LaserComponentTypes.MINING, new LaserMiningComponent(type.getMiningEffect(), type != LensType.NONE));
    }

    @Override
    public void onIrradiated(BaseLaserBlockEntity source) {
        if (determineEmissionDirection(source)) {
            super.onIrradiated(source);
            this.enabled = true;
        }
    }

    @Override
    public void onCancelingIrradiation(BaseLaserBlockEntity source) {
        if (!this.irradiateSelfLaserBlockSet.contains(source)) return;
        super.onCancelingIrradiation(source);
        this.enabled = !this.irradiateSelfLaserBlockSet.isEmpty();
    }

    @Override
    public void resetLaserStateAfterMove() {
        this.enabled = false;
        super.resetLaserStateAfterMove();
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

}
