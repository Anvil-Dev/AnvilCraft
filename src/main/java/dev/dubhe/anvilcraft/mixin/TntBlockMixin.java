package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.item.MultitoolItem;
import net.minecraft.core.BlockPos;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TntBlock.class)
abstract class TntBlockMixin extends Block {
    public TntBlockMixin(Properties properties) {
        super(properties);
    }

    @Inject(
        method = "useItemOn",
        at = @At("HEAD"),
        cancellable = true
    )
    private void useItemOn(
        ItemStack stack,
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        InteractionHand hand,
        BlockHitResult hitResult,
        CallbackInfoReturnable<ItemInteractionResult> cir
    ) {
        if (stack.is(ModItems.MULTITOOL_ITEM) && MultitoolItem.getMode(stack) == MultitoolItem.FLINT_AND_STEEL_MODE) {
            onCaughtFire(state, level, pos, hitResult.getDirection(), player);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
            Item item = stack.getItem();
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));

            player.awardStat(Stats.ITEM_USED.get(item));
            cir.setReturnValue(ItemInteractionResult.sidedSuccess(level.isClientSide));
        }
    }
}
