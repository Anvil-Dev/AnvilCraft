package dev.dubhe.anvilcraft.event.forge;


import dev.dubhe.anvilcraft.util.IBlockHighlightUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.NotNull;

public class ClientEventListener {
    /**
     * 构造
     */
    @OnlyIn(Dist.CLIENT)
    public ClientEventListener() {
        MinecraftForge.EVENT_BUS.addListener(this::blockHighlight);
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
}
