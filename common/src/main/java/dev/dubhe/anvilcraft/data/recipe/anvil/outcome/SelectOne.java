package dev.dubhe.anvilcraft.data.recipe.anvil.outcome;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilCraftingContainer;
import dev.dubhe.anvilcraft.data.recipe.anvil.RecipeOutcome;
import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Getter
public class SelectOne implements RecipeOutcome {
    private final String type = "select_one";
    private final double chance;
    private final List<RecipeOutcome> outcomes = new ArrayList<>();

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
        for (int i = 0; i < buffer.readVarInt(); i++) {
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
    public boolean process(@NotNull AnvilCraftingContainer container) {
        RandomSource random = container.getLevel().random;
        if (random.nextDouble() > this.chance) return true;
        List<Double> weights = this.outcomes.stream().map(RecipeOutcome::getChance).toList();
        RecipeOutcome outcome = SelectOne.weightedRandomSelect(this.outcomes, weights, random);
        if (outcome != null) outcome.process(container);
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
        double cumulativeSum = 0;
        for (double weight : weights) {
            cumulativeSum += weight / totalWeight; // 归一化处理
            cumulativeProbabilities.add(cumulativeSum);
        }

        double randomWeight = random.nextDouble(); // 生成0到1之间的随机浮点数

        // 使用二分搜索找到对应的元素
        int index = binarySearch(cumulativeProbabilities, randomWeight);
        if (index < 0 || index >= elements.size()) return null;
        return elements.get(index);
    }

    private static int binarySearch(@NotNull List<Double> cumulativeProbabilities, double value) {
        int low = 0;
        int high = cumulativeProbabilities.size() - 1;

        while (low <= high) {
            int mid = (low + high) / 2;
            if (cumulativeProbabilities.get(mid) < value) {
                low = mid + 1;
            } else if (mid > 0 && cumulativeProbabilities.get(mid - 1) >= value) {
                high = mid - 1;
            } else {
                return mid;
            }
        }
        return -1; // 如果权重正确，则永远不应该到达这里
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
