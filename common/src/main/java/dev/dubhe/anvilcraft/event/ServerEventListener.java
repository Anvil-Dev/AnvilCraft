package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.api.event.SubscribeEvent;
import dev.dubhe.anvilcraft.api.event.server.ServerEndDataPackReloadEvent;
import dev.dubhe.anvilcraft.api.event.server.ServerStartedEvent;
import dev.dubhe.anvilcraft.api.hammer.HammerManager;
import dev.dubhe.anvilcraft.api.recipe.AnvilRecipeManager;
import dev.dubhe.anvilcraft.init.ModHammerInits;

import org.jetbrains.annotations.NotNull;

public class ServerEventListener {
    /**
     * 服务器开启事件
     *
     * @param event 事件
     */
    @SubscribeEvent
    public void onServerStarted(@NotNull ServerStartedEvent event) {
        ModHammerInits.init();
        HammerManager.register();
        AnvilRecipeManager.updateRecipes(event.getServer());
    }

    @SubscribeEvent
    public void onServerEndDataPackReload(@NotNull ServerEndDataPackReloadEvent event) {
        AnvilRecipeManager.updateRecipes(event.getServer());
    }
}
