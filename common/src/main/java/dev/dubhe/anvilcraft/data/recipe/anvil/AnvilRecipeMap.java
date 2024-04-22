package dev.dubhe.anvilcraft.data.recipe.anvil;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AnvilRecipeMap extends LinkedHashMap<ResourceLocation, Recipe<?>> {
    /**
     * 排序
     */
    public void sort() {
        List<Map.Entry<ResourceLocation, Recipe<?>>> entryList = new ArrayList<>();
        this.entrySet().forEach(entryList::add);
        this.clear();
        entryList.sort(this::compare);
        entryList.forEach(entry -> this.put(entry.getKey(), entry.getValue()));
    }

    /**
     * @return 不可变 Map
     */
    public Map<ResourceLocation, Recipe<?>> unmodifiable() {
        return Collections.unmodifiableMap(this);
    }

    private int compare(
        @NotNull Map.Entry<ResourceLocation, Recipe<?>> entry1,
        @NotNull Map.Entry<ResourceLocation, Recipe<?>> entry2
    ) {
        if (!(entry1.getValue() instanceof AnvilRecipe value1 && entry2.getValue() instanceof AnvilRecipe value2)) {
            return 0;
        }
        int size1 = value1.getPredicates().size();
        int size2 = value2.getPredicates().size();
        if (size1 != size2) return size1 - size2;
        size1 = value1.getOutcomes().size();
        size2 = value2.getOutcomes().size();
        return size1 - size2;
    }
}
