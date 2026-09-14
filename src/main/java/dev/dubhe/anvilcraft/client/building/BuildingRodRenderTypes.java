package dev.dubhe.anvilcraft.client.building;

import com.mojang.blaze3d.systems.RenderSystem;
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
        return TYPES.computeIfAbsent(original, type -> new DoubleSided(RenderSupport.useTranslucentIfPossible(type)));
    }

    private static final class DoubleSided extends RenderType {
        private DoubleSided(RenderType original) {
            super("anvilcraft:building_rod_ghost", original.format(), original.mode(), original.bufferSize(),
                original.affectsCrumbling(), original.sortOnUpload(), () -> {
                    original.setupRenderState();
                    RenderSystem.disableCull();
                }, () -> {
                    RenderSystem.enableCull();
                    original.clearRenderState();
                });
        }
    }
}
