package dev.dubhe.anvilcraft.client.gui.component;

import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyClass;
import dev.dubhe.anvilcraft.block.entity.celestial.CelestialBodyMatcher;
import dev.dubhe.anvilcraft.client.gui.screen.CelestialForgingAnvilScreen;
import dev.dubhe.anvilcraft.constant.SharedTextures;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.ContainerInput;

public final class CelestialMapsGuideWidget extends AbstractWidget {

    public static final Identifier TEX_CELESTIAL_MAPS = SharedTextures.texture("block/celestial_maps");
    public static final int MAP_SIZE = 160;

    public static final int COLOR_TIME = 0xBF_A0FFA0;

    public static final int COLOR_SPACE = 0xBF_00FFFF;

    public static final int COLOR_MASS = 0xBF_FFFFA0;

    public static final int COLOR_ENERGY = 0xBF_FF8080;
    public static final int X1 = 11;
    public static final int X2 = 91;
    public static final int Y1 = 12;
    public static final int Y2 = 92;

    private static boolean isIn(double x, double y) {
        return x >= 0 && x < 80 && y >= 0 && y < 80;
    }

    public static void renderCelestialMapsGuide(
        GuiGraphicsExtractor guiGraphics,
        Font font,
        int time,
        int space,
        int mass,
        int energy
    ) {

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEX_CELESTIAL_MAPS, 0, 0, 0, 0, MAP_SIZE, MAP_SIZE, MAP_SIZE, MAP_SIZE);

        if (time > 0) {
            int x = X1 + Math.round((time - 1) * 64f / 63f);
            guiGraphics.fill(x, 0, x + 2, MAP_SIZE, COLOR_TIME);
            String text = String.valueOf(time);
            int textX = x + 1 - font.width(text) / 2;
            int textY = -font.lineHeight - 4;
            guiGraphics.text(font, text, textX, textY, COLOR_TIME, false);
        }

        if (space > 0) {
            int y = 160 - Y2 - Math.round((space - 1) * 64f / 63f) - 2;
            guiGraphics.fill(0, y, MAP_SIZE, y + 2, COLOR_SPACE);
            String text = String.valueOf(space);
            int textX = -font.width(text) - 6;
            int textY = y + 1 - font.lineHeight / 2;
            guiGraphics.text(font, text, textX, textY, COLOR_SPACE, false);
        }

        if (mass > 0) {
            int x = X2 + Math.round((mass - 1) * 64f / 63f);
            guiGraphics.fill(x, 0, x + 2, MAP_SIZE / 2, COLOR_MASS);
            String text = String.valueOf(mass);
            int textX = x + 1 - font.width(text) / 2;
            int textY = -font.lineHeight - 4;
            guiGraphics.text(font, text, textX, textY, COLOR_MASS, false);
        }

        if (energy > 0) {
            int y = 160 - Y1 - Math.round((energy - 1) * 64f / 63f) - 2;
            guiGraphics.fill(0, y, MAP_SIZE / 2, y + 2, COLOR_ENERGY);
            String text = String.valueOf(energy);
            int textX = -font.width(text) - 6;
            int textY = y + 1 - font.lineHeight / 2;
            guiGraphics.text(font, text, textX, textY, COLOR_ENERGY, false);
        }

