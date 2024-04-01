package dev.dubhe.anvilcraft.data.recipe;

import com.google.gson.*;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.util.Serializable;
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

public class Component implements Predicate<BlockState>, Serializable {
    public static final Component EMPTY = new Component(Stream.empty());
    private final List<Value<?>> values;

    public Component(@NotNull Stream<Value<?>> values) {
        this.values = values.toList();
    }

    public static @NotNull Component of() {
        return Component.EMPTY;
    }

    public static @NotNull Component of(Block... blocks) {
        return new Component(Arrays.stream(blocks).map(Value::of));
    }

    public static @NotNull Component ofBlocks(Stream<Block> blocks) {
        return new Component(blocks.map(Value::of));
    }

    public static @NotNull Component of(BlockState... states) {
        return Component.ofStates(Arrays.stream(states));
    }

    public static @NotNull Component ofStates(@NotNull Stream<BlockState> states) {
        Stream<Value<?>> values = states.map(state -> {
            Block block = state.getBlock();
            Value<?> value = Value.of(block);
            state.getValues().forEach((k, v) -> value.with(k.getName(), v));
            return value;
        });
        return new Component(values);
    }

    public static @NotNull Component of(Value<?>... values) {
        return new Component(Arrays.stream(values));
    }

    public static @NotNull Component of(@NotNull Stream<Value<?>> values) {
        return new Component(values);
    }

    @SafeVarargs
    public static @NotNull Component of(TagKey<Block>... tags) {
        return new Component(Arrays.stream(tags).map(Value::of));
    }

    @Override
    public boolean test(BlockState state) {
        for (Value<?> value : this.values) {
            if (value.test(state)) return true;
        }
        return false;
    }

    @Override
    public void toNetwork(@NotNull FriendlyByteBuf buffer) {
        buffer.writeVarInt(this.values.size());
        values.forEach(value -> value.toNetwork(buffer));
    }

