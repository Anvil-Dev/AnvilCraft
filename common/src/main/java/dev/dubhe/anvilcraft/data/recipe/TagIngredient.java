package dev.dubhe.anvilcraft.data.recipe;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.dubhe.anvilcraft.util.Serializable;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

@SuppressWarnings("unused")
public class TagIngredient implements Predicate<ItemStack>, Serializable {
    public static final TagIngredient EMPTY = new TagIngredient(Stream.empty());
    private final List<Value<?>> values;

    private TagIngredient(@NotNull Stream<Value<?>> values) {
        this.values = values.toList();
    }

    public Ingredient toVanilla() {
        return Ingredient.of(this.getItems().stream());
    }

    public static @NotNull TagIngredient of() {
        return TagIngredient.EMPTY;
    }

    public static @NotNull TagIngredient of(@NotNull Item... items) {
        return TagIngredient.of(Arrays.stream(items).map(Value::of));
    }

    public static @NotNull TagIngredient of(@NotNull ItemLike... items) {
        return TagIngredient.of(Arrays.stream(items).map(Value::of));
    }

    public static @NotNull TagIngredient of(@NotNull ItemStack... stacks) {
        return TagIngredient.of(Arrays.stream(stacks).map(Value::of));
    }

    @SafeVarargs
    public static @NotNull TagIngredient of(@NotNull TagKey<Item>... tags) {
        return TagIngredient.of(Arrays.stream(tags).map(Value::of));
    }

    public static @NotNull TagIngredient of(@NotNull Value<?>... values) {
        return TagIngredient.of(Arrays.stream(values));
    }

    public static @NotNull TagIngredient ofItemLikes(@NotNull Stream<ItemLike> items) {
        return TagIngredient.of(items.map(Value::of));
    }

    public static @NotNull TagIngredient ofItems(@NotNull Stream<Item> items) {
        return TagIngredient.of(items.map(Value::of));
    }

    public static @NotNull TagIngredient ofStacks(@NotNull Stream<ItemStack> stacks) {
        return TagIngredient.of(stacks.map(Value::of));
    }

    public static @NotNull TagIngredient ofTags(@NotNull Stream<TagKey<Item>> tags) {
        return TagIngredient.of(tags.map(Value::of));
    }

    public static @NotNull TagIngredient of(@NotNull Stream<Value<?>> values) {
        return new TagIngredient(values);
    }

    public static @NotNull TagIngredient of(@NotNull Ingredient ingredient) {
        List<Value<?>> valueList = new ArrayList<>();
        for (Ingredient.Value value : ingredient.values) {
            if (value instanceof Ingredient.TagValue tagValue) valueList.add(TagValue.of(tagValue.tag));
            if (value instanceof Ingredient.ItemValue itemValue) valueList.add(TagValue.of(itemValue.item));
        }
        return new TagIngredient(valueList.stream());
    }

    private @NotNull Collection<ItemStack> getItems() {
        Collection<ItemStack> stacks = new ArrayList<>();
        this.values.forEach(value -> stacks.addAll(value.getItems()));
        return stacks;
    }

    @Override
    public boolean test(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        if (this.isEmpty()) {
            return stack.isEmpty();
        }
        for (Value<?> value : values) {
            if (value.test(stack)) return true;
        }
        return false;
    }

    @Override
    public void toNetwork(@NotNull FriendlyByteBuf buffer) {
        buffer.writeVarInt(this.values.size());
        this.values.forEach(value -> value.toNetwork(buffer));
    }

