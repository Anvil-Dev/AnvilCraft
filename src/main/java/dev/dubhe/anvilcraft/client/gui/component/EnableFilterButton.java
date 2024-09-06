package dev.dubhe.anvilcraft.client.gui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.dubhe.anvilcraft.AnvilCraft;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Getter
public class EnableFilterButton extends Button {
    private final Supplier<Boolean> filterEnabled;
    private static final ResourceLocation YES = AnvilCraft.of("textures/gui/container/machine/button_yes.png");
    private static final ResourceLocation NO = AnvilCraft.of("textures/gui/container/machine/button_no.png");
    private static final MutableComponent defaultMessage = Component
        .translatable("screen.anvilcraft.button.record", Component.translatable("screen.anvilcraft.button.off"));

    public EnableFilterButton(int x, int y, OnPress onPress, Supplier<Boolean> filterEnabled) {
        super(x, y, 16, 16, defaultMessage, onPress, (var) -> defaultMessage);
        this.filterEnabled = filterEnabled;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (this.isHovered()) {
            guiGraphics.renderTooltip(Minecraft.getInstance().font,
                List.of(getMessage()), Optional.empty(), mouseX, mouseY);
        }
    }

    /**
     * 刷新
     */
    public void flush() {
        this.setMessage(
            Component.translatable("screen.anvilcraft.button.record",
                Component.translatable("screen.anvilcraft.button." + (this.getFilterEnabled().get() ? "on" : "off"))));
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        ResourceLocation location = this.filterEnabled.get() ? EnableFilterButton.YES : EnableFilterButton.NO;
        this.renderTexture(guiGraphics, location, this.getX(), this.getY(), 0, 0, 16, this.width, this.height, 16, 32);
    }

    @Override
    public void renderTexture(@NotNull GuiGraphics guiGraphics, @NotNull ResourceLocation texture,
                              int x, int y, int puOffset, int pvOffset, int textureDifference,
                              int width, int height, int textureWidth, int textureHeight) {
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
