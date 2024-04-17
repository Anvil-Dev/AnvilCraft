package dev.dubhe.anvilcraft.client.gui.component;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class OutputDirectionButton extends Button {
    private Direction direction;
    private final List<Direction> skip = new ArrayList<>();
    private static final ResourceLocation UP = AnvilCraft.of("textures/gui/container/machine/button_u.png");
    private static final ResourceLocation DOWN = AnvilCraft.of("textures/gui/container/machine/button_d.png");
    private static final ResourceLocation EAST = AnvilCraft.of("textures/gui/container/machine/button_e.png");
    private static final ResourceLocation WEST = AnvilCraft.of("textures/gui/container/machine/button_w.png");
    private static final ResourceLocation SOUTH = AnvilCraft.of("textures/gui/container/machine/button_s.png");
    private static final ResourceLocation NORTH = AnvilCraft.of("textures/gui/container/machine/button_n.png");
    private static final MutableComponent defaultMessage =
        Component.translatable("screen.anvilcraft.button.direction",
            Component.translatable("screen.anvilcraft.button.direction.up"));

    public OutputDirectionButton(int x, int y, OnPress onPress, Direction direction) {
        super(x, y, 16, 16, defaultMessage, onPress, (var) -> defaultMessage);
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
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
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
        this.setMessage(Component.translatable("screen.anvilcraft.button.direction",
            Component.translatable("screen.anvilcraft.button.direction." + this.direction.getName())));
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        ResourceLocation location = switch (direction) {
            case UP -> UP;
            case EAST -> EAST;
            case WEST -> WEST;
            case SOUTH -> SOUTH;
            case NORTH -> NORTH;
            default -> DOWN;
        };
        this.renderTexture(guiGraphics, location, this.getX(), this.getY(),
            0, 0, 16, this.width, this.height, 16, 32);
    }

    @Override
    public void renderTexture(
        @NotNull GuiGraphics guiGraphics, @NotNull ResourceLocation texture,
        int x, int y, int puOffset, int pvOffset, int textureDifference,
        int width, int height, int textureWidth, int textureHeight
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
    public Direction next(@NotNull Direction direction) {
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
