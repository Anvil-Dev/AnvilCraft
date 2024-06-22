package dev.dubhe.anvilcraft.data.recipe;

import lombok.Getter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

@Getter
public class RecipeResult {
    private final RecipeBlock recipeBlock;
    private final RecipeItem recipeItem;
    private final Vec3 offset;
    private final ItemLike icon;

    /**
     * 配方结果
     */
    public RecipeResult(RecipeBlock recipeBlock, RecipeItem recipeItem, Vec3 offset, ItemLike icon) {
        this.recipeBlock = recipeBlock;
        this.recipeItem = recipeItem;
        this.offset = offset;
        this.icon = icon;
    }

    public static RecipeResult of(RecipeBlock recipeBlock, ItemLike icon) {
        return new RecipeResult(recipeBlock, null, Vec3.ZERO, icon);
    }

    public static RecipeResult of(RecipeBlock recipeBlock, Vec3 vec3, ItemLike icon) {
        return new RecipeResult(recipeBlock, null, vec3, icon);
    }

    public static RecipeResult of(Block block, ItemLike icon) {
        return new RecipeResult(RecipeBlock.of(block), null, Vec3.ZERO, icon);
    }

    public static RecipeResult of(Block block, Vec3 vec3, ItemLike icon) {
        return new RecipeResult(RecipeBlock.of(block), null, vec3, icon);
    }

    public static RecipeResult of(RecipeItem recipeItem) {
        return new RecipeResult(null, recipeItem, Vec3.ZERO, recipeItem.getIcon());
    }

    public static RecipeResult of(RecipeItem recipeItem, Vec3 vec3) {
        return new RecipeResult(null, recipeItem, vec3, recipeItem.getIcon());
    }

    public static RecipeResult of(ItemLike itemLike) {
        return new RecipeResult(null, RecipeItem.of(itemLike), Vec3.ZERO, itemLike);
    }

    public static RecipeResult of(ItemLike itemLike, Vec3 vec3) {
        return new RecipeResult(null, RecipeItem.of(itemLike), vec3, itemLike);
    }

    public boolean isItem() {
        return recipeBlock == null;
    }

    /**
     *  获取key
     *
     * @return key
     */
    public String getKey() {
        return isItem()
                ? recipeItem.getKey()
                : recipeBlock.getKey();
    }
}
