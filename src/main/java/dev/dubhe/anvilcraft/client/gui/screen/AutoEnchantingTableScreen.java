package dev.dubhe.anvilcraft.client.gui.screen;

import dev.dubhe.anvilcraft.client.gui.component.FluidDisplayWidget;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import dev.dubhe.anvilcraft.inventory.AutoEnchantingTableMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

import java.util.List;

public class AutoEnchantingTableScreen extends AbstractContainerScreen<AutoEnchantingTableMenu> {
    private static final Identifier BACKGROUND = SharedTextures.bg("machine", "auto_enchanting_table");

    public AutoEnchantingTableScreen(AutoEnchantingTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(
            new FluidDisplayWidget(
                this.leftPos + 151, this.topPos + 16,
                18, 56,
                this.menu.getBlockEntity().getFluidHandler(),
                (fluidHandler) -> Component.literal("Exp: ")
                    .append(String.valueOf(fluidHandler.getAmountAsInt(0)))
                    .append("/")
                    .append(String.valueOf(fluidHandler.getCapacityAsInt(0, FluidResource.EMPTY)))
            )
        );
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            BACKGROUND,
            this.leftPos,
            this.topPos,
            0,
            0,
            this.getImageWidth(),
            this.getImageHeight(),
            256,
            256
        );
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        int x = (this.imageWidth - this.font.width(this.title)) / 2;
        graphics.text(this.font, this.title, x, 2, -12566464, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        if (this.isHovering(151, 16, 18, 56, mouseX, mouseY)) {
            graphics.tooltip(
                Minecraft.getInstance().font,
                List.of(
                    ClientTooltipComponent.create(
                        Component.literal("Exp: ")
                            .append(String.valueOf(this.menu.getBlockEntity().getFluidHandler().getAmountAsInt(0)))
                            .append("/")
                            .append(String.valueOf(this.menu.getBlockEntity().getFluidHandler().getCapacityAsInt(0, FluidResource.EMPTY)))
                            .append("mB")
                            .getVisualOrderText()
                    )
                ),
                mouseX,
                mouseY,
                DefaultTooltipPositioner.INSTANCE,
                null
            );
        }
    }
}
