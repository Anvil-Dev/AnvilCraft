package dev.dubhe.anvilcraft.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.NotNull;

public interface IItemStackInjector {
    @Deprecated
    default ItemStack dataCopy(ItemStack stack) {
        return ItemStack.EMPTY;
    }

    @Deprecated
    default boolean removeEnchant(Enchantment enchantment) {
        return false;
    }

    @SuppressWarnings("DuplicatedCode")
    static @NotNull ItemStack fromJson(@NotNull JsonElement element) {
        if (!element.isJsonObject()) throw new JsonSyntaxException("Expected item to be string");
        JsonObject object = element.getAsJsonObject();
        Item item = ShapedRecipe.itemFromJson(object);
        int i = GsonHelper.getAsInt(object, "count", 1);
        if (i < 1) {
            throw new JsonSyntaxException("Invalid output count: " + i);
        }
        ItemStack stack = item.getDefaultInstance();
        stack.setCount(i);
        if (object.has("data")) {
            if (!object.get("data").isJsonPrimitive()) throw new JsonSyntaxException("Expected item to be string");
            CompoundTag tag;
            try {
                tag = TagParser.parseTag(object.get("data").getAsString());
            } catch (CommandSyntaxException ignored) {
                throw new JsonSyntaxException("Invalid NBT string");
            }
            stack.setTag(stack.getOrCreateTag().merge(tag));
        }
        return stack;
    }

    @Deprecated
    default JsonElement toJson() {
        return this.anvilcraft$toJson();
    }

    default JsonElement anvilcraft$toJson() {
        return new JsonObject();
    }
}
