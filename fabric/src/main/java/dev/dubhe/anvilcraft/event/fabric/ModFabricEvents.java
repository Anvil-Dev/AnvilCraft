package dev.dubhe.anvilcraft.event.fabric;

public class ModFabricEvents {
    /**
     * 初始化
     */
    public static void init() {
        ServerLifecycleEvent.init();
        BlockEvent.init();
        LootTableEvent.init();
        LightningEvent.init();
        AnvilEntityEvent.init();
        CommandEvent.init();
    }
}
