package dev.dubhe.anvilcraft.data.advancement;

import com.tterrag.registrate.providers.RegistrateAdvancementProvider;
import dev.dubhe.anvilcraft.init.ModAdvancements;
import dev.dubhe.anvilcraft.util.Util;
import lombok.SneakyThrows;
import net.minecraft.advancements.AdvancementHolder;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class AnvilCraftAdvancement {
    @SneakyThrows
    public static void init(RegistrateAdvancementProvider provider) {
        for (Field field : ModAdvancements.class.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && Modifier.isFinal(modifiers)) {
                provider.accept(Util.cast(field.get(null)));
            }
        }
    }
}
