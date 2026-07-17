package dev.dubhe.anvilcraft.client.gui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Optional;

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
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (this.visible && this.isHovered()) {
            graphics.renderTooltip(
                Minecraft.getInstance().font,
                List.of(this.getMessage()),
                Optional.empty(),
                mouseX,
                mouseY
            );
        }
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int offsetY = this.active && this.isHovered() ? 16 : 0;
        float color = this.active ? 1.0F : 0.45F;
        RenderSystem.setShaderColor(color, color, color, 1.0F);
        graphics.blit(
            SharedTextures.BUTTON_REDO,
            this.getX() + 1,
            this.getY() + 1,
            0,
            offsetY,
            16,
            16,
            16,
            32
        );
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
