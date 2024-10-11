package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.util.Utils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CraftingMenu.class)
public class CraftingMenuMixin {
    @Shadow
    @Final
    private ContainerLevelAccess access;

    @Inject(method = "stillValid", at = @At("RETURN"), cancellable = true)
    private void stillValid(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue()) {
            cir.setReturnValue(
                access.evaluate(
                    (level, blockPos) ->
                        level.getBlockState(blockPos).is(ModBlocks.TRANSPARENT_CRAFTING_TABLE.get())
                            && Utils.canInteract(player, blockPos, 8),
                    true
                )
            );
        }
    }
}