    public static Component fromNetwork(@NotNull FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        List<Value<?>> values = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            values.add(Value.fromNetwork(buffer));
        }
        return Component.of(values.stream());
    }

    @Override
    public @NotNull JsonElement toJson() {
        if (this.values.size() == 1) return this.values.get(0).toJson();
        JsonArray array = new JsonArray();
        this.values.forEach(value -> array.add(value.toJson()));
        return array;
    }

    public static Component fromJson(@NotNull JsonElement element) {
        if (element.isJsonObject()) return Component.of(Value.fromJson(element));
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            List<Value<?>> values = new ArrayList<>();
            array.forEach(value -> values.add(Value.fromJson(value)));
            return Component.of(values.stream());
        }
        return Component.of();
    }

    public static class TagValue extends Value<TagKey<Block>> {
        private TagValue(TagKey<Block> tag) {
            super(tag);
        }

        @Override
        public boolean test(@NotNull BlockState state) {
            return state.is(this.block) && super.test(state);
        }

        @Override
        public void toNetwork(@NotNull FriendlyByteBuf buffer) {
            buffer.writeUtf("#%s".formatted(this.block.location().toString()));
            super.toNetwork(buffer);
        }

        @Override
        public @NotNull JsonElement toJson() {
            JsonObject object = super.toJson().getAsJsonObject();
            object.addProperty("block", "#%s".formatted(this.block.location().toString()));
            return object;
        }
    }

    public static class BlockValue extends Value<Block> {
        private BlockValue(Block block) {
            super(block);
        }

        @Override
        public boolean test(@NotNull BlockState state) {
            return state.is(this.block) && super.test(state);
        }

        @Override
        public void toNetwork(@NotNull FriendlyByteBuf buffer) {
            buffer.writeUtf(BuiltInRegistries.BLOCK.getKey(this.block).toString());
            super.toNetwork(buffer);
        }

        @Override
        public @NotNull JsonElement toJson() {
            JsonObject object = super.toJson().getAsJsonObject();
            object.addProperty("block", BuiltInRegistries.BLOCK.getKey(this.block).toString());
            return object;
        }
    }

    public abstract static class Value<T> implements Predicate<BlockState>, Serializable {
        protected final T block;
        protected final Map<String, Comparable<?>> states = new HashMap<>();

        protected Value(T block) {
            this.block = block;
        }

        public static @NotNull Value<?> of(Block block) {
            return new BlockValue(block);
        }

        public static @NotNull Value<?> of(TagKey<Block> block) {
            return new TagValue(block);
        }

        @SuppressWarnings("UnusedReturnValue")
        public Value<?> with(String key, Comparable<?> value) {
            this.states.put(key, value);
            return this;
        }

        public static @NotNull Value<?> fromJson(@NotNull JsonElement element) {
            if (!element.isJsonObject()) throw new JsonSyntaxException("Expected item to be object");
            JsonObject object = element.getAsJsonObject();
            if (!object.has("block")) throw new JsonSyntaxException("Missing block element");
            JsonElement element1 = object.get("block");
            if (!element1.isJsonPrimitive()) throw new JsonSyntaxException("Expected item to be string");
            String id = element1.getAsString();
            Value<?> value;
            if (!id.startsWith("#"))
                value = new BlockValue(BuiltInRegistries.BLOCK.get(new ResourceLocation(id)));
            else value = new TagValue(TagKey.create(Registries.BLOCK, new ResourceLocation(id.substring(1))));
            value.states.putAll(Value.statesFromJson(element));
            return value;
        }

        public static @NotNull Map<String, Comparable<?>> statesFromJson(@NotNull JsonElement element) {
            Map<String, Comparable<?>> map = new HashMap<>();
            if (!element.isJsonObject()) throw new JsonSyntaxException("Expected item to be object");
            JsonObject object = element.getAsJsonObject();
            if (!object.has("state")) return map;
            element = object.get("state");
            if (!element.isJsonObject()) throw new JsonSyntaxException("Expected item to be object");
            object = element.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                String key = entry.getKey();
                Object obj = Value.getObject(entry.getValue());
                if (obj instanceof Comparable<?> value) map.put(key, value);
            }
            return map;
        }

        public @NotNull JsonElement toJson() {
            JsonObject object = new JsonObject();
            if (this.states.isEmpty()) return object;
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
            object.add("state", state);
            return object;
        }

        public static @NotNull Value<?> fromNetwork(@NotNull FriendlyByteBuf buffer) {
            String id = buffer.readUtf();
            Value<?> value;
            if (!id.startsWith("#"))
                value = new BlockValue(BuiltInRegistries.BLOCK.get(new ResourceLocation(id)));
            else value = new TagValue(TagKey.create(Registries.BLOCK, new ResourceLocation(id.substring(1))));
            value.states.putAll(Value.statesFromNetwork(buffer));
            return value;
        }

        public static @NotNull Map<String, Comparable<?>> statesFromNetwork(@NotNull FriendlyByteBuf buffer) {
            Map<String, Comparable<?>> map = new HashMap<>();
            String string = buffer.readUtf();
            String[] strings = string.substring(1, string.length() - 1).split(", ");
            for (String str : strings) {
                String[] strings1 = str.split("=");
                if (strings1.length != 2) continue;
                String key = strings1[0];
                String valueStr = strings1[1];
                Object object = valueStr;
                try {
                    object = Long.valueOf(valueStr);
                } catch (NumberFormatException ignored) {
                    try {
                        object = Double.valueOf(valueStr);
                    } catch (NumberFormatException _ignored) {
                        if ("true".equals(valueStr)) object = true;
                        else if ("false".equals(valueStr)) object = false;
                    }
                }
                if (object instanceof Comparable<?> value) map.put(key, value);
            }
            return map;
        }

        public void toNetwork(@NotNull FriendlyByteBuf buffer) {
            StringBuilder builder = new StringBuilder("[");
            Iterator<Map.Entry<String, Comparable<?>>> iterator = this.states.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, Comparable<?>> entry = iterator.next();
                builder.append("%s=%s".formatted(entry.getKey(), entry.getValue()));
                if (iterator.hasNext()) builder.append(", ");
            }
            builder.append("]");
            buffer.writeUtf(builder.toString());
        }

        @Nullable
        private static Object getObject(@NotNull JsonElement element) {
            if (!element.isJsonPrimitive())
                throw new JsonSyntaxException("Expected item to be primitive");
            JsonPrimitive primitive = element.getAsJsonPrimitive();
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

        @Override
        public boolean test(@NotNull BlockState state) {
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
                    if (value1.equals(value2)) {
                        checkStates = false;
                        break;
                    }
                } catch (Exception e) {
                    AnvilCraft.LOGGER.printStackTrace(e);
                }
            }
            return checkStates;
        }
    }
}
