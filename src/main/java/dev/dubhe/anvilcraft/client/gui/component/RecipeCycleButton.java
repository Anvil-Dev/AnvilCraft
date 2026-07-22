package dev.dubhe.anvilcraft.client.gui.component;

import dev.dubhe.anvilcraft.constant.SharedTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

import java.util.List;

public class RecipeCycleButton extends Button {
    public RecipeCycleButton(int x, int y, OnPress onPress) {
        super(
            x,
            y,
            18,
            18,
            Component.translatable("screen.anvilcraft.batch_crafter.switch_recipe"),
            onPress,
            DEFAULT_NARRATION
        );
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) return;
        this.isHovered = this.isMouseOver(mouseX, mouseY);
        if (this.isHovered()) {
            graphics.tooltip(
                Minecraft.getInstance().font,
                List.of(ClientTooltipComponent.create(this.getMessage().getVisualOrderText())),
                mouseX,
                mouseY,
                DefaultTooltipPositioner.INSTANCE,
                null
            );
        }
        int offsetY = this.active && this.isHovered() ? 16 : 0;
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            SharedTextures.REDO,
            this.getX() + 1,
            this.getY() + 1,
            0,
            offsetY,
            16,
            16,
            16,
            32
        );
    }
}
