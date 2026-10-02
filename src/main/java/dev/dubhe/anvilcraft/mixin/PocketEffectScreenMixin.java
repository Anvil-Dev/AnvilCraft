package dev.dubhe.anvilcraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.dubhe.anvilcraft.client.gui.component.PocketEffectLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

@Mixin(EffectsInInventory.class)
abstract class PocketEffectScreenMixin {
    @Shadow
    @Final
    private AbstractContainerScreen<?> screen;
    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    protected abstract Component getEffectName(MobEffectInstance effect);

    @ModifyReturnValue(method = "canSeeEffects", at = @At("RETURN"))
    private boolean anvilcraft$effectsAbovePockets(boolean visible) {
        return PocketEffectLayout.isEnabled(this.screen) ? !PocketEffectLayout.isHiddenByRecipeBook(this.screen) : visible;
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
    private void anvilcraft$effectsAbovePockets(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (!PocketEffectLayout.isEnabled(this.screen) || this.minecraft.level == null) return;
        ci.cancel();
        if (PocketEffectLayout.isHiddenByRecipeBook(this.screen)) return;
        List<MobEffectInstance> effects = PocketEffectLayout.visibleEffects();
        List<Rect2i> areas = PocketEffectLayout.areas(this.screen, effects.size());
        for (int index = 0; index < effects.size(); index++) {
            Rect2i area = areas.get(index);
            final MobEffectInstance effect = effects.get(index);
            graphics.pose().pushMatrix();
            graphics.pose().translate(area.getX(), area.getY());
            float scale = area.getWidth() / 32.0F;
            graphics.pose().scale(scale, scale);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Identifier.withDefaultNamespace(effect.isAmbient()
                ? "container/inventory/effect_background_ambient" : "container/inventory/effect_background"), 0, 0, 32, 32);
            if (!IClientMobEffectExtensions.of(effect).renderInventoryIcon(effect, this.screen, graphics, 7, 0, 0)) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Gui.getMobEffectSprite(effect.getEffect()), 7, 7, 18, 18);
            }
            graphics.pose().popMatrix();
        }
        for (int index = effects.size() - 1; index >= 0; index--) {
            if (!areas.get(index).contains(mouseX, mouseY)) continue;
            final MobEffectInstance effect = effects.get(index);
            List<Component> tooltip = List.of(this.getEffectName(effect),
                MobEffectUtil.formatDuration(effect, 1, this.minecraft.level.tickRateManager().tickrate()));
            tooltip = ClientHooks.getEffectTooltip(this.screen, effect, tooltip);
            graphics.setTooltipForNextFrame(this.screen.getFont(), tooltip, Optional.empty(), mouseX, mouseY);
            break;
        }
    }
}
