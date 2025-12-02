package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.init.block.ModBlocks;
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
        BlockState state = level.getBlockState(blockPos);
        Entity entity = getOrCreateDisplayEntity(level);
        if (!state.is(ModBlocks.MOB_AMBER_BLOCK) || !(entity instanceof LivingEntity displayEntity)) return;

        displayEntity.setPos(blockPos.getCenter());
        setYRot(displayEntity, state.getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot());
    }

    private void setYRot(LivingEntity displayEntity, float rotY) {
        if (displayEntity.yHeadRot != rotY) {
            displayEntity.yHeadRot = rotY;
            displayEntity.yHeadRotO = rotY;
        }
        if (displayEntity.yBodyRot != rotY) {
            displayEntity.yBodyRot = rotY;
            displayEntity.yBodyRotO = rotY;
        }
    }
}
