package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.event.anvil.AnvilEventListener;
import dev.dubhe.anvilcraft.event.anvil.AnvilHitBlockDevourerEventListener;
import dev.dubhe.anvilcraft.event.anvil.AnvilHitBlockPlacerEventListener;
import dev.dubhe.anvilcraft.event.anvil.AnvilHitImpactPileEventListener;
import dev.dubhe.anvilcraft.event.anvil.AnvilHitPiezoelectricCrystalBlockEventListener;
import dev.dubhe.anvilcraft.event.anvil.AnvilHurtVillagerEventListener;
import dev.dubhe.anvilcraft.event.anvil.GiantAnvilMultiblockCraftingEventListener;
import dev.dubhe.anvilcraft.event.LightningEventListener;
import dev.dubhe.anvilcraft.event.PlayerEventListener;
import dev.dubhe.anvilcraft.event.ServerBlockEntityEventListener;
import dev.dubhe.anvilcraft.event.client.ClientPlayerDisconnectEventListener;
import dev.dubhe.anvilcraft.event.server.ServerEventListener;

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
        AnvilCraft.EVENT_BUS.register(new GiantAnvilMultiblockCraftingEventListener());
        AnvilCraft.EVENT_BUS.register(new ServerBlockEntityEventListener());
        AnvilCraft.EVENT_BUS.register(new ClientPlayerDisconnectEventListener());
    }
}
