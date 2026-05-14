package dev.dubhe.anvilcraft.client.gui.component;

import dev.dubhe.anvilcraft.constant.SharedTextures;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.function.Supplier;

@Getter
public class EnableFilterButton extends Button {
    private final Supplier<Boolean> filterEnabled;
    private static final MutableComponent defaultMessage = Component.translatable(
        "screen.anvilcraft.button.record", Component.translatable("screen.anvilcraft.button.off"));

    public EnableFilterButton(int x, int y, OnPress onPress, Supplier<Boolean> filterEnabled) {
        super(x, y, 16, 16, defaultMessage, onPress, var -> defaultMessage);
        this.filterEnabled = filterEnabled;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (this.isHovered()) {
            graphics.tooltip(
                Minecraft.getInstance().font,
                List.of(ClientTooltipComponent.create(getMessage().getVisualOrderText())),
                mouseX,
                mouseY,
                DefaultTooltipPositioner.INSTANCE,
                null
            );
        }
        Identifier location = this.filterEnabled.get() ? SharedTextures.BUTTON_YES : SharedTextures.BUTTON_NO;
        this.renderTexture(graphics, location, this.getX(), this.getY(), 0, 0, 16, this.width, this.height, 16, 32);
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

    public void renderTexture(
        GuiGraphicsExtractor guiGraphics,
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
        guiGraphics.blit(texture, x, y, puOffset, i, width, height, textureWidth, textureHeight);
    }

    public boolean next() {
        return !this.filterEnabled.get();
    }
}
