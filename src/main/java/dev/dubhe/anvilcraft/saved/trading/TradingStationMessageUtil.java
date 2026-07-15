package dev.dubhe.anvilcraft.saved.trading;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.world.level.Level;

import java.util.UUID;

final class TradingStationMessageUtil {
    private TradingStationMessageUtil() {
    }

    static Component dimension(ResourceKey<Level> key) {
        return Component.translatable("dimension." + key.identifier().toString().replace(':', '.'));
    }

    static Component findPlayerName(MinecraftServer server, UUID id) {
        return server.services().nameToIdCache().get(id)
            .map(NameAndId::name)
            .map(Component::literal)
            .orElse(Component.literal("Unknown[" + id + "]"))
            .withStyle(style -> style.withClickEvent(new ClickEvent.CopyToClipboard(id.toString())));
    }
}
