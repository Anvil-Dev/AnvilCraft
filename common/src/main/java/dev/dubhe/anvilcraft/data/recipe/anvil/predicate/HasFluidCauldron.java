package dev.dubhe.anvilcraft.data.recipe.anvil.predicate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilCraftingContext;
import dev.dubhe.anvilcraft.data.recipe.anvil.RecipePredicate;
import dev.dubhe.anvilcraft.init.ModBlocks;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

@Getter
public class HasFluidCauldron implements RecipePredicate {
    private final String type = "has_fluid_cauldron";
    protected final Vec3 offset;
    protected final Block matchBlock;
    protected final int deplete;

    /**
     * 需要流体炼药锅
     */
    public HasFluidCauldron(Vec3 offset, Block matchBlock, int deplete) {
        this.offset = offset;
        this.matchBlock = matchBlock;
        this.deplete = deplete;
        if (!((this.matchBlock) instanceof LayeredCauldronBlock) && this.matchBlock != Blocks.LAVA_CAULDRON) {
            throw new IllegalStateException("match block is not layered cauldron block");
        }
    }

    /**
     * 拥有流体炼药锅
     *
     * @param serializedRecipe 序列化配方
     */
    public HasFluidCauldron(JsonObject serializedRecipe) {
        JsonArray array = GsonHelper.getAsJsonArray(serializedRecipe, "offset");
        double[] vec3 = {0.0d, 0.0d, 0.0d};
        for (int i = 0; i < array.size() && i < 3; i++) {
            JsonElement element = array.get(i);
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                vec3[i] = element.getAsDouble();
            } else throw new JsonSyntaxException("Expected offset to be a Double, was " + GsonHelper.getType(element));
        }
        this.offset = new Vec3(vec3[0], vec3[1], vec3[2]);
        String block = GsonHelper.getAsString(serializedRecipe, "match_block");
        this.matchBlock = BuiltInRegistries.BLOCK.get(new ResourceLocation(block));
        this.deplete = GsonHelper.getAsInt(serializedRecipe, "deplete");
        if (!((this.matchBlock) instanceof LayeredCauldronBlock) && this.matchBlock != Blocks.LAVA_CAULDRON) {
            throw new IllegalStateException("match block is not layered cauldron block");
        }
    }

    /**
     * 拥有流体炼药锅
     *
     * @param buffer 缓冲区
     */
    public HasFluidCauldron(@NotNull FriendlyByteBuf buffer) {
        this(
            new Vec3(buffer.readVector3f()),
            BuiltInRegistries.BLOCK.get(buffer.readResourceLocation()),
            buffer.readInt()
        );
    }


    @Override
    public boolean matches(@NotNull AnvilCraftingContext context) {
        BlockPos containerPos = context.getPos();
        Vec3 vec3 = containerPos.getCenter().add(this.offset);
        BlockPos pos = BlockPos.containing(vec3.x, vec3.y, vec3.z);
        Level containerLevel = context.getLevel();
        if (this.matchBlock == Blocks.LAVA_CAULDRON) return lavaMatches(containerLevel, pos);
        if (!(containerLevel instanceof ServerLevel level)) return false;
        BlockState state = level.getBlockState(pos);
        if (!state.is(this.matchBlock)) return false;
        return state.getValue(LayeredCauldronBlock.LEVEL) >= this.deplete;
    }

    private boolean lavaMatches(@NotNull Level level, @NotNull BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.LAVA_CAULDRON) && !state.is(ModBlocks.LAVA_CAULDRON.get())) return false;
        int cauldronLevel = state.is(Blocks.LAVA_CAULDRON) ? 3 : state.getValue(LayeredCauldronBlock.LEVEL);
        return cauldronLevel >= this.deplete;
    }

    @Override
    public boolean process(@NotNull AnvilCraftingContext context) {
        BlockPos containerPos = context.getPos();
        Vec3 vec3 = containerPos.getCenter().add(this.offset);
        BlockPos pos = BlockPos.containing(vec3.x, vec3.y, vec3.z);
        Level level = context.getLevel();
        if (this.matchBlock == Blocks.LAVA_CAULDRON) return lavaProcess(level, pos);
        BlockState state = level.getBlockState(pos);
        if (!state.is(this.matchBlock)) return false;
        for (int i = 0; i < this.deplete; i++) {
            LayeredCauldronBlock.lowerFillLevel(level.getBlockState(pos), level, pos);
        }
        return true;
    }

    private boolean lavaProcess(@NotNull Level level, @NotNull BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        state = state.is(Blocks.LAVA_CAULDRON) ? ModBlocks.LAVA_CAULDRON.getDefaultState() : state;
        if (state.is(ModBlocks.LAVA_CAULDRON.get())) {
            LayeredCauldronBlock.lowerFillLevel(level.getBlockState(pos), level, pos);
        } else return false;
        return true;
    }

    @Override
    public void toNetwork(@NotNull FriendlyByteBuf buffer) {
        buffer.writeUtf(this.getType());
        buffer.writeVector3f(this.offset.toVector3f());
        buffer.writeResourceLocation(BuiltInRegistries.BLOCK.getKey(this.matchBlock));
        buffer.writeInt(this.deplete);
    }

    @Override
    public @NotNull JsonElement toJson() {
        double[] vec3 = {this.offset.x(), this.offset.y(), this.offset.z()};
        JsonArray offset = new JsonArray();
        for (double v : vec3) {
            offset.add(new JsonPrimitive(v));
        }
        JsonObject object = new JsonObject();
        object.addProperty("type", this.getType());
        object.add("offset", offset);
        object.addProperty("match_block", BuiltInRegistries.BLOCK.getKey(this.matchBlock).toString());
        object.addProperty("deplete", this.deplete);
        return object;
    }
}
