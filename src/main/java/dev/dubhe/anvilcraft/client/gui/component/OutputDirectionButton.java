package dev.dubhe.anvilcraft.client.gui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OutputDirectionButton extends Button {
    private Direction direction;
    private final List<Direction> skip = new ArrayList<>();
    private static final MutableComponent DEFAULT_MESSAGE = Component.translatable(
        "screen.anvilcraft.button.direction", Component.translatable("screen.anvilcraft.button.direction.up"));

    public OutputDirectionButton(int x, int y, OnPress onPress, Direction direction) {
        super(x, y, 16, 16, DEFAULT_MESSAGE, onPress, var -> DEFAULT_MESSAGE);
        this.direction = direction;
    }

    /**
     * 跳过某个方向
     *
     * @param direction 方向
     * @return 方向按钮
     */
    @SuppressWarnings("UnusedReturnValue")
    public OutputDirectionButton skip(Direction direction) {
        this.skip.add(direction);
        return this;
    }

    @Override
    public void render(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        if (this.isHovered()) {
            List<Component> components = new ArrayList<>() {
                {
                    this.add(getMessage());
                }
            };
            guiGraphics.renderTooltip(Minecraft.getInstance().font, components, Optional.empty(), mouseX, mouseY);
        }
    }

    /**
     * 设置方向
     *
     * @param direction 方向
     */
    public void setDirection(Direction direction) {
        this.direction = direction;
        this.setMessage(Component.translatable(
            "screen.anvilcraft.button.direction",
            Component.translatable("screen.anvilcraft.button.direction." + this.direction.getName())));
    }

    @Override
    public void renderWidget(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        Identifier location = switch (direction) {
            case UP -> SharedTextures.BUTTON_U;
            case EAST -> SharedTextures.BUTTON_E;
            case WEST -> SharedTextures.BUTTON_W;
            case SOUTH -> SharedTextures.BUTTON_S;
            case NORTH -> SharedTextures.BUTTON_N;
            default -> SharedTextures.BUTTON_D;
        };
        this.renderTexture(guiGraphics, location, this.getX(), this.getY(), 0, 0, 16, this.width, this.height, 16, 32);
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
        RenderSystem.enableDepthTest();
        guiGraphics.blit(texture, x, y, puOffset, i, width, height, textureWidth, textureHeight);
    }

    public Direction next() {
        return this.next(this.direction);
    }

    /**
     * 下一个方向
     *
     * @param direction 方向
     * @return 方向
     */
    public Direction next(Direction direction) {
        Direction direction1 = switch (direction) {
            case UP -> Direction.DOWN;
            case EAST -> Direction.WEST;
            case WEST -> Direction.SOUTH;
            case SOUTH -> Direction.NORTH;
            case NORTH -> Direction.UP;
            default -> Direction.EAST;
        };
        return this.skip.contains(direction1) ? next(direction1) : direction1;
    }
}
