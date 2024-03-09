package dev.dubhe.anvilcraft.client.gui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.dubhe.anvilcraft.AnvilCraft;
import lombok.Setter;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;

@Setter
public class OutputDirectionButton extends Button {
    private Direction direction;
    private static final ResourceLocation UP = AnvilCraft.of("textures/gui/container/button_u.png");
    private static final ResourceLocation DOWN = AnvilCraft.of("textures/gui/container/button_d.png");
    private static final ResourceLocation EAST = AnvilCraft.of("textures/gui/container/button_e.png");
    private static final ResourceLocation WEST = AnvilCraft.of("textures/gui/container/button_w.png");
    private static final ResourceLocation SOUTH = AnvilCraft.of("textures/gui/container/button_s.png");
    private static final ResourceLocation NORTH = AnvilCraft.of("textures/gui/container/button_n.png");
    private static final MutableComponent defaultMessage = Component.translatable("");

    public OutputDirectionButton(int x, int y, OnPress onPress, Direction direction) {
        super(x, y, 16, 16, defaultMessage, onPress, (var) -> defaultMessage);
        this.direction = direction;
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        ResourceLocation location = switch (direction) {
            case UP -> UP;
            case EAST -> EAST;
            case WEST -> WEST;
            case SOUTH -> SOUTH;
            case NORTH -> NORTH;
            default -> DOWN;
        };
        this.renderTexture(guiGraphics, location, this.getX(), this.getY(), 0, 0, 16, this.width, this.height, 16, 32);
    }

    @Override
    public void renderTexture(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int uOffset, int vOffset, int textureDifference, int width, int height, int textureWidth, int textureHeight) {
        int i = vOffset;
        if (this.isHovered()) {
            i += textureDifference;
        }
        RenderSystem.enableDepthTest();
        guiGraphics.blit(texture, x, y, uOffset, i, width, height, textureWidth, textureHeight);
    }

    public Direction next() {
        return switch (direction) {
            case UP -> Direction.DOWN;
            case EAST -> Direction.WEST;
            case WEST -> Direction.SOUTH;
            case SOUTH -> Direction.NORTH;
            case NORTH -> Direction.UP;
            default -> Direction.EAST;
        };
    }
}
