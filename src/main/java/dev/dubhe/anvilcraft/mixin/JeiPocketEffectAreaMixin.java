package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.client.gui.component.PocketEffectLayout;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.renderer.Rect2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(targets = "mezz.jei.library.plugins.vanilla.gui.InventoryEffectRendererGuiHandler", remap = false)
abstract class JeiPocketEffectAreaMixin {
    @Inject(method = "getGuiExtraAreas(Lnet/minecraft/client/gui/screens/inventory/EffectRenderingInventoryScreen;)Ljava/util/List;",
        at = @At("HEAD"), cancellable = true)
    private void anvilcraft$effectsAbovePockets(EffectRenderingInventoryScreen<?> screen,
                                              CallbackInfoReturnable<List<Rect2i>> cir) {
        if (PocketEffectLayout.isEnabled(screen)) {
            cir.setReturnValue(PocketEffectLayout.areas(screen, PocketEffectLayout.visibleEffects().size()));
        }
    }
}
