package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.util.BlockHighlightUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public class ClientEventListener {
    public static void blockHighlight(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        if (BlockHighlightUtil.SUBCHUNKS.isEmpty()) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        BlockHighlightUtil.render(
                level, Minecraft.getInstance().renderBuffers().bufferSource(), event.getPoseStack(), event.getCamera());
    }
}
