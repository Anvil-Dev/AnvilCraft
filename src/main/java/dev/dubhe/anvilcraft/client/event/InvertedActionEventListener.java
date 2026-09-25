package dev.dubhe.anvilcraft.client.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.AnvilCraftClient;
import dev.dubhe.anvilcraft.client.rpc.BundleLikeClientStub;
import dev.dubhe.anvilcraft.rpc.BundleLikeServerStub;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jspecify.annotations.Nullable;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public final class InvertedActionEventListener {
    private static @Nullable ClientPacketListener lastConnection;
    private static boolean lastInverted;

    private InvertedActionEventListener() {
    }

    public static boolean isInverted() {
        return AnvilCraftClient.CONFIG.invertOverrideAction;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var client = Minecraft.getInstance();
        var connection = client.getConnection();
        if (client.player == null || connection == null) return;
        boolean inverted = isInverted();
        BundleLikeServerStub.setClientInverted(inverted);
        if (lastConnection != connection || lastInverted != inverted) {
            BundleLikeClientStub.updateInverted(inverted);
            lastConnection = connection;
            lastInverted = inverted;
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        lastConnection = null;
        BundleLikeServerStub.setClientInverted(false);
    }
}
