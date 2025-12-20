package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.entity.LevitatingBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.util.GravityManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FallingBlock.class)
public abstract class FallingBlockMixin extends Block {

    public FallingBlockMixin(Properties properties) {
        super(properties);
    }

    @Shadow
    public static boolean isFree(BlockState state) {
        throw new AssertionError();
    }

    @Inject(
        method = "tick", at = @At("HEAD"), cancellable = true
    )
    private void anvilcraft$fall(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        // 1. 计算净重力
        GravityManager.GravityType gravityType = GravityManager.getFallingBlockGravityType(state.getBlock());
        Vec3 netGravity = GravityManager.getNetGravityVectorForFallingBlock(level, Vec3.atCenterOf(pos), gravityType);
        double gravitySq = netGravity.lengthSqr();

        // 如果受力极小，忽略
        if (gravitySq < 1.0E-5) {
            return;
        }

        // 2. 寻找主受力方向
        Direction primaryDir = Direction.getNearest(netGravity.x, netGravity.y, netGravity.z);
        BlockPos targetPos = pos.relative(primaryDir);
        BlockState targetState = level.getBlockState(targetPos);

        // 3. 判断是否可以移动
        boolean canMove = false;

        if (isFree(targetState)) {
            // 主方向是空的，直接起飞
            canMove = true;
        } else {
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
                    if (dir != primaryDir && isFree(level.getBlockState(pos.relative(dir)))) canMove = true;
                }
                // 检查 Y 轴
                if (!canMove && Math.abs(netGravity.y) > 1.0E-5) {
                    Direction dir = netGravity.y > 0 ? Direction.UP : Direction.DOWN;
                    if (dir != primaryDir && isFree(level.getBlockState(pos.relative(dir)))) canMove = true;
                }
                // 检查 Z 轴
                if (!canMove && Math.abs(netGravity.z) > 1.0E-5) {
                    Direction dir = netGravity.z > 0 ? Direction.SOUTH : Direction.NORTH;
                    if (dir != primaryDir && isFree(level.getBlockState(pos.relative(dir)))) canMove = true;
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
            } else {
                FallingBlockEntity.fall(level, pos, state);
            }
            ci.cancel();
        } else {
            ci.cancel();
        }
    }
}