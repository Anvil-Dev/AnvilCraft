package dev.dubhe.anvilcraft.data.recipe.anvil.outcome;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilCraftingContainer;
import dev.dubhe.anvilcraft.data.recipe.anvil.RecipeOutcome;
import dev.dubhe.anvilcraft.util.IBlockStateUtil;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class SetBlock implements RecipeOutcome {
    @Getter
    private final String type = "set_block";
    private final Vec3 offset;
    private final double chance;
    private final BlockState result;

    /**
     * 放置方块
     *
     * @param offset 偏移
     * @param chance 几率
     * @param result 方块
     */
    public SetBlock(Vec3 offset, double chance, BlockState result) {
        this.offset = offset;
        this.chance = chance;
        this.result = result;
    }

    /**
     * @param buffer 缓冲区
     */
    public SetBlock(@NotNull FriendlyByteBuf buffer) {
        this.offset = new Vec3(buffer.readVector3f());
        this.chance = buffer.readDouble();
        this.result = IBlockStateUtil.fromJson(AnvilCraft.GSON.fromJson(buffer.readUtf(), JsonElement.class));
    }

    /**
     * @param serializedRecipe json
     */
    public SetBlock(@NotNull JsonObject serializedRecipe) {
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
        this.result = IBlockStateUtil.fromJson(GsonHelper.getAsJsonObject(serializedRecipe, "result"));
    }

    @Override
    public boolean process(@NotNull AnvilCraftingContainer container) {
        Level level = container.getLevel();
        RandomSource random = level.getRandom();
        if (random.nextDouble() > this.chance) return true;
        BlockPos pos = container.getPos();
        Vec3 vec3 = pos.getCenter().add(this.offset);
        BlockPos blockPos = BlockPos.containing(vec3.x, vec3.y, vec3.z);
        return level.setBlockAndUpdate(blockPos, this.result);
    }

    @Override
    public void toNetwork(@NotNull FriendlyByteBuf buffer) {
        buffer.writeUtf(this.getType());
        buffer.writeVector3f(this.offset.toVector3f());
        buffer.writeDouble(this.chance);
        buffer.writeUtf(IBlockStateUtil.toJson(this.result).toString());
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
        object.add("result", IBlockStateUtil.toJson(this.result));
        return object;
    }
}
