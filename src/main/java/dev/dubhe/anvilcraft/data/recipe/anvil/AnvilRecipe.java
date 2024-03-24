package dev.dubhe.anvilcraft.data.recipe.anvil;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.HasItem;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.HasItemIngredient;
import dev.dubhe.anvilcraft.util.IItemStackInjector;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AnvilRecipe implements Recipe<AnvilCraftingContainer> {
    private final ResourceLocation id;
    private final List<RecipePredicate> predicates = new ArrayList<>();
    private final List<RecipeOutcome> outcomes = new ArrayList<>();
    private final ItemStack icon;

    public AnvilRecipe(ResourceLocation id, ItemStack icon) {
        this.id = id;
        this.icon = icon;
    }

    @SuppressWarnings("UnusedReturnValue")
    public AnvilRecipe addPredicates(RecipePredicate... predicates) {
        this.predicates.addAll(Arrays.stream(predicates).toList());
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public AnvilRecipe addOutcomes(RecipeOutcome... outcomes) {
        this.outcomes.addAll(Arrays.stream(outcomes).toList());
        return this;
    }

    @Override
    public boolean matches(AnvilCraftingContainer container, Level level) {
        for (RecipePredicate predicate : this.predicates) {
            if (!predicate.matches(container)) return false;
        }
        return true;
    }

    public boolean craft(@NotNull AnvilCraftingContainer container) {
        if (!this.matches(container, container.getLevel())) return false;
        for (RecipePredicate predicate : this.predicates) predicate.process(container);
        for (RecipeOutcome outcome : this.outcomes) outcome.process(container);
        return true;
    }

    @Override
    public @NotNull ItemStack assemble(AnvilCraftingContainer container, RegistryAccess registryAccess) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(RegistryAccess registryAccess) {
        return this.icon;
    }

    @Override
    public @NotNull ResourceLocation getId() {
        return this.id;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return Type.INSTANCE;
    }

    public enum Serializer implements RecipeSerializer<AnvilRecipe> {
        INSTANCE;

        @Override
        public @NotNull AnvilRecipe fromJson(ResourceLocation recipeId, @NotNull JsonObject serializedRecipe) {
            ItemStack icon = ItemStack.EMPTY;
            if (serializedRecipe.has("icon")) {
                icon = IItemStackInjector.fromJson(serializedRecipe.get("icon"));
            }
            AnvilRecipe recipe = new AnvilRecipe(recipeId, icon);
            JsonArray predicates = GsonHelper.getAsJsonArray(serializedRecipe, "predicates");
            for (JsonElement predicate : predicates) {
                if (!predicate.isJsonObject()) {
                    throw new JsonSyntaxException("The predicate in the Anvil Recipe should be an object.");
                }
                recipe.addPredicates(RecipePredicate.fromJson(predicate.getAsJsonObject()));
            }
            JsonArray outcomes = GsonHelper.getAsJsonArray(serializedRecipe, "outcomes");
            for (JsonElement outcome : outcomes) {
                if (!outcome.isJsonObject()) {
                    throw new JsonSyntaxException("The outcome in the Anvil Recipe should be an object.");
                }
                recipe.addOutcomes(RecipeOutcome.fromJson(outcome.getAsJsonObject()));
            }
            return recipe;
        }

        @Override
        public @NotNull AnvilRecipe fromNetwork(ResourceLocation recipeId, @NotNull FriendlyByteBuf buffer) {
            AnvilRecipe recipe = new AnvilRecipe(recipeId, buffer.readItem());
            int size;
            size = buffer.readVarInt();
            for (int i = 0; i < size; i++) {
                recipe.addPredicates(RecipePredicate.fromNetwork(buffer));
            }
            size = buffer.readVarInt();
            for (int i = 0; i < size; i++) {
                recipe.addOutcomes(RecipeOutcome.fromNetwork(buffer));
            }
            return recipe;
        }

        @Override
        public void toNetwork(@NotNull FriendlyByteBuf buffer, @NotNull AnvilRecipe recipe) {
            buffer.writeItem(recipe.icon);
            buffer.writeVarInt(recipe.predicates.size());
            for (RecipePredicate predicate : recipe.predicates) {
                predicate.toNetwork(buffer);
            }
            buffer.writeVarInt(recipe.outcomes.size());
            for (RecipeOutcome outcome : recipe.outcomes) {
                outcome.toNetwork(buffer);
            }
        }
    }

    public enum Type implements RecipeType<AnvilRecipe> {
        INSTANCE
    }

    static {
        RecipePredicate.register("has_item", HasItem::new, HasItem::new);
        RecipePredicate.register("has_item_ingredient", HasItemIngredient::new, HasItemIngredient::new);
    }
}
