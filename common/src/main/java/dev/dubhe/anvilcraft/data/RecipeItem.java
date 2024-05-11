package dev.dubhe.anvilcraft.data;

import lombok.Getter;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ItemLike;

@Getter
public class RecipeItem {
    private final ItemLike item;
    private final TagKey<Item> itemTagKey;
    private final double chance;
    private final int count;
    private final boolean isSelectOne;

    /**
     * 配方物品
     *
     * @param item 配方物品
     * @param chance 概率
     * @param count 数量
     * @param isSelectOne 是否属于多选一
     */
    public RecipeItem(double chance, ItemLike item, int count, boolean isSelectOne) {
        this.item = item;
        this.itemTagKey = null;
        this.chance = chance;
        this.count = count;
        this.isSelectOne = isSelectOne;
    }

    /**
     * 配方物品
     *
     * @param chance 概率
     * @param item 配方物品
     * @param count 数量
     */
    public RecipeItem(double chance, ItemLike item, int count) {
        this.item = item;
        this.itemTagKey = null;
        this.chance = chance;
        this.count = count;
        this.isSelectOne = false;
    }

    /**
     * 配方物品
     *
     * @param item 配方物品
     */
    public RecipeItem(ItemLike item) {
        this.item = item;
        this.itemTagKey = null;
        this.chance = 1;
        this.count = 1;
        this.isSelectOne = false;
    }

    /**
     * 配方物品
     *
     * @param item 配方物品
     * @param count 数量
     */
    public RecipeItem(ItemLike item, int count) {
        this.item = item;
        this.itemTagKey = null;
        this.chance = 1;
        this.count = count;
        this.isSelectOne = false;
    }

    /**
     * 配方物品
     *
     * @param chance 概率
     * @param item 配方物品
     */
    public RecipeItem(double chance, ItemLike item) {
        this.item = item;
        this.itemTagKey = null;
        this.chance = chance;
        this.count = 1;
        this.isSelectOne = false;
    }

    /**
     * 配方物品
     *
     * @param item 配方物品
     */
    public RecipeItem(ItemLike item, boolean isSelectOne) {
        this.item = item;
        this.itemTagKey = null;
        this.chance = 1;
        this.count = 1;
        this.isSelectOne = isSelectOne;
    }

    /**
     * 配方物品
     *
     * @param item 配方物品
     * @param count 数量
     */
    public RecipeItem(ItemLike item, int count, boolean isSelectOne) {
        this.item = item;
        this.itemTagKey = null;
        this.chance = 1;
        this.count = count;
        this.isSelectOne = isSelectOne;
    }

    /**
     * 配方物品
     *
     * @param chance 概率
     * @param item 配方物品
     */
    public RecipeItem(double chance, ItemLike item, boolean isSelectOne) {
        this.item = item;
        this.itemTagKey = null;
        this.chance = chance;
        this.count = 1;
        this.isSelectOne = isSelectOne;
    }

    /**
     * 配方物品
     *
     * @param chance 概率
     * @param itemTagKey 配方物品标签
     * @param count 数量
     */
    public RecipeItem(double chance, TagKey<Item> itemTagKey, int count) {
        this.item = null;
        this.itemTagKey = itemTagKey;
        this.chance = chance;
        this.count = count;
        this.isSelectOne = false;
    }

    /**
     * 配方物品
     *
     * @param itemTagKey 配方物品标签
     */
    public RecipeItem(TagKey<Item> itemTagKey) {
        this.item = null;
        this.itemTagKey = itemTagKey;
        this.chance = 1;
        this.count = 1;
        this.isSelectOne = false;
    }

    /**
     * 配方物品
     *
     * @param itemTagKey 配方物品标签
     * @param count 数量
     */
    public RecipeItem(TagKey<Item> itemTagKey, int count) {
        this.item = null;
        this.itemTagKey = itemTagKey;
        this.chance = 1;
        this.count = count;
        this.isSelectOne = false;
    }

    /**
     * 配方物品
     *
     * @param chance 概率
     * @param itemTagKey 配方物品标签
     */
    public RecipeItem(double chance, TagKey<Item> itemTagKey) {
        this.item = null;
        this.itemTagKey = itemTagKey;
        this.chance = chance;
        this.count = 1;
        this.isSelectOne = false;
    }

    /**
     * 配方物品
     *
     * @param itemTagKey 配方物品标签
     */
    public RecipeItem(TagKey<Item> itemTagKey, boolean isSelectOne) {
        this.item = null;
        this.itemTagKey = itemTagKey;
        this.chance = 1;
        this.count = 1;
        this.isSelectOne = isSelectOne;
    }

    /**
     * 配方物品
     *
     * @param itemTagKey 配方物品标签
     * @param count 数量
     */
    public RecipeItem(TagKey<Item> itemTagKey, int count, boolean isSelectOne) {
        this.item = null;
        this.itemTagKey = itemTagKey;
        this.chance = 1;
        this.count = count;
        this.isSelectOne = isSelectOne;
    }

    /**
     * 配方物品
     *
     * @param chance 概率
     * @param itemTagKey 配方物品标签
     */
    public RecipeItem(double chance, TagKey<Item> itemTagKey, boolean isSelectOne) {
        this.item = null;
        this.itemTagKey = itemTagKey;
        this.chance = chance;
        this.count = 1;
        this.isSelectOne = isSelectOne;
    }
}
