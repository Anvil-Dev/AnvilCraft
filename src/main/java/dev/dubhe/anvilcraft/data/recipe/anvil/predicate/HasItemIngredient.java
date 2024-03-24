package dev.dubhe.anvilcraft.data.recipe.anvil.predicate;

import com.google.common.base.Predicates;
import com.google.gson.JsonObject;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilCraftingContainer;
import lombok.Getter;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Getter
public class HasItemIngredient extends HasItem {
    private final String type = "has_item_ingredient";

    public HasItemIngredient(Vec3 offset, ItemPredicate matchItem) {
        super(offset, matchItem);
    }

    public HasItemIngredient(JsonObject serializedRecipe) {
        super(serializedRecipe);
    }

    public HasItemIngredient(@NotNull FriendlyByteBuf buffer) {
        super(buffer);
    }

    @Override
    public boolean process(@NotNull AnvilCraftingContainer container) {
        Level level = container.getLevel();
        BlockPos pos = container.getPos();
        AABB aabb = new AABB(pos).move(this.offset);
        List<ItemEntity> entities = level.getEntities(EntityTypeTest.forClass(ItemEntity.class), aabb, Predicates.alwaysTrue());
        for (ItemEntity entity : entities) {
            ItemStack item = entity.getItem();
            if (this.matchItem.matches(item)) {
                int count = this.matchItem.count.getMin() != null ? this.matchItem.count.getMin() : 1;
                item.shrink(count);
                entity.setItem(item.copy());
                return true;
            }
        }
        return false;
    }
}
