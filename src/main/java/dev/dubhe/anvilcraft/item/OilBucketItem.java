package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class OilBucketItem extends BucketItem {
    public OilBucketItem(Properties properties) {
        super(ModFluids.OIL.get(), properties);
    }

    @Override
    public boolean emptyContents(
        @Nullable Player player,
        Level level,
        BlockPos pos,
        @Nullable BlockHitResult result,
        @Nullable ItemStack container
    ) {
        if (result == null) {
            return super.emptyContents(player, level, pos, result, container);
        }
        BlockPos hitPos = result.getBlockPos();
        BlockState state = level.getBlockState(hitPos);
        if (state.is(Blocks.CAULDRON)) {
            level.setBlockAndUpdate(
                hitPos,
                ModBlocks.OIL_CAULDRON.getDefaultState()
                    .setValue(LayeredCauldronBlock.LEVEL, 3)
            );
            return true;
        }
        return super.emptyContents(player, level, pos, result, container);
    }
}
