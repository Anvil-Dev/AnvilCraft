package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.sound.SoundHelper;
import dev.dubhe.anvilcraft.client.init.ModKeyMappings;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import dev.dubhe.anvilcraft.network.SwitchPhasePacket;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.util.BlockHighlightUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.InputEvent.Key;
import net.neoforged.neoforge.client.event.RenderBlockScreenEffectEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

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
    public static void onRenderBlockOverlay(RenderBlockScreenEffectEvent event) {
        if (event.getOverlayType() == RenderBlockScreenEffectEvent.OverlayType.BLOCK
            && (event.getBlockState().is(ModBlocks.ACCELERATION_RING) || event.getBlockState().is(ModBlocks.DEFLECTION_RING))
        ) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onClientPlayerDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        SoundHelper.INSTANCE.clear();
    }

    @SubscribeEvent
    public static void onKeyPress(Key event) {
        if (ModKeyMappings.TOGGLE_GOGGLE.get().isDown()) AnvilHammerItem.goggleEnabled = !AnvilHammerItem.goggleEnabled;
        if (ModKeyMappings.SWITCH_PHASE.get().isDown()) PacketDistributor.sendToServer(new SwitchPhasePacket());
    }
}
