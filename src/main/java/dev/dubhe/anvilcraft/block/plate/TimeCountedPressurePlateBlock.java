package dev.dubhe.anvilcraft.block.plate;

import dev.dubhe.anvilcraft.block.entity.plate.TimeCountedPressurePlateBlockEntity;
import dev.dubhe.anvilcraft.init.ModBlockEntities;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class TimeCountedPressurePlateBlock extends PressurePlateBlock implements EntityBlock {
    public static final IntegerProperty POWER = BlockStateProperties.POWER;
    public final int needTick;

    public TimeCountedPressurePlateBlock(BlockSetType type, Properties properties, int needTick) {
        super(type, properties);
        this.needTick = needTick;
        this.registerDefaultState(this.stateDefinition.any().setValue(POWER, 0).setValue(BlockStateProperties.POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWER);
    }

    @Override
    protected int getSignalStrength(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof TimeCountedPressurePlateBlockEntity blockEntity ? blockEntity.getSignalStrength() : 0;
    }

    @Override
    protected int getSignalForState(BlockState state) {
        return state.getValue(POWER);
    }

    @Override
    protected BlockState setSignalForState(BlockState state, int signal) {
        return state.setValue(POWER, Math.clamp(signal, 0, 15)).setValue(BlockStateProperties.POWERED, signal > 0);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TimeCountedPressurePlateBlockEntity(pos, state, needTick);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide()) return null;
        if (!(blockEntityType == ModBlockEntities.TIME_COUNTED_PRESSURE_PLATE.get())) return null;
        return (level1, pos, state1, blockEntity) -> ((TimeCountedPressurePlateBlockEntity) blockEntity).tick(level1, pos);
    }

    protected int getPressedTime() {
        return 1;
    }
}
