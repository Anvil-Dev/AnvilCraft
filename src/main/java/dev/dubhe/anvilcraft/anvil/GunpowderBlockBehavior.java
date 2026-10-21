package dev.dubhe.anvilcraft.anvil;

import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.block.GunpowderBlock;
import dev.dubhe.anvilcraft.block.multipart.AbstractMultiPartBlock;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.util.MagnetUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class GunpowderBlockBehavior implements IAnvilBehavior {
    @Override
    public boolean handle(Level level, BlockPos hitBlockPos, BlockState hitBlockState, float fallDistance, AnvilEvent.OnLand event) {
        final BlockPos pos = event.getPos();
        final BlockState blockState = level.getBlockState(pos);
        GunpowderBlock block = (GunpowderBlock) hitBlockState.getBlock();
        block.explosion(level, hitBlockPos);
        int distance = (int) Math.ceil(event.getFallDistance()) + 1;
        BlockPos above = pos;
        for (int i = 1; i < distance + 1; i++) {
            above = above.above();
            if (!level.getBlockState(above).isAir()) {
                break;
            }
        }
        above = above.below();
        if (blockState.is(BlockTags.ANVIL) && !(blockState.getBlock() instanceof AbstractMultiPartBlock<?>)
            && !blockState.is(ModBlocks.SPECTRAL_ANVIL)) {
            level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(above, blockState);
            MagnetUtil.animateAscendingBlock(level, pos.getBottomCenter(), above, blockState);
        }
        return true;
    }
}
