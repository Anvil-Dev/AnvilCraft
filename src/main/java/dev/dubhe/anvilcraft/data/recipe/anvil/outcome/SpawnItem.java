package dev.dubhe.anvilcraft.data.recipe.anvil.outcome;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilCraftingContext;
import dev.dubhe.anvilcraft.data.recipe.anvil.CanSetData;
import dev.dubhe.anvilcraft.data.recipe.anvil.RecipeOutcome;
import dev.dubhe.anvilcraft.util.IItemStackUtil;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@Getter
public class SpawnItem implements RecipeOutcome, CanSetData {
    private final String type = "spawn_item";
    private final Vec3 offset;
    private final double chance;
    private final ItemStack result;
    private String path = null;
    @Setter
    private Map<String, CompoundTag> data = null;

    public SpawnItem loadItemData(String path) {
        this.path = path;
        return this;
    }

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
        if (buffer.readBoolean()) {
            this.path = buffer.readUtf();
        }
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
        if (serializedRecipe.has("data_path")) {
            this.path = GsonHelper.getAsString(serializedRecipe, "data_path");
        }
        this.result = IItemStackUtil.fromJson(GsonHelper.getAsJsonObject(serializedRecipe, "result"));
    }

    @Override
    public boolean process(@NotNull AnvilCraftingContext context) {
        ItemStack stack = this.result.copy();
        if (this.path != null && this.data != null && this.data.containsKey(this.path)) {
            stack.setTag(this.data.get(this.path));
        }
        return context.addOutputsItem(this.offset, stack);
    }

    @Override
    public void toNetwork(@NotNull FriendlyByteBuf buffer) {
        buffer.writeUtf(this.getType());
        buffer.writeVector3f(this.offset.toVector3f());
        buffer.writeDouble(this.chance);
        buffer.writeBoolean(this.path != null);
        if (this.path != null) {
            buffer.writeUtf(this.path);
        }
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
        if (this.path != null) object.addProperty("data_path", this.path);
        return object;
    }
}
