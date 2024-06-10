package dev.dubhe.anvilcraft.mixin;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

public class IngredientValueAccessor {
    /**
     * Mixin
     */
    @Mixin(Ingredient.ItemValue.class)
    public interface Item {
        @Accessor
        ItemStack getItem();
    }

    /**
     * Mixin
     */
    @Mixin(Ingredient.TagValue.class)
    public interface Tag {
        @Accessor
        TagKey<net.minecraft.world.item.Item> getTag();
    }
}
