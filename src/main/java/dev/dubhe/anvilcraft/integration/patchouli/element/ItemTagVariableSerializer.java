package dev.dubhe.anvilcraft.integration.patchouli.element;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import vazkii.patchouli.api.IVariableSerializer;

@MethodsReturnNonnullByDefault
public class ItemTagVariableSerializer implements IVariableSerializer<TagKey<Item>> {
    @Override
    public TagKey<Item> fromJson(JsonElement json, HolderLookup.Provider registries) {
        if (!json.isJsonPrimitive()) throw new IllegalArgumentException("Can't make a TagKey from this json!");
        String tagIdRaw = json.getAsString();
        if (!tagIdRaw.startsWith("#")) throw new IllegalArgumentException("Can't make a TagKey from this json! Valid tag must starts with '#'");
        ResourceLocation tagId = ResourceLocation.parse(tagIdRaw.split("#")[1]);
        return TagKey.create(Registries.ITEM, tagId);
    }

    @Override
    public JsonElement toJson(TagKey<Item> object, HolderLookup.Provider registries) {
        return new JsonPrimitive("#" + object.location());
    }

    @SuppressWarnings("unchecked")
    public static Class<TagKey<Item>> getClazz() {
        return (Class<TagKey<Item>>) TagKey.create(Registries.ITEM, AnvilCraft.of("empty")).getClass();
    }
}
