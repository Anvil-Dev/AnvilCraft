package dev.dubhe.anvilcraft.block.heatable;

import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.api.heat.HeatRecorder;
import dev.dubhe.anvilcraft.api.heat.HeatTier;
import dev.dubhe.anvilcraft.api.heat.HeaterManager;
import dev.dubhe.anvilcraft.block.entity.heatable.HeatableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

public abstract class HeatableBlock extends Block {
    protected HeatableBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        Direction[] directions = Direction.values();
        if (HeatRecorder.getTier(level, pos, state).orElse(HeatTier.NORMAL) == HeatTier.NORMAL) return;
        for (Direction direction : directions) {
            BlockState tnt = level.getBlockState(pos.relative(direction));
            if (tnt.is(Blocks.TNT)) {
                tnt.onCaughtFire(level, pos.relative(direction), direction.getOpposite(), null);
                level.removeBlock(pos.relative(direction), false);
            }
        }
        HeaterManager.addHeatableBlock(pos, level);
    }

    protected abstract boolean hasBlockEntity();

    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState ignored, BlockEntityType<T> ignored1) {
        if (level.isClientSide()) return null;
        if (!this.hasBlockEntity()) return null;
        return (level1, pos, _, _) -> HeatableBlockEntity.tick(level1, pos);
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
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        if (!this.hasBlockEntity()) return 0;
        return Util.castSafely(level.getBlockEntity(pos), HeatableBlockEntity.class)
            .map(HeatableBlockEntity::getSignal)
            .orElse(0);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (placer instanceof ServerPlayer player) {
            if (player.gameMode.isCreative()) {
                Optional.ofNullable(level.getBlockEntity(pos))
                    .filter(HeatableBlockEntity.class::isInstance)
                    .map(HeatableBlockEntity.class::cast)
                    .ifPresent(be -> be.setDuration(1200));
            } else {
                Optional.ofNullable(level.getBlockEntity(pos))
                    .filter(HeatableBlockEntity.class::isInstance)
                    .map(HeatableBlockEntity.class::cast)
                    .ifPresent(be -> be.setDuration(200));
            }
        }
        super.setPlacedBy(level, pos, state, placer, stack);
    }

    @Override
    protected void neighborChanged(
        BlockState state,
        Level level,
        BlockPos pos,
        Block block,
        @Nullable Orientation orientation,
        boolean movedByPiston
    ) {
        if (HeatRecorder.getTier(level, pos, state).orElse(HeatTier.NORMAL) == HeatTier.NORMAL) return;
        for (Direction direction : Direction.values()) {
            BlockPos neighbourPos = pos.relative(direction);
            BlockState tnt = level.getBlockState(neighbourPos);
            if (tnt.is(Blocks.TNT)) {
                tnt.onCaughtFire(level, neighbourPos, direction.getOpposite(), null);
                level.removeBlock(neighbourPos, false);
            }
        }
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
    }
}
