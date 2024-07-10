package dev.dubhe.anvilcraft.data.recipe.anvil.predicate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilCraftingContext;
import dev.dubhe.anvilcraft.data.recipe.anvil.RecipePredicate;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class HasBlock implements RecipePredicate {
    @Getter
    private final String type = "has_block";
    @Getter
    protected final Vec3 offset;
    @Getter
    protected final ModBlockPredicate matchBlock;

    public HasBlock(Vec3 offset, ModBlockPredicate matchBlock) {
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
        this.matchBlock = ModBlockPredicate.fromJson(serializedRecipe.get("match_block"));
    }

    public HasBlock(@NotNull FriendlyByteBuf buffer) {
        this.offset = new Vec3(buffer.readVector3f());
        this.matchBlock = ModBlockPredicate.fromJson(AnvilCraft.GSON.fromJson(buffer.readUtf(), JsonElement.class));
    }

    @Override
    public boolean matches(@NotNull AnvilCraftingContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel level1)) return false;
        BlockPos pos = context.getPos();
        Vec3 vec3 = pos.getCenter().add(this.offset);
        BlockPos blockPos = BlockPos.containing(vec3.x, vec3.y, vec3.z);
        return this.matchBlock.matches(level1, blockPos);
    }

    @Override
    public boolean process(AnvilCraftingContext context) {
        return true;
    }

    @Override
    public void toNetwork(@NotNull FriendlyByteBuf buffer) {
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

    @SuppressWarnings("UnusedReturnValue")
    @Getter
    public static class ModBlockPredicate {
        private TagKey<Block> tag = null;
        private final Set<Block> blocks = new HashSet<>();
        private final Map<String, String> properties = new HashMap<>();

        /**
         * 尝试匹配方块
         *
         * @param level 维度
         * @param pos   位置
         * @return 是否匹配
         */
        public boolean matches(@NotNull ServerLevel level, BlockPos pos) {
            boolean bl = false;
            BlockState state = level.getBlockState(pos);
            if (this.tag != null && state.is(this.tag)) bl = true;
            if (blocks.stream().anyMatch(state::is)) bl = true;
            if (!bl) return false;
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                for (Map.Entry<Property<?>, Comparable<?>> entry1 : state.getValues().entrySet()) {
                    if (!entry1.getKey().getName().equals(entry.getKey())) continue;
                    if (!entry1.getValue().toString().equals(entry.getValue())) return false;
                }
            }
            return true;
        }

        /**
         * @param json 序列化 Json
         * @return {@link ModBlockPredicate}
         */
        public static @NotNull ModBlockPredicate fromJson(@Nullable JsonElement json) {
            if (json == null || json.isJsonNull()) {
                return ANY;
            }
            JsonObject jsonObject = GsonHelper.convertToJsonObject(json, "block");
            JsonArray jsonArray = GsonHelper.getAsJsonArray(jsonObject, "blocks", null);
            Set<Block> set = new HashSet<>();
            if (jsonArray != null) {
                for (JsonElement jsonElement : jsonArray) {
                    ResourceLocation resourceLocation = new ResourceLocation(
                        GsonHelper.convertToString(jsonElement, "block")
                    );
                    set.add(
                        BuiltInRegistries.BLOCK
                            .getOptional(resourceLocation)
                            .orElseThrow(() -> new JsonSyntaxException("Unknown block id '" + resourceLocation + "'")));
                }
            }
            TagKey<Block> tag = null;
            if (jsonObject.has("tag")) {
                ResourceLocation resourceLocation2 = new ResourceLocation(GsonHelper.getAsString(jsonObject, "tag"));
                tag = TagKey.create(Registries.BLOCK, resourceLocation2);
            }
            ModBlockPredicate predicate = new ModBlockPredicate().block(set.toArray(Block[]::new)).block(tag);
            JsonElement stateJson = jsonObject.get("state");
            if (stateJson == null) return predicate;
            Map<String, Comparable<?>> properties = new HashMap<>();
            for (Map.Entry<String, JsonElement> entry : stateJson.getAsJsonObject().entrySet()) {
                JsonElement value = entry.getValue();
                if (value.isJsonPrimitive()) {
                    String string = value.getAsString();
                    properties.put(entry.getKey(), string);
                } else {
                    throw new JsonSyntaxException("Expected state to be a string, was " + GsonHelper.getType(value));
                }
            }
            properties.forEach(predicate::property);
            return predicate;
        }

        /**
         * @return 序列化 Json
         */
        public JsonElement serializeToJson() {
            JsonObject object = new JsonObject();
            if (!blocks.isEmpty()) {
                JsonArray array = new JsonArray();
                for (Block block : blocks) {
                    array.add(BuiltInRegistries.BLOCK.getKey(block).toString());
                }
                object.add("blocks", array);
            }
            if (this.tag != null) {
                object.addProperty("tag", this.tag.location().toString());
            }
            if (!this.properties.isEmpty()) {
                JsonObject states = new JsonObject();
                this.properties.forEach(states::addProperty);
                object.add("state", states);
            }
            return object;
        }

        public @NotNull ModBlockPredicate block(Block @NotNull ... blocks) {
            this.blocks.addAll(Arrays.asList(blocks));
            return this;
        }

        public @NotNull ModBlockPredicate block(TagKey<Block> block) {
            this.tag = block;
            return this;
        }

        public @NotNull <T> ModBlockPredicate property(String key, @NotNull Comparable<T> value) {
            this.properties.put(key, value.toString());
            return this;
        }

        public @NotNull <T extends Comparable<T>> ModBlockPredicate property(
            @NotNull Property<T> key, @NotNull T value
        ) {
            this.properties.put(key.getName(), value.toString());
            return this;
        }

        /**
         * @param properties 属性
         */
        @SafeVarargs
        public final @NotNull ModBlockPredicate property(
            Map.Entry<Property<?>, Comparable<?>> @NotNull ... properties
        ) {
            Arrays.stream(properties).forEach(
                entry -> this.properties.put(entry.getKey().getName(), entry.getValue().toString())
            );
            return this;
        }

        public static final ModBlockPredicate ANY = new ModBlockPredicate() {
            @Override
            public boolean matches(@NotNull ServerLevel level, BlockPos pos) {
                return true;
            }
        };
    }
}
