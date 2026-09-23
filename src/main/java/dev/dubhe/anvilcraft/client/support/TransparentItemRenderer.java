package dev.dubhe.anvilcraft.client.support;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

public final class TransparentItemRenderer extends PictureInPictureRenderer<TransparentItemRenderer.State> {
    private @Nullable GpuTextureView texture;

    public TransparentItemRenderer(MultiBufferSource.BufferSource buffer) {
        super(buffer);
    }

    public static void extract(ItemStack stack, GuiGraphicsExtractor graphics, int x, int y, float alpha) {
        if (stack.isEmpty()) return;
        var client = Minecraft.getInstance();
        var item = new TrackingItemStackRenderState();
        client.getItemModelResolver().updateForTopItem(item, stack, ItemDisplayContext.GUI, client.level, client.player, 0);
        var gui = new GuiItemRenderState(new Matrix3x2f(graphics.pose()), item, x, y, graphics.peekScissorStack());
        var bounds = gui.oversizedItemBounds();
        if (bounds == null) bounds = new ScreenRectangle(x, y, 16, 16);
        graphics.submitPictureInPictureRenderState(new State(gui, bounds.left(), bounds.top(), bounds.right(), bounds.bottom(), alpha));
    }

    @Override
    public Class<State> getRenderStateClass() {
        return State.class;
    }

    @Override
    protected void renderToTexture(State state, PoseStack pose) {
        this.texture = RenderSystem.outputColorTextureOverride;
        pose.scale(1, -1, -1);
        var gui = state.item();
        pose.translate((gui.x() + 8 - (state.x0() + state.x1()) / 2F) / 16F,
            ((state.y0() + state.y1()) / 2F - gui.y() - 8) / 16F, 0);
        var client = Minecraft.getInstance();
        client.gameRenderer.getLighting().setupFor(gui.itemStackRenderState().usesBlockLight()
            ? Lighting.Entry.ITEMS_3D : Lighting.Entry.ITEMS_FLAT);
        var dispatcher = client.gameRenderer.getFeatureRenderDispatcher();
        gui.itemStackRenderState().submit(pose, dispatcher.getSubmitNodeStorage(), 15728880, OverlayTexture.NO_OVERLAY, 0);
        dispatcher.renderAllFeatures();
    }

    @Override
    protected void blitTexture(State state, GuiRenderState graphics) {
        int alpha = Math.clamp(Math.round(state.alpha() * 255), 0, 255);
        graphics.addBlitToCurrentLayer(new BlitRenderState(RenderPipelines.GUI_TEXTURED_PREMULTIPLIED_ALPHA,
            TextureSetup.singleTexture(this.texture, RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST)),
            state.pose(), state.x0(), state.y0(), state.x1(), state.y1(), 0, 1, 1, 0,
            ARGB.color(alpha, alpha, alpha, alpha), state.scissorArea(), null));
    }

    @Override
    protected float getTranslateY(int height, int guiScale) {
        return height / 2F;
    }

    @Override
    protected String getTextureLabel() {
        return "anvilcraft_transparent_item";
    }

    public record State(GuiItemRenderState item, int x0, int y0, int x1, int y1, float alpha) implements PictureInPictureRenderState {
        @Override
        public float scale() {
            return 16;
        }

        @Override
        public Matrix3x2f pose() {
            return this.item.pose();
        }

        @Override
        public @Nullable ScreenRectangle scissorArea() {
            return this.item.scissorArea();
        }

        @Override
        public @Nullable ScreenRectangle bounds() {
            return this.item.bounds();
        }
    }
}
