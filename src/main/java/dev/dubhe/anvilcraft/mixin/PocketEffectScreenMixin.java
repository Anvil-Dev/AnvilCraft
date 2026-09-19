package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.client.gui.component.PocketEffectLayout;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.neoforged.neoforge.client.ClientHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

@Mixin(EffectRenderingInventoryScreen.class)
abstract class PocketEffectScreenMixin<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    protected PocketEffectScreenMixin(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Shadow
    protected abstract void renderBackgrounds(GuiGraphics graphics, int renderX, int offset,
                                             Iterable<MobEffectInstance> effects, boolean large);

    @Shadow
    protected abstract void renderIcons(GuiGraphics graphics, int renderX, int offset,
                                       Iterable<MobEffectInstance> effects, boolean large);

    @Shadow
    protected abstract Component getEffectName(MobEffectInstance effect);

    @Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
    private void anvilcraft$effectsAbovePockets(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (!PocketEffectLayout.isEnabled(this) || this.minecraft == null || this.minecraft.level == null) return;
        ci.cancel();
        List<MobEffectInstance> effects = PocketEffectLayout.visibleEffects();
        List<Rect2i> areas = PocketEffectLayout.areas(this, effects.size());
        for (int index = 0; index < effects.size(); index++) {
            Rect2i area = areas.get(index);
            graphics.pose().pushPose();
            graphics.pose().translate(area.getX(), area.getY(), 0);
            float scale = area.getWidth() / 32.0F;
            graphics.pose().scale(scale, scale, 1);
            graphics.pose().translate(0, -this.topPos, 0);
            List<MobEffectInstance> entry = List.of(effects.get(index));
            this.renderBackgrounds(graphics, 0, 0, entry, false);
            this.renderIcons(graphics, 0, 0, entry, false);
            graphics.pose().popPose();
        }
        for (int index = effects.size() - 1; index >= 0; index--) {
            if (!areas.get(index).contains(mouseX, mouseY)) continue;
            MobEffectInstance effect = effects.get(index);
            List<Component> tooltip = List.of(this.getEffectName(effect),
                MobEffectUtil.formatDuration(effect, 1, this.minecraft.level.tickRateManager().tickrate()));
            tooltip = ClientHooks.getEffectTooltip((EffectRenderingInventoryScreen<?>) (Object) this, effect, tooltip);
            graphics.renderTooltip(this.font, tooltip, Optional.empty(), mouseX, mouseY);
            break;
        }
    }
}
