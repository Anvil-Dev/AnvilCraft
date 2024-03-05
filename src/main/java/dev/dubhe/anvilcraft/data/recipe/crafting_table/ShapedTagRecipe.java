package dev.dubhe.anvilcraft.data.recipe.crafting_table;

import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import dev.dubhe.anvilcraft.data.recipe.CompoundTagPredicate;
import dev.dubhe.anvilcraft.data.recipe.RecipeSerializerBase;
import lombok.Getter;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

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
    final NonNullList<CompoundTagPredicate> compoundTagPredicates;

    public ShapedTagRecipe(ResourceLocation id, String group, CraftingBookCategory category, int width, int height, NonNullList<Ingredient> recipeItems, NonNullList<CompoundTagPredicate> compoundTagPredicates, ItemStack result, boolean showNotification) {
        super(id, category);
        this.id = id;
        this.group = group;
        this.category = category;
        this.width = width;
        this.height = height;
        this.recipeItems = recipeItems;
        this.compoundTagPredicates = compoundTagPredicates;
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
                CompoundTagPredicate tag = CompoundTagPredicate.EMPTY;
                if (k >= 0 && l >= 0 && k < this.width && l < this.height) {
                    ingredient = mirrored ? this.getIngredients().get(this.width - k - 1 + l * this.width) : this.getIngredients().get(k + l * this.width);
                    tag = mirrored ? this.compoundTagPredicates.get(this.width - k - 1 + l * this.width) : this.compoundTagPredicates.get(k + l * this.width);
                }
                ItemStack stack = craftingInventory.getItem(i + j * craftingInventory.getWidth());
                if (ingredient.test(stack) && tag.test(stack.getOrCreateTag())) continue;
                return false;
            }
        }
        return true;
    }

    public static class Serializer implements RecipeSerializerBase<ShapedTagRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        private Serializer() {
        }

        @Override
        public @NotNull ShapedTagRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            String group = GsonHelper.getAsString(json, "group", "");
            CraftingBookCategory category = CraftingBookCategory.CODEC.byName(GsonHelper.getAsString(json, "category", null), CraftingBookCategory.MISC);
            Pair<Map<String, Ingredient>, Map<String, CompoundTagPredicate>> input = shapedFromJson(GsonHelper.getAsJsonObject(json, "key"));
            String[] strings = shrink(patternFromJson(GsonHelper.getAsJsonArray(json, "pattern")));
            int weight = strings[0].length();
            int height = strings.length;
            Pair<NonNullList<Ingredient>, NonNullList<CompoundTagPredicate>> pair = dissolveShaped(strings, input.getFirst(), input.getSecond(), weight, height);
            NonNullList<Ingredient> ingredients = pair.getFirst();
            NonNullList<CompoundTagPredicate> tags = pair.getSecond();
            ItemStack stack = itemStackFromJson(GsonHelper.getAsJsonObject(json, "result"));
            boolean bl = GsonHelper.getAsBoolean(json, "show_notification", true);
            return new ShapedTagRecipe(recipeId, group, category, weight, height, ingredients, tags, stack, bl);
        }

        @Override
        public @NotNull ShapedTagRecipe fromNetwork(ResourceLocation recipeId, @NotNull FriendlyByteBuf buffer) {
            int i = buffer.readVarInt();
            int j = buffer.readVarInt();
            String string = buffer.readUtf();
            CraftingBookCategory craftingBookCategory = buffer.readEnum(CraftingBookCategory.class);
            NonNullList<Ingredient> ingredients = NonNullList.withSize(i * j, Ingredient.EMPTY);
            ingredients.replaceAll(ignored -> Ingredient.fromNetwork(buffer));
            NonNullList<CompoundTagPredicate> predicates = NonNullList.withSize(i * j, CompoundTagPredicate.EMPTY);
            predicates.replaceAll(ignored -> CompoundTagPredicate.fromNetwork(buffer));
            ItemStack itemStack = buffer.readItem();
            boolean bl = buffer.readBoolean();
            return new ShapedTagRecipe(recipeId, string, craftingBookCategory, i, j, ingredients, predicates, itemStack, bl);
        }

        @Override
        public void toNetwork(@NotNull FriendlyByteBuf buffer, @NotNull ShapedTagRecipe recipe) {
            buffer.writeVarInt(recipe.width);
            buffer.writeVarInt(recipe.height);
            buffer.writeUtf(recipe.getGroup());
            buffer.writeEnum(recipe.category());
            for (Ingredient ingredient : recipe.getIngredients()) {
                ingredient.toNetwork(buffer);
            }
            for (CompoundTagPredicate predicate : recipe.getCompoundTagPredicates()) {
                predicate.toNetwork(buffer);
            }
            buffer.writeItem(recipe.result);
            buffer.writeBoolean(recipe.showNotification());
        }
    }
}
