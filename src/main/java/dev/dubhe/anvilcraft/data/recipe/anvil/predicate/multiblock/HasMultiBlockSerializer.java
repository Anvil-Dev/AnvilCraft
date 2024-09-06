package dev.dubhe.anvilcraft.data.recipe.anvil.predicate.multiblock;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.NotNull;

public enum HasMultiBlockSerializer {
    INSTANCE;


    static @NotNull HasMultiBlock fromJson(
            @NotNull JsonObject serializedRecipe
    ) {
        return HasMultiBlock.CODEC
                .parse(JsonOps.INSTANCE, serializedRecipe)
                .getOrThrow(false, ignored -> {
                });
    }

    static @NotNull HasMultiBlock fromNetwork(FriendlyByteBuf buffer) {
        CompoundTag tag = buffer.readNbt();
        Tag recipe = tag.get("Recipe");
        return HasMultiBlock.CODEC.decode(NbtOps.INSTANCE, recipe)
                .getOrThrow(false, (s) -> {
                }).getFirst();
    }

    static void toNetwork(FriendlyByteBuf buffer, HasMultiBlock recipe) {
        CompoundTag tag = new CompoundTag();
        var result = HasMultiBlock.CODEC.encodeStart(NbtOps.INSTANCE, recipe);
        Tag tag1 = result.getOrThrow(false, (e) -> {
        });
        tag.put("Recipe", tag1);
        buffer.writeNbt(tag);
    }
}