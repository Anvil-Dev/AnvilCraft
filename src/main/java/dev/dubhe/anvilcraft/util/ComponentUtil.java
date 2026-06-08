package dev.dubhe.anvilcraft.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.players.GameProfileCache;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class ComponentUtil {
    public static Component dimension(ResourceKey<Level> key) {
        return Component.translatable("dimension." + key.location().toString().replace(':', '.'));
    }

    public static Component findPlayerName(GameProfileCache cache, UUID id) {
        return cache.get(id)
            .map(GameProfile::getName)
            .map(Component::literal)
            .orElse(Component.literal("Unknown[" + id + "]"))
            .setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, id.toString())));
    }
}
