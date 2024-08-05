package dev.dubhe.anvilcraft.data.recipe.anvil.predicate.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Getter;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public class ModItemWithNoNbtPredicate {
    @Nullable
    private TagKey<Item> tag = null;
    private final Set<Item> items = new HashSet<>();
    @Getter
    public MinMaxBounds.Ints count = null;
    private MinMaxBounds.Ints durability = null;

    public ModItemWithNoNbtPredicate() {
    }

    /**
     * 通过普通的物品判据的数据构建无NBT的物品判据
     * itemPredicate: 输入的物品判据
     */
    public ModItemWithNoNbtPredicate(HasItem.ModItemPredicate itemPredicate) {
        this.tag = itemPredicate.getTag();
        this.items.addAll(itemPredicate.getItems());
        this.count = itemPredicate.getCount();
        this.durability = itemPredicate.getDurability();
    }

    /**
     *
     * @return 将此无NBT物品判据转化为物品判据的形式
     */
    public HasItem.ModItemPredicate TurnIntoModItemPredicate() {
        HasItem.ModItemPredicate predicate = HasItem.ModItemPredicate.of(this.tag);
        for (Item i : this.items) predicate.add(i);
        predicate.withCount(this.count);
        predicate.withDurability(durability);
        return predicate;
    }

    /**
     * 是否具有相同的物品/标签
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean sameItemsOrTag(ModItemWithNoNbtPredicate predicate) {
        if (this == predicate) return true;
        if (this.tag == null && predicate.tag != null) return false;
        if (this.tag != null && predicate.tag == null) return false;
        if (this.tag != null && !this.tag.location().equals(predicate.tag.location())) {
            return false;
        }
        if (this.items.size() != predicate.items.size()) return false;
        for (Item item : items) {
            if (!predicate.items.contains(item)) return false;
        }
        return true;
    }

    public static @NotNull ModItemWithNoNbtPredicate of() {
        return new ModItemWithNoNbtPredicate();
    }

    /**
     *
     * @param items 所需判定匹配的物品
     * @return 通过输入构建出的判据
     */
    public static @NotNull ModItemWithNoNbtPredicate of(ItemLike @NotNull ... items) {
        ModItemWithNoNbtPredicate predicate = new ModItemWithNoNbtPredicate();
        for (ItemLike item : items) {
            predicate.items.add(item.asItem());
        }
        return predicate;
    }

    /**
     *
     * @param tag 所可以接受的物品标签
     * @return 通过输入构建出的判据
     */
    public static @NotNull ModItemWithNoNbtPredicate of(TagKey<Item> tag) {
        ModItemWithNoNbtPredicate predicate = new ModItemWithNoNbtPredicate();
        predicate.tag = tag;
        return predicate;
    }

    /**
     *
     * @param items 所需添加判定匹配的物品
     * @return 添加物品后的此对象本身
     */
    public @NotNull ModItemWithNoNbtPredicate with(ItemLike @NotNull ... items) {
        for (ItemLike item : items) {
            this.items.add(item.asItem());
        }
        return this;
    }

    /**
     *
     * @param tag 所可以添加的物品标签
     * @return 添加标签后的此对象本身
     */
    public @NotNull ModItemWithNoNbtPredicate withTag(TagKey<Item> tag) {
        this.tag = tag;
        return this;
    }

    /**
     *
     * @param items 所需添加判定匹配的物品
     * @return 添加物品后的此对象本身
     */
    public ModItemWithNoNbtPredicate add(ItemLike @NotNull ... items) {
        for (ItemLike item : items) {
            this.items.add(item.asItem());
        }
        return this;
    }

    public ModItemWithNoNbtPredicate withCount(MinMaxBounds.Ints count) {
        this.count = count;
        return this;
    }

    /**
     *
     * @param durability 所需匹配的物品耐久度
     * @return 添加物品耐久度后的此对象本身
     */
    public ModItemWithNoNbtPredicate withDurability(MinMaxBounds.Ints durability) {
        this.durability = durability;
        return this;
    }

    /**
     *
     * @param matchItem 从Json获取的物品匹配判据数据
     * @return 从Json构建出的无NBT物品判据
     */
    public static @NotNull ModItemWithNoNbtPredicate fromJson(JsonElement matchItem) {
        JsonObject object = GsonHelper.convertToJsonObject(matchItem, "match_item");
        ModItemWithNoNbtPredicate predicate = ModItemWithNoNbtPredicate.of();
        if (object.has("count")) {
            predicate.withCount(MinMaxBounds.Ints.fromJson(GsonHelper.getNonNull(object, "count")));
        }
        if (object.has("durability")) {
            predicate.withDurability(MinMaxBounds.Ints.fromJson(GsonHelper.getNonNull(object, "durability")));
        }
        if (object.has("tag")) {
            predicate.withTag(
                    TagKey.create(Registries.ITEM, new ResourceLocation(GsonHelper.getAsString(object, "tag")))
            );
        }
        if (object.has("items")) {
            JsonArray array = GsonHelper.getAsJsonArray(object, "items");
            for (JsonElement element : array) {
                String id = GsonHelper.convertToString(element, "item");
                Item item = BuiltInRegistries.ITEM.get(new ResourceLocation(id));
                predicate.with(item);
            }
        }
        return predicate;
    }

    /**
     * ModItemPredicate
     */
    public JsonElement serializeToJson() {
        JsonObject object = new JsonObject();
        if (this.count != null) object.add("count", this.count.serializeToJson());
        if (this.durability != null) object.add("durability", this.durability.serializeToJson());
        if (this.tag != null) object.addProperty("tag", this.tag.location().toString());
        if (!this.items.isEmpty()) {
            JsonArray items = new JsonArray();
            for (Item item : this.items) {
                items.add(BuiltInRegistries.ITEM.getKey(item).toString());
            }
            object.add("items", items);
        }
        return object;
    }

    /**
     * ModItemPredicate
     */
    public boolean matches(ItemStack item) {
        boolean isItem = (this.tag != null && item.is(this.tag)) || this.items.stream().anyMatch(item::is);
        boolean sameNbt = !item.hasTag();
        return isItem
            && sameNbt
            && (this.count == null || this.count.matches(item.getCount()))
            && (this.durability == null || this.durability.matches(item.getDamageValue()));
    }
}
