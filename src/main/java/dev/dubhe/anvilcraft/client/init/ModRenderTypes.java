package dev.dubhe.anvilcraft.client.init;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;

import java.util.OptionalDouble;
import java.util.function.Function;

import static dev.dubhe.anvilcraft.client.init.ModRenderTargets.LASER_TARGET;
import static dev.dubhe.anvilcraft.client.init.ModRenderTargets.LINE_BLOOM_TARGET;
import static dev.dubhe.anvilcraft.client.init.ModShaders.renderTypeLaserShader;
import static net.minecraft.client.renderer.RenderStateShard.BLOCK_SHEET_MIPPED;
import static net.minecraft.client.renderer.RenderStateShard.COLOR_DEPTH_WRITE;
import static net.minecraft.client.renderer.RenderStateShard.COLOR_WRITE;
import static net.minecraft.client.renderer.RenderStateShard.CULL;
import static net.minecraft.client.renderer.RenderStateShard.LEQUAL_DEPTH_TEST;
import static net.minecraft.client.renderer.RenderStateShard.LIGHTMAP;
import static net.minecraft.client.renderer.RenderStateShard.NO_CULL;
import static net.minecraft.client.renderer.RenderStateShard.NO_TRANSPARENCY;
import static net.minecraft.client.renderer.RenderStateShard.OVERLAY;
import static net.minecraft.client.renderer.RenderStateShard.RENDERTYPE_CUTOUT_SHADER;
import static net.minecraft.client.renderer.RenderStateShard.RENDERTYPE_LINES_SHADER;
import static net.minecraft.client.renderer.RenderStateShard.RENDERTYPE_TRANSLUCENT_SHADER;
import static net.minecraft.client.renderer.RenderStateShard.TRANSLUCENT_TARGET;
import static net.minecraft.client.renderer.RenderStateShard.TRANSLUCENT_TRANSPARENCY;
import static net.minecraft.client.renderer.RenderStateShard.VIEW_OFFSET_Z_LAYERING;

