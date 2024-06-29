package dev.dubhe.anvilcraft.event.fabric;

import dev.dubhe.anvilcraft.api.entity.fakeplayer.fabric.AnvilCraftBlockPlacerFakePlayer;
import dev.dubhe.anvilcraft.api.entity.player.AnvilCraftBlockPlacer;
import dev.dubhe.anvilcraft.api.recipe.AnvilRecipeManager;
import dev.dubhe.anvilcraft.api.world.load.LevelLoadManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public class ServerWorldEventListener {
    /**
     * 初始化
     */
    public static void init() {
        ServerWorldEvents.LOAD.register(ServerWorldEventListener::onload);
        ServerWorldEvents.UNLOAD.register(ServerWorldEventListener::onUnload);
    }

    private static void onload(MinecraftServer server, Level level) {
        if (level instanceof ServerLevel serverLevel)
            AnvilCraftBlockPlacer.anvilCraftBlockPlacer = new AnvilCraftBlockPlacerFakePlayer(serverLevel);
        AnvilRecipeManager.updateRecipes(server.getRecipeManager(), server.registryAccess());
    }

    private static void onUnload(MinecraftServer server, Level level) {
        if (level instanceof ServerLevel serverLevel) LevelLoadManager.removeAll(serverLevel);
    }
}
