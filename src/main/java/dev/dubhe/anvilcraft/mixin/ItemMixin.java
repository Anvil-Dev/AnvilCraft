package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.util.ModEnchantmentHelper;
import dev.dubhe.anvilcraft.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class ItemMixin {
    @Inject(method = "mineBlock", at = @At("RETURN"))
    private void onMineBlock(
        ItemStack stack,
        Level level,
        BlockState state,
        BlockPos pos,
        LivingEntity miningEntity,
        CallbackInfoReturnable<Boolean> cir
    ) {
        if (level.isClientSide) return;
        ModEnchantmentHelper.onPostBreakBlock(
            (ServerLevel) level,
            stack,
            miningEntity,
            Util.convertToSlot(miningEntity.getUsedItemHand()),
            pos.getCenter(),
            state
        );
    }
}
