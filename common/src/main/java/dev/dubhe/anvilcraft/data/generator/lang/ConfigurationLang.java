package dev.dubhe.anvilcraft.data.generator.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.config.AnvilCraftConfig;
import dev.toma.configuration.Configuration;
import dev.toma.configuration.config.format.ConfigFormats;
import dev.toma.configuration.config.value.ConfigValue;
import dev.toma.configuration.config.value.ObjectValue;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ConfigurationLang {
    public static void init(RegistrateLangProvider provider) {
        dfs(provider, new HashSet<>(), Configuration.registerConfig(AnvilCraftConfig.class, ConfigFormats.yaml()).getValueMap());
    }

    private static void dfs(RegistrateLangProvider provider, Set<String> added, @NotNull Map<String, ConfigValue<?>> map) {
        for (var entry : map.entrySet()) {
            String id = entry.getValue().getId();
            if (added.add(id)) {
                provider.add("config.%s.option.%s".formatted(AnvilCraft.MOD_ID, id), id);
            }
            if (entry.getValue() instanceof ObjectValue objectValue) {
                dfs(provider, added, objectValue.get());
            }
        }
    }
}
