package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.api.event.CheckIntegrationLoadedEvent;
import dev.dubhe.anvilcraft.api.event.GuideBookEvent;
import dev.dubhe.anvilcraft.integration.IntegrationUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;

public class ModEventUtil {
    public static boolean hasGuideBook() {
        return NeoForge.EVENT_BUS.post(new GuideBookEvent.HasGuideBookEvent()).isHasGuideBook();
    }

    public static void openGuideBook(Level level, ServerPlayer player, InteractionHand usedHand) {
        NeoForge.EVENT_BUS.post(new GuideBookEvent.OpenGuideBookEvent(level, player, usedHand));
    }

    public static IntegrationUtil.LoadStatus checkIntegration(String id, boolean hasExtra) {
        boolean loaded = ModList.get().isLoaded(id);
        if (!loaded) return IntegrationUtil.LoadStatus.NOT_FOUND;
        if (!hasExtra) return IntegrationUtil.LoadStatus.LOADED;
        CheckIntegrationLoadedEvent event = new CheckIntegrationLoadedEvent(id);
        NeoForge.EVENT_BUS.post(event);
        return event.isLoaded() ? IntegrationUtil.LoadStatus.LOADED : IntegrationUtil.LoadStatus.NOT_LOADED;
    }
}
