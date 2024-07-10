package dev.dubhe.anvilcraft.data.recipe.anvil.outcome;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilCraftingContext;
import dev.dubhe.anvilcraft.data.recipe.anvil.CanSetData;
import dev.dubhe.anvilcraft.data.recipe.anvil.RecipeOutcome;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class SelectOne implements RecipeOutcome, CanSetData {
    private final String type = "select_one";
    private final double chance;
    private final List<RecipeOutcome> outcomes = new ArrayList<>();
    @Setter
    private Map<String, CompoundTag> data = new HashMap<>();

    public boolean isEmpty() {
        return this.outcomes.isEmpty();
    }

    public SelectOne add(RecipeOutcome outcome) {
        this.outcomes.add(outcome);
        return this;
    }

    public SelectOne(double chance) {
        this.chance = chance;
    }

    public SelectOne() {
        this(1.0);
    }

    /**
     * @param buffer 缓冲区
     */
    public SelectOne(@NotNull FriendlyByteBuf buffer) {
        this(buffer.readDouble());
        int size = buffer.readVarInt();
        for (int i = 0; i < size; i++) {
            this.add(RecipeOutcome.fromNetwork(buffer));
        }
    }

    /**
     * @param serializedRecipe 序列化配方
     */
    public SelectOne(@NotNull JsonObject serializedRecipe) {
        if (serializedRecipe.has("chance")) {
            this.chance = GsonHelper.getAsDouble(serializedRecipe, "chance");
        } else this.chance = 1.0;
        if (!serializedRecipe.has("outcomes")) return;
        for (JsonElement element : GsonHelper.getAsJsonArray(serializedRecipe, "outcomes")) {
            if (element instanceof JsonObject object) this.add(RecipeOutcome.fromJson(object));
        }
    }

    @Override
    public boolean process(@NotNull AnvilCraftingContext context) {
        RandomSource random = context.getLevel().random;
        List<Double> weights = this.outcomes.stream().map(RecipeOutcome::getChance).toList();
        RecipeOutcome outcome = SelectOne.weightedRandomSelect(this.outcomes, weights, random);
        if (outcome != null) {
            if (outcome instanceof CanSetData canSetData) {
                canSetData.setData(this.data);
            }
            outcome.process(context);
        }
        return true;
    }

    private static @Nullable <T> T weightedRandomSelect(
        @NotNull List<T> elements, @NotNull List<Double> weights, RandomSource random
    ) {
        if (elements.size() != weights.size()) {
            throw new IllegalArgumentException("Elements and weights must be of the same size");
        }

        // 计算权重总和
        double totalWeight = weights.stream().mapToDouble(Double::doubleValue).sum();

        // 创建累积概率数组
        List<Double> cumulativeProbabilities = new ArrayList<>();
        for (double weight : weights) {
            cumulativeProbabilities.add(weight / totalWeight); // 归一化处理
        }

        double randomWeight = random.nextDouble(); // 生成0到1之间的随机浮点数

        for (int index = 0; index < cumulativeProbabilities.size(); index++) {
            randomWeight -= cumulativeProbabilities.get(index);
            if (randomWeight <= 0) {
                if (index >= elements.size()) break;
                return elements.get(index);
            }
        }
        return null;
    }

    @Override
    public void toNetwork(@NotNull FriendlyByteBuf buffer) {
        buffer.writeUtf(this.getType());
        buffer.writeDouble(this.chance);
        buffer.writeVarInt(this.outcomes.size());
        for (RecipeOutcome outcome : this.outcomes) {
            outcome.toNetwork(buffer);
        }
    }

    @Override
    public JsonElement toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("type", this.getType());
        object.addProperty("chance", this.chance);
        JsonArray outcomes = new JsonArray();
        for (RecipeOutcome outcome : this.outcomes) {
            outcomes.add(outcome.toJson());
        }
        object.add("outcomes", outcomes);
        return object;
    }
}
