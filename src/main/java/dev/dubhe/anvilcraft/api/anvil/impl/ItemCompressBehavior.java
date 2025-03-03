package dev.dubhe.anvilcraft.api.anvil.impl;

import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.event.anvil.AnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.block.HeaterBlock;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.util.AnvilUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

public class ItemCompressBehavior implements IAnvilBehavior {
    @Override
    public boolean handle(
        Level level,
        BlockPos hitBlockPos,
        BlockState hitBlockState,
        float fallDistance,
        AnvilFallOnLandEvent event
    ) {
        BlockState heaterIfPossible = level.getBlockState(hitBlockPos.below());
        if ((heaterIfPossible.is(ModBlocks.HEATER) && !heaterIfPossible.getValue(HeaterBlock.OVERLOAD))
            || (heaterIfPossible.is(Blocks.CAMPFIRE) && !heaterIfPossible.getValue(CampfireBlock.LIT))
        ) return false;
        return AnvilUtil.itemProcess(
            ModRecipeTypes.ITEM_COMPRESS_TYPE.get(),
            level,
            hitBlockPos,
            hitBlockPos.getCenter()
        );
    }
}