        renderGuideStepText(guiGraphics, font, time, space, mass, energy);
    }

    private static void renderGuideStepText(GuiGraphicsExtractor guiGraphics, Font font, int time, int space, int mass, int energy) {
        var poseStack = guiGraphics.pose();
        poseStack.pushMatrix();
        int corner = 88;
        poseStack.translate(corner, corner);
        poseStack.scale(2, 2);
        int lineSpacing = font.lineHeight + 2;

        int step1Rgb = CelestialBodyMatcher.getMassRadiusRgb(mass, space);
        String step1Name = getTypeDisplayName(step1Rgb);
        drawGuideLine(guiGraphics, font, "↑" + step1Name, 0, 0);

        CelestialBodyClass step1Class = CelestialBodyClass.fromRgb(step1Rgb);
        int step2Rgb;
        if (step1Class != null && step1Class.step2UsesSp()) {
            step2Rgb = CelestialBodyMatcher.getAgeTempSpRgb(time, energy);
        } else {
            step2Rgb = CelestialBodyMatcher.getAgeTempRgb(time, energy);
        }
        String step2Name = getTypeDisplayName(step2Rgb);
        drawGuideLine(guiGraphics, font, "←" + step2Name, 0, lineSpacing * 2);

        int step3Rgb = CelestialBodyMatcher.getAgeRadiusRgb(time, space);
        String step3Name = getTypeDisplayName(step3Rgb);
        drawGuideLine(guiGraphics, font, "↖" + step3Name, 0, lineSpacing);
        poseStack.popMatrix();
    }

    private static String getTypeDisplayName(int rgb) {
        if (rgb == 0x000000) {
            return Component.translatable("screen.anvilcraft.cfa.class.no_match").getString();
        }
        CelestialBodyClass bodyClass = CelestialBodyClass.fromRgb(rgb);
        if (bodyClass == null) {
            return Component.translatable("screen.anvilcraft.cfa.class.no_match").getString();
        }
        String key;
        if (bodyClass.isRockyPlanet()) {
            key = "screen.anvilcraft.cfa.class.rocky_planet";
        } else {
            key = "screen.anvilcraft.cfa.class." + bodyClass.name().toLowerCase();
        }
        return Component.translatable(key).getString();
    }

    private static void drawGuideLine(GuiGraphicsExtractor guiGraphics, Font font, String text, int x, int y) {
        guiGraphics.text(font, text, x, y, 0xFFCCCCCC, false);
    }

    private final IntList anvilCounts;

    public CelestialMapsGuideWidget(IntList anvilCounts) {
        super(0, 0, 1, 1, Component.empty());
        this.anvilCounts = anvilCounts;
    }

    public Font getFont() {
        return Minecraft.getInstance().font;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        var poseStack = guiGraphics.pose();
        poseStack.pushMatrix();
        poseStack.translate(this.getX(), this.getY());
        poseStack.scale((float) this.getWidth() / MAP_SIZE, (float) this.getHeight() / MAP_SIZE);
        renderCelestialMapsGuide(
            guiGraphics,
            this.getFont(),
            this.anvilCounts.getInt(0),
            this.anvilCounts.getInt(1),
            this.anvilCounts.getInt(2),
            this.anvilCounts.getInt(3)
        );
        poseStack.popMatrix();
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!super.isMouseOver(mouseX, mouseY) || !(Minecraft.getInstance().screen instanceof CelestialForgingAnvilScreen)) {
            return false;
        }
        var x0 = (mouseX - this.getX()) / this.getWidth() * MAP_SIZE;
        var y0 = (mouseY - this.getY()) / this.getHeight() * MAP_SIZE;
        return isIn(x0, y0)
            || isIn(x0 - 80, y0)
            || isIn(x0, y0 - 80);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        var interactor = Minecraft.getInstance().gameMode;
        var player = Minecraft.getInstance().player;
        if (interactor == null || player == null || !(Minecraft.getInstance().screen instanceof CelestialForgingAnvilScreen screen)) {
            return;
        }
        var containerId = screen.getMenu().containerId;
        var x0 = (mouseX - this.getX()) / this.getWidth() * MAP_SIZE;
        var y0 = (mouseY - this.getY()) / this.getHeight() * MAP_SIZE;
        var time = Math.clamp((int) Math.floor(x0 - X1), 1, 64);
        var space = Math.clamp(64 - (int) (Math.floor(y0 - Y1 + 8)), 1, 64);
        var energy = Math.clamp(64 - (int) (Math.floor(y0 - Y2 + 8)), 1, 64);
        var mass = Math.clamp((int) Math.floor(x0 - X2), 1, 64);
        if (isIn(x0, y0)) {
            interactor.handleContainerInput(containerId, 0, time, ContainerInput.QUICK_CRAFT, player);
            interactor.handleContainerInput(containerId, 1, space, ContainerInput.QUICK_CRAFT, player);
        } else if (isIn(x0 - 80, y0)) {
            interactor.handleContainerInput(containerId, 2, mass, ContainerInput.QUICK_CRAFT, player);
            interactor.handleContainerInput(containerId, 1, space, ContainerInput.QUICK_CRAFT, player);
        } else if (isIn(x0, y0 - 80)) {
            interactor.handleContainerInput(containerId, 0, time, ContainerInput.QUICK_CRAFT, player);
            interactor.handleContainerInput(containerId, 3, energy, ContainerInput.QUICK_CRAFT, player);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!this.visible || !this.active) return false;
        var interactor = Minecraft.getInstance().gameMode;
        var player = Minecraft.getInstance().player;
        if (interactor == null || player == null || !(Minecraft.getInstance().screen instanceof CelestialForgingAnvilScreen screen)) {
            return false;
        }
        var containerId = screen.getMenu().containerId;
        var x0 = (mouseX - this.getX()) / this.getWidth() * MAP_SIZE;
        var y0 = (mouseY - this.getY()) / this.getHeight() * MAP_SIZE;
        if (Minecraft.getInstance().hasShiftDown() && scrollX == 0) {
            scrollX = scrollY;
            scrollY = 0;
        }
        var addX = scrollX < 0 ? 4 : 0;
        var addY = scrollY < 0 ? 4 : 0;
        var scrolledX = scrollX != 0;
        var scrolledY = scrollY != 0;
        if (isIn(x0, y0)) {
            if (scrolledX) {
                interactor.handleInventoryButtonClick(containerId, 1 + addX);
            }
            if (scrolledY) {
                interactor.handleInventoryButtonClick(containerId, 2 + addY);
            }
        } else if (isIn(x0 - 80, y0)) {
            if (scrolledX) {
                interactor.handleInventoryButtonClick(containerId, 3 + addX);
            }
            if (scrolledY) {
                interactor.handleInventoryButtonClick(containerId, 2 + addY);
            }
        } else if (isIn(x0, y0 - 80)) {
            if (scrolledX) {
                interactor.handleInventoryButtonClick(containerId, 1 + addX);
            }
            if (scrolledY) {
                interactor.handleInventoryButtonClick(containerId, 4 + addY);
            }
        } else {
            return false;
        }
        return true;
    }

    @Override
    public void playDownSound(SoundManager handler) {

    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }
}
