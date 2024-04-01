package dev.dubhe.anvilcraft.data.recipe.anvil;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;

public interface RecipePredicate {
    Map<String, Function<JsonObject, RecipePredicate>> JSON_DECODER = new HashMap<>();
    Map<String, Function<FriendlyByteBuf, RecipePredicate>> NETWORK_DECODER = new HashMap<>();

    static void register(String id, Function<JsonObject, RecipePredicate> jsonDecoder, Function<FriendlyByteBuf, RecipePredicate> networkDecoder) {
        RecipePredicate.JSON_DECODER.put(id, jsonDecoder);
        RecipePredicate.NETWORK_DECODER.put(id, networkDecoder);
    }

    String getType();

    boolean matches(AnvilCraftingContainer container);

    boolean process(AnvilCraftingContainer container);

    static @NotNull RecipePredicate fromJson(JsonObject serializedRecipe) {
        String id = GsonHelper.getAsString(serializedRecipe, "type");
        if (!RecipePredicate.JSON_DECODER.containsKey(id)) {
            throw new NoSuchElementException("Outcome type %s doesn't exist.".formatted(id));
        }
        return RecipePredicate.JSON_DECODER.get(id).apply(serializedRecipe);
    }

    static @NotNull RecipePredicate fromNetwork(@NotNull FriendlyByteBuf buffer) {
        String id = buffer.readUtf();
        if (!RecipePredicate.NETWORK_DECODER.containsKey(id)) {
            throw new NoSuchElementException("Outcome type %s doesn't exist.".formatted(id));
        }
        return RecipePredicate.NETWORK_DECODER.get(id).apply(buffer);
    }

    void toNetwork(FriendlyByteBuf buffer);

    @NotNull JsonElement toJson();
}
