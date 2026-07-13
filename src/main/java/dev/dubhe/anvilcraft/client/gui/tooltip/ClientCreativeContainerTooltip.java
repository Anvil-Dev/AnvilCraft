package dev.dubhe.anvilcraft.client.gui.tooltip;

import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import dev.dubhe.anvilcraft.inventory.tooltip.CreativeContainerTooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

public class ClientCreativeContainerTooltip implements ClientTooltipComponent {
    private static final int ICON_SIZE = 16;
    private static final int ROW_HEIGHT = 18;
    private static final int TEXT_X_OFFSET = 20;
    private static final int TEXT_Y_OFFSET = 4;
    private final CreativeContainerTooltip tooltip;

    public ClientCreativeContainerTooltip(CreativeContainerTooltip tooltip) {
        this.tooltip = tooltip;
    }

    @Override
    public int getHeight(Font font) {
        return this.tooltip.entries().size() * ROW_HEIGHT;
    }

    @Override
    public int getWidth(Font font) {
        int width = 0;
        for (CreativeContainerTooltip.Entry entry : this.tooltip.entries()) {
            width = Math.max(width, TEXT_X_OFFSET + font.width(entry.text()));
        }
        return width;
    }

    @Override
    public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
        int row = 0;
        for (CreativeContainerTooltip.Entry entry : this.tooltip.entries()) {
            int rowY = y + row * ROW_HEIGHT;
            if (entry.isFluid()) {
                renderFluidIcon(graphics, entry.fluid(), x, rowY);
            } else {
                graphics.item(entry.item(), x, rowY);
            }
            graphics.text(font, entry.text(), x + TEXT_X_OFFSET, rowY + TEXT_Y_OFFSET, 0xFFFFFFFF, false);
            row++;
        }
    }

    private static void renderFluidIcon(GuiGraphicsExtractor graphics, FluidStack fluid, int x, int y) {
        FluidResource resource = FluidResource.of(fluid);
        FluidModel model = FluidRenderHelper.getModel(
            Minecraft.getInstance().getModelManager().getFluidStateModelSet(),
            resource.getFluid()
        );
        var tintSource = model.fluidTintSource();
        if (tintSource == null) return;
        TextureAtlasSprite sprite = model.stillMaterial().sprite();
        int tint = tintSource.colorAsStack(resource.toStack(1));
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, ICON_SIZE, ICON_SIZE, tint);
    }
}
