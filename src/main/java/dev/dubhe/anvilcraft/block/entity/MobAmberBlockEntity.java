package dev.dubhe.anvilcraft.block.entity;

import dev.dubhe.anvilcraft.init.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jspecify.annotations.Nullable;

public class MobAmberBlockEntity extends HasMobBlockEntity {
    protected MobAmberBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Override
    protected @Nullable Entity createDefaultEntity(Level level) {
        return EntityType.MOOSHROOM.create(level, EntitySpawnReason.SPAWN_ITEM_USE);
    }

    public static MobAmberBlockEntity createBlockEntity(
        BlockEntityType<?> type,
        BlockPos pos,
        BlockState blockState
    ) {
        return new MobAmberBlockEntity(type, pos, blockState);
    }

    // @OnlyIn(Dist.CLIENT)
    public void clientTick(Level level, BlockPos blockPos) {
        BlockState state = level.getBlockState(blockPos);
        Entity entity = this.getOrCreateDisplayEntity(level);
        if (!state.is(ModBlocks.MOB_AMBER_BLOCK) || !(entity instanceof LivingEntity displayEntity)) return;

        displayEntity.setPos(blockPos.getCenter());
        this.setYRot(displayEntity, state.getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot());
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
