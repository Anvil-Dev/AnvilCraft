package dev.dubhe.anvilcraft.integration.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.dubhe.anvilcraft.config.AnvilCraftConfig;

public class AnvilCraftModMenuApiImpl implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return AnvilCraftConfig::getConfigScreen;
    }
}
