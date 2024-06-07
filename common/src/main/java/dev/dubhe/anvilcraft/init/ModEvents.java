package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.event.*;

public class ModEvents {
    /**
     * 注册模组事件
     */
    public static void register() {
        AnvilCraft.EVENT_BUS.register(new AnvilEventListener());
        AnvilCraft.EVENT_BUS.register(new LightningEventListener());
        AnvilCraft.EVENT_BUS.register(new ServerEventListener());
        AnvilCraft.EVENT_BUS.register(new PlayerEventListener());
        AnvilCraft.EVENT_BUS.register(new AnvilHitPiezoelectricCrystalBlockEventListener());
        AnvilCraft.EVENT_BUS.register(new AnvilHurtVillagerEventListener());
        AnvilCraft.EVENT_BUS.register(new AnvilHitBlockPlacerEventListener());
        AnvilCraft.EVENT_BUS.register(new AnvilHitBlockDevourerEventListener());
        AnvilCraft.EVENT_BUS.register(new AnvilHitImpactPileEventListener());
    }
}
