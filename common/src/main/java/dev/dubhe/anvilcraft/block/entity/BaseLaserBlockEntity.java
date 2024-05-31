package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.init.ModBlockTags;
import net.fabricmc.fabric.mixin.attachment.BlockEntityUpdateS2CPacketMixin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

public abstract class BaseLaserBlockEntity extends BlockEntity {
    public BlockPos irradiateBlockPos = null;

    public BaseLaserBlockEntity(BlockEntityType<?> type,
        BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    private boolean canPassThrough(Direction direction, BlockPos blockPos) {
        if (level == null) return false;
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

    private BlockPos getIrradiateBlockPos(int expectedLength, Direction direction, BlockPos originPos) {
        for (int length = 1; length <= expectedLength; length++) {
            if (!canPassThrough(direction, originPos.relative(direction, length)))
                return originPos.relative(direction, length);
        }
        return  originPos.relative(direction, expectedLength);
    }

    public void emitLaser(Direction direction) {
        if (level == null) return;
        BlockPos tempIrradiateBlockPos = getIrradiateBlockPos(128, direction, getBlockPos());
        if (irradiateBlockPos != null
            && level.getBlockEntity(irradiateBlockPos) instanceof BaseLaserBlockEntity baseLaserBlockEntity
            && tempIrradiateBlockPos != irradiateBlockPos) baseLaserBlockEntity.onCancelingIrradiation();
        irradiateBlockPos = tempIrradiateBlockPos;
        if (level.getBlockEntity(irradiateBlockPos) instanceof BaseLaserBlockEntity irradiatedLaserBlockEntity)
            irradiatedLaserBlockEntity.onIrradiated();
        AABB trackBoundingBox =
            new AABB(getBlockPos().relative(direction), irradiateBlockPos.relative(direction.getOpposite()));
    }

    public void onIrradiated() {
        irradiateBlockPos = null;
    }

    public void onCancelingIrradiation() {
    }

    public boolean isSwitch() {
        return false;
    }

    public Direction getDirection() {
        return Direction.UP;
    }

    /**
     * 为了适配forge中修改的渲染逻辑所添加的函数
     * 返回一个无限碰撞箱
     *
     * @return forge中为原版信标生成的无限碰撞箱
     */
    @SuppressWarnings("unused")
    public AABB getRenderBoundingBox() {
        return new AABB(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY,
            Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    }
}
