package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.util.IBlockHighlightUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import org.jetbrains.annotations.NotNull;

public class ClientEventListener {
    public static void blockHighlight(@NotNull RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        if (IBlockHighlightUtil.SUBCHUNKS.isEmpty()) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        IBlockHighlightUtil.render(
                level, Minecraft.getInstance().renderBuffers().bufferSource(), event.getPoseStack(), event.getCamera());
    }
}