    public static @NotNull TagIngredient fromNetwork(@NotNull FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        List<Value<?>> values = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            values.add(Value.fromNetwork(buffer));
        }
        return new TagIngredient(values.stream());
    }

    @Override
    public @NotNull JsonElement toJson() {
        if (this.values.size() == 1) return this.values.get(0).toJson();
        JsonArray array = new JsonArray();
        this.values.forEach(value -> array.add(value.toJson()));
        return array;
    }

    public static @NotNull TagIngredient fromJson(@NotNull JsonElement element) {
        if (element.isJsonObject()) {
            return TagIngredient.of(Value.formJson(element));
        }
        if (!element.isJsonObject()) throw new JsonSyntaxException("Expected item to be object or array");
        JsonArray array = element.getAsJsonArray();
        return TagIngredient.of(array.asList().stream().map(Value::formJson));
    }

    public boolean isEmpty() {
        return this.values.isEmpty();
    }

    public static class ItemValue extends Value<Item> {
        private ItemValue(@NotNull ItemLike item) {
            super(item.asItem());
        }

        @Override
        public void toNetwork(@NotNull FriendlyByteBuf buffer) {
            buffer.writeUtf(BuiltInRegistries.ITEM.getKey(this.item).toString());
            super.toNetwork(buffer);
        }

        @Override
        public @NotNull JsonElement toJson() {
            JsonObject object = new JsonObject();
            object.addProperty("item", BuiltInRegistries.ITEM.getKey(this.item).toString());
            if (!this.tag.isEmpty()) object.add("data", super.toJson());
            return object;
        }

        @Override
        public Collection<ItemStack> getItems() {
            return Collections.singleton(this.item.getDefaultInstance());
        }

        @Override
        public boolean test(@NotNull ItemStack stack) {
            return stack.is(this.item) && super.test(stack);
        }
    }

    public static class TagValue extends Value<TagKey<Item>> {
        private TagValue(TagKey<Item> item) {
            super(item);
        }

        @Override
        public void toNetwork(@NotNull FriendlyByteBuf buffer) {
            buffer.writeUtf("#%s".formatted(this.item.location().toString()));
            super.toNetwork(buffer);
        }

        @Override
        public @NotNull JsonElement toJson() {
            JsonObject object = new JsonObject();
            object.addProperty("item", "#%s".formatted(this.item.location().toString()));
            if (!this.tag.isEmpty()) object.add("data", super.toJson());
            return object;
        }

        @Override
        public Collection<ItemStack> getItems() {
            ArrayList<ItemStack> list = Lists.newArrayList();
            for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(this.item)) {
                list.add(new ItemStack(holder));
            }
            return list;
        }

        @Override
        public boolean test(@NotNull ItemStack stack) {
            return stack.is(this.item) && super.test(stack);
        }
    }

    public static abstract class Value<T> implements Predicate<ItemStack>, Serializable {
        @Getter
        final T item;
        @Setter
        CompoundTag tag = new CompoundTag();

        protected Value(T item) {
            this.item = item;
        }

        public static @NotNull Value<?> of(@NotNull ItemLike item) {
            return new ItemValue(item);
        }

        public static @NotNull Value<?> of(@NotNull TagKey<Item> tag) {
            return new TagValue(tag);
        }

        public static @NotNull Value<?> of(@NotNull ItemStack item) {
            Value<?> value = new ItemValue(item.getItem());
            value.setTag(item.getOrCreateTag());
            return value;
        }

        public Value<?> with(String key, Tag value) {
            this.tag.put(key, value);
            return this;
        }

        public abstract Collection<ItemStack> getItems();

        public void toNetwork(@NotNull FriendlyByteBuf buffer) {
            buffer.writeUtf(this.tag.toString());
        }

        public static @NotNull Value<?> fromNetwork(@NotNull FriendlyByteBuf buffer) {
            Value<?> value;
            String id = buffer.readUtf();
            if (id.startsWith("#")) {
                TagKey<Item> item = TagKey.create(Registries.ITEM, new ResourceLocation(id.substring(1)));
                value = Value.of(item);
            } else {
                Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(id));
                value = Value.of(item);
            }
            value.setTag(Value.tagFromNetwork(buffer));
            return value;
        }

        private static @NotNull CompoundTag tagFromNetwork(@NotNull FriendlyByteBuf buffer) {
            return Value.parseTag(buffer.readUtf());
        }

        public @NotNull JsonElement toJson() {
            return new JsonPrimitive(this.tag.toString());
        }

        public static @NotNull Value<?> formJson(@NotNull JsonElement element) {
            if (!element.isJsonObject()) throw new JsonSyntaxException("Expected item to be object");
            Value<?> value;
            String id = GsonHelper.getAsString(element.getAsJsonObject(), "item");
            if (id.startsWith("#")) {
                TagKey<Item> item = TagKey.create(Registries.ITEM, new ResourceLocation(id.substring(1)));
                value = Value.of(item);
            } else {
                Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(id));
                value = Value.of(item);
            }
            value.setTag(Value.tagFromJson(element));
            return value;
        }

        private static @NotNull CompoundTag tagFromJson(@NotNull JsonElement element) {
            if (!element.isJsonObject()) throw new JsonSyntaxException("Expected item to be object");
            if (element.getAsJsonObject().has("data"))
                return Value.parseTag(GsonHelper.getAsString(element.getAsJsonObject(), "data"));
            return new CompoundTag();
        }

        private static @NotNull CompoundTag parseTag(String string) {
            try {
                return TagParser.parseTag(string);
            } catch (CommandSyntaxException ignored) {
                throw new JsonSyntaxException("Invalid NBT string");
            }
        }

        @Override
        public boolean test(@NotNull ItemStack stack) {
            if (tag.isEmpty()) return true;
            CompoundTag tag1 = stack.getOrCreateTag();
            boolean bl = true;
            for (String key : tag.getAllKeys()) {
                if (!tag1.contains(key) || !Objects.equals(tag.get(key), tag1.get(key))) {
                    bl = false;
                    break;
                }
            }
            return bl;
        }
    }
}
