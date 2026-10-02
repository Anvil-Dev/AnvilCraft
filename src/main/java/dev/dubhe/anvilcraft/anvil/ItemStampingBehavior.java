package dev.dubhe.anvilcraft.anvil;

import dev.anvilcraft.lib.v2.recipe.event.InWorldRecipeEvent;
import dev.anvilcraft.lib.v2.recipe.util.InWorldRecipeContext;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.api.anvil.IAnvilBehavior;
import dev.dubhe.anvilcraft.api.event.AnvilEvent;
import dev.dubhe.anvilcraft.block.entity.StampingPlatformBlockEntity;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.StampingDiffRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import org.jspecify.annotations.Nullable;

import java.util.stream.IntStream;

public class ItemStampingBehavior implements IAnvilBehavior {
    @Override
    public boolean handle(
        ServerLevel level, BlockPos hitBlockPos, BlockState hitBlockState, double fallDistance, AnvilEvent.OnLand event
    ) {
        if (!(level.getBlockEntity(hitBlockPos) instanceof StampingPlatformBlockEntity platform)) return false;
        var input = platform.getInput();
        if (IntStream.range(0, input.size()).allMatch(slot -> input.getResource(slot).isEmpty())) return false;
        @Nullable Match selected = null;
        var recipes = level.getServer().getRecipeManager().recipeMap().byType(ModRecipeTypes.STAMPING_DIFF.get());
        for (RecipeHolder<StampingDiffRecipe> holder : recipes) {
            if (holder.value().getDiffInputItems().isEmpty()) continue;
            InWorldRecipeContext context = new InWorldRecipeContext(level, hitBlockPos.above().getBottomCenter(), event.getEntity());
            if (!holder.value().matches(context, level)) continue;
            if (selected == null || ItemStampingBehavior.compareRecipeHolders(holder, selected.recipe()) > 0) {
                selected = new Match(holder, context);
            }
        }
        if (selected == null) return false;
        RecipeHolder<StampingDiffRecipe> recipe = selected.recipe();
        InWorldRecipeContext context = selected.context();
        recipe.value().assemble(context);
        NeoForge.EVENT_BUS.post(new InWorldRecipeEvent(recipe.value().getType(), recipe.id().identifier(), recipe.value(), context));
        context.accept();
        return true;
    }

    public static int compareRecipeHolders(RecipeHolder<StampingDiffRecipe> first, RecipeHolder<StampingDiffRecipe> second) {
        int groups = Long.compare(
            ItemStampingBehavior.distinctIngredients(first.value()), ItemStampingBehavior.distinctIngredients(second.value())
        );
        if (groups != 0) return groups;
        return Long.compare(
            first.value().getDiffInputItems().stream().mapToLong(ItemIngredientPredicate::count).sum(),
            second.value().getDiffInputItems().stream().mapToLong(ItemIngredientPredicate::count).sum()
        );
    }

    private static long distinctIngredients(StampingDiffRecipe recipe) {
        return recipe.getDiffInputItems().stream()
            .map(ingredient -> new ItemIngredientPredicate(ingredient.items(), 1, ingredient.components()))
            .distinct().count();
    }

    private record Match(RecipeHolder<StampingDiffRecipe> recipe, InWorldRecipeContext context) {
    }
}
