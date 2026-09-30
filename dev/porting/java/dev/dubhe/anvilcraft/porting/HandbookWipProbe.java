package dev.dubhe.anvilcraft.porting;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.anvilcraft.lib.v2.rendering.gui.state.StructurePipRenderingState;
import dev.dubhe.anvilcraft.client.renderer.blockentity.WipBlockEntityRenderer;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.recipe.anvil.procedural.ProceduralProcessRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.util.LightCoordsUtil;

import java.lang.reflect.Proxy;

public final class HandbookWipProbe {
    public static void verify(ProceduralProcessRecipe recipe) {
        for (int step = 0; step <= recipe.steps().size() * recipe.loop(); step++) {
            var model = recipe.getDisplayedModelForStep(step).orElse(null);
            if (model != null && model.getPath().startsWith("block/wip_display/")) {
                var legacy = model.withPath(model.getPath().replace("block/wip_display/", "block/"));
                if (WipBlockEntityRenderer.getModelKey(model) != WipBlockEntityRenderer.getModelKey(legacy)) {
                    throw new IllegalStateException("Legacy WIP model ID lost compatibility");
                }
            }
            var state = new GuiRenderState();
            var graphics = new GuiGraphicsExtractor(Minecraft.getInstance(), state, 0, 0);
            graphics.pose().scale(2, 2);
            RenderSupport.renderWipBlockAt(graphics, model, 80, 60, 16);
            int[] parts = {0};
            var collector = (SubmitNodeCollector) Proxy.newProxyInstance(SubmitNodeCollector.class.getClassLoader(),
                new Class<?>[]{SubmitNodeCollector.class}, (proxy, method, args) -> {
                    if (method.getName().equals("order")) return proxy;
                    if (method.getName().equals("submitMultiLayerBlockModel")) {
                        if ((int) args[4] != LightCoordsUtil.FULL_BRIGHT) {
                            throw new IllegalStateException("WIP preview samples world light");
                        }
                        parts[0]++;
                    }
                    return null;
                });
            state.forEachPictureInPicture(pip -> {
                if (!(pip instanceof StructurePipRenderingState structure) || structure.structureAccess() != BlockAndTintGetter.EMPTY) {
                    throw new IllegalStateException("WIP preview depends on a live world");
                }
                structure.drawAdditionalCallback().accept(collector, new PoseStack());
            });
            if (parts[0] != (model == null ? 1 : 2)) throw new IllegalStateException("Missing WIP body or translucent shell at " + step);
        }
    }
}
