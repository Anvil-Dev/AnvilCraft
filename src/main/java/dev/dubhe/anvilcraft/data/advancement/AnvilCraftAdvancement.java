package dev.dubhe.anvilcraft.data.advancement;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumAdvancementProvider;
import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.init.ModAdvancements;
import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class AnvilCraftAdvancement {
    @SneakyThrows
    public static void init(RegistrumAdvancementProvider provider) {
        for (Field field : ModAdvancements.class.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers)) {
                provider.accept(Util.cast(field.get(null)));
            }
        }
    }
}
