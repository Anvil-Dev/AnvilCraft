package dev.dubhe.anvilcraft.block.storage;

import dev.dubhe.anvilcraft.api.block.ITranscendiumBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class TranscendiumBlock extends Block implements ITranscendiumBlock {
    public TranscendiumBlock(Properties properties) {
        super(properties);
    }

    @Override
    public float getEnchantPowerBonus(BlockState state, BlockGetter level, BlockPos pos) {
        return 10f;
    }
}
