package dev.dubhe.anvilcraft.data.recipe.anvil.predicate;

import com.google.common.base.Predicates;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilCraftingContainer;
import dev.dubhe.anvilcraft.data.recipe.anvil.RecipePredicate;
import lombok.Getter;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HasItem implements RecipePredicate {
    @Getter
    private final String type = "has_item";
    protected final Vec3 offset;
    protected final ItemPredicate matchItem;

    public HasItem(Vec3 offset, ItemPredicate matchItem) {
        this.offset = offset;
        this.matchItem = matchItem;
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
        if (!serializedRecipe.has("match_item")) throw new JsonSyntaxException("Missing match_item");
        this.matchItem = ItemPredicate.fromJson(serializedRecipe.get("match_item"));
    }

    public HasItem(@NotNull FriendlyByteBuf buffer) {
        this.offset = new Vec3(buffer.readVector3f());
        this.matchItem = ItemPredicate.fromJson(AnvilCraft.GSON.fromJson(buffer.readUtf(), JsonElement.class));
    }

    @Override
    public boolean matches(@NotNull AnvilCraftingContainer container) {
        Level level = container.getLevel();
        BlockPos pos = container.getPos();
        AABB aabb = new AABB(pos).move(this.offset);
        List<ItemEntity> entities =
            level.getEntities(EntityTypeTest.forClass(ItemEntity.class), aabb, Predicates.alwaysTrue());
        for (ItemEntity entity : entities) {
            ItemStack item = entity.getItem();
            if (this.matchItem.matches(item)) return true;
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
        buffer.writeUtf(this.matchItem.serializeToJson().toString());
    }

    @Override
    public @NotNull JsonElement toJson() {
        double[] vec3 = {this.offset.x(), this.offset.y(), this.offset.z()};
        JsonArray offset = new JsonArray();
        for (double v : vec3) {
            offset.add(new JsonPrimitive(v));
        }
        JsonElement matchItem = this.matchItem.serializeToJson();
        JsonObject object = new JsonObject();
        object.addProperty("type", this.getType());
        object.add("offset", offset);
        object.add("match_item", matchItem);
        return object;
    }
}
