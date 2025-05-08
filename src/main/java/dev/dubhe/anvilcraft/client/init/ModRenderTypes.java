package dev.dubhe.anvilcraft.client.init;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.util.FastColor;

import java.util.OptionalDouble;

import static dev.dubhe.anvilcraft.client.init.ModRenderTargets.LASER_TARGET;
import static dev.dubhe.anvilcraft.client.init.ModRenderTargets.LINE_BLOOM_TARGET;
import static dev.dubhe.anvilcraft.client.init.ModShaders.renderTypeLaserShader;
import static net.minecraft.client.renderer.RenderStateShard.BLOCK_SHEET_MIPPED;
import static net.minecraft.client.renderer.RenderStateShard.COLOR_DEPTH_WRITE;
import static net.minecraft.client.renderer.RenderStateShard.COLOR_WRITE;
import static net.minecraft.client.renderer.RenderStateShard.CULL;
import static net.minecraft.client.renderer.RenderStateShard.LIGHTMAP;
import static net.minecraft.client.renderer.RenderStateShard.NO_CULL;
import static net.minecraft.client.renderer.RenderStateShard.OVERLAY;
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

    private static <T> T supplyNothing() {
        return null;
    }
}
