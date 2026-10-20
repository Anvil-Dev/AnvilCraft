package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.config.AnvilCraftClientConfig.OverworldSkyMode;
import dev.dubhe.anvilcraft.network.OverworldSkyModeSyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class OverworldSkyModeEventListener {
    private static @Nullable ClientPacketListener lastConnection;
    private static boolean lastSpecial;

    private OverworldSkyModeEventListener() {
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var client = Minecraft.getInstance();
        var connection = client.getConnection();
        if (client.player == null || connection == null) {
            lastConnection = null;
            return;
        }
        boolean special = AnvilCraft.CLIENT_CONFIG.overworldSkyMode == OverworldSkyMode.SPECIAL;
        if (lastConnection == connection && lastSpecial == special) return;
        ClientPacketDistributor.sendToServer(new OverworldSkyModeSyncPacket(special));
        lastConnection = connection;
        lastSpecial = special;
    }
}
