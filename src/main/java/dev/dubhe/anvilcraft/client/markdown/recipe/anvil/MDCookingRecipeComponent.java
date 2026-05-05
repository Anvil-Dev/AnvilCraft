package dev.dubhe.anvilcraft.client.markdown.recipe.anvil;

import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.CookingRecipe;
import lombok.Getter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MDCookingRecipeComponent extends MDBaseAnvilRecipeComponent {
    @Getter
    @Nullable private final List<ItemIngredientPredicate> ingredients;

    @Getter
    @Nullable private final List<ChanceItemStack> resultItems;

    @Getter
    private final List<BlockState> inputBlockStates;

    public MDCookingRecipeComponent(CookingRecipe recipe, boolean enableAlignCenter) {
        super(enableAlignCenter);
        ingredients = recipe.getInputItems();
        resultItems = recipe.getResultItems();
        inputBlockStates = List.of(
            Blocks.CAULDRON.defaultBlockState(),
            Blocks.CAMPFIRE.defaultBlockState()
        );
    }
}
