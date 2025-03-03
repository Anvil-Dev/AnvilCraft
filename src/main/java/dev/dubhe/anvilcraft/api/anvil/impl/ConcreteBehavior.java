package dev.dubhe.anvilcraft.api.anvil.impl;

import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.event.anvil.AnvilFallOnLandEvent;
import dev.dubhe.anvilcraft.block.CementCauldronBlock;
import dev.dubhe.anvilcraft.block.state.Color;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.ConcreteRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.input.ItemProcessInput;
import dev.dubhe.anvilcraft.util.AnvilUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ConcreteBehavior implements IAnvilBehavior {
    @SuppressWarnings("DuplicatedCode")
    @Override
    public boolean handle(
        Level level,
        BlockPos hitBlockPos,
        BlockState hitBlockState,
        float fallDistance,
        AnvilFallOnLandEvent event
    ) {
        Map<ItemEntity, ItemStack> items = level.getEntitiesOfClass(ItemEntity.class, new AABB(hitBlockPos)).stream()
            .map(it -> Map.entry(it, it.getItem()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        ItemProcessInput input = new ItemProcessInput(items.values().stream().toList());
        Optional<RecipeHolder<ConcreteRecipe>> recipeOptional =
            level.getRecipeManager().getRecipeFor(ModRecipeTypes.CONCRETE_TYPE.get(), input, level);
        if (recipeOptional.isPresent() && hitBlockState.getBlock() instanceof CementCauldronBlock cauldronBlock) {
            RecipeHolder<ConcreteRecipe> recipe = recipeOptional.get();
            Color color = cauldronBlock.getColor();
            ItemStack result = new ItemStack(
                BuiltInRegistries.ITEM.get(
                    ResourceLocation.parse(
                        recipe.value().result.formatted(color.getSerializedName())
                    )
                ),
                recipe.value().resultCount
            );
            int times = recipe.value().getMaxCraftTime(input);
            result.setCount(result.getCount() * times);
            for (int i = 0; i < times; i++) {
                for (Ingredient ingredient : recipe.value().getIngredients()) {
                    for (ItemStack stack : items.values()) {
                        if (ingredient.test(stack)) {
                            stack.shrink(1);
                            break;
                        }
                    }
                }
            }
            AnvilUtil.dropItems(List.of(result), level, hitBlockPos.getCenter());
            items.forEach((k, v) -> {
                if (v.isEmpty()) {
                    k.discard();
                    return;
                }
                k.setItem(v.copy());
            });
            return true;
        }
        return false;
    }
}
