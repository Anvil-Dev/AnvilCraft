package dev.dubhe.anvilcraft.client.overseer;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

public class OverseerMapStyle {
    public static @Nullable int[] colors(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        int flags = OverseerMapData.flags(dimension, chunkX, chunkZ);
        if (flags < 0) return null;
        int rgb = color(flags);
        // Xaero stores colors as BBGGRRAA, with the alpha in the lowest byte.
        int packed = (rgb & 0xFF) << 24 | (rgb & 0xFF00) << 8 | (rgb >> 16 & 0xFF) << 8;
        int fill = packed | 0x55;
        int border = packed | 0xDD;
        return new int[]{
            fill,
            OverseerMapData.flags(dimension, chunkX, chunkZ - 1) == flags ? fill : border,
            OverseerMapData.flags(dimension, chunkX + 1, chunkZ) == flags ? fill : border,
            OverseerMapData.flags(dimension, chunkX, chunkZ + 1) == flags ? fill : border,
            OverseerMapData.flags(dimension, chunkX - 1, chunkZ) == flags ? fill : border
        };
    }

    private static int color(int flags) {
        return flags == 0 ? 0x55CCFF : 0x55CC66;
    }

    public static List<Component> tooltipLines(ResourceKey<Level> dimension, int chunkX, int chunkZ, int width) {
        Component tooltip = tooltip(dimension, chunkX, chunkZ);
        List<Component> lines = new ArrayList<>();
        for (FormattedText line : Minecraft.getInstance().font.getSplitter().splitLines(tooltip, width, tooltip.getStyle())) {
            lines.add(Component.literal(line.getString()).setStyle(tooltip.getStyle()));
        }
        return lines;
    }

    public static Component tooltip(ResourceKey<Level> dimension, int chunkX, int chunkZ) {
        int flags = OverseerMapData.flags(dimension, chunkX, chunkZ);
        if (flags < 0) return Component.empty();
        MutableComponent result = Component.translatable("map.anvilcraft.overseer.loaded");
        result.append(" · ").append(Component.translatable(flags != 0
            ? "map.anvilcraft.overseer.random_tick" : "map.anvilcraft.overseer.no_random_tick"));
        return result.withStyle(style -> style.withColor(color(flags)));
    }
}
