package dev.dubhe.anvilcraft.client.building;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.init.ModRenderPipelines;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.LinkedHashMap;
import java.util.Map;

final class BuildingRodRenderTypes {
    static final RenderType BLOCK_GHOST = RenderType.create("anvilcraft:building_rod_ghost",
        RenderSetup.builder(ModRenderPipelines.BUILDING_ROD_GHOST).useLightmap().sortOnUpload()
            .bufferSize(786432).withTexture("Sampler0", Sheets.BLOCKS_MAPPER.sheet()).createRenderSetup());
    private static final Map<RenderType, RenderType> TYPES = new LinkedHashMap<>();
    private static int serial;

    private BuildingRodRenderTypes() {
    }

    static RenderType ghost(RenderType original) {
        if (original.format() == DefaultVertexFormat.BLOCK) return BLOCK_GHOST;
        if (original.hasBlending()) return original;
        if (TYPES.size() >= 128) TYPES.clear();
        return TYPES.computeIfAbsent(original, type -> {
            var previous = type.state;
            boolean entity = type.format() == RenderPipelines.ENTITY_TRANSLUCENT_CULL.getVertexFormat()
                && previous.textures.containsKey("Sampler0");
            var pipeline = (entity ? RenderPipelines.ENTITY_TRANSLUCENT_CULL : type.pipeline()).toBuilder()
                .withLocation(AnvilCraft.of("pipeline/blueprint_entity_" + serial++))
                .withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT)).build();
            var builder = RenderSetup.builder(pipeline).sortOnUpload().bufferSize(type.bufferSize())
                .setLayeringTransform(previous.layeringTransform).setOutputTarget(previous.outputTarget)
                .setTextureTransform(previous.textureTransform);
            previous.textures.forEach((name, binding) -> {
                if (pipeline.getSamplers().contains(name)) builder.withTexture(name, binding.location(), binding.sampler());
            });
            var textures = previous.getTextures();
            if (entity || textures.containsKey("Sampler1")) builder.useOverlay();
            if (entity || textures.containsKey("Sampler2")) builder.useLightmap();
            return RenderType.create("anvilcraft:blueprint_entity_" + serial, builder.createRenderSetup());
        });
    }
}
