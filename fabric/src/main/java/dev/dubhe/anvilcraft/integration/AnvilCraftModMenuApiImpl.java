package dev.dubhe.anvilcraft.integration;

import dev.dubhe.anvilcraft.config.AnvilCraftConfig;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;

public class AnvilCraftModMenuApiImpl implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return screen -> AutoConfig.getConfigScreen(AnvilCraftConfig.class, screen).get();
    }
}
