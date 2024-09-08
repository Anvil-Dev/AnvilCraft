package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.item.enchantment.FellingEnchantment;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DiggerItem.class)
abstract class DiggerItemMixin {
    @Inject(method = "mineBlock", at = @At("RETURN"))
    private void digger(
        ItemStack stack, Level level,
        BlockState state, BlockPos pos,
        LivingEntity miningEntity, CallbackInfoReturnable<Boolean> cir
    ) {
        if (!(miningEntity instanceof Player player) || !state.is(BlockTags.LOGS)) return;
        FellingEnchantment.felling(player, level, pos, stack);
    }
}
