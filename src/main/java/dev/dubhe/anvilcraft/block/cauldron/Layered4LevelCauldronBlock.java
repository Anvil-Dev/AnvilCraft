package dev.dubhe.anvilcraft.block.cauldron;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.cauldron.CauldronInteractions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Optional;

public class Layered4LevelCauldronBlock extends BaseCauldronBlock {
    public static final MapCodec<Layered4LevelCauldronBlock> CODEC = RecordCodecBuilder.mapCodec(ins -> ins.group(
        BlockBehaviour.propertiesCodec(),
        CauldronInteractions.CODEC
            .fieldOf("interactions")
            .forGetter(block -> block.interactions)
    ).apply(ins, Layered4LevelCauldronBlock::new));

    public static final int MAX_LEVEL = 4;
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 1, Layered4LevelCauldronBlock.MAX_LEVEL);

    public Layered4LevelCauldronBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(Layered4LevelCauldronBlock.LEVEL, 1));
    }

    public Layered4LevelCauldronBlock(Properties properties, CauldronInteraction.Dispatcher interactions) {
        super(properties, interactions);
        this.registerDefaultState(this.stateDefinition.any().setValue(Layered4LevelCauldronBlock.LEVEL, 1));
    }

    public static void lowerFillLevel(BlockState state, Level level, BlockPos pos) {
        int i = state.getValue(Layered4LevelCauldronBlock.LEVEL) - 1;
        BlockState blockstate = i == 0 ? Blocks.CAULDRON.defaultBlockState() : state.setValue(Layered4LevelCauldronBlock.LEVEL, i);
        level.setBlockAndUpdate(pos, blockstate);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(blockstate));
    }

    @Override
    protected MapCodec<? extends AbstractCauldronBlock> codec() {
        return Layered4LevelCauldronBlock.CODEC;
    }

    @Override
    public boolean isFull(BlockState state) {
        return state.getValue(Layered4LevelCauldronBlock.LEVEL) == Layered4LevelCauldronBlock.MAX_LEVEL;
    }

    @Override
    protected double getContentHeight(BlockState state) {
        return (6.0 + state.getValue(Layered4LevelCauldronBlock.LEVEL) * 2.0) / 16.0;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return state.getValue(Layered4LevelCauldronBlock.LEVEL);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(Layered4LevelCauldronBlock.LEVEL);
    }

    public BlockState copyLevelFrom(BlockState otherCauldron) {
        return this.defaultBlockState().setValue(
            Layered4LevelCauldronBlock.LEVEL, Optional.of(otherCauldron)
            .filter(state -> state.getBlock() instanceof Layered4LevelCauldronBlock)
            .map(state -> state.getValue(Layered4LevelCauldronBlock.LEVEL))
            .orElse(1));
    }

    public BlockState fullFilled() {
        return this.defaultBlockState().setValue(Layered4LevelCauldronBlock.LEVEL, Layered4LevelCauldronBlock.MAX_LEVEL);
    }

    // Shapes

    @Override
    protected VoxelShape getEntityInsideCollisionShape(BlockState state, BlockGetter level, BlockPos pos, Entity entity) {
        return switch (state.getValue(Layered4LevelCauldronBlock.LEVEL)) {
            case 1 -> Layered4LevelCauldronBlock.LEVEL1;
            case 2 -> Layered4LevelCauldronBlock.LEVEL2;
            case 3 -> Layered4LevelCauldronBlock.LEVEL3;
            case 4 -> Layered4LevelCauldronBlock.LEVEL4;
            case null, default -> throw new IllegalStateException(
                "Unexpected value "
                + state.getValue(Layered4LevelCauldronBlock.LEVEL)
                + ". How did you get here?"
            );
        };
    }

    protected static final VoxelShape LEVEL1 = Block.box(2, 4, 2, 14, 6, 14);
    protected static final VoxelShape LEVEL2 = Block.box(2, 4, 2, 14, 9, 14);
    protected static final VoxelShape LEVEL3 = Block.box(2, 4, 2, 14, 12, 14);
    protected static final VoxelShape LEVEL4 = Block.box(2, 4, 2, 14, 15, 14);
}
