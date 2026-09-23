package dev.dubhe.anvilcraft.client.gui.component;

import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import dev.dubhe.anvilcraft.util.FluidUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

import java.util.Optional;

public class FluidDisplayWidget extends AbstractWidget {
    private final ResourceHandler<FluidResource> handler;
    private final Runnable onClick;

    public FluidDisplayWidget(int x, int y, int width, int height, ResourceHandler<FluidResource> handler, Runnable onClick) {
        super(x, y, width, height, Component.empty());
        this.handler = handler;
        this.onClick = onClick;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        var resource = this.handler.getResource(0);
        int amount = this.handler.getAmountAsInt(0);
        int capacity = this.handler.getCapacityAsInt(0, resource);
        if (resource.isEmpty() || amount <= 0 || capacity <= 0) return;
        int filled = Math.clamp((long) amount * this.height / capacity, 1, this.height);
        var client = Minecraft.getInstance();
        var stack = resource.toStack(amount);
        var model = FluidRenderHelper.getModel(client.getModelManager().getFluidStateModelSet(), resource.getFluid());
        int color = model.fluidTintSource() == null ? -1 : model.fluidTintSource().colorAsStack(stack);
        int bottom = this.getY() + this.height;
        for (int x = 0; x < this.width; x += 16) {
            for (int y = 0; y < filled; y += 16) {
                int tileHeight = Math.min(16, filled - y);
                int tileTop = bottom - y - tileHeight;
                graphics.enableScissor(this.getX() + x, tileTop, Math.min(this.getRight(), this.getX() + x + 16), tileTop + tileHeight);
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, model.stillMaterial().sprite(),
                    this.getX() + x, tileTop, 16, 16, color);
                graphics.disableScissor();
            }
        }
        if (this.isHovered()) {
            graphics.fill(RenderPipelines.GUI, this.getX(), this.getY(), this.getRight(), bottom, 0x80FFFFFF);
            graphics.setTooltipForNextFrame(client.font, FluidUtil.getTooltip(stack, capacity,
                client.options.advancedItemTooltips ? TooltipFlag.ADVANCED.asCreative() : TooltipFlag.NORMAL.asCreative()),
                Optional.empty(), mouseX, mouseY);
        }
    }

    @Override
    protected boolean isValidClickButton(MouseButtonInfo event) {
        return event.button() == 1;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        this.onClick.run();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}
