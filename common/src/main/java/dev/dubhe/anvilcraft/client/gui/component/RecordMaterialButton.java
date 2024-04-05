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

@Getter
public class RecordMaterialButton extends Button {
    private boolean record;
    private static final ResourceLocation YES = AnvilCraft.of("textures/gui/container/button_yes.png");
    private static final ResourceLocation NO = AnvilCraft.of("textures/gui/container/button_no.png");
    private static final MutableComponent defaultMessage = Component.translatable("screen.anvilcraft.button.record", Component.translatable("screen.anvilcraft.button.off"));

    public RecordMaterialButton(int x, int y, OnPress onPress, boolean record) {
        super(x, y, 16, 16, defaultMessage, onPress, (var) -> defaultMessage);
        this.record = record;
    }
    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (this.isHovered()) {
            guiGraphics.renderTooltip(Minecraft.getInstance().font, List.of(getMessage()), Optional.empty(), mouseX, mouseY);
        }
    }

    public void setRecord(boolean record) {
        this.record = record;
        this.setMessage(Component.translatable("screen.anvilcraft.button.record", Component.translatable("screen.anvilcraft.button." + (this.isRecord() ? "on" : "off"))));
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        ResourceLocation location = this.record ? RecordMaterialButton.YES : RecordMaterialButton.NO;
        this.renderTexture(guiGraphics, location, this.getX(), this.getY(), 0, 0, 16, this.width, this.height, 16, 32);
    }

    @Override
    public void renderTexture(@NotNull GuiGraphics guiGraphics, @NotNull ResourceLocation texture, int x, int y, int uOffset, int vOffset, int textureDifference, int width, int height, int textureWidth, int textureHeight) {
        int i = vOffset;
        if (this.isHovered()) {
            i += textureDifference;
        }
        RenderSystem.enableDepthTest();
        guiGraphics.blit(texture, x, y, uOffset, i, width, height, textureWidth, textureHeight);
    }

    public boolean next() {
        return !this.record;
    }
}
