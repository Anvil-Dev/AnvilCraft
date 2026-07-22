package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.power.IPowerComponent;
import dev.dubhe.anvilcraft.block.AccelerationRingBlock;
import dev.dubhe.anvilcraft.block.DeflectionRingBlock;
import dev.dubhe.anvilcraft.block.GiantAnvilBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.block.state.DirectionCube3x3PartHalf;
import dev.dubhe.anvilcraft.entity.FallingGiantAnvilEntity;
import dev.dubhe.anvilcraft.entity.LevitatingBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.util.GravityManager;
import dev.dubhe.anvilcraft.util.GravityType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({FallingBlock.class, GiantAnvilBlock.class})
public abstract class FallingBlockMixin extends Block {

    public FallingBlockMixin(Properties properties) {
        super(properties);
    }

    @Inject(
        method = "tick", at = @At("HEAD"), cancellable = true
    )
    private void anvilcraft$fall(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        // 1. 计算净重力
        GravityType gravityType = GravityManager.getFallingBlockGravityType(state.getBlock());
        Vec3 netGravity = GravityManager.getNetGravityVectorForFallingBlock(level, Vec3.atCenterOf(pos), gravityType);
        double gravitySq = netGravity.lengthSqr();

        // 如果受力极小，忽略
        if (gravitySq < 1.0E-5) {
            ci.cancel();
            return;
        }

        // 2. 寻找主受力方向
        Direction primaryDir = Direction.getNearest(netGravity.x, netGravity.y, netGravity.z);
        // Preserve every vanilla falling-block edge case while gravity is still primarily downward.
        if (primaryDir == Direction.DOWN) return;

        GiantAnvilBlock giantAnvil = (Object) this instanceof GiantAnvilBlock block ? block : null;
        if (giantAnvil != null) {
            if (!state.hasProperty(GiantAnvilBlock.HALF)
                || state.getValue(GiantAnvilBlock.HALF) != Cube3x3PartHalf.BOTTOM_CENTER) {
                return;
            }
            if (anvilcraft$isHeldByRing(level, pos, state)) {
                ci.cancel();
                return;
            }
        }

        BlockPos targetPos = this.anvilcraft$getBlockingPos(level, pos, primaryDir, giantAnvil != null);

        // 3. 判断是否可以移动
        boolean canMove = targetPos == null;

        if (canMove) {
            // 主方向是空的，直接起飞
        } else {
            BlockState targetState = level.getBlockState(targetPos);
            // 主方向被阻挡，检查是否可以克服摩擦力滑行
            double normalForce = Math.abs(netGravity.get(primaryDir.getAxis()));
            double tangentialForce = Math.sqrt(Math.max(0, gravitySq - normalForce * normalForce));

            // 获取阻挡方块的摩擦系数
            float friction = targetState.getFriction(level, targetPos, null);
            double grip = 1.0 - friction;

            // 只有切向力足够大，才能克服摩擦力开始滑动
            if (tangentialForce > normalForce * grip * 2.0) {
                // 摩擦力无法束缚，检查滑动方向是否有空位
                // 遍历其它轴寻找出路

                // 检查 X 轴
                if (!canMove && Math.abs(netGravity.x) > 1.0E-5) {
                    Direction dir = netGravity.x > 0 ? Direction.EAST : Direction.WEST;
                    if (dir != primaryDir
                        && this.anvilcraft$getBlockingPos(level, pos, dir, giantAnvil != null) == null) {
                        canMove = true;
                    }
                }
                // 检查 Y 轴
                if (!canMove && Math.abs(netGravity.y) > 1.0E-5) {
                    Direction dir = netGravity.y > 0 ? Direction.UP : Direction.DOWN;
                    if (dir != primaryDir
                        && this.anvilcraft$getBlockingPos(level, pos, dir, giantAnvil != null) == null) {
                        canMove = true;
                    }
                }
                // 检查 Z 轴
                if (!canMove && Math.abs(netGravity.z) > 1.0E-5) {
                    Direction dir = netGravity.z > 0 ? Direction.SOUTH : Direction.NORTH;
                    if (dir != primaryDir
                        && this.anvilcraft$getBlockingPos(level, pos, dir, giantAnvil != null) == null) {
                        canMove = true;
                    }
                }
            } else {
                // 摩擦力太大，被死死按在墙上/地板上/天花板上
                canMove = false;
            }
        }

        // 5. 执行转换
        if (canMove) {
            if (state.is(ModBlocks.LEVITATION_POWDER_BLOCK.get())) {
                LevitatingBlockEntity.levitate(level, pos, state);
            } else if (giantAnvil != null) {
                BlockPos mainPartPos = giantAnvil.getMainPartPos(pos, state);
                BlockState mainPartState = level.getBlockState(mainPartPos);
                if (!mainPartState.is(giantAnvil)
                    || !mainPartState.hasProperty(GiantAnvilBlock.HALF)
                    || mainPartState.getValue(GiantAnvilBlock.HALF) != Cube3x3PartHalf.MID_CENTER) {
                    ci.cancel();
                    return;
                }
                giantAnvil.removePartsAndUpdate(level, pos);
                FallingBlockEntity entity = FallingGiantAnvilEntity.fall(
                    level, mainPartPos, mainPartState, false
                );
                entity.setHurtsEntities(10.0F, AnvilCraft.CONFIG.giantAnvilFallDamageMax);
            } else {
                FallingBlockEntity entity = FallingBlockEntity.fall(level, pos, state);
                ((FallingBlock) (Object) this).falling(entity);
            }
            ci.cancel();
        } else {
            ci.cancel();
        }
    }

