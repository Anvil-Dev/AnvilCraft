package dev.dubhe.anvilcraft.block.heatable;

import dev.dubhe.anvilcraft.api.heat.HeatRecorder;
import dev.dubhe.anvilcraft.api.heat.HeaterManager;
import dev.dubhe.anvilcraft.block.entity.heatable.HeatableBlockEntity;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public abstract class HeatableBlock extends Block {
    protected HeatableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        HeaterManager.addHeatableBlock(pos, level);
    }

    protected abstract boolean hasBlockEntity();

    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState ignored, BlockEntityType<T> ignored1) {
        if (level.isClientSide) return null;
        if (!this.hasBlockEntity()) return null;
        return (level1, pos, it, it1) -> HeatableBlockEntity.tick(level1, pos);
    }

    public Optional<BlockState> getPrevTier(Level level, BlockPos pos, BlockState state) {
        return HeatRecorder.getPrevTierHeatableBlock(level, pos, state)
            .map(Block::defaultBlockState);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return this.hasBlockEntity();
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (!this.hasBlockEntity()) return 0;
        return Util.castSafely(level.getBlockEntity(pos), HeatableBlockEntity.class)
            .map(HeatableBlockEntity::getSignal)
            .orElse(0);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (placer instanceof ServerPlayer player && player.gameMode.isCreative()) {
            Optional.ofNullable(level.getBlockEntity(pos))
                .filter(HeatableBlockEntity.class::isInstance)
                .map(HeatableBlockEntity.class::cast)
                .ifPresent(be -> be.setDuration(1200));
        }
        super.setPlacedBy(level, pos, state, placer, stack);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (level.getBlockState(neighborPos).is(Blocks.TNT)) {
            TntBlock.explode(level, neighborPos);
            level.removeBlock(neighborPos, false);
        }
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
    }
}
