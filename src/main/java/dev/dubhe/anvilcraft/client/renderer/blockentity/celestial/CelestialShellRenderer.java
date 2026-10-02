package dev.dubhe.anvilcraft.client.renderer.blockentity.celestial;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.api.rendering.BlockStateModelRenderer;
import dev.dubhe.anvilcraft.client.init.ModRenderPipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.block.Blocks;
import org.joml.Vector3f;

/** 使用源版白色混凝土图集模型绘制天体乘色层和半透明外壳。 */
public final class CelestialShellRenderer {
    private CelestialShellRenderer() {
    }

    public static void colorOverlay(PoseStack pose, OrderedSubmitNodeCollector collector, float[] color) {
        cube(pose, collector, Layers.MULTIPLY, color, 1);
    }

    public static void translucent(PoseStack pose, OrderedSubmitNodeCollector collector, float[] color, float alpha) {
        cube(pose, collector, Layers.TRANSLUCENT, color, alpha);
    }

    public static void atmosphere(PoseStack pose, OrderedSubmitNodeCollector collector, float[] color, Vector3f view) {
        var model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(Blocks.WHITE_CONCRETE.defaultBlockState());
        collector.submitCustomGeometry(pose, Layers.TRANSLUCENT, (matrix, vertices) ->
            BlockStateModelRenderer.INSTANCE.getTessellatorNoLighting().tesselateBlock((x, y, z, quad, instance) -> {
                var direction = quad.direction();
                float alpha = CelestialBodyRenderer.computeAtmosphereAlpha(matrix, direction.getStepX(), direction.getStepY(),
                    direction.getStepZ(), 0.2F, view.x, view.y, view.z);
                instance.setColor(ARGB.colorFromFloat(alpha, color[0], color[1], color[2]));
                instance.setLightCoords(LightCoordsUtil.FULL_BRIGHT);
                instance.setOverlayCoords(OverlayTexture.NO_OVERLAY);
                vertices.putBakedQuad(matrix, quad, instance);
            }, 0, 0, 0, BlockAndTintGetter.EMPTY, BlockPos.ZERO, Blocks.AIR.defaultBlockState(), model, 42));
    }

    private static void cube(PoseStack pose, OrderedSubmitNodeCollector collector, RenderType type, float[] color, float alpha) {
        var model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(Blocks.WHITE_CONCRETE.defaultBlockState());
        StellarEmissionRenderer.drawModel(model, pose, collector, type, ARGB.colorFromFloat(alpha, color[0], color[1], color[2]));
    }

    private static final class Layers {
        private static final RenderType MULTIPLY = RenderType.create("anvilcraft:celestial_color_shell",
            RenderSetup.builder(ModRenderPipelines.CELESTIAL_COLOR_SHELL).withTexture("Sampler0", Sheets.BLOCKS_MAPPER.sheet(),
                () -> RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST, true))
                .useLightmap().sortOnUpload().createRenderSetup());
        private static final RenderType TRANSLUCENT = RenderType.create("anvilcraft:celestial_translucent_shell",
            RenderSetup.builder(ModRenderPipelines.STELLAR_ENVELOPE).withTexture("Sampler0", Sheets.BLOCKS_MAPPER.sheet(),
                () -> RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST, true))
                .useLightmap().sortOnUpload().createRenderSetup());
    }
}
