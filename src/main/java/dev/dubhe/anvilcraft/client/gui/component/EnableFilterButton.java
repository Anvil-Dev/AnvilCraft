package dev.dubhe.anvilcraft.client.gui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

@Getter
public class EnableFilterButton extends Button {
    private final Supplier<Boolean> filterEnabled;
    private static final MutableComponent defaultMessage = Component.translatable(
        "screen.anvilcraft.button.record", Component.translatable("screen.anvilcraft.button.off"));

    public EnableFilterButton(int x, int y, OnPress onPress, Supplier<Boolean> filterEnabled) {
        super(x, y, 16, 16, defaultMessage, onPress, (var) -> defaultMessage);
        this.filterEnabled = filterEnabled;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (this.isHovered()) {
            Screen screen = Minecraft.getInstance().screen;
            if (screen != null) {
                screen.setTooltipForNextRenderPass(getMessage());
            }
        }
    }

    /**
     * 刷新
     */
    public void flush() {
        this.setMessage(Component.translatable(
            "screen.anvilcraft.button.record",
            Component.translatable(
                "screen.anvilcraft.button." + (this.getFilterEnabled().get() ? "on" : "off"))));
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        ResourceLocation location = this.filterEnabled.get() ? SharedTextures.BUTTON_YES : SharedTextures.BUTTON_NO;
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

    public boolean next() {
        return !this.filterEnabled.get();
    }
}
