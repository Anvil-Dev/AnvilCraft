package dev.dubhe.anvilcraft.client.markdown.recipe.anvil;

import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.UnpackRecipe;
import lombok.Getter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MDUnpackRecipeComponent extends MDBaseAnvilRecipeComponent {
    @Getter
    @Nullable private final List<ItemIngredientPredicate> ingredients;

    @Getter
    @Nullable private final List<ChanceItemStack> resultItems;

    @Getter
    private final List<BlockState> inputBlockStates;

    public MDUnpackRecipeComponent(UnpackRecipe recipe, boolean enableAlignCenter) {
        super(enableAlignCenter);
        ingredients = recipe.getInputItems();
        resultItems = recipe.getResultItems();
        inputBlockStates = List.of(
            Blocks.IRON_TRAPDOOR.defaultBlockState()
        );
    }
}
