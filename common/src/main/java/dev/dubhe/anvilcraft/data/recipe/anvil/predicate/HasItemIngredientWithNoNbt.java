package dev.dubhe.anvilcraft.data.recipe.anvil.predicate;

import com.google.common.base.Predicates;
import com.google.gson.JsonObject;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilCraftingContext;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class HasItemIngredientWithNoNbt extends HasItemIngredient{
    public ModItemWithNoNbtPredicate matchItemReal;
    @Getter
    public final String type = "has_item_ingredient_no_nbt";

    public HasItemIngredientWithNoNbt(Vec3 offset, ModItemWithNoNbtPredicate matchItem) {
        super(offset, matchItem.TurnIntoModItemPredicate());
        this.matchItemReal = matchItem;
    }

    public HasItemIngredientWithNoNbt(JsonObject serializedRecipe) {
        super(serializedRecipe);
        this.matchItemReal = new ModItemWithNoNbtPredicate(super.getMatchItem());
    }

    public HasItemIngredientWithNoNbt(@NotNull FriendlyByteBuf buffer) {
        super(buffer);
        this.matchItemReal = new ModItemWithNoNbtPredicate(super.getMatchItem());
    }

    @Override
    public boolean matches(@NotNull AnvilCraftingContext context){
        Level level = context.getLevel();
        BlockPos pos = context.getPos();
        AABB aabb = new AABB(pos).move(this.offset);
        List<ItemEntity> entities =
                level.getEntities(EntityTypeTest.forClass(ItemEntity.class), aabb, Predicates.alwaysTrue());
        entities:
        for (ItemEntity entity : entities) {
            ItemStack item = entity.getItem();
            if (this.matchItemReal.matches(item)) {
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

    public boolean process(@NotNull AnvilCraftingContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getPos();
        AABB aabb = new AABB(pos).move(this.offset);
        List<ItemEntity> entities =
                level.getEntities(EntityTypeTest.forClass(ItemEntity.class), aabb, Predicates.alwaysTrue());
        for (ItemEntity entity : entities) {
            ItemStack item = entity.getItem();
            if (this.matchItemReal.matches(item)) {
                int count = this.matchItemReal.count.getMin() != null ? this.matchItemReal.count.getMin() : 1;
                if (item.getItem().hasCraftingRemainingItem()) {
                    assert item.getItem().getCraftingRemainingItem() != null;
                    ItemStack stack = new ItemStack(item.getItem().getCraftingRemainingItem(), count);
                    Vec3 vec3 = pos.getCenter().add(this.offset);
                    ItemEntity itemEntity = new ItemEntity(level, vec3.x, vec3.y, vec3.z, stack, 0.0, 0.0, 0.0);
                    itemEntity.setDefaultPickUpDelay();
                    level.addFreshEntity(itemEntity);
                }
                item.shrink(count);
                entity.setItem(item.copy());
                return true;
            }
        }
        return false;
    }


}
