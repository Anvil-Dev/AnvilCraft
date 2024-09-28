package dev.dubhe.anvilcraft.event.server;

import dev.dubhe.anvilcraft.api.hammer.HammerManager;
import dev.dubhe.anvilcraft.api.recipe.AnvilRecipeManager;
import dev.dubhe.anvilcraft.api.world.load.LevelLoadManager;
import dev.dubhe.anvilcraft.init.ModHammerInits;
import org.jetbrains.annotations.NotNull;

public class ServerEventListener {
    /**
     * 服务器开启事件
     *
     * @param event 事件
     */
    @SuppressWarnings("unused")
    @SubscribeEvent
    public void onServerStarted(@NotNull ServerStartedEvent event) {
        ModHammerInits.init();
        HammerManager.register();
        AnvilRecipeManager.updateRecipes(event.getServer().getRecipeManager(), event.getServer().registryAccess());
        LevelLoadManager.notifyServerStarted();
    }

    @SuppressWarnings("unused")
    @SubscribeEvent
    public void onServerEndDataPackReload(@NotNull ServerEndDataPackReloadEvent event) {
        AnvilRecipeManager.updateRecipes(event.getServer().getRecipeManager(), event.getServer().registryAccess());
    }

}
