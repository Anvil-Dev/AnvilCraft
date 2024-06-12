package dev.dubhe.anvilcraft.data;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

import lombok.Getter;

@Getter
public class RecipeItem {
    private final ItemLike item;
    private final TagKey<Item> itemTagKey;
    private final double chance;
    private final int count;
    private final boolean isSelectOne;

    private RecipeItem(
            ItemLike item, TagKey<Item> itemTagKey, int count, double chance, boolean isSelectOne) {
        this.item = item;
        this.itemTagKey = itemTagKey;
        this.count = count;
        this.chance = chance;
        this.isSelectOne = isSelectOne;
    }

    public static RecipeItem of(ItemLike item, int count, double chance, boolean isSelectOne) {
        return new RecipeItem(item, null, count, chance, isSelectOne);
    }

    public static RecipeItem of(ItemLike item, int count, double chance) {
        return new RecipeItem(item, null, count, chance, false);
    }

    public static RecipeItem of(ItemLike item, int count) {
        return new RecipeItem(item, null, count, 1d, false);
    }

    public static RecipeItem of(ItemLike item, double chance) {
        return new RecipeItem(item, null, 1, chance, false);
    }

    public static RecipeItem of(ItemLike item, int count, boolean isSelectOne) {
        return new RecipeItem(item, null, count, 1d, isSelectOne);
    }

    public static RecipeItem of(ItemLike item, double chance, boolean isSelectOne) {
        return new RecipeItem(item, null, 1, chance, isSelectOne);
    }

    public static RecipeItem of(
            TagKey<Item> itemTagKey, int count, double chance, boolean isSelectOne) {
        return new RecipeItem(null, itemTagKey, count, chance, isSelectOne);
    }

    public static RecipeItem of(TagKey<Item> itemTagKey, int count, double chance) {
        return new RecipeItem(null, itemTagKey, count, chance, false);
    }

    public static RecipeItem of(TagKey<Item> itemTagKey, int count) {
        return new RecipeItem(null, itemTagKey, count, 1d, false);
    }

    public static RecipeItem of(TagKey<Item> itemTagKey, double chance) {
        return new RecipeItem(null, itemTagKey, 1, chance, false);
    }

    public static RecipeItem of(TagKey<Item> itemTagKey, int count, boolean isSelectOne) {
        return new RecipeItem(null, itemTagKey, count, 1d, isSelectOne);
    }

    public static RecipeItem of(TagKey<Item> itemTagKey, double chance, boolean isSelectOne) {
        return new RecipeItem(null, itemTagKey, 1, chance, isSelectOne);
    }

    public static RecipeItem of(ItemLike item) {
        return new RecipeItem(item, null, 1, 1d, false);
    }

    public static RecipeItem of(TagKey<Item> itemTagKey) {
        return new RecipeItem(null, itemTagKey, 1, 1d, false);
    }

    /**
     *  获取key
     *
     * @return key
     */
    public String getKey() {
        return this.item == null
                ? this.itemTagKey == null
                        ? ""
                        : itemTagKey.location().getNamespace() + "_" + itemTagKey.location().getPath()
                : BuiltInRegistries.ITEM.getKey(this.item.asItem()).getPath();
    }
}
