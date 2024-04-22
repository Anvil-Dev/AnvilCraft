package dev.dubhe.anvilcraft.data.recipe.anvil.outcome;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilCraftingContainer;
import dev.dubhe.anvilcraft.data.recipe.anvil.RecipeOutcome;
import dev.dubhe.anvilcraft.util.IItemStackUtil;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

@Getter
public class SpawnItem implements RecipeOutcome {
    private final String type = "spawn_item";
    private final Vec3 offset;
    private final double chance;
    private final ItemStack result;

    /**
     * 产生物品
     *
     * @param offset 偏移
     * @param chance 几率
     * @param result 产物
     */
    public SpawnItem(Vec3 offset, double chance, ItemStack result) {
        this.offset = offset;
        this.chance = chance;
        this.result = result;
    }

    /**
     * @param buffer 缓冲区
     */
    public SpawnItem(@NotNull FriendlyByteBuf buffer) {
        this.offset = new Vec3(buffer.readVector3f());
        this.chance = buffer.readDouble();
        this.result = buffer.readItem();
    }

    /**
     * @param serializedRecipe json
     */
    public SpawnItem(@NotNull JsonObject serializedRecipe) {
        double[] vec3 = {0.0d, 0.0d, 0.0d};
        if (serializedRecipe.has("offset")) {
            JsonArray array = GsonHelper.getAsJsonArray(serializedRecipe, "offset");
            for (int i = 0; i < array.size() && i < 3; i++) {
                JsonElement element = array.get(i);
                if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                    vec3[i] = element.getAsDouble();
                } else
                    throw new JsonSyntaxException("Expected offset to be a Double, was " + GsonHelper.getType(element));
            }
        }
        this.offset = new Vec3(vec3[0], vec3[1], vec3[2]);
        if (serializedRecipe.has("chance")) {
            this.chance = GsonHelper.getAsDouble(serializedRecipe, "chance");
        } else this.chance = 1.0;
        this.result = IItemStackUtil.fromJson(GsonHelper.getAsJsonObject(serializedRecipe, "result"));
    }

    @Override
    public boolean process(@NotNull AnvilCraftingContainer container) {
        Level level = container.getLevel();
        RandomSource random = level.getRandom();
        if (random.nextDouble() > this.chance) return true;
        BlockPos pos = container.getPos();
        Vec3 vec3 = pos.getCenter().add(this.offset);
        ItemEntity entity = new ItemEntity(level, vec3.x, vec3.y, vec3.z, this.result.copy(), 0.0d, 0.0d, 0.0d);
        return level.addFreshEntity(entity);
    }

    @Override
    public void toNetwork(@NotNull FriendlyByteBuf buffer) {
        buffer.writeUtf(this.getType());
        buffer.writeVector3f(this.offset.toVector3f());
        buffer.writeDouble(this.chance);
        buffer.writeItem(this.result);
    }

    @Override
    public JsonElement toJson() {
        JsonObject object = new JsonObject();
        double[] vec3 = {this.offset.x(), this.offset.y(), this.offset.z()};
        JsonArray offset = new JsonArray();
        for (double v : vec3) offset.add(new JsonPrimitive(v));
        object.addProperty("type", this.getType());
        object.add("offset", offset);
        object.addProperty("chance", this.chance);
        object.add("result", IItemStackUtil.toJson(this.result));
        return object;
    }
}
