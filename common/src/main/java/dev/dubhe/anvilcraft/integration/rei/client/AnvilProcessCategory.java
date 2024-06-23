package dev.dubhe.anvilcraft.integration.rei.client;

import dev.dubhe.anvilcraft.integration.rei.display.AnvilProcessDisplay;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class AnvilProcessCategory implements DisplayCategory<AnvilProcessDisplay> {
    private AnvilProcessCategory() {

    }

    public static @NotNull AnvilProcessCategory of() {
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

    @Override
    public List<Widget> setupDisplay(AnvilProcessDisplay display, Rectangle bounds) {
        return DisplayCategory.super.setupDisplay(display, bounds);
    }
}
