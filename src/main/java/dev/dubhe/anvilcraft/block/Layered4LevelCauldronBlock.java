package dev.dubhe.anvilcraft.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.Optional;

public class Layered4LevelCauldronBlock extends AbstractCauldronBlock {
    public static final MapCodec<Layered4LevelCauldronBlock> CODEC = RecordCodecBuilder.mapCodec(
        ins -> ins.group(
                propertiesCodec(),
                CauldronInteraction.CODEC.fieldOf("interactions").forGetter(block -> block.interactions)
            )
            .apply(ins, Layered4LevelCauldronBlock::new)
    );

    public static final int MAX_LEVEL = 4;

    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 1, MAX_LEVEL);

    public Layered4LevelCauldronBlock(Properties properties, CauldronInteraction.InteractionMap interactions) {
        super(properties, interactions);
        this.registerDefaultState(this.stateDefinition.any().setValue(LEVEL, 1));
    }

    public static void lowerFillLevel(BlockState state, Level level, BlockPos pos) {
        int i = state.getValue(LEVEL) - 1;
        BlockState blockstate = i == 0 ? Blocks.CAULDRON.defaultBlockState() : state.setValue(LEVEL, i);
        level.setBlockAndUpdate(pos, blockstate);
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(blockstate));
    }

    @Override
    protected MapCodec<? extends AbstractCauldronBlock> codec() {
        return CODEC;
    }

    @Override
    public boolean isFull(BlockState state) {
        return state.getValue(LEVEL) == MAX_LEVEL;
    }

    @Override
    protected double getContentHeight(BlockState state) {
        return (6.0 + state.getValue(LEVEL) * 2.0) / 16.0;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return state.getValue(LEVEL);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    public BlockState copyLevelFrom(BlockState otherCauldron) {
        return this.defaultBlockState().setValue(LEVEL, Optional.of(otherCauldron)
            .filter(state -> state.getBlock() instanceof Layered4LevelCauldronBlock)
            .map(state -> state.getValue(LEVEL))
            .orElse(1));
    }

    public BlockState fullFilled() {
        return this.defaultBlockState().setValue(LEVEL, MAX_LEVEL);
    }
}
