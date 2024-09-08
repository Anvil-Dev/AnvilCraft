package dev.dubhe.anvilcraft.data.recipe.anvil.outcome;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilCraftingContext;
import dev.dubhe.anvilcraft.data.recipe.anvil.RecipeOutcome;
import lombok.Getter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

@Getter
public class SpawnExperience implements RecipeOutcome {
    private final String type = "spawn_experience";
    private final Vec3 offset;
    private final double chance;
    private final int experience;

    /**
     * 产生经验
     *
     * @param offset     偏移
     * @param chance     概率
     * @param experience 经验
     */
    public SpawnExperience(Vec3 offset, double chance, int experience) {
        this.offset = offset;
        this.chance = chance;
        this.experience = experience;
    }

    /**
     * @param buffer 缓冲区
     */
    public SpawnExperience(@NotNull FriendlyByteBuf buffer) {
        this.offset = new Vec3(buffer.readVector3f());
        this.chance = buffer.readDouble();
        this.experience = buffer.readVarInt();
    }

    /**
     * @param serializedRecipe json
     */
    public SpawnExperience(@NotNull JsonObject serializedRecipe) {
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
        this.experience = GsonHelper.getAsInt(serializedRecipe, "experience");
    }

    @Override
    public boolean process(@NotNull AnvilCraftingContext context) {
        Level level = context.getLevel();
        ExperienceOrb entity = new ExperienceOrb(level, this.offset.x, this.offset.y, this.offset.z, this.experience);
        return level.addFreshEntity(entity);
    }

    @Override
    public void toNetwork(@NotNull FriendlyByteBuf buffer) {
        buffer.writeUtf(this.getType());
        buffer.writeVector3f(this.offset.toVector3f());
        buffer.writeDouble(this.chance);
        buffer.writeVarInt(this.experience);
    }

    @Override
    public JsonElement toJson() {
        double[] vec3 = {this.offset.x(), this.offset.y(), this.offset.z()};
        JsonArray offset = new JsonArray();
        for (double v : vec3) offset.add(new JsonPrimitive(v));
        JsonObject object = new JsonObject();
        object.addProperty("type", this.getType());
        object.add("offset", offset);
        object.addProperty("chance", this.chance);
        object.addProperty("experience", this.experience);
        return object;
    }
}
