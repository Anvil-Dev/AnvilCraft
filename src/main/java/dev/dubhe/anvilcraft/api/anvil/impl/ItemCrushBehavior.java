package dev.dubhe.anvilcraft.api.anvil.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.event.anvil.AnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.util.AnvilUtil;

public class ItemCrushBehavior implements IAnvilBehavior {
    @Override
    public boolean handle(
            Level level,
            BlockPos hitBlockPos,
            BlockState hitBlockState,
            float fallDistance,
            AnvilFallOnLandEvent event
    ) {
        return AnvilUtil.itemProcess(
                ModRecipeTypes.ITEM_CRUSH_TYPE.get(),
                level,
                hitBlockPos,
                hitBlockPos.getCenter().add(0, 0.25, 0)
        );
    }
}
