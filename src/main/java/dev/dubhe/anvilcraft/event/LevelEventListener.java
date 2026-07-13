package dev.dubhe.anvilcraft.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.entity.fakeplayer.AnvilCraftBlockPlacerFakePlayer;
import dev.dubhe.anvilcraft.api.entity.fakeplayer.AnvilCraftDestroyerFakePlayer;
import dev.dubhe.anvilcraft.api.entity.fakeplayer.AnvilCraftFakePlayers;
import dev.dubhe.anvilcraft.api.entity.fakeplayer.AnvilCraftKillerFakePlayer;
import dev.dubhe.anvilcraft.api.world.load.LevelLoadManager;
import dev.dubhe.anvilcraft.block.RedstoneWireNetworkManager;
import dev.dubhe.anvilcraft.block.entity.AccelerationRingBlockEntity;
import dev.dubhe.anvilcraft.block.entity.DeflectionRingBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class LevelEventListener {

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            // 网络缓存不持久化，区块进入服务端内存时必须用其中的导线作为重建种子。
            RedstoneWireNetworkManager.chunkLoaded(serverLevel, event.getChunk());
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            // 主动拆除跨区块缓存，避免网络继续引用已卸载节点或从其读取幽灵信号。
            RedstoneWireNetworkManager.chunkUnloaded(serverLevel, event.getChunk().getPos());
        }
    }

    /**
     * 世界加载事件
     */
    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            AnvilCraftFakePlayers.anvilcraftBlockPlacer = new AnvilCraftBlockPlacerFakePlayer(serverLevel);
            AnvilCraftFakePlayers.anvilcraftKiller = new AnvilCraftKillerFakePlayer();
            AnvilCraftFakePlayers.anvilcraftDestroyer = new AnvilCraftDestroyerFakePlayer();
        }
    }

    /**
     * 世界卸载事件
     */
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level) {
            AccelerationRingBlockEntity.clear(level);
            DeflectionRingBlockEntity.clear(level);
        }
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            LevelLoadManager.removeAll(serverLevel);
            // LEVELS 按 ServerLevel 对象持有强引用，世界卸载时清理才能释放整张拓扑缓存。
            RedstoneWireNetworkManager.clear(serverLevel);
        }
    }
}
