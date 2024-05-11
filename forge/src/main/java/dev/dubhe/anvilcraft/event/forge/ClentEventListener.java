package dev.dubhe.anvilcraft.event.forge;


import dev.dubhe.anvilcraft.client.renderer.PowerGridRenderer;
import dev.dubhe.anvilcraft.util.IBlockHighlightUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.LevelEvent;
import org.jetbrains.annotations.NotNull;

public class ClentEventListener {
    /**
     * 构造
     */
    @OnlyIn(Dist.CLIENT)
    public ClentEventListener() {
        MinecraftForge.EVENT_BUS.addListener(this::blockHighlight);
        MinecraftForge.EVENT_BUS.addListener(this::onLevelUnload);
    }

    @OnlyIn(Dist.CLIENT)
    private void blockHighlight(@NotNull RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        if (IBlockHighlightUtil.SUBCHUNKS.isEmpty()) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        IBlockHighlightUtil.render(level, Minecraft.getInstance().renderBuffers().bufferSource(),
            event.getPoseStack(), event.getCamera());
    }

    @OnlyIn(Dist.CLIENT)
    private void onLevelUnload(@NotNull LevelEvent.Unload event) {
        PowerGridRenderer.cleanAllGrid();
    }
}
