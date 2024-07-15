package dev.dubhe.anvilcraft.data.recipe.multiblock;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.advancements.Advancement;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FinishedMultiblockCraftingRecipe implements FinishedRecipe {

    private final MultiblockCraftingRecipe recipe;
    private final Advancement.Builder advancement;
    private final ResourceLocation advancementId;

    /**
     * 生物转化配方
     */
    public FinishedMultiblockCraftingRecipe(
            MultiblockCraftingRecipe recipe,
            Advancement.Builder advancement,
            ResourceLocation advancementId
    ) {
        this.recipe = recipe;
        this.advancement = advancement;
        this.advancementId = advancementId;
    }

    @Override
    public void serializeRecipeData(@NotNull JsonObject json) {
        JsonElement jsonElement = MultiblockCraftingRecipe.CODEC
                .encodeStart(JsonOps.INSTANCE, this.recipe)
                .getOrThrow(false, ignored -> {
                });
        jsonElement.getAsJsonObject()
                .asMap()
                .forEach(json::add);
    }

    @Override
    public @NotNull ResourceLocation getId() {
        return recipe.getId();
    }

    @Override
    public @NotNull RecipeSerializer<?> getType() {
        return MultiblockCraftingRecipeSerializer.INSTANCE;
    }

    @Nullable
    @Override
    public JsonObject serializeAdvancement() {
        return advancement.serializeToJson();
    }

    @Nullable
    @Override
    public ResourceLocation getAdvancementId() {
        return advancementId;
    }
}
