package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.entity.fakeplayer.AnvilCraftBlockPlacerFakePlayer;
import dev.dubhe.anvilcraft.api.entity.fakeplayer.AnvilCraftDestroyerFakePlayer;
import dev.dubhe.anvilcraft.api.entity.fakeplayer.AnvilCraftFakePlayers;
import dev.dubhe.anvilcraft.api.entity.fakeplayer.AnvilCraftKillerFakePlayer;
import dev.dubhe.anvilcraft.api.heat.collector.HeatCollectorManager;
import dev.dubhe.anvilcraft.api.world.load.LevelLoadManager;
import dev.dubhe.anvilcraft.block.entity.DeflectionRingBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class LevelEventListener {

    /// 世界加载事件
    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            AnvilCraftFakePlayers.anvilcraftBlockPlacer = new AnvilCraftBlockPlacerFakePlayer(serverLevel);
            AnvilCraftFakePlayers.anvilcraftKiller = new AnvilCraftKillerFakePlayer();
            AnvilCraftFakePlayers.anvilcraftDestroyer = new AnvilCraftDestroyerFakePlayer();
        }
    }

    /// 世界卸载事件
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        LevelAccessor accessor = event.getLevel();
        if (accessor instanceof Level level) {
            if (accessor instanceof ServerLevel serverLevel) {
                LevelLoadManager.removeAll(serverLevel);
            }
            HeatCollectorManager.remove(level);
        }
        DeflectionRingBlockEntity.clear();
    }
}
