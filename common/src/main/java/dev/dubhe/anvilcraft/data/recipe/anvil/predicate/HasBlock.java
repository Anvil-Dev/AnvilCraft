package dev.dubhe.anvilcraft.data.recipe.anvil.predicate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilCraftingContainer;
import dev.dubhe.anvilcraft.data.recipe.anvil.RecipePredicate;
import lombok.Getter;
import net.minecraft.advancements.critereon.BlockPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class HasBlock implements RecipePredicate {
    @Getter
    private final String type = "has_block";
    protected final Vec3 offset;
    protected final BlockPredicate matchBlock;

    public HasBlock(Vec3 offset, BlockPredicate matchBlock) {
        this.offset = offset;
        this.matchBlock = matchBlock;
    }

    /**
     * 拥有方块
     *
     * @param serializedRecipe 序列化配方
     */
    public HasBlock(JsonObject serializedRecipe) {
        JsonArray array = GsonHelper.getAsJsonArray(serializedRecipe, "offset");
        double[] vec3 = {0.0d, 0.0d, 0.0d};
        for (int i = 0; i < array.size() && i < 3; i++) {
            JsonElement element = array.get(i);
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                vec3[i] = element.getAsDouble();
            } else throw new JsonSyntaxException("Expected offset to be a Double, was " + GsonHelper.getType(element));
        }
        this.offset = new Vec3(vec3[0], vec3[1], vec3[2]);
        if (!serializedRecipe.has("match_block")) throw new JsonSyntaxException("Missing match_block");
        this.matchBlock = BlockPredicate.fromJson(serializedRecipe.get("match_block"));
    }

    public HasBlock(@NotNull FriendlyByteBuf buffer) {
        this.offset = new Vec3(buffer.readVector3f());
        this.matchBlock = BlockPredicate.fromJson(AnvilCraft.GSON.fromJson(buffer.readUtf(), JsonElement.class));
    }

    @Override
    public boolean matches(@NotNull AnvilCraftingContainer container) {
        Level level = container.getLevel();
        if (!(level instanceof ServerLevel level1)) return false;
        BlockPos pos = container.getPos();
        Vec3 vec3 = pos.getCenter().add(this.offset);
        BlockPos blockPos = BlockPos.containing(vec3.x, vec3.y, vec3.z);
        return this.matchBlock.matches(level1, blockPos);
    }

    @Override
    public boolean process(AnvilCraftingContainer container) {
        return true;
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer) {
        buffer.writeUtf(this.getType());
        buffer.writeVector3f(this.offset.toVector3f());
        buffer.writeUtf(this.matchBlock.serializeToJson().toString());
    }

    @Override
    public @NotNull JsonElement toJson() {
        double[] vec3 = {this.offset.x(), this.offset.y(), this.offset.z()};
        JsonArray offset = new JsonArray();
        for (double v : vec3) {
            offset.add(new JsonPrimitive(v));
        }
        JsonElement matchBlock = this.matchBlock.serializeToJson();
        JsonObject object = new JsonObject();
        object.addProperty("type", this.getType());
        object.add("offset", offset);
        object.add("match_block", matchBlock);
        return object;
    }
}
