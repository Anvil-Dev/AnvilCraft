package dev.dubhe.anvilcraft.anvil;

import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.block.MagnetBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.util.AnvilUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;

public class MagnetBlockBehavior implements IAnvilBehavior {
    @Override
    public boolean handle(Level level, BlockPos hitBlockPos, BlockState hitBlockState, float fallDistance, AnvilEvent.OnLand event) {
        if (hitBlockState.is(ModBlocks.MAGNET_BLOCK.get())) {
            boolean lit = hitBlockState.getValue(MagnetBlock.LIT);
            level.setBlockAndUpdate(hitBlockPos, ModBlocks.HOLLOW_MAGNET_BLOCK.get().defaultBlockState().setValue(MagnetBlock.LIT, lit));
            AnvilUtil.dropItems(
                Collections.singletonList(new ItemStack(ModItems.MAGNET_INGOT.get())),
                level,
                hitBlockPos.getCenter()
            );
            return true;
        } else if (hitBlockState.is(ModBlocks.FERRITE_CORE_MAGNET_BLOCK.get())) {
            boolean lit = hitBlockState.getValue(MagnetBlock.LIT);
            level.setBlockAndUpdate(hitBlockPos, ModBlocks.HOLLOW_MAGNET_BLOCK.get().defaultBlockState().setValue(MagnetBlock.LIT, lit));
            AnvilUtil.dropItems(
                Collections.singletonList(new ItemStack(Items.IRON_INGOT)),
                level,
                hitBlockPos.getCenter()
            );
            return true;
        }
        return false;
    }
}
