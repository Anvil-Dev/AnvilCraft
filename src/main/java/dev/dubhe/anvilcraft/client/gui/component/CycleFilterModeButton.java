package dev.dubhe.anvilcraft.client.gui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static dev.dubhe.anvilcraft.block.entity.ItemDetectorBlockEntity.Mode;

public class CycleFilterModeButton extends Button {

    private final Supplier<Mode> filterMode;
    private static final String LOCATION_PREFIX = "textures/gui/container/machine/button_";
    private static final String LOCATION_SUFFIX = ".png";
    private static final MutableComponent DEFAULT_MESSAGE = Component.translatable(
        "screen.anvilcraft.button.filter_mode",
        Component.translatable("screen.anvilcraft.button.filter_mode_any"));

    public CycleFilterModeButton(int x, int y, OnPress onPress, Supplier<Mode> filterMode) {
        super(x, y, 16, 16, DEFAULT_MESSAGE, onPress, Button.DEFAULT_NARRATION);
        this.filterMode = filterMode;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (this.isHovered()) {
            guiGraphics.renderTooltip(
                Minecraft.getInstance().font, List.of(getMessage()), Optional.empty(), mouseX, mouseY);
        }
    }

    private String buttonPath() {
        return this.filterMode.get().buttonPath;
    }

    @Override
    public Component getMessage() {
        return Component.translatable("screen.anvilcraft.button.filter_mode",
            Component.translatable("screen.anvilcraft.button.filter_mode_" + this.buttonPath()));
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        ResourceLocation location = AnvilCraft.of(LOCATION_PREFIX + this.buttonPath() + LOCATION_SUFFIX);
        this.renderTexture(guiGraphics, location, this.getX(), this.getY(), 0, 0, 16, this.width, this.height, 16, 32);
    }

    public void renderTexture(
        GuiGraphics guiGraphics,
        ResourceLocation texture,
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
        RenderSystem.enableDepthTest();
        guiGraphics.blit(texture, x, y, puOffset, i, width, height, textureWidth, textureHeight);
    }

    public Mode cycle() {
        return this.filterMode.get().cycle();
    }
}
