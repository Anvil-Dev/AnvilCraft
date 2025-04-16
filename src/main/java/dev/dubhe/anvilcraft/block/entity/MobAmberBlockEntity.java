package dev.dubhe.anvilcraft.block.entity;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

public class MobAmberBlockEntity extends HasMobBlockEntity {
    protected MobAmberBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    public static @NotNull MobAmberBlockEntity createBlockEntity(
        BlockEntityType<?> type,
        BlockPos pos,
        BlockState blockState
    ) {
        return new MobAmberBlockEntity(type, pos, blockState);
    }

    @OnlyIn(Dist.CLIENT)
    public void clientTick(ClientLevel level, BlockPos blockPos) {
        Entity entity = this.getOrCreateDisplayEntity(level);
        if (!(entity instanceof LivingEntity)) return;
        LivingEntity displayEntity = (LivingEntity) entity;
        displayEntity.setPos(blockPos.getCenter());

        displayEntity.setYHeadRot(level.getBlockState(blockPos).getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot());
        displayEntity.yBodyRot = displayEntity.yHeadRot;
        displayEntity.yHeadRotO = displayEntity.yHeadRot;
        displayEntity.yBodyRotO = displayEntity.yBodyRot;

    }
}
