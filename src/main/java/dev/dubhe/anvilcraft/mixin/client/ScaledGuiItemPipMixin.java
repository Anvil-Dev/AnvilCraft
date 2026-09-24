package dev.dubhe.anvilcraft.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.dubhe.anvilcraft.client.support.GatewayGuiProjection;
import dev.dubhe.anvilcraft.client.support.ScaledGuiItemAtlases;
import dev.dubhe.anvilcraft.client.support.TransparentItemRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.OversizedItemRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PictureInPictureRenderer.class)
abstract class ScaledGuiItemPipMixin {
    @WrapMethod(method = "prepare")
    private void anvilcraft$projectGateway(PictureInPictureRenderState state, GuiRenderState gui, int scale, Operation<Void> original) {
        GuiItemRenderState item = switch (state) {
            case OversizedItemRenderState oversized -> oversized.guiItemRenderState();
            case TransparentItemRenderer.State transparent -> transparent.item();
            default -> null;
        };
        int effectiveScale = item == null ? scale
            : ScaledGuiItemAtlases.pipScale(item, state, scale, RenderSystem.getDevice().getMaxTextureSize());
        if (item != null && GatewayGuiProjection.isProjected(item.itemStackRenderState())) {
            GatewayGuiProjection.withProjection(GatewayGuiProjection.pip(state), () -> original.call(state, gui, effectiveScale));
        } else {
            original.call(state, gui, effectiveScale);
        }
    }
}
