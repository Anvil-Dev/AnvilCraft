package dev.dubhe.anvilcraft.data.recipe.multiblock;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.NotNull;

public enum MultiblockCraftingRecipeSerializer implements RecipeSerializer<MultiblockCraftingRecipe> {
    INSTANCE;

    @Override
    public @NotNull MultiblockCraftingRecipe fromJson(
            @NotNull ResourceLocation recipeId,
            @NotNull JsonObject serializedRecipe
    ) {
        return MultiblockCraftingRecipe.CODEC
                .parse(JsonOps.INSTANCE, serializedRecipe)
                .getOrThrow(false, ignored -> {
                });
    }

    @Override
    public @NotNull MultiblockCraftingRecipe fromNetwork(@NotNull ResourceLocation recipeId, FriendlyByteBuf buffer) {
        CompoundTag tag = buffer.readNbt();
        Tag recipe = tag.get("Recipe");
        MultiblockCraftingRecipe result = MultiblockCraftingRecipe.CODEC.decode(NbtOps.INSTANCE, recipe)
                .getOrThrow(false, (s) -> {
                }).getFirst();
        return result;
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer, MultiblockCraftingRecipe recipe) {
        CompoundTag tag = new CompoundTag();
        var result = MultiblockCraftingRecipe.CODEC.encodeStart(NbtOps.INSTANCE, recipe);
        Tag tag1 = result.getOrThrow(false, (e) -> {
        });
        tag.put("Recipe", tag1);
        buffer.writeNbt(tag);
    }
}