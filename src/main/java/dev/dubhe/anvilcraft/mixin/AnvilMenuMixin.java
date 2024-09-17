package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.init.ModBlocks;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilMenu.class)
public class AnvilMenuMixin {
    @Inject(method = "isValidBlock", at = @At("HEAD"), cancellable = true)
    private void voj(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(state.is(ModBlocks.GIANT_ANVIL.get()) || state.is(BlockTags.ANVIL));
    }
}
