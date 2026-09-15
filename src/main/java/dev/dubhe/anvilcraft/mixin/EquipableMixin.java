package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.inventory.PocketInventory;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Equipable.class)
interface EquipableMixin {
    @Inject(method = "swapWithEquipmentSlot", at = @At("HEAD"), cancellable = true)
    private void anvilcraft$lockFilledPockets(Item item, Level level, Player player, InteractionHand hand,
                                            CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (((Equipable) this).getEquipmentSlot() == EquipmentSlot.LEGS && PocketInventory.isLocked(player)) {
            cir.setReturnValue(InteractionResultHolder.fail(player.getItemInHand(hand)));
        }
    }
}
