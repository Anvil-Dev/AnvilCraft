package dev.dubhe.anvilcraft.data.recipe;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public interface RecipeSerializerBase<T extends Recipe<?>> extends RecipeSerializer<T> {
    default @NotNull NonNullList<TagIngredient> itemsFromJson(@NotNull JsonArray ingredientArray) {
        NonNullList<TagIngredient> nonNullList = NonNullList.create();
        for (int i = 0; i < ingredientArray.size(); ++i) {
            TagIngredient ingredient = TagIngredient.fromJson(ingredientArray.get(i));
            if (ingredient.isEmpty()) continue;
            nonNullList.add(ingredient);
        }
        return nonNullList;
    }

    default NonNullList<TagIngredient> dissolveShaped(String[] pattern, Map<String, TagIngredient> keys, int patternWidth, int patternHeight) {
        NonNullList<TagIngredient> ingredients = NonNullList.withSize(patternWidth * patternHeight, TagIngredient.EMPTY);
        HashSet<String> set1 = Sets.newHashSet(keys.keySet());
        set1.remove(" ");
        for (int i = 0; i < pattern.length; ++i) {
            for (int j = 0; j < pattern[i].length(); ++j) {
                String string = pattern[i].substring(j, j + 1);
                TagIngredient ingredient = keys.get(string);
                if (null == ingredient) {
                    throw new JsonSyntaxException("Pattern references symbol '" + string + "' but it's not defined in the key");
                }
                set1.remove(string);
                ingredients.set(j + patternWidth * i, ingredient);
            }
        }
        if (!set1.isEmpty()) {
            throw new JsonSyntaxException("Key defines symbols that aren't used in pattern: " + set1);
        }
        return ingredients;
    }

    default String[] patternFromJson(JsonArray patternArray) {
        String[] strings = new String[patternArray.size()];
        if (strings.length > 3) {
            throw new JsonSyntaxException("Invalid pattern: too many rows, 3 is maximum");
        }
        if (strings.length == 0) {
            throw new JsonSyntaxException("Invalid pattern: empty pattern not allowed");
        }
        for (int i = 0; i < strings.length; ++i) {
            String string = GsonHelper.convertToString(patternArray.get(i), "pattern[" + i + "]");
            if (string.length() > 3) {
                throw new JsonSyntaxException("Invalid pattern: too many columns, 3 is maximum");
            }
            if (i > 0 && strings[0].length() != string.length()) {
                throw new JsonSyntaxException("Invalid pattern: each row must be the same width");
            }
            strings[i] = string;
        }
        return strings;
    }

    default @NotNull Map<String, TagIngredient> shapedFromJson(@NotNull JsonObject keyEntry) {
        HashMap<String, TagIngredient> ingredientHashMap = Maps.newHashMap();
        for (Map.Entry<String, JsonElement> entry : keyEntry.entrySet()) {
            if (entry.getKey().length() != 1) {
                throw new JsonSyntaxException("Invalid key entry: '" + entry.getKey() + "' is an invalid symbol (must be 1 character only).");
            }
            if (" ".equals(entry.getKey())) {
                throw new JsonSyntaxException("Invalid key entry: ' ' is a reserved symbol.");
            }
            ingredientHashMap.put(entry.getKey(), TagIngredient.fromJson(entry.getValue()));
        }
        ingredientHashMap.put(" ", TagIngredient.EMPTY);
        return ingredientHashMap;
    }

    default NonNullList<TagIngredient> shapelessFromJson(@NotNull JsonArray array) {
        NonNullList<TagIngredient> ingredients = NonNullList.create();
        for (JsonElement element : array) {
            ingredients.add(TagIngredient.fromJson(element));
        }
        return ingredients;
    }

    default NonNullList<Component> componentsFromJson(@NotNull JsonArray array) {
        NonNullList<Component> components = NonNullList.create();
        for (JsonElement element : array) {
            components.add(Component.fromJson(element));
        }
        return components;
    }

    @VisibleForTesting
    default String[] shrink(String... toShrink) {
        int i = Integer.MAX_VALUE;
        int j = 0;
        int k = 0;
        int l = 0;
        for (int m = 0; m < toShrink.length; ++m) {
            String string = toShrink[m];
            i = Math.min(i, firstNonSpace(string));
            int n = lastNonSpace(string);
            j = Math.max(j, n);
            if (n < 0) {
                if (k == m) {
                    ++k;
                }
                ++l;
                continue;
            }
            l = 0;
        }
        if (toShrink.length == l) {
            return new String[0];
        }
        String[] strings = new String[toShrink.length - l - k];
        for (int o = 0; o < strings.length; ++o) {
            strings[o] = toShrink[o + k].substring(i, j + 1);
        }
        return strings;
    }

    default int firstNonSpace(@NotNull String entry) {
        int i = 0;
        while (i < entry.length() && entry.charAt(i) == ' ') ++i;
        return i;
    }

    default int lastNonSpace(@NotNull String entry) {
        int i = entry.length() - 1;
        while (i >= 0 && entry.charAt(i) == ' ') --i;
        return i;
    }

    default @NotNull ItemStack itemStackFromJson(JsonElement element) {
        if(!element.isJsonObject()) throw new JsonSyntaxException("Expected item to be string");
        JsonObject object = element.getAsJsonObject();
        Item item = ShapedRecipe.itemFromJson(object);
        int i = GsonHelper.getAsInt(object, "count", 1);
        if (i < 1) {
            throw new JsonSyntaxException("Invalid output count: " + i);
        }
        ItemStack stack = item.getDefaultInstance();
        stack.setCount(i);
        if (object.has("data")) {
            if (!object.get("data").isJsonPrimitive()) throw new JsonSyntaxException("Expected item to be string");
            CompoundTag tag;
            try {
                tag = TagParser.parseTag(object.get("data").getAsString());
            } catch (CommandSyntaxException ignored) {
                throw new JsonSyntaxException("Invalid NBT string");
            }
            stack.setTag(stack.getOrCreateTag().merge(tag));
        }
        return stack;
    }

    default NonNullList<BlockState> getBlockResults(@NotNull JsonArray array) {
        NonNullList<BlockState> results = NonNullList.create();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) throw new JsonSyntaxException("Expected item to be object");
            JsonObject object = element.getAsJsonObject();
            if (!object.has("block")) throw new JsonSyntaxException("The field block is missing");
            JsonElement blockElement = object.get("block");
            if (!blockElement.isJsonPrimitive()) throw new JsonSyntaxException("Expected item to be string");
            StringBuilder block = new StringBuilder(blockElement.getAsString());
            if (object.has("state")) {
                block.append("[");
                JsonElement stateElement = object.get("state");
                if (!stateElement.isJsonObject()) throw new JsonSyntaxException("Expected item to be object");
                JsonObject state = stateElement.getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : state.entrySet()) {
                    block.append("%s=%s, ".formatted(entry.getKey(), entry.getValue().getAsString()));
                }
                if (block.charAt(block.length() - 2) == ',') block.delete(block.length() - 2, block.length() - 1);
                block.append("]");
            }
            HolderLookup<Block> blocks = new BlockHolderLookup();
            BlockStateParser.BlockResult blockResult;
            try {
                blockResult = BlockStateParser.parseForBlock(blocks, block.toString(), true);
            } catch (CommandSyntaxException e) {
                throw new RuntimeException(e);
            }
            results.add(blockResult.blockState());
        }
        return results;
    }

    default BlockState stateFromNetwork(@NotNull FriendlyByteBuf buffer) {
        String block = buffer.readUtf();
        HolderLookup<Block> blocks = new BlockHolderLookup();
        BlockStateParser.BlockResult blockResult;
        try {
            blockResult = BlockStateParser.parseForBlock(blocks, block, true);
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
        return blockResult.blockState();
    }

    default void stateToNetwork(@NotNull FriendlyByteBuf buffer, @NotNull BlockState state) {
        buffer.writeUtf(state.toString());
    }

    class BlockHolderLookup implements HolderLookup<Block>, HolderOwner<Block> {
        @Override
        public @NotNull Stream<Holder.Reference<Block>> listElements() {
            return BuiltInRegistries.BLOCK.stream().map(BuiltInRegistries.BLOCK::getResourceKey).filter(Optional::isPresent).map(key -> BuiltInRegistries.BLOCK.getHolderOrThrow(key.get()));
        }

        @Override
        public @NotNull Stream<HolderSet.Named<Block>> listTags() {
            return BuiltInRegistries.BLOCK.getTags().map(Pair::getSecond);
        }

        @Override
        public @NotNull Optional<Holder.Reference<Block>> get(ResourceKey<Block> resourceKey) {
            return Optional.of(BuiltInRegistries.BLOCK.getHolderOrThrow(resourceKey));
        }

        @Override
        public @NotNull Optional<HolderSet.Named<Block>> get(TagKey<Block> tagKey) {
            return BuiltInRegistries.BLOCK.getTag(tagKey);
        }
    }
}
