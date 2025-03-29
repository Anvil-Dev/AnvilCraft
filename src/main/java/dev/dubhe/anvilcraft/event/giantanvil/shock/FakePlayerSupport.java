package dev.dubhe.anvilcraft.event.giantanvil.shock;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber
public class FakePlayerSupport {
    public static final GameProfile GAME_PROFILE = new GameProfile(UUID.fromString("11451400-4ed2-4b39-8ef5-7d39ce3379a6"), "TheGiantAnvil");

    private static Map<ServerLevel, FakePlayer> fakePlayers = new HashMap<>();

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            if (fakePlayers.get(level) == null) {
                FakePlayer fakePlayer = FakePlayerFactory.get(level, GAME_PROFILE);
                fakePlayers.put(level, fakePlayer);
            }
        }
    }

    public static FakePlayer get(ServerLevel level) {
        return fakePlayers.get(level);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            fakePlayers.remove(level);
        }
    }
}
