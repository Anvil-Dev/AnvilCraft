package dev.dubhe.anvilcraft.api.anvil.impl;

import dev.dubhe.anvilcraft.api.anvil.AnvilBehavior;
import dev.dubhe.anvilcraft.api.event.entity.AnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

import static dev.dubhe.anvilcraft.util.AnvilUtil.returnItems;

public class HitBeeNestBehavior implements AnvilBehavior {
    @Override
    public void handle(Level level, BlockPos pos, BlockState state, float fallDistance, AnvilFallOnLandEvent event) {
        if (!state.hasBlockEntity()) return;
        int honeyLevel = state.getValue(BeehiveBlock.HONEY_LEVEL);
        if (honeyLevel < BeehiveBlock.MAX_HONEY_LEVELS) return;
        BlockPos potPos = pos.below();
        BlockState pot = level.getBlockState(potPos);
        if (pot.is(Blocks.CAULDRON)) {
            level.setBlockAndUpdate(pos, state.setValue(BeehiveBlock.HONEY_LEVEL, 0));
            level.setBlockAndUpdate(potPos, ModBlocks.HONEY_CAULDRON.getDefaultState());
            level.setBlockAndUpdate(
                potPos, level.getBlockState(potPos).setValue(LayeredCauldronBlock.LEVEL, 1));
        } else {
            if (pot.is(ModBlocks.HONEY_CAULDRON.get())) {
                int cauldronHoneyLevel = pot.getValue(LayeredCauldronBlock.LEVEL);
                level.setBlockAndUpdate(pos, state.setValue(BeehiveBlock.HONEY_LEVEL, 0));
                if (cauldronHoneyLevel < LayeredCauldronBlock.MAX_FILL_LEVEL) {
                    level.setBlockAndUpdate(
                        potPos, pot.setValue(LayeredCauldronBlock.LEVEL, cauldronHoneyLevel + 1));
                } else {
                    level.setBlockAndUpdate(potPos, Blocks.CAULDRON.defaultBlockState());
                    returnItems(level, potPos, List.of(Items.HONEY_BLOCK.getDefaultInstance()));
                }
            }
        }
    }
}
