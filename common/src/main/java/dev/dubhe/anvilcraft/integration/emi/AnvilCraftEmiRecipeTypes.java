package dev.dubhe.anvilcraft.integration.emi;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiRenderable;
import dev.emi.emi.api.stack.EmiStack;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public class AnvilCraftEmiRecipeTypes {

    public static final Map<ResourceLocation, EmiRecipeCategory> ALL = new LinkedHashMap<>();

    public static final EmiRecipeCategory STAMPING = register("milling", EmiStack.of(ModBlocks.STAMPING_PLATFORM));

    private static EmiRecipeCategory register(String name, EmiRenderable icon) {
        ResourceLocation id = AnvilCraft.of(name);
        EmiRecipeCategory category = new EmiRecipeCategory(id, icon);
        ALL.put(id, category);
        return category;
    }
}
