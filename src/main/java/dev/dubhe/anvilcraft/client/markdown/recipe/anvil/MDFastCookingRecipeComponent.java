package dev.dubhe.anvilcraft.client.markdown.recipe.anvil;

import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.FastCookingRecipe;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import lombok.Getter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class MDFastCookingRecipeComponent extends MDBaseAnvilRecipeComponent {
    @Getter
    private final @Nullable List<ItemIngredientPredicate> ingredients;
    @Getter
    private final @Nullable List<ChanceItemStack> resultItems;
    @Getter
    private final List<BlockState> inputBlockStates;

    public MDFastCookingRecipeComponent(FastCookingRecipe recipe, boolean enableAlignCenter) {
        super(enableAlignCenter);
        this.ingredients = recipe.getInputItems();
        this.resultItems = recipe.getResultItems();
        this.inputBlockStates = List.of(
            CauldronUtil.fullState(recipe.getHasCauldron().getFluidCauldron()),
            Blocks.CAMPFIRE.defaultBlockState()
        );
    }
}
