package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.chargecollector.ThermoManager;
import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.entity.ThermoelectricConverterBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class ThermoelectricConverterBlock extends BaseEntityBlock implements IHammerRemovable {
    public static final Direction[] DIRECTIONS = {Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST};

    public ThermoelectricConverterBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull RenderShape getRenderShape(@NotNull BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void neighborChanged(
            @NotNull BlockState state,
            @NotNull Level level,
            @NotNull BlockPos pos,
            @NotNull Block neighborBlock,
            @NotNull BlockPos neighborPos,
            boolean movedByPiston
    ) {
        ThermoManager.getInstance(level).removeThermalBlock(neighborPos);
        ThermoManager.getInstance(level).addThermoBlock(neighborPos, level.getBlockState(neighborPos));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(@NotNull BlockPos pos, @NotNull BlockState state) {
        return new ThermoelectricConverterBlockEntity(ModBlockEntities.THERMOELECTRIC_CONVERTER.get(), pos, state);
    }


    @Override
    public void onRemove(
            @NotNull BlockState state,
            @NotNull Level level,
            @NotNull BlockPos pos,
            @NotNull BlockState newState,
            boolean movedByPiston
    ) {
        Arrays.stream(DIRECTIONS).map(pos::relative).forEach(ThermoManager.getInstance(level)::removeThermalBlock);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            @NotNull Level level, @NotNull BlockState state, @NotNull BlockEntityType<T> type
    ) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(
                type, ModBlockEntities.THERMOELECTRIC_CONVERTER.get(),
                ((level1, blockPos, blockState, blockEntity) -> blockEntity.tick()));
    }
}
