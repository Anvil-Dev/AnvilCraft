package dev.dubhe.anvilcraft.client.building;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/** 沿用塑料工艺的七工具图标、选中帧和双行部署状态栏。 */
final class BuildingRodTraditionalOverlay {
    private static final int ICON_SIZE = 16;
    private static final int ICON_SPACING = 20;
    private static final int ATLAS_HEIGHT = 64;
    private static final int SELECTED_FRAME_V = 32;
    private static final int BACKGROUND_COLOR = 0x80000000;
    private static final int LINE_HEIGHT = 10;
    private static final int LINE_GAP = 12;
    private static final Map<BuildingRodTraditionalControls.Tool, ResourceLocation> ICONS =
        new EnumMap<>(BuildingRodTraditionalControls.Tool.class);

    static {
        for (var tool : BuildingRodTraditionalControls.Tool.values()) {
            ICONS.put(tool, AnvilCraft.of("textures/gui/button/blueprint/" + tool.id() + ".png"));
        }
    }

    private BuildingRodTraditionalOverlay() {
    }

    static void render(GuiGraphics graphics) {
        if (!BuildingRodTraditionalControls.isActive()) return;
        var selected = BuildingRodTraditionalControls.selectedTool();
        var tools = BuildingRodTraditionalControls.Tool.values();
        int totalWidth = tools.length * ICON_SPACING - (ICON_SPACING - ICON_SIZE);
        int left = (graphics.guiWidth() - totalWidth) / 2;
        int top = graphics.guiHeight() - 64;
        for (int index = 0; index < tools.length; index++) {
            var tool = tools[index];
            int frameV = tool == selected ? SELECTED_FRAME_V : 0;
            graphics.blit(ICONS.get(tool), left + index * ICON_SPACING, top, 0, frameV,
                ICON_SIZE, ICON_SIZE, ICON_SIZE, ATLAS_HEIGHT);
        }
        var font = Minecraft.getInstance().font;
        int maxWidth = graphics.guiWidth() - 20;
        var statusLines = font.split(statusLine(selected), maxWidth);
        var hintLines = font.split(BuildingRodClient.traditionalHint(), maxWidth);
        int statusTop = top - LINE_GAP - (statusLines.size() - 1) * LINE_HEIGHT;
        int hintTop = statusTop - LINE_GAP - (hintLines.size() - 1) * LINE_HEIGHT;
        for (int index = 0; index < hintLines.size(); index++) {
            drawLine(graphics, font, hintLines.get(index), hintTop + index * LINE_HEIGHT);
        }
        for (int index = 0; index < statusLines.size(); index++) {
            drawLine(graphics, font, statusLines.get(index), statusTop + index * LINE_HEIGHT);
        }
    }

    private static void drawLine(GuiGraphics graphics, Font font, FormattedCharSequence text, int y) {
        int width = font.width(text);
        int left = (graphics.guiWidth() - width) / 2;
        graphics.fill(left - 3, y - 1, left + width + 3, y + LINE_HEIGHT, BACKGROUND_COLOR);
        graphics.drawString(font, text, left, y, 0xFFFFFF, false);
    }

    private static Component statusLine(BuildingRodTraditionalControls.Tool selected) {
        int layer = BuildingRodClient.layer;
        Component layerText = layer == -1 ? Component.translatable("screen.anvilcraft.building_rod.traditional.layer_all")
            : Component.literal(String.valueOf(layer + 1));
        Component lockText = Component.translatable("screen.anvilcraft.building_rod.traditional."
            + (BuildingRodClient.locked ? "anchor_locked" : "anchor_following"));
        var placement = BuildingRodClient.placement;
        return Component.translatable("screen.anvilcraft.building_rod.traditional.status", BuildingRodClient.blueprintName(),
            Component.translatable("screen.anvilcraft.building_rod.tool." + selected.id()),
            placement.rotation().name().toLowerCase(Locale.ROOT), placement.mirror().name().toLowerCase(Locale.ROOT), layerText, lockText);
    }
}
