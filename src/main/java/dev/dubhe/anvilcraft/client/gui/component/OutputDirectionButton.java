package dev.dubhe.anvilcraft.client.gui.component;

import dev.dubhe.anvilcraft.constant.SharedTextures;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

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
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        if (this.isHovered()) {
            List<ClientTooltipComponent> components = new ArrayList<>() {
                {
                    this.add(ClientTooltipComponent.create(getMessage().getVisualOrderText()));
                }
            };
            graphics.tooltip(Minecraft.getInstance().font, components, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE, null);
        }
        Identifier location = switch (this.direction) {
            case UP -> SharedTextures.BUTTON_U;
            case EAST -> SharedTextures.BUTTON_E;
            case WEST -> SharedTextures.BUTTON_W;
            case SOUTH -> SharedTextures.BUTTON_S;
            case NORTH -> SharedTextures.BUTTON_N;
            default -> SharedTextures.BUTTON_D;
        };
        this.renderTexture(graphics, location, this.getX(), this.getY(), 0, 0, 16, this.width, this.height, 16, 32);
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
        return this.skip.contains(direction1) ? this.next(direction1) : direction1;
    }
}
