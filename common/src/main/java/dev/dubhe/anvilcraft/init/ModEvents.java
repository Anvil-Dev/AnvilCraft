package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.event.AnvilEventListener;
import dev.dubhe.anvilcraft.event.LightningEventListener;
import dev.dubhe.anvilcraft.event.ServerEventListener;

public class ModEvents {
    /**
     * 注册模组事件
     */
    public static void register() {
        AnvilCraft.EVENT_BUS.register(new AnvilEventListener());
        AnvilCraft.EVENT_BUS.register(new LightningEventListener());
        AnvilCraft.EVENT_BUS.register(new ServerEventListener());
    }
}
