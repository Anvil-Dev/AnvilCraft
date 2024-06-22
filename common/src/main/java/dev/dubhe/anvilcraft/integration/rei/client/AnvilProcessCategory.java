package dev.dubhe.anvilcraft.integration.rei.client;

import dev.dubhe.anvilcraft.integration.rei.display.AnvilProcessDisplay;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import net.minecraft.network.chat.Component;

public class AnvilProcessCategory implements DisplayCategory<AnvilProcessDisplay> {
    private AnvilProcessCategory() {

    }

    public AnvilProcessCategory of() {
        return new AnvilProcessCategory();
    }

    @Override
    public CategoryIdentifier<? extends AnvilProcessDisplay> getCategoryIdentifier() {
        return null;
    }

    @Override
    public Component getTitle() {
        return null;
    }

    @Override
    public Renderer getIcon() {
        return null;
    }
}
