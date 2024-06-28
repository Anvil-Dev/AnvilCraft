package dev.dubhe.anvilcraft.event.forge;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.entity.fakeplayer.forge.AnvilCraftBlockPlacerFakePlayer;
import dev.dubhe.anvilcraft.api.entity.player.AnvilCraftBlockPlacer;
import dev.dubhe.anvilcraft.api.recipe.AnvilRecipeManager;
import dev.dubhe.anvilcraft.api.world.load.LevelLoadManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

@Mod.EventBusSubscriber(modid = AnvilCraft.MOD_ID)
public class LevelEventListener {

    /**
     * 世界加载事件
     */
    @SubscribeEvent
    public static void onLevelLoad(@NotNull LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel serverLevel)
            AnvilCraftBlockPlacer.anvilCraftBlockPlacer =
                new AnvilCraftBlockPlacerFakePlayer(serverLevel);
        if (event.getLevel() instanceof ClientLevel clientLevel) {
            AnvilRecipeManager.updateRecipes(clientLevel.getRecipeManager(), clientLevel.registryAccess());
        }
    }

    /**
     * 世界卸载事件
     */
    @SubscribeEvent
    public static void onLevelUnload(@NotNull LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            LevelLoadManager.removeAll(serverLevel);
        }
    }
}
