package dev.dubhe.anvilcraft.block;

import dev.dubhe.anvilcraft.api.hammer.IHammerRemovable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class LavaCauldronBlock extends LayeredCauldronBlock implements IHammerRemovable {
    public static final Map<Item, CauldronInteraction> LAYERED_LAVA = CauldronInteraction.newInteractionMap();

    static {
        LAYERED_LAVA.put(
            Items.BUCKET,
            (blockState, level, blockPos, player, interactionHand, itemStack) -> CauldronInteraction.fillBucket(
                blockState,
                level,
                blockPos,
                player,
                interactionHand,
                itemStack,
                Items.LAVA_BUCKET.getDefaultInstance(),
                (state) -> state.getValue(LayeredCauldronBlock.LEVEL) == 3,
                SoundEvents.BUCKET_FILL
            )
        );
    }

    public LavaCauldronBlock(Properties properties) {
        super(properties, p -> false, LAYERED_LAVA);
    }

    @Override
    public void entityInside(
        @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Entity entity
    ) {
        if (this.isEntityInsideContent(state, pos, entity)) {
            entity.lavaHurt();
        }
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(
        @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull BlockState state
    ) {
        return new ItemStack(Items.CAULDRON);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(
        @NotNull BlockState state,
        @NotNull Level level,
        @NotNull BlockPos pos,
        @NotNull Block neighborBlock,
        @NotNull BlockPos neighborPos,
        boolean movedByPiston
    ) {
        if (level.isClientSide) {
            return;
        }
        if (state.getValue(LEVEL) == 3) {
            level.setBlockAndUpdate(pos, Blocks.LAVA_CAULDRON.defaultBlockState());
        }
    }
}
