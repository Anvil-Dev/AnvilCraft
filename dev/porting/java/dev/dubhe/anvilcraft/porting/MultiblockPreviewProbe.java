package dev.dubhe.anvilcraft.porting;

import dev.anvilcraft.lib.v2.rendering.gui.state.StructurePipRenderingState;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.util.LevelLike;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;

import java.util.ArrayList;
import java.util.List;

public final class MultiblockPreviewProbe {
    public static void verify(LevelLike level) {
        int height = level.verticalSize();
        for (int layer = -1; layer < height; layer++) {
            level.setAllLayersVisible(layer == -1);
            level.setCurrentVisibleLayer(Math.max(layer, 0));
            var state = new GuiRenderState();
            var graphics = new GuiGraphicsExtractor(Minecraft.getInstance(), state, 0, 0);
            RenderSupport.renderLevelLikeAt(level, graphics, 45, 50, 80, 2);
            RenderSupport.renderLevelLike(level, graphics, 8, 8, 80, 16, 4, false);
            List<StructurePipRenderingState> previews = new ArrayList<>();
            state.forEachPictureInPicture(pip -> {
                if (pip instanceof StructurePipRenderingState structure) previews.add(structure);
            });
            if (previews.size() != 2) throw new IllegalStateException("Missing preview states");
            for (var preview : previews) {
                int expectedMin = layer == -1 ? 0 : layer;
                int expectedMax = layer == -1 ? height - 1 : layer;
                if (preview.startPos().getY() != expectedMin || preview.endPos().getY() != expectedMax) {
                    throw new IllegalStateException("Hidden layers would still submit block entities");
                }
            }
        }
        level.setAllLayersVisible(true);
        level.setCurrentVisibleLayer(0);
    }
}
