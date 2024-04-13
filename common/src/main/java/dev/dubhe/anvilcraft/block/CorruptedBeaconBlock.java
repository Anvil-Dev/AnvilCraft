package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.block.entity.CorruptedBeaconBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeaconBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CorruptedBeaconBlock extends BeaconBlock {
    public CorruptedBeaconBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult use(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Player player,
        @NotNull InteractionHand hand,
        @NotNull BlockHitResult hit
    ) {
        return InteractionResult.PASS;
    }

    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return CorruptedBeaconBlockEntity.createBlockEntity(ModBlockEntities.CORRUPTED_BEACON.get(), pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
        @NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> blockEntityType
    ) {
        return CorruptedBeaconBlock.createTickerHelper(
            blockEntityType, ModBlockEntities.CORRUPTED_BEACON.get(), CorruptedBeaconBlockEntity::tick
        );
    }
}
