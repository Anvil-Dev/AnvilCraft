package dev.dubhe.anvilcraft.client.gui.component;

import dev.dubhe.anvilcraft.block.entity.ItemDetectorBlockEntity.Mode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public class CycleFilterModeButton extends Button {

    private final Supplier<Mode> filterMode;
    private static final MutableComponent DEFAULT_MESSAGE = Component.translatable(
        "screen.anvilcraft.button.filter_mode",
        Component.translatable("screen.anvilcraft.button.filter_mode_any"));

    public CycleFilterModeButton(int x, int y, OnPress onPress, Supplier<Mode> filterMode) {
        super(x, y, 16, 16, DEFAULT_MESSAGE, onPress, Button.DEFAULT_NARRATION);
        this.filterMode = filterMode;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
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
        Identifier location = this.filterMode.get().buttonTexture;
        this.renderTexture(
            graphics,
            location,
            this.getX(),
            this.getY(),
            0,
            0,
            16,
            this.width,
            this.height,
            16,
            32
        );
    }

    @Override
    public Component getMessage() {
        return Component.translatable("screen.anvilcraft.button.filter_mode",
            Component.translatable("screen.anvilcraft.button.filter_mode_" + this.filterMode.get().name().toLowerCase(Locale.ROOT)));
    }

    public void renderTexture(
        GuiGraphicsExtractor graphics,
        Identifier texture,
        int x,
        int y,
        int puOffset,
        int pvOffset,
        int textureDifference,
        int width,
        int height,
        int textureWidth,
        int textureHeight
    ) {
        int i = pvOffset;
        if (this.isHovered()) {
            i += textureDifference;
        }
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, puOffset, i, width, height, textureWidth, textureHeight);
    }

    public Mode cycle() {
        return this.filterMode.get().cycle();
    }
}
