package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import dev.dubhe.anvilcraft.block.better.BetterAbstractCauldronBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

import com.mojang.serialization.MapCodec;
import org.jetbrains.annotations.NotNull;

public class MeltGemCauldron extends BetterAbstractCauldronBlock implements IHammerRemovable {

    public MeltGemCauldron(Properties properties) {
        super(properties, CauldronInteraction.EMPTY);
    }

    @Override
    protected MapCodec<? extends AbstractCauldronBlock> codec() {
        return simpleCodec(MeltGemCauldron::new);
    }

    @Override
    protected double getContentHeight(@NotNull BlockState state) {
        return 0.9375;
    }

    @Override
    public boolean isFull(@NotNull BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(@NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos) {
        return 3;
    }
}
