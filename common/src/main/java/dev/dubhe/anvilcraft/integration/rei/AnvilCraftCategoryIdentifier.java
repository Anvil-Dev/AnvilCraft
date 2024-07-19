package dev.dubhe.anvilcraft.integration.rei;

import dev.dubhe.anvilcraft.integration.rei.client.AnvilRecipeDisplay;
import lombok.Getter;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

@Getter
public class AnvilCraftCategoryIdentifier {
    private CategoryIdentifier<AnvilRecipeDisplay> categoryIdentifier = null;
    private Component title = null;
    private Renderer icon = null;

    public static @NotNull AnvilCraftCategoryIdentifier creat() {
        return new AnvilCraftCategoryIdentifier();
    }

    public AnvilCraftCategoryIdentifier setCategoryIdentifier(
            CategoryIdentifier<AnvilRecipeDisplay> categoryIdentifier
    ) {
        this.categoryIdentifier = categoryIdentifier;
        return this;
    }

    public AnvilCraftCategoryIdentifier setTitle(Component title) {
        this.title = title;
        return this;
    }

    public AnvilCraftCategoryIdentifier setIcon(Renderer icon) {
        this.icon = icon;
        return this;
    }
}
