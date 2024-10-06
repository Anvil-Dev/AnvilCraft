package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.sound.SoundHelper;
import dev.dubhe.anvilcraft.util.BlockHighlightUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class ClientEventListener {
    @SubscribeEvent
    public static void blockHighlight(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        if (BlockHighlightUtil.SUBCHUNKS.isEmpty()) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        BlockHighlightUtil.render(
            level,
            Minecraft.getInstance().renderBuffers().bufferSource(),
            event.getPoseStack(),
            event.getCamera()
        );
    }

    @SubscribeEvent
    public static void onClientPlayerDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        SoundHelper.INSTANCE.clear();
    }
}
