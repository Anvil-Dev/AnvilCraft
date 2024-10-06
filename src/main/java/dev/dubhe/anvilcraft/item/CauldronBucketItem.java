package dev.dubhe.anvilcraft.item;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class CauldronBucketItem extends BucketItem {
    private final AbstractCauldronBlock cauldron;
    public CauldronBucketItem(Fluid fluid, Properties properties, AbstractCauldronBlock cauldron) {
        super(fluid, properties);
        this.cauldron = cauldron;
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
            BlockState newState = cauldron.defaultBlockState();
            if (cauldron instanceof LayeredCauldronBlock) {
                newState = newState.setValue(LayeredCauldronBlock.LEVEL, 3);
            }
            level.setBlockAndUpdate(
                hitPos,
                newState
            );
            playEmptySound(player, level, hitPos);
            return true;
        }
        return super.emptyContents(player, level, pos, result, container);
    }
}
