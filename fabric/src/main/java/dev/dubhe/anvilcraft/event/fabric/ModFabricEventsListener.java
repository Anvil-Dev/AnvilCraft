package dev.dubhe.anvilcraft.event.fabric;

public class ModFabricEventsListener {
    /**
     * 初始化
     */
    public static void init() {
        ServerLifecycleEventListener.init();
        BlockEventListener.init();
        LootTableEventListener.init();
        LightningEventListener.init();
        AnvilEntityEventListener.init();
        PlayerEventListener.init();
        CommandEventListener.init();
        ServerWorldEventListener.init();
        CommonEventHandlerListener.serverInit();
    }
}
