package dev.dubhe.anvilcraft.data.crafting;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.*;
import com.mojang.datafixers.util.Pair;
import lombok.Getter;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class ShapedTagRecipe extends CustomRecipe implements CraftingRecipe {
    @Getter
    final int width;
    @Getter
    final int height;
    @Getter
    final NonNullList<Ingredient> recipeItems;
    final ItemStack result;
    @Getter
    private final ResourceLocation id;
    @Getter
    final String group;
    final CraftingBookCategory category;
    final boolean showNotification;
    @Getter
    final NonNullList<TagPredicate> tagPredicates;

    public ShapedTagRecipe(ResourceLocation id, String group, CraftingBookCategory category, int width, int height, NonNullList<Ingredient> recipeItems, NonNullList<TagPredicate> tagPredicates, ItemStack result, boolean showNotification) {
        super(id, category);
        this.id = id;
        this.group = group;
        this.category = category;
        this.width = width;
        this.height = height;
        this.recipeItems = recipeItems;
        this.tagPredicates = tagPredicates;
        this.result = result;
        this.showNotification = showNotification;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public @NotNull CraftingBookCategory category() {
        return this.category;
    }

    @Override
    public @NotNull ItemStack getResultItem(RegistryAccess registryAccess) {
        return this.result;
    }

    @Override
    public @NotNull NonNullList<Ingredient> getIngredients() {
        return this.recipeItems;
    }

    @Override
    public boolean showNotification() {
        return this.showNotification;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= this.width && height >= this.height;
    }

    @Override
    public boolean matches(CraftingContainer inv, Level level) {
        for (int i = 0; i <= inv.getWidth() - this.width; ++i) {
            for (int j = 0; j <= inv.getHeight() - this.height; ++j) {
                if (this.matches(inv, i, j, true)) {
                    return true;
                }
                if (!this.matches(inv, i, j, false)) continue;
                return true;
            }
        }
        return false;
    }

    @Override
    public @NotNull ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        return this.getResultItem(registryAccess).copy();
    }

    /**
     * 检查制作物品栏的区域是否与配方匹配。
     */
    private boolean matches(@NotNull CraftingContainer craftingInventory, int width, int height, boolean mirrored) {
        for (int i = 0; i < craftingInventory.getWidth(); ++i) {
            for (int j = 0; j < craftingInventory.getHeight(); ++j) {
                int k = i - width;
                int l = j - height;
                Ingredient ingredient = Ingredient.EMPTY;
                TagPredicate tag = TagPredicate.EMPTY;
                if (k >= 0 && l >= 0 && k < this.width && l < this.height) {
                    ingredient = mirrored ? this.getIngredients().get(this.width - k - 1 + l * this.width) : this.getIngredients().get(k + l * this.width);
                    tag = mirrored ? this.tagPredicates.get(this.width - k - 1 + l * this.width) : this.tagPredicates.get(k + l * this.width);
                }
                ItemStack stack = craftingInventory.getItem(i + j * craftingInventory.getWidth());
                if (ingredient.test(stack) && tag.test(stack.getOrCreateTag())) continue;
                return false;
            }
        }
        return true;
    }

    public static class Serializer implements RecipeSerializer<ShapedTagRecipe> {
        public static final Serializer INSTANCE = new Serializer();
        public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

        @Override
        public @NotNull ShapedTagRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            String string = GsonHelper.getAsString(json, "group", "");
            CraftingBookCategory craftingBookCategory = CraftingBookCategory.CODEC.byName(GsonHelper.getAsString(json, "category", null), CraftingBookCategory.MISC);
            Map<String, Ingredient> map = keyFromJson(GsonHelper.getAsJsonObject(json, "key"));
            Map<String, TagPredicate> tag = tagFromJson(GsonHelper.getAsJsonObject(json, "key"));
            System.out.println(GSON.toJson(GsonHelper.getAsJsonArray(json, "pattern")));
            System.out.println(Arrays.toString(patternFromJson(GsonHelper.getAsJsonArray(json, "pattern"))));
            System.out.println(Arrays.toString(shrink(patternFromJson(GsonHelper.getAsJsonArray(json, "pattern")))));
            String[] strings = shrink(patternFromJson(GsonHelper.getAsJsonArray(json, "pattern")));
            int weight = strings[0].length();
            int height = strings.length;
            Pair<NonNullList<Ingredient>, NonNullList<TagPredicate>> pair = dissolvePattern(strings, map, tag, weight, height);
            NonNullList<Ingredient> ingredients = pair.getFirst();
            NonNullList<TagPredicate> tags = pair.getSecond();
            ItemStack itemStack = itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            boolean bl = GsonHelper.getAsBoolean(json, "show_notification", true);
            return new ShapedTagRecipe(recipeId, string, craftingBookCategory, weight, height, ingredients, tags, itemStack, bl);
        }

        @Override
        public @NotNull ShapedTagRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            int i = buffer.readVarInt();
            int j = buffer.readVarInt();
            String string = buffer.readUtf();
            CraftingBookCategory craftingBookCategory = buffer.readEnum(CraftingBookCategory.class);
            NonNullList<Ingredient> ingredients = NonNullList.withSize(i * j, Ingredient.EMPTY);
            ingredients.replaceAll(ignored -> Ingredient.fromNetwork(buffer));
            NonNullList<TagPredicate> predicates = NonNullList.withSize(i * j, TagPredicate.EMPTY);
            predicates.replaceAll(ignored -> TagPredicate.fromNetwork(buffer));
            ItemStack itemStack = buffer.readItem();
            boolean bl = buffer.readBoolean();
            return new ShapedTagRecipe(recipeId, string, craftingBookCategory, i, j, ingredients, predicates, itemStack, bl);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ShapedTagRecipe recipe) {
            buffer.writeVarInt(recipe.width);
            buffer.writeVarInt(recipe.height);
            buffer.writeUtf(recipe.getGroup());
            buffer.writeEnum(recipe.category());
            for (Ingredient ingredient : recipe.getIngredients()) {
                ingredient.toNetwork(buffer);
            }
            for (TagPredicate predicate : recipe.getTagPredicates()) {
                predicate.toNetwork(buffer);
            }
            buffer.writeItem(recipe.result);
            buffer.writeBoolean(recipe.showNotification());
        }

        static String[] patternFromJson(JsonArray patternArray) {
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

        /**
         * 以 Java HashMap 的形式返回键 json 对象。
         */
        static Map<String, Ingredient> keyFromJson(JsonObject keyEntry) {
            HashMap<String, Ingredient> map = Maps.newHashMap();
            for (Map.Entry<String, JsonElement> entry : keyEntry.entrySet()) {
                if (entry.getKey().length() != 1) {
                    throw new JsonSyntaxException("Invalid key entry: '" + entry.getKey() + "' is an invalid symbol (must be 1 character only).");
                }
                if (" ".equals(entry.getKey())) {
                    throw new JsonSyntaxException("Invalid key entry: ' ' is a reserved symbol.");
                }
                map.put(entry.getKey(), Ingredient.fromJson(entry.getValue(), false));
            }
            map.put(" ", Ingredient.EMPTY);
            return map;
        }

        static @NotNull Map<String, TagPredicate> tagFromJson(@NotNull JsonObject keyEntry) {
            HashMap<String, TagPredicate> map = Maps.newHashMap();
            for (Map.Entry<String, JsonElement> entry : keyEntry.entrySet()) {
                if (entry.getKey().length() != 1) {
                    throw new JsonSyntaxException("Invalid key entry: '" + entry.getKey() + "' is an invalid symbol (must be 1 character only).");
                }
                if (" ".equals(entry.getKey())) {
                    throw new JsonSyntaxException("Invalid key entry: ' ' is a reserved symbol.");
                }
                map.put(entry.getKey(), TagPredicate.fromJson(entry.getValue()));
            }
            map.put(" ", TagPredicate.EMPTY);
            return map;
        }

        static Pair<NonNullList<Ingredient>, NonNullList<TagPredicate>> dissolvePattern(String[] pattern, Map<String, Ingredient> keys, Map<String, TagPredicate> tags, int patternWidth, int patternHeight) {
            NonNullList<Ingredient> ingredients = NonNullList.withSize(patternWidth * patternHeight, Ingredient.EMPTY);
            NonNullList<TagPredicate> tagPredicates = NonNullList.withSize(patternWidth * patternHeight, TagPredicate.EMPTY);
            HashSet<String> set1 = Sets.newHashSet(keys.keySet());
            HashSet<String> set2 = Sets.newHashSet(tags.keySet());
            set1.remove(" ");
            set2.remove(" ");
            for (int i = 0; i < pattern.length; ++i) {
                for (int j = 0; j < pattern[i].length(); ++j) {
                    String string = pattern[i].substring(j, j + 1);
                    Ingredient ingredient = keys.get(string);
                    TagPredicate tagPredicate = tags.get(string);
                    if (null == ingredient || null == tagPredicate) {
                        throw new JsonSyntaxException("Pattern references symbol '" + string + "' but it's not defined in the key");
                    }
                    set1.remove(string);
                    set2.remove(string);
                    ingredients.set(j + patternWidth * i, ingredient);
                    tagPredicates.set(j + patternWidth * i, tagPredicate);
                }
            }
            if (!set1.isEmpty() || !set2.isEmpty()) {
                throw new JsonSyntaxException("Key defines symbols that aren't used in pattern: " + set1.addAll(set2));
            }
            return new Pair<>(ingredients, tagPredicates);
        }

        @VisibleForTesting
        static String[] shrink(String... toShrink) {
            int i = Integer.MAX_VALUE;
            int j = 0;
            int k = 0;
            int l = 0;
            for (int m = 0; m < toShrink.length; ++m) {
                String string = toShrink[m];
                i = Math.min(i, Serializer.firstNonSpace(string));
                int n = Serializer.lastNonSpace(string);
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

        private static int firstNonSpace(@NotNull String entry) {
            int i = 0;
            while (i < entry.length() && entry.charAt(i) == ' ') ++i;
            return i;
        }

        private static int lastNonSpace(@NotNull String entry) {
            int i = entry.length() - 1;
            while (i >= 0 && entry.charAt(i) == ' ') --i;
            return i;
        }

        public static @NotNull ItemStack itemStackFromJson(JsonObject object) {
            Item item = ShapedRecipe.itemFromJson(object);
            int i = GsonHelper.getAsInt(object, "count", 1);
            if (i < 1) {
                throw new JsonSyntaxException("Invalid output count: " + i);
            }
            ItemStack stack = item.getDefaultInstance();
            stack.setCount(i);
            if (object.has("data")) {
                if (!object.get("data").isJsonPrimitive()) throw new JsonSyntaxException("Expected item to be string");
                CompoundTag tag = TagPredicate.parseTag(object.get("data").getAsString());
                stack.setTag(stack.getOrCreateTag().merge(tag));
            }
            return stack;
        }
    }
}