    @Unique
    private BlockPos anvilcraft$getBlockingPos(
        Level level,
        BlockPos pos,
        Direction direction,
        boolean giantAnvil
    ) {
        if (!giantAnvil) {
            BlockPos targetPos = pos.relative(direction);
            return FallingBlock.isFree(level.getBlockState(targetPos)) ? null : targetPos;
        }
        for (Cube3x3PartHalf part : Cube3x3PartHalf.values()) {
            if (!anvilcraft$isOnFace(part, direction)) continue;
            BlockPos targetPos = pos.offset(part.getOffset()).relative(direction);
            if (!FallingBlock.isFree(level.getBlockState(targetPos))) return targetPos;
        }
        return null;
    }

    @Unique
    private static boolean anvilcraft$isOnFace(Cube3x3PartHalf part, Direction direction) {
        return switch (direction) {
            case DOWN -> part.getOffsetY() == 0;
            case UP -> part.getOffsetY() == 2;
            case WEST -> part.getOffsetX() == -1;
            case EAST -> part.getOffsetX() == 1;
            case NORTH -> part.getOffsetZ() == -1;
            case SOUTH -> part.getOffsetZ() == 1;
        };
    }

    @Unique
    private static boolean anvilcraft$isHeldByRing(ServerLevel level, BlockPos pos, BlockState state) {
        BlockState ringState = level.getBlockState(
            pos.subtract(state.getValue(GiantAnvilBlock.HALF).getOffset()).above(3)
        );
        boolean isHeldByAcceleration = ringState.getBlock() instanceof AccelerationRingBlock
                                       && ringState.getValue(AccelerationRingBlock.HALF)
                                          == DirectionCube3x3PartHalf.BOTTOM_CENTER
                                       && ringState.getValue(AccelerationRingBlock.SWITCH)
                                          == IPowerComponent.Switch.ON
                                       && !ringState.getValue(AccelerationRingBlock.OVERLOAD)
                                       && ringState.getValue(AccelerationRingBlock.FACING) == Direction.UP;
        boolean isHeldByDeflection = ringState.getBlock() instanceof DeflectionRingBlock
                                     && ringState.getValue(DeflectionRingBlock.HALF)
                                        == DirectionCube3x3PartHalf.BOTTOM_CENTER
                                     && ringState.getValue(DeflectionRingBlock.SWITCH)
                                        == IPowerComponent.Switch.ON
                                     && !ringState.getValue(DeflectionRingBlock.OVERLOAD)
                                     && ringState.getValue(DeflectionRingBlock.FACING).getAxis()
                                        == Direction.Axis.Y;
        return isHeldByAcceleration || isHeldByDeflection;
    }
}
