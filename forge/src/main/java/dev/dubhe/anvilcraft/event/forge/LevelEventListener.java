package dev.dubhe.anvilcraft.event.forge;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.chargecollector.ChargeCollectorManager;
import dev.dubhe.anvilcraft.client.renderer.PowerGridRenderer;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import net.minecraft.world.InteractionHand;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class LevelEventListener {
    @SubscribeEvent
    public static void anvilHammerAttack(@NotNull LevelEvent.Unload event) {
        PowerGridRenderer.cleanAllGrid();
        ChargeCollectorManager.cleanMap();
    }

}
