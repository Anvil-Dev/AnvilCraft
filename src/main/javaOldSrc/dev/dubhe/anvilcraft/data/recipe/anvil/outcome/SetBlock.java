package dev.dubhe.anvilcraft.data.recipe.anvil.outcome;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilCraftingContext;
import dev.dubhe.anvilcraft.data.recipe.anvil.RecipeOutcome;
import dev.dubhe.anvilcraft.util.IBlockStateUtil;
import lombok.Getter;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@Getter
public class SetBlock implements RecipeOutcome {
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
                } else {
                    throw new JsonSyntaxException("Expected offset to be a Double, was " + GsonHelper.getType(element));
                }
            }
        }
        this.offset = new Vec3(vec3[0], vec3[1], vec3[2]);
        if (serializedRecipe.has("chance")) {
            this.chance = GsonHelper.getAsDouble(serializedRecipe, "chance");
        } else this.chance = 1.0;

        JsonObject stateJson = GsonHelper.getAsJsonObject(serializedRecipe, "result");

        if (!stateJson.isJsonObject()) throw new JsonSyntaxException("Expected item to be object");
        JsonObject object = stateJson.getAsJsonObject();
        if (!object.has("block")) throw new JsonSyntaxException("The field block is missing");
        JsonElement blockElement = object.get("block");
        if (!blockElement.isJsonPrimitive()) throw new JsonSyntaxException("Expected item to be string");
        StringBuilder block = new StringBuilder(blockElement.getAsString());
        if (object.has("state")) {
            JsonObject state = GsonHelper.getAsJsonObject(object, "state");
            if (!state.asMap().isEmpty()) {
                block.append('[');
                for (Map.Entry<String, JsonElement> entry : state.entrySet()) {
                    block.append("%s=%s,".formatted(entry.getKey(), entry.getValue().getAsString()));
                }
                block.deleteCharAt(block.length() - 1);
                block.append(']');
            }
        }
        HolderLookup<Block> blocks = new IBlockStateUtil.BlockHolderLookup();
        BlockStateParser.BlockResult blockResult;
        try {
            blockResult = BlockStateParser.parseForBlock(blocks, block.toString(), true);
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }

        this.result = blockResult.blockState();
    }

    @Override
    public boolean process(@NotNull AnvilCraftingContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getPos();
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
        JsonObject block = new JsonObject();
        block.addProperty("block", BuiltInRegistries.BLOCK.getKey(this.result.getBlock()).toString());
        JsonObject state = new JsonObject();
        for (Map.Entry<Property<?>, Comparable<?>> entry : this.result.getValues().entrySet()) {
            state.addProperty(entry.getKey().getName(), entry.getValue().toString());
        }
        if (!this.result.getValues().isEmpty()) block.add("state", state);
        object.add("result", block);
        return object;
    }
}
