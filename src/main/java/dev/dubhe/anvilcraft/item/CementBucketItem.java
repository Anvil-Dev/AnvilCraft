package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.init.ModBlocks;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CementBucketItem extends BucketItem {
    public final Color color;

    public CementBucketItem(Fluid content, Properties properties, Color color) {
        super(content, properties);
        this.color = color;
    }

    @Override
    public boolean emptyContents(@Nullable Player player, Level level, BlockPos pos, @Nullable BlockHitResult result, @Nullable ItemStack container) {
        if (result == null) {
            return super.emptyContents(player, level, pos, result, container);
        }
        BlockPos hitPos = result.getBlockPos();
        BlockState state = level.getBlockState(hitPos);
        if (state.is(Blocks.CAULDRON)) {
            level.setBlockAndUpdate(hitPos, ModBlocks.CEMENT_CAULDRONS.get(color).getDefaultState());
            playEmptySound(player, level, hitPos);
            return true;
        }
        return super.emptyContents(player, level, pos, result, container);
    }
}
