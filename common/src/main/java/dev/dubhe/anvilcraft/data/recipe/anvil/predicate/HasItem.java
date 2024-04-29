package dev.dubhe.anvilcraft.data.recipe.anvil.predicate;

import com.google.common.base.Predicates;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilCraftingContainer;
import dev.dubhe.anvilcraft.data.recipe.anvil.HasData;
import dev.dubhe.anvilcraft.data.recipe.anvil.RecipePredicate;
import lombok.Getter;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HasItem implements RecipePredicate, HasData {
    @Getter
    private final String type = "has_item";
    protected final Vec3 offset;
    protected final ItemPredicate matchItem;
    protected String path = null;
    @Getter
    protected Map.Entry<String, CompoundTag> data = null;
    protected final List<String> hasTag = new ArrayList<>();
    protected final List<String> notHasTag = new ArrayList<>();

    public HasItem(Vec3 offset, ItemPredicate matchItem) {
        this.offset = offset;
        this.matchItem = matchItem;
    }

    public HasItem saveItemData(String path) {
        this.path = path;
        return this;
    }

    public HasItem hasTag(String path) {
        this.hasTag.add(path);
        return this;
    }

    public HasItem notHasTag(String path) {
        this.notHasTag.add(path);
        return this;
    }

    /**
     * 拥有物品
     *
     * @param serializedRecipe 序列化配方
     */
    public HasItem(JsonObject serializedRecipe) {
        JsonArray array = GsonHelper.getAsJsonArray(serializedRecipe, "offset");
        double[] vec3 = {0.0d, 0.0d, 0.0d};
        for (int i = 0; i < array.size() && i < 3; i++) {
            JsonElement element = array.get(i);
            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
                vec3[i] = element.getAsDouble();
            } else throw new JsonSyntaxException("Expected offset to be a Double, was " + GsonHelper.getType(element));
        }
        this.offset = new Vec3(vec3[0], vec3[1], vec3[2]);
        if (serializedRecipe.has("data_path")) {
            this.path = GsonHelper.getAsString(serializedRecipe, "data_path");
        }
        if (!serializedRecipe.has("match_item")) throw new JsonSyntaxException("Missing match_item");
        if (serializedRecipe.has("has_tag")) {
            JsonArray array1 = GsonHelper.getAsJsonArray(serializedRecipe, "has_tag");
            array1.forEach(element -> this.hasTag.add(element.getAsString()));
        }
        if (serializedRecipe.has("not_has_tag")) {
            JsonArray array1 = GsonHelper.getAsJsonArray(serializedRecipe, "not_has_tag");
            array1.forEach(element -> this.notHasTag.add(element.getAsString()));
        }
        this.matchItem = ItemPredicate.fromJson(serializedRecipe.get("match_item"));
    }

    /**
     * @param buffer 缓冲区
     */
    public HasItem(@NotNull FriendlyByteBuf buffer) {
        this.offset = new Vec3(buffer.readVector3f());
        if (buffer.readBoolean()) {
            this.path = buffer.readUtf();
        }
        int read = buffer.readVarInt();
        for (int i = 0; i < read; i++) {
            this.hasTag.add(buffer.readUtf());
        }
        read = buffer.readVarInt();
        for (int i = 0; i < read; i++) {
            this.notHasTag.add(buffer.readUtf());
        }
        this.matchItem = ItemPredicate.fromJson(AnvilCraft.GSON.fromJson(buffer.readUtf(), JsonElement.class));
    }

    @Override
    public boolean matches(@NotNull AnvilCraftingContainer container) {
        Level level = container.getLevel();
        BlockPos pos = container.getPos();
        AABB aabb = new AABB(pos).move(this.offset);
        List<ItemEntity> entities =
            level.getEntities(EntityTypeTest.forClass(ItemEntity.class), aabb, Predicates.alwaysTrue());
        entities:
        for (ItemEntity entity : entities) {
            ItemStack item = entity.getItem();
            if (this.matchItem.matches(item)) {
                for (String path : this.hasTag) {
                    if (!item.hasTag()) continue;
                    CompoundTag tag = item.getOrCreateTag();
                    String[] paths = path.split("\\.");
                    for (int i = 0; i < paths.length; i++) {
                        if (!tag.contains(paths[i])) continue entities;
                        if (i != paths.length - 1) {
                            tag = tag.getCompound(paths[i]);
                        }
                    }
                }
                for (String path : this.notHasTag) {
                    CompoundTag tag = item.getOrCreateTag();
                    String[] paths = path.split("\\.");
                    for (int i = 0; i < paths.length; i++) {
                        if (i != paths.length - 1) {
                            tag = tag.getCompound(paths[i]);
                        } else if (tag.contains(paths[i])) continue entities;
                    }
                }
                if (this.path != null) {
                    this.data = Map.entry(this.path, item.hasTag() ? item.getOrCreateTag() : new CompoundTag());
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean process(AnvilCraftingContainer container) {
        return true;
    }

    @Override
    public void toNetwork(@NotNull FriendlyByteBuf buffer) {
        buffer.writeUtf(this.getType());
        buffer.writeVector3f(this.offset.toVector3f());
        buffer.writeBoolean(this.path != null);
        if (this.path != null) {
            buffer.writeUtf(this.path);
        }
        buffer.writeVarInt(this.hasTag.size());
        this.hasTag.forEach(buffer::writeUtf);
        buffer.writeVarInt(this.notHasTag.size());
        this.notHasTag.forEach(buffer::writeUtf);
        buffer.writeUtf(this.matchItem.serializeToJson().toString());
    }

    @Override
    public @NotNull JsonElement toJson() {
        double[] vec3 = {this.offset.x(), this.offset.y(), this.offset.z()};
        JsonArray offset = new JsonArray();
        for (double v : vec3) {
            offset.add(new JsonPrimitive(v));
        }
        JsonObject object = new JsonObject();
        object.addProperty("type", this.getType());
        object.add("offset", offset);
        if (this.path != null) object.addProperty("data_path", this.path);
        JsonElement matchItem = this.matchItem.serializeToJson();
        object.add("match_item", matchItem);
        JsonArray hasTag = new JsonArray();
        this.hasTag.forEach(hasTag::add);
        if (!hasTag.isEmpty()) object.add("has_tag", hasTag);
        JsonArray notHasTag = new JsonArray();
        this.notHasTag.forEach(notHasTag::add);
        if (!notHasTag.isEmpty()) object.add("not_has_tag", notHasTag);
        return object;
    }
}