public class ModRenderTypes {
    public static final RenderStateShard.TransparencyStateShard LASER_TRANSPARENCY = new RenderStateShard.TransparencyStateShard(
        "anvilcraft:laser_transparency",
        () -> {
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_COLOR,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ZERO,
                GlStateManager.DestFactor.ONE
            );
        },
        () -> {
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        }
    );

    public static RenderStateShard.ShaderStateShard RENDERTYPE_LASER_SHADER = new RenderStateShard.ShaderStateShard(
        () -> renderTypeLaserShader
    );

    public static RenderStateShard.ShaderStateShard RENDERTYPE_LIGHTNING_SHADER = new RenderStateShard.ShaderStateShard(
        ModShaders::getRenderTypeLightningShader
    );

    private static RenderStateShard.ShaderStateShard createRenderTypeColoredOverlayShader(int color) {
        return new RenderStateShard.ShaderStateShard(
            ModRenderTypes::supplyNothing
        ) {
            @Override
            public void setupRenderState() {
                this.getShader()
                    .safeGetUniform("OverlayColor")
                    .set(
                        FastColor.ARGB32.red(color) / 255f,
                        FastColor.ARGB32.green(color) / 255f,
                        FastColor.ARGB32.blue(color) / 255f,
                        FastColor.ARGB32.alpha(color) / 255f
                    );
                RenderSystem.setShader(this::getShader);
            }

            private ShaderInstance getShader() {
                return ModShaders.renderTypeColoredOverlayShader;
            }

            @Override
            public void clearRenderState() {

            }
        };
    }

    public static final RenderType TRANSLUCENT_COLORED_OVERLAY = RenderType.create(
        "translucent",
        DefaultVertexFormat.BLOCK,
        VertexFormat.Mode.QUADS,
        786432,
        true,
        true,
        RenderType.CompositeState.builder()
            .setLightmapState(LIGHTMAP)
            .setShaderState(createRenderTypeColoredOverlayShader(0xDD66CCFF))
            .setTextureState(BLOCK_SHEET_MIPPED)
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setOutputState(TRANSLUCENT_TARGET)
            .createCompositeState(true)
    );

    public static final RenderType LINE_BLOOM = RenderType.create(
        "anvilcraft:line_bloom",
        DefaultVertexFormat.POSITION_COLOR_NORMAL,
        VertexFormat.Mode.LINES,
        1536,
        RenderType.CompositeState.builder()
            .setShaderState(RENDERTYPE_LINES_SHADER)
            .setLineState(new RenderStateShard.LineStateShard(OptionalDouble.empty()))
            .setLayeringState(VIEW_OFFSET_Z_LAYERING)
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setOutputState(LINE_BLOOM_TARGET)
            .setWriteMaskState(COLOR_DEPTH_WRITE)
            .setCullState(NO_CULL)
            .createCompositeState(false)
    );

    public static final RenderType LASER = RenderType.create(
        "anvilcraft:laser",
        DefaultVertexFormat.BLOCK,
        VertexFormat.Mode.QUADS,
        1536,
        true,
        true,
        RenderType.CompositeState.builder()
            .setLightmapState(LIGHTMAP)
            .setShaderState(RENDERTYPE_LASER_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(
                TextureAtlas.LOCATION_BLOCKS,
                false,
                false
            )).setTransparencyState(LASER_TRANSPARENCY)
            .setCullState(CULL)
            .setWriteMaskState(COLOR_DEPTH_WRITE)
            .setOverlayState(OVERLAY)
            .setOutputState(LASER_TARGET)
            .createCompositeState(true)
    );

    public static final RenderType LIGHTNING = RenderType.create(
        "anvilcraft:lightning",
        DefaultVertexFormat.BLOCK,
        VertexFormat.Mode.QUADS,
        1536,
        true,
        true,
        RenderType.CompositeState.builder()
            .setLightmapState(LIGHTMAP)
            .setShaderState(RENDERTYPE_LIGHTNING_SHADER)
            .setTextureState(new RenderStateShard.TextureStateShard(
                TextureAtlas.LOCATION_BLOCKS,
                false,
                false
            ))
            .setTransparencyState(LASER_TRANSPARENCY)
            .setCullState(NO_CULL)
            .setWriteMaskState(COLOR_WRITE)
            .setOutputState(LASER_TARGET)
            .createCompositeState(true)
    );

    /**
     * Multiplicative blend render type for star color overlay.
     * Uses {@code DST_COLOR * SRC_COLOR} so that the grayscale star
     * animation is multiplied by the star's RGB, producing accurate
     * palette-like coloring without washing out.
     */
    public static final RenderType STAR_COLOR_OVERLAY = RenderType.create(
        "anvilcraft:star_color_overlay",
        DefaultVertexFormat.BLOCK,
        VertexFormat.Mode.QUADS,
        786432,
        true,
        true,
        RenderType.CompositeState.builder()
            .setLightmapState(LIGHTMAP)
            .setShaderState(RENDERTYPE_TRANSLUCENT_SHADER)
            .setTextureState(BLOCK_SHEET_MIPPED)
            .setTransparencyState(new RenderStateShard.TransparencyStateShard(
                "star_multiply",
                () -> {
                    RenderSystem.enableBlend();
                    RenderSystem.blendFuncSeparate(
                        GlStateManager.SourceFactor.DST_COLOR,
                        GlStateManager.DestFactor.ZERO,
                        GlStateManager.SourceFactor.ZERO,
                        GlStateManager.DestFactor.ONE
                    );
                },
                () -> {
                    RenderSystem.disableBlend();
                    RenderSystem.defaultBlendFunc();
                }
            ))
            .setWriteMaskState(COLOR_WRITE)
            .createCompositeState(true)
    );

    public static final RenderType CELESTIAL_ATMOSPHERE = RenderType.create(
        "anvilcraft:celestial_atmosphere",
        DefaultVertexFormat.BLOCK,
        VertexFormat.Mode.QUADS,
        786432,
        true,
        true,
        RenderType.CompositeState.builder()
            .setLightmapState(LIGHTMAP)
            .setShaderState(RENDERTYPE_TRANSLUCENT_SHADER)
            .setTextureState(BLOCK_SHEET_MIPPED)
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setWriteMaskState(COLOR_WRITE)
            .createCompositeState(true)
    );

    public static final RenderType BEACON_GLASS = RenderType.create(
        "anvilcraft:beacon_glass",
        DefaultVertexFormat.BLOCK,
        VertexFormat.Mode.QUADS,
        786432,
        true,
        true,
        RenderType.CompositeState.builder()
            .setLightmapState(LIGHTMAP)
            .setShaderState(RENDERTYPE_TRANSLUCENT_SHADER)
            .setTextureState(BLOCK_SHEET_MIPPED)
            .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
            .setOutputState(TRANSLUCENT_TARGET)
            .setWriteMaskState(COLOR_WRITE)
            .createCompositeState(true)
    );

    public static final Function<ResourceLocation, RenderType> STAR_CUTOUT = Util.memoize(
        (resourceLocation) -> {
            RenderType.CompositeState compositeState = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_CUTOUT_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(resourceLocation, false, false))
                .setTransparencyState(NO_TRANSPARENCY)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .createCompositeState(true);
            return RenderType.create(
                "entity_cutout",
                DefaultVertexFormat.BLOCK,
                VertexFormat.Mode.QUADS,
                1536,
                true,
                false,
                compositeState
            );
        }
    );

    /**
     * Translucent render type for celestial rings in UI previews.
     * Uses the block translucent shader ({@code RENDERTYPE_TRANSLUCENT_SHADER})
     * for correct palette color reproduction, with depth test enabled so the
     * celestial body occludes the back portion of the ring — matching the
     * world-space rendering.
     */
    public static final Function<ResourceLocation, RenderType> CELESTIAL_RING = Util.memoize(
        (resourceLocation) -> {
            RenderType.CompositeState compositeState = RenderType.CompositeState.builder()
                .setShaderState(RENDERTYPE_TRANSLUCENT_SHADER)
                .setTextureState(new RenderStateShard.TextureStateShard(resourceLocation, false, false))
                .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                .setDepthTestState(LEQUAL_DEPTH_TEST)
                .setCullState(NO_CULL)
                .setLightmapState(LIGHTMAP)
                .setOverlayState(OVERLAY)
                .setWriteMaskState(COLOR_WRITE)
                .createCompositeState(true);
            return RenderType.create(
                "anvilcraft:celestial_ring",
                DefaultVertexFormat.BLOCK,
                VertexFormat.Mode.QUADS,
                1536,
                true,
                false,
                compositeState
            );
        }
    );

    private static <T> T supplyNothing() {
        return null;
    }
}
