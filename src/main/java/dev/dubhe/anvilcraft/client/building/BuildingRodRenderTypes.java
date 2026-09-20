package dev.dubhe.anvilcraft.client.building;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.dubhe.anvilcraft.client.init.ModShaders;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;

import java.util.LinkedHashMap;
import java.util.Map;

final class BuildingRodRenderTypes {
    private static final RenderType BLOCK_GHOST = RenderType.create(
        "anvilcraft:building_rod_ghost", DefaultVertexFormat.BLOCK, VertexFormat.Mode.QUADS, 786432, true, true,
        RenderType.CompositeState.builder()
            .setShaderState(new RenderStateShard.ShaderStateShard(ModShaders::getBuildingRodGhostShader))
            .setTextureState(RenderStateShard.BLOCK_SHEET_MIPPED)
            .setLightmapState(RenderStateShard.LIGHTMAP)
            .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
            .setOutputState(RenderStateShard.TRANSLUCENT_TARGET)
            .createCompositeState(true)
    );
    private static final Map<RenderType, RenderType> TYPES = new LinkedHashMap<>();

    private BuildingRodRenderTypes() {
    }

    static RenderType ghost(RenderType original) {
        if (original == RenderType.solid() || original == RenderType.cutout()
            || original == RenderType.cutoutMipped() || original == RenderType.tripwire() || original == RenderType.translucent()) {
            return BLOCK_GHOST;
        }
        if (TYPES.size() >= 128) TYPES.clear();
        return TYPES.computeIfAbsent(original, RenderSupport::useTranslucentIfPossible);
    }
}
