package dev.dubhe.anvilcraft.integration.emi.recipe;

import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SetBlock;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.HasBlock;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.HasFluidCauldron;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.NotHasBlock;
import dev.dubhe.anvilcraft.integration.emi.ui.BlockWidget;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BaseBlockEmiRecipe extends BaseEmiRecipe {
    protected ArrayList<BlockState> inputBlockStates = new ArrayList<>(List.of(ANVIL_BLOCK_STATE));
    protected ArrayList<Vec2> inputBlockPoses = new ArrayList<>(List.of(new Vec2(0, 1)));
    protected ArrayList<BlockState> outPutBlockStates = new ArrayList<>(List.of(ANVIL_BLOCK_STATE));
    protected ArrayList<Vec2> outPutBlockPoses = new ArrayList<>(List.of(new Vec2(0, 0)));

    /**
     * 基础方块配方
     *
     * @param category 配方类型
     * @param recipe {@link AnvilRecipe}配方
     */
    public BaseBlockEmiRecipe(EmiRecipeCategory category,
                             AnvilRecipe recipe) {
        super(category, recipe, 246, 84);
        recipe.getPredicates().forEach(ingredient -> {
            if (ingredient instanceof HasBlock hasBlock
                        && !(ingredient instanceof NotHasBlock)
                        && !hasBlock.getMatchBlock().getBlocks().isEmpty()) {
                Block block = hasBlock.getMatchBlock().getBlocks().iterator().next();
                BlockState workBlockState = block.defaultBlockState();
                for (BlockState blockState : block.getStateDefinition().getPossibleStates()) {
                    first:
                    for (Map.Entry<String, String> entry : hasBlock.getMatchBlock().getProperties().entrySet()) {
                        for (Map.Entry<Property<?>, Comparable<?>> entry1 : blockState.getValues().entrySet()) {
                            if (!entry1.getKey().getName().equals(entry.getKey())) continue;
                            if (!entry1.getValue().toString().equals(entry.getValue())) break first;
                        }
                        workBlockState = blockState;
                    }
                }
                this.inputBlockStates.add(workBlockState);
                this.inputBlockPoses.add(new Vec2((float) hasBlock.getOffset().x, (float) hasBlock.getOffset().y));
                simpleOutputs.add(EmiStack.of(workBlockState.getBlock().asItem().getDefaultInstance()));
            } else if (
                    ingredient instanceof HasFluidCauldron hasFluidCauldron) {
                this.inputBlockStates.add(hasFluidCauldron.getMatchBlock().defaultBlockState());
                this.inputBlockPoses.add(
                        new Vec2((float) hasFluidCauldron.getOffset().x, (float) hasFluidCauldron.getOffset().y));
            }
        });
        recipe.getOutcomes().forEach(recipeOutcome -> {
            if (recipeOutcome instanceof SetBlock setBlock) {
                simpleOutputs.add(EmiStack.of(setBlock.getResult().getBlock().asItem().getDefaultInstance()));
                outPutBlockStates.add(setBlock.getResult());
                outPutBlockPoses.add(new Vec2((float) setBlock.getOffset().x, (float) setBlock.getOffset().y));
            }
        });
        this.height = 84 + (15 * Math.max(0, inputBlockStates.size() - 2));
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        addStraightArrow(widgets, 120, 41);
        widgets.addDrawable(90, 25, 0, 0, new BlockWidget(
                inputBlockStates,
                inputBlockPoses
        ));
        widgets.addDrawable(135, 25, 0, 0, new BlockWidget(
                outPutBlockStates,
                outPutBlockPoses)
        );
    }
}
