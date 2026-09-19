package dev.dubhe.anvilcraft.client.building;

import dev.dubhe.anvilcraft.client.support.RenderSupport;
import net.minecraft.client.renderer.RenderType;

import java.util.LinkedHashMap;
import java.util.Map;

final class BuildingRodRenderTypes {
    private static final Map<RenderType, RenderType> TYPES = new LinkedHashMap<>();

    private BuildingRodRenderTypes() {
    }

    static RenderType ghost(RenderType original) {
        if (TYPES.size() >= 128) TYPES.clear();
        return TYPES.computeIfAbsent(original, RenderSupport::useTranslucentIfPossible);
    }
}
