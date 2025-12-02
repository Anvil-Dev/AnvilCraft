package dev.dubhe.anvilcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

public class StepEffectStairBlock extends StairBlock {
    private final Consumer<Entity> stepAction;

    public StepEffectStairBlock(BlockState baseState, Properties properties, Consumer<Entity> stepAction) {
        super(baseState, properties);
        this.stepAction = stepAction;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        stepAction.accept(entity);
    }

}
