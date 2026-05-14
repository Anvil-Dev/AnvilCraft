package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.init.ModCriterionTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class ItemMixin {
    @Inject(
        method = "use",
        at = @At(
            value = "HEAD"
        )
    )
    private void use(Level level, Player player, InteractionHand usedHand, CallbackInfoReturnable<InteractionResult> cir) {
        if (!level.isClientSide()) {
            ModCriterionTriggers.USE_ITEM.get().trigger((ServerPlayer) player, player.getItemInHand(usedHand).getItem());
        }
    }
}
