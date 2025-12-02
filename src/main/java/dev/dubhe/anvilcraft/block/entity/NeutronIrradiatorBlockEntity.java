package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.block.NeutronIrradiatorBlock;
import dev.dubhe.anvilcraft.entity.LevitatingBlockEntity;
import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class NeutronIrradiatorBlockEntity extends BlockEntity {
    public static final int RANGE = 7;

    public NeutronIrradiatorBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.NEUTRON_IRRADIATOR.get(), pos, blockState);
    }

    //    public static void tick(Level level, BlockPos pos, BlockState state, NeutronIrradiatorBlockEntity blockEntity) {
    //        if (level.getGameTime() % 2 != 0) return;
    //
    //        // 定义检测范围 - 中子辐照器自身及上方7格范围
    //        AABB detectionArea = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + RANGE + 1, pos.getZ() + 1);
    //        // 获取范围内的所有实体
    //        level.getEntitiesOfClass(
    //            Entity.class,
    //            detectionArea,
    //            entity -> entity instanceof ItemEntity || entity instanceof FallingBlockEntity || entity instanceof LivingEntity
    //        ).forEach(entity -> {
    //            if (entity instanceof FallingBlockEntity) { // 特殊处理下落方块瞬间落地
    //                double distanceToIrradiator = entity.getY() - (pos.getY() + 0.9);
    //                entity.setDeltaMovement(entity.getDeltaMovement().x, -7, entity.getDeltaMovement().z);
    //                // 距离10倍增加fallDistance
    //                entity.fallDistance += 10 * Math.abs(distanceToIrradiator);
    //            } else if (entity instanceof LevitatingBlockEntity) { // 特殊处理漂浮粉块实体，增加重力速度到3倍
    //                entity.setDeltaMovement(entity.getDeltaMovement().x, entity.getDeltaMovement().y * 3, entity.getDeltaMovement().z);
    //            } else if (entity.getDeltaMovement().y < 0) { // 增加其他实体重力速度到3倍，fallDistance也3倍
    //                entity.setDeltaMovement(entity.getDeltaMovement().x, entity.getDeltaMovement().y * 3, entity.getDeltaMovement().z);
    //                entity.fallDistance *= 3;
    //            } else if (entity.getDeltaMovement().y > 0) { // 上升的实体更快的减速
    //                entity.setDeltaMovement(entity.getDeltaMovement().x, entity.getDeltaMovement().y / 3, entity.getDeltaMovement().z);
    //            }
    //        });
    //    }

    public static NeutronIrradiatorBlockEntity createBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        return new NeutronIrradiatorBlockEntity(pos, blockState);
    }

    public static boolean isInIrradiatorRange(Level level, BlockPos pos) {
        // 检查下方最多7格内是否有中子辐照器
        for (int i = 0; i <= RANGE; i++) {
            BlockPos checkPos = pos.below(i);
            BlockState blockState = level.getBlockState(checkPos);

            if (blockState.getBlock() instanceof NeutronIrradiatorBlock) {
                BlockEntity blockEntity = level.getBlockEntity(checkPos);
                if (blockEntity != null && blockEntity.getType() == ModBlockEntities.NEUTRON_IRRADIATOR.get()) {
                    return true;
                }
            }
        }
        return false;
    }
}