package dev.dubhe.anvilcraft.client.gui.tooltip;

import dev.dubhe.anvilcraft.client.support.FluidRenderHelper;
import dev.dubhe.anvilcraft.inventory.tooltip.CreativeContainerTooltip;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

public class ClientCreativeContainerTooltip implements ClientTooltipComponent {
    private static final int ICON_SIZE = 16;
    private static final int ROW_HEIGHT = 16;
    private static final int HEADER_HEIGHT = 10;
    private static final int TEXT_X_OFFSET = 17;
    private static final int TEXT_Y_OFFSET = 3;
    private final CreativeContainerTooltip tooltip;

    public ClientCreativeContainerTooltip(CreativeContainerTooltip tooltip) {
        this.tooltip = tooltip;
    }

    @Override
    public int getHeight(Font font) {
        int height = this.tooltip.entries().size() * ROW_HEIGHT;
        for (int index = 0; index < this.tooltip.entries().size(); index++) {
            if (this.hasHeader(index)) height += HEADER_HEIGHT;
        }
        return height;
    }

    @Override
    public int getWidth(Font font) {
        int width = 0;
        for (CreativeContainerTooltip.Entry entry : this.tooltip.entries()) {
            width = Math.max(width, TEXT_X_OFFSET + font.width(entry.text()));
            width = Math.max(width, font.width(header(entry)));
        }
        return width;
    }

    @Override
    public void extractImage(Font font, int x, int y, int w, int h, GuiGraphicsExtractor graphics) {
        int rowY = y;
        int index = 0;
        for (CreativeContainerTooltip.Entry entry : this.tooltip.entries()) {
            if (this.hasHeader(index)) {
                graphics.text(font, header(entry).copy().withStyle(ChatFormatting.BLUE), x, rowY, -1, true);
                rowY += HEADER_HEIGHT;
            }
            if (entry.isFluid()) {
                ClientCreativeContainerTooltip.renderFluidIcon(graphics, entry.fluid(), x, rowY);
            } else {
                graphics.item(entry.item(), x, rowY);
            }
            graphics.text(
                font, entry.text(), x + ClientCreativeContainerTooltip.TEXT_X_OFFSET, rowY + ClientCreativeContainerTooltip.TEXT_Y_OFFSET,
                0xFFFFFFFF, true
            );
            rowY += ROW_HEIGHT;
            index++;
        }
    }

    private boolean hasHeader(int index) {
        return index == 0 || this.tooltip.entries().get(index).isFluid() != this.tooltip.entries().get(index - 1).isFluid();
    }

    private static Component header(CreativeContainerTooltip.Entry entry) {
        return Component.translatable(entry.isFluid()
            ? "tooltip.anvilcraft.fluid_tank.fluid" : "tooltip.anvilcraft.creative_crate.item");
    }

    private static void renderFluidIcon(GuiGraphicsExtractor graphics, FluidStack fluid, int x, int y) {
        FluidResource resource = FluidResource.of(fluid);
        FluidModel model = FluidRenderHelper.getModel(
            Minecraft.getInstance().getModelManager().getFluidStateModelSet(),
            resource.getFluid()
        );
        var tintSource = model.fluidTintSource();
        TextureAtlasSprite sprite = model.stillMaterial().sprite();
        int tint = tintSource == null ? -1 : tintSource.colorAsStack(resource.toStack(1));
        graphics.blitSprite(
            RenderPipelines.GUI_TEXTURED, sprite, x, y, ClientCreativeContainerTooltip.ICON_SIZE, ClientCreativeContainerTooltip.ICON_SIZE,
            ARGB.opaque(tint)
        );
    }
}
