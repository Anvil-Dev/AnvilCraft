package dev.dubhe.anvilcraft.integration.rei.client;

import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import org.jetbrains.annotations.NotNull;

public class AnvilCraftReiClientPlugin implements REIClientPlugin {
    @Override
    public void registerCategories(@NotNull CategoryRegistry registry) {
        registry.add(AnvilProcessCategory.of());
    }
}
