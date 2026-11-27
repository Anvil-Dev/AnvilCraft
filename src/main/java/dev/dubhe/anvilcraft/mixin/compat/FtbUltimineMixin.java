package dev.dubhe.anvilcraft.mixin.compat;

import dev.dubhe.anvilcraft.item.tool.AnvilHammerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.ftb.mods.ftbultimine.FTBUltimine", remap = false)
abstract class FtbUltimineMixin {
    @Inject(method = "isValidTool", at = @At("HEAD"), cancellable = true)
    private static void anvilcraft$skipHammerRemoval(
        Player player, BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> cir
    ) {
        if (AnvilHammerItem.isRemovingBlock(player, pos)) {
            cir.setReturnValue(false);
        }
    }
}
