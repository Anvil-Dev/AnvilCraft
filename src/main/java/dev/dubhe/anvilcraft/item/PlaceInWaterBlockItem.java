package dev.dubhe.anvilcraft.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public class PlaceInWaterBlockItem extends BlockItem {

    public PlaceInWaterBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(
        @NotNull Level level,
        @NotNull Player player,
        @NotNull InteractionHand usedHand
    ) {
        BlockHitResult fluidHit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        BlockHitResult blockHitResult2 = fluidHit.withPosition(fluidHit.getBlockPos());
        if (blockHitResult2.miss) return InteractionResultHolder.pass(player.getItemInHand(usedHand));
        InteractionResult interactionResult = super.useOn(new UseOnContext(player, usedHand, blockHitResult2));
        if (!interactionResult.indicateItemUse()) {
            blockHitResult2 = fluidHit.withPosition(fluidHit.getBlockPos().relative(player.getDirection()));
            interactionResult = super.useOn(new UseOnContext(player, usedHand, blockHitResult2));
        }
        return new InteractionResultHolder<>(interactionResult, player.getItemInHand(usedHand));
    }
}
