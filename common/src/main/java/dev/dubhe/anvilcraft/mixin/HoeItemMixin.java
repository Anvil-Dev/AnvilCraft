package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.item.enchantment.HarvestEnchantment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HoeItem.class)
public class HoeItemMixin {
    @Inject(method = "useOn", at = @At("RETURN"), cancellable = true)
    private void useOn(@NotNull UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return;
        }
        if (
            cir.getReturnValue() == InteractionResult.PASS
                && level.getBlockState(pos).getBlock() instanceof CropBlock
        ) {
            Player player = context.getPlayer();
            if (player == null) return;
            ItemStack item = context.getItemInHand();
            HarvestEnchantment.harvest(player, level, pos, item);
            item.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(context.getHand()));
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}
