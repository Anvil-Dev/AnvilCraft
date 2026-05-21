package dev.dubhe.anvilcraft.block.storage;

import dev.dubhe.anvilcraft.init.block.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class ResentfulAmberBlock extends MobAmberBlock {
    public ResentfulAmberBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return ModBlockEntities.RESENTFUL_AMBER_BLOCK.create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        Level level, BlockState state, BlockEntityType<T> type) {
        if (!level.isClientSide()) {
            return null;
        }
        return createTickerHelper(
            type,
            ModBlockEntities.RESENTFUL_AMBER_BLOCK.get(),
            (level1, blockPos, _, blockEntity) -> blockEntity.clientTick(level1, blockPos)
        );
    }
}
