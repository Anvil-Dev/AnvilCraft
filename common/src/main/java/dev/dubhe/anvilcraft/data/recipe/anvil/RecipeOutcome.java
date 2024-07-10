package dev.dubhe.anvilcraft.data.recipe.anvil;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;

/**
 * 配方结果
 */
public interface RecipeOutcome {
    Map<String, Function<JsonObject, RecipeOutcome>> JSON_DECODER = new HashMap<>();
    Map<String, Function<FriendlyByteBuf, RecipeOutcome>> NETWORK_DECODER = new HashMap<>();

    static void register(
        String id,
        Function<JsonObject, RecipeOutcome> jsonDecoder,
        Function<FriendlyByteBuf, RecipeOutcome> networkDecoder
    ) {
        RecipeOutcome.JSON_DECODER.put(id, jsonDecoder);
        RecipeOutcome.NETWORK_DECODER.put(id, networkDecoder);
    }

    String getType();

    boolean process(AnvilCraftingContext context);

    /**
     * 从 Json 读取
     *
     * @param serializedRecipe json
     * @return 配方结果
     */
    static @NotNull RecipeOutcome fromJson(JsonObject serializedRecipe) {
        String id = GsonHelper.getAsString(serializedRecipe, "type");
        if (!RecipeOutcome.JSON_DECODER.containsKey(id)) {
            throw new NoSuchElementException("Outcome type %s doesn't exist.".formatted(id));
        }
        return RecipeOutcome.JSON_DECODER.get(id).apply(serializedRecipe);
    }

    /**
     * 从网络读取
     *
     * @param buffer buffer
     * @return 配方结果
     */
    static @NotNull RecipeOutcome fromNetwork(@NotNull FriendlyByteBuf buffer) {
        String id = buffer.readUtf();
        if (!RecipeOutcome.NETWORK_DECODER.containsKey(id)) {
            throw new NoSuchElementException("Outcome type %s doesn't exist.".formatted(id));
        }
        return RecipeOutcome.NETWORK_DECODER.get(id).apply(buffer);
    }

    void toNetwork(FriendlyByteBuf buffer);

    JsonElement toJson();

    default double getChance() {
        return 1.0;
    }

    /**
     *
     */
    default boolean processWithChance(@NotNull AnvilCraftingContext context) {
        Level level = context.getLevel();
        RandomSource random = level.getRandom();
        if (random.nextDouble() > this.getChance()) return true;
        return this.process(context);
    }
}
