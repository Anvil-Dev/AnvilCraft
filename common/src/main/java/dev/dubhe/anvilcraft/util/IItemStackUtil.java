package dev.dubhe.anvilcraft.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.jetbrains.annotations.NotNull;

/**
 * 物品堆栈注入器
 */
public interface IItemStackUtil {
    /**
     * 从 json 读取
     *
     * @param element json
     * @return 物品堆栈
     */
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

    /**
     * 序列化 {@link ItemStack}
     *
     * @param stack 物品
     * @return 序列化Json
     */
    static @NotNull JsonElement toJson(@NotNull ItemStack stack) {
        JsonObject object = new JsonObject();
        object.addProperty("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        if (stack.getCount() > 1) object.addProperty("count", stack.getCount());
        if (stack.hasTag()) object.addProperty("data", stack.getOrCreateTag().toString());
        return object;
    }

    default JsonElement anvilcraft$toJson() {
        return new JsonObject();
    }
}
