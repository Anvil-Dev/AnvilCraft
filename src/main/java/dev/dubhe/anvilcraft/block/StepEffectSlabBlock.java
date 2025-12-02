package dev.dubhe.anvilcraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

public class StepEffectSlabBlock extends SlabBlock {
    private final Consumer<Entity> stepAction;

    public StepEffectSlabBlock(Properties properties, Consumer<Entity> stepAction) {
        super(properties);
        this.stepAction = stepAction;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        stepAction.accept(entity);
    }

}
