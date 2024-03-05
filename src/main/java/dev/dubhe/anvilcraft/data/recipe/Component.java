package dev.dubhe.anvilcraft.data.recipe;

import com.google.common.collect.Lists;
import com.google.gson.*;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class Component implements Predicate<BlockState> {
    public static final Component EMPTY = new Component(Stream.empty());
    private final Block[] values;
    private final List<TagKey<Block>> tags;
    private Collection<Block> blocks = null;
    private String[] stringTags = null;
    private final Map<String, Comparable<?>> states = new HashMap<>();

    public Component(@NotNull Stream<? extends Block> values) {
        this(values, new ArrayList<>());
    }

    public Component(List<TagKey<Block>> tags) {
        this(Stream.of(), tags);
    }

    public Component(@NotNull Stream<? extends Block> values, List<TagKey<Block>> tags) {
        this.values = values.toArray(Block[]::new);
        this.tags = tags;
    }

    public static @NotNull Component of() {
        return Component.EMPTY;
    }

    public static @NotNull Component of(Block... components) {
        return of(Arrays.stream(components));
    }

    public static @NotNull Component of(@NotNull Stream<Block> components) {
        return fromValues(components.filter((component) -> !component.defaultBlockState().isAir()));
    }

    @SafeVarargs
    public static @NotNull Component of(TagKey<Block>... tags) {
        return new Component(Arrays.stream(tags).toList());
    }

    public static @NotNull Component of(List<TagKey<Block>> tags) {
        return new Component(tags);
    }

    @SuppressWarnings("UnusedReturnValue")
    public Component withState(String key, Comparable<?> value) {
        this.states.put(key, value);
        return this;
    }

    public static Component fromJson(JsonElement json) {
        if (null == json || json.isJsonNull()) {
            throw new JsonSyntaxException("Item cannot be null");
        }
        if (json.isJsonObject()) return fromJsonObj(json.getAsJsonObject());
        if (json.isJsonArray()) {
            JsonArray array = json.getAsJsonArray();
            List<Block> blocks = new ArrayList<>();
            List<TagKey<Block>> tags = new ArrayList<>();
            Map<String, Comparable<?>> states = new HashMap<>();
            for (JsonElement element : array) {
                if (element.isJsonObject()) {
                    JsonObject object = element.getAsJsonObject();
                    if (!object.has("block")) continue;
                    JsonElement data = object.get("block");
                    if (!data.isJsonPrimitive()) throw new JsonSyntaxException("Expected item to be string");
                    String id = data.getAsString();
                    if (!id.startsWith("#"))
                        blocks.add(BuiltInRegistries.BLOCK.get(new ResourceLocation(data.getAsString())));
                    else tags.add(TagKey.create(Registries.BLOCK, new ResourceLocation(id.substring(1))));
                    if (!object.has("state")) continue;
                    JsonElement state = object.get("state");
                    if (!state.isJsonObject()) throw new JsonSyntaxException("Expected item to be object");
                    JsonObject object1 = state.getAsJsonObject();
                    for (Map.Entry<String, JsonElement> entry : object1.entrySet()) {
                        Object obj = getObject(entry);
                        if (obj instanceof Comparable<?> comparable) states.put(entry.getKey(), comparable);
                    }
                }
            }
            Component component = new Component(blocks.stream(), tags);
            states.forEach(component::withState);
            return component;
        }
        throw new JsonSyntaxException("Expected item to be object or array of objects");
    }

    private static Component fromJsonObj(@NotNull JsonObject object) {
        boolean hasBlock = object.has("block");
        boolean hasState = object.has("state");
        if (!hasBlock && !hasState) return Component.EMPTY;
        Component component;
        if (hasBlock) {
            Block block = null;
            TagKey<Block> tag = null;
            JsonElement data = object.get("block");
            if (!data.isJsonPrimitive()) throw new JsonSyntaxException("Expected item to be string");
            String id = data.getAsString();
            if (!id.startsWith("#"))
                block = BuiltInRegistries.BLOCK.get(new ResourceLocation(data.getAsString()));
            else tag = TagKey.create(Registries.BLOCK, new ResourceLocation(id.substring(1)));
            component = null != block ? Component.of(block) : Component.of(tag);
        } else component = Component.of();
        if (!hasState) return component;
        JsonElement data = object.get("state");
        if (!data.isJsonObject()) throw new JsonSyntaxException("Expected item to be object");
        JsonObject jsonObject = data.getAsJsonObject();
        for (Map.Entry<String, JsonElement> element : jsonObject.entrySet()) {
            Object obj = getObject(element);
            if (obj instanceof Comparable<?> comparable) component.withState(element.getKey(), comparable);
        }
        return component;
    }

    @Nullable
    private static Object getObject(Map.@NotNull Entry<String, JsonElement> element) {
        if (!element.getValue().isJsonPrimitive())
            throw new JsonSyntaxException("Expected item to be primitive");
        JsonPrimitive primitive = element.getValue().getAsJsonPrimitive();
        Object obj = null;
        if (primitive.isString()) {
            obj = primitive.getAsString();
        } else if (primitive.isBoolean()) {
            obj = primitive.getAsBoolean();
        } else if (primitive.isNumber()) {
            Number number = primitive.getAsNumber();
            if (number instanceof Long || number instanceof Integer || number instanceof Short || number instanceof Byte) {
                obj = primitive.getAsLong();
            } else obj = primitive.getAsDouble();
        }
        return obj;
    }

    @SuppressWarnings("DuplicatedCode")
    private @NotNull JsonObject stateToJson() {
        JsonObject state = new JsonObject();
        for (Map.Entry<String, Comparable<?>> entry : this.states.entrySet()) {
            String key = entry.getKey();
            Comparable<?> comparable = entry.getValue();
            if (comparable instanceof String obj) {
                state.addProperty(key, obj);
            } else if (comparable instanceof Boolean obj) {
                state.addProperty(key, obj);
            } else if (comparable instanceof Number obj) {
                state.addProperty(key, obj);
            } else if (comparable instanceof Character obj) {
                state.addProperty(key, obj);
            }
        }
        return state;
    }

    public JsonElement toJson() {
        if (this.tags.size() + this.values.length == 1) {
            JsonObject object = new JsonObject();
            if (this.tags.isEmpty())
                object.addProperty("block", BuiltInRegistries.BLOCK.getKey(this.values[0]).toString());
            else object.addProperty("block", "#" + this.tags.get(0).location());
            if (!this.states.isEmpty()) object.add("state", this.stateToJson());
            return object;
        }
        JsonArray array = new JsonArray();
        for (TagKey<Block> tag : this.tags) {
            JsonObject object = new JsonObject();
            object.addProperty("block", "#" + tag.location());
            if (!this.states.isEmpty()) object.add("state", this.stateToJson());
            array.add(object);
        }
        for (Block value : values) {
            JsonObject object = new JsonObject();
            object.addProperty("block", BuiltInRegistries.BLOCK.getKey(value).toString());
            if (!this.states.isEmpty()) object.add("state", this.stateToJson());
            array.add(object);
        }
        return array;
    }

    public static @NotNull Component fromNetwork(@NotNull FriendlyByteBuf buffer) {
        return Component.fromValues(buffer.readList(FriendlyByteBuf::readUtf).stream().map(Component::parseBlock));
    }

    public void toNetwork(@NotNull FriendlyByteBuf buffer) {
        buffer.writeCollection(Arrays.asList(this.getStringTags()), FriendlyByteBuf::writeUtf);
    }

    public static @NotNull Block parseBlock(String id) {
        return BuiltInRegistries.BLOCK.get(new ResourceLocation(id));
    }

    public String[] getStringTags() {
        if (null == this.stringTags) {
            this.stringTags = this.getBlocks()
                    .stream()
                    .map(BuiltInRegistries.BLOCK::getKey)
                    .map(ResourceLocation::toString)
                    .distinct()
                    .toArray(String[]::new);
        }
        return this.stringTags;
    }

    public Collection<Block> getBlocks() {
        if (this.blocks != null) return this.blocks;
        List<Block> list = Lists.newArrayList(values);
        for (TagKey<Block> tag : this.tags) {
            if (null != tag) for (Holder<Block> blockHolder : BuiltInRegistries.BLOCK.getTagOrEmpty(tag)) {
                list.add(blockHolder.value());
            }
        }
        this.blocks = list;
        return list;
    }

    private static Component fromValues(Stream<? extends Block> stream) {
        Component ingredient = new Component(stream);
        return ingredient.isEmpty() ? EMPTY : ingredient;
    }

    public boolean isEmpty() {
        return this.tags.isEmpty() && this.values.length == 0;
    }

    @Override
    @SuppressWarnings("all")
    public boolean test(BlockState state) {
        boolean hasBlock = false;
        for (TagKey<Block> tag : this.tags) {
            if (state.is(tag)) hasBlock = true;
        }
        if (this.isEmpty() || Arrays.stream(this.values).toList().contains(state.getBlock())) hasBlock = true;
        if (this.states.isEmpty()) return hasBlock;
        boolean checkStates = true;
        Map<String, Comparable<?>> states = new HashMap<>() {{
            state.getValues().forEach((key, value) -> this.put(key.getName(), value));
        }};
        for (Map.Entry<String, Comparable<?>> entry1 : this.states.entrySet()) {
            String key = entry1.getKey();
            if (!states.containsKey(key)) {
                checkStates = false;
                break;
            }
            Comparable<?> value1 = entry1.getValue();
            Comparable<?> value2 = states.get(key);
            if (value1 instanceof Number number1 && value2 instanceof Number number2) {
                double double1 = number1.doubleValue();
                double double2 = number2.doubleValue();
                if (double1 != double2) {
                    checkStates = false;
                    break;
                }
            } else try {
                if (((Comparable) value1).compareTo(value2) != 0) {
                    checkStates = false;
                    break;
                }
            } catch (Exception e) {
                AnvilCraft.LOGGER.printStackTrace(e);
            }
        }
        return hasBlock && checkStates;
    }
}
