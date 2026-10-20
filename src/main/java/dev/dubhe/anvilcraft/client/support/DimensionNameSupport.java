package dev.dubhe.anvilcraft.client.support;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class DimensionNameSupport {
    private DimensionNameSupport() {
    }

    public static Component getName(Identifier dimension) {
        String key = dimension.toLanguageKey("dimension");
        if (!I18n.exists(key) && AnvilCraft.MOD_ID.equals(dimension.getNamespace())) {
            String celestialKey = "screen.anvilcraft.cfa.class.special." + dimension.getPath();
            if (I18n.exists(celestialKey)) {
                key = celestialKey;
            }
        }
        return Component.translatableWithFallback(key, dimension.toString());
    }
}
