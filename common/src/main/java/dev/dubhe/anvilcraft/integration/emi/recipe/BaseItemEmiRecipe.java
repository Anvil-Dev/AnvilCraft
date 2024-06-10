package dev.dubhe.anvilcraft.integration.emi.recipe;

import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SelectOne;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SetBlock;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SpawnItem;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.HasBlock;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.HasFluidCauldron;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.HasItem;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.NotHasBlock;
import dev.dubhe.anvilcraft.integration.emi.ui.BlockWidget;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.TagEmiIngredient;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class BaseItemEmiRecipe extends BaseEmiRecipe {
    protected ArrayList<BlockState> workBlockStates = new ArrayList<>(List.of(ANVIL_BLOCK_STATE));
    protected ArrayList<Vec2> workBlockPoses = new ArrayList<>(List.of(new Vec2(0, 1)));
    protected ArrayList<Vec2> outputWorkBlockPoses = new ArrayList<>(List.of(new Vec2(0, 0)));
    protected boolean isOutputBlock = false;
    protected BlockState outputBlockState = Blocks.AIR.defaultBlockState();

    /**
     * 基础物品配方(可自动适配绝大多数配方)
     *
     * @param category 配方类型
     * @param recipe {@link AnvilRecipe}配方
     */
    public BaseItemEmiRecipe(EmiRecipeCategory category,
        AnvilRecipe recipe) {
        super(category, recipe, 246, 84);
        recipe.getPredicates().forEach(ingredient -> {
            if (ingredient instanceof HasItem hasItem && hasItem.getMatchItem().count.getMin() != null) {
                if (hasItem.getMatchItem().getTag() != null)
                    inputs.add(
                            new TagEmiIngredient(hasItem.getMatchItem().getTag(),
                                    hasItem.getMatchItem().count.getMin()));
                else {
                    hasItem.getMatchItem().getItems().forEach(item -> inputs.add(EmiStack.of(item,
                            hasItem.getMatchItem().count.getMin())));
                }
            } else if (
                    ingredient instanceof HasBlock hasBlock
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
                this.workBlockStates.add(workBlockState);
                this.workBlockPoses.add(new Vec2((float) hasBlock.getOffset().x, (float) hasBlock.getOffset().y));
                this.outputWorkBlockPoses.add(new Vec2((float) hasBlock.getOffset().x, (float) hasBlock.getOffset().y));
            } else if (
                    ingredient instanceof HasFluidCauldron hasFluidCauldron) {
                this.workBlockStates.add(hasFluidCauldron.getMatchBlock().defaultBlockState());
                this.workBlockPoses.add(
                        new Vec2((float) hasFluidCauldron.getOffset().x, (float) hasFluidCauldron.getOffset().y));
                this.outputWorkBlockPoses.add(
                        new Vec2((float) hasFluidCauldron.getOffset().x, (float) hasFluidCauldron.getOffset().y));
            }
        });
        recipe.getOutcomes().forEach(recipeOutcome -> {
            if (recipeOutcome instanceof SpawnItem spawnItem && !spawnItem.getResult().isEmpty()) {
                simpleOutputs.add(EmiStack.of(spawnItem.getResult()).setChance((float) spawnItem.getChance()));
            } else if (recipeOutcome instanceof SelectOne selectOne) {
                ArrayList<EmiStack> list = new ArrayList<>();
                selectOne.getOutcomes().forEach(recipeOutcome1 -> {
                    if (recipeOutcome1 instanceof SpawnItem spawnItem) {
                        list.add(EmiStack.of(spawnItem.getResult())
                                .setChance((float) spawnItem.getChance()));
                    }
                });
                selectOneItemList.add(list);
            } else if (recipeOutcome instanceof SetBlock setBlock) {
                blockOutputs.add(EmiStack.of(setBlock.getResult().getBlock().asItem().getDefaultInstance()));
                isOutputBlock = true;
                outputBlockState = setBlock.getResult();
            }
        });
        this.height = 84 + (15 * Math.max(0, workBlockStates.size() - 2));
    }

    protected void addInputSlots(WidgetHolder widgets) {
        List<Vec2> posLis = getSlotsPosLis(inputs.size());
        Vec2 size = getSlotsComposeSize(inputs.size());
        int x = inputs.size() == 1 ? 40 : (int) ((26 - size.x / 2) + 10);
        int y = (int) ((26 - size.y / 2) + 15);
        Iterator<EmiIngredient> ingredientIterator = inputs.iterator();
        for (Vec2 vec2 : posLis) {
            addSlot(ingredientIterator.next(), (int) (x + vec2.x), (int) (y + vec2.y), widgets);
        }
    }

    protected void addOutputs(WidgetHolder widgets) {
        int outputSize = simpleOutputs.size() + selectOneItemList.size();
        List<Vec2> posLis = getSlotsPosLis(outputSize);
        Vec2 composeSize = getSlotsComposeSize(outputSize);
        int x = outputSize == 1 ? 190 : (int) ((26 - composeSize.x / 2) + 190);
        int y = (int) ((26 - composeSize.y / 2) + 15);
        Iterator<EmiStack> emiStackIterator = simpleOutputs.iterator();
        Iterator<List<EmiStack>> emiStackListIterator = selectOneItemList.iterator();
        for (Vec2 vec2 : posLis) {
            if (emiStackIterator.hasNext()) {
                addSlot(emiStackIterator.next(), (int) (x + vec2.x), (int) (y + vec2.y), widgets);
            } else {
                addSelectOneSlot(emiStackListIterator.next(), (int) (x + vec2.x), (int) (y + vec2.y), widgets);
            }
        }
    }

    protected List<Vec2> getSlotsPosLis(int length) {
        ArrayList<Vec2> vec2List = new ArrayList<>();
        final int modelNumber = length <= 4 ? 2 : 3;
        for (int index = 0; index < length; index++) {
            vec2List.add(new Vec2(
                    20 * (index % modelNumber),
                    20 * (int) ((float) index / modelNumber)
            ));
        }
        return vec2List;
    }

    protected Vec2 getSlotsComposeSize(int length) {
        if (length <= 0) return Vec2.ZERO;
        if (length == 1) return new Vec2(18, 18);
        final int modelNumber = length <= 4 ? 2 : 3;
        return new Vec2(
                modelNumber * 20 + (modelNumber - 1) * 2,
                (int) ((float) length / modelNumber) * 20 + ((int) ((float) length / modelNumber) - 1) * 2
        );
    }


    @Override
    public void addWidgets(WidgetHolder widgets) {
        addStraightArrow(widgets, 120, 41);
        if (!inputs.isEmpty()) {
            addInputArrow(widgets, 72, 32);
            addInputSlots(widgets);
        }
        if (!isOutputBlock || !getItemOutputs().isEmpty()) {
            addOutputArrow(widgets, 163, 30);
            addOutputs(widgets);
        } else {
            addStraightArrow(widgets, 163, 41);
            widgets.addDrawable(180, 25, 0, 0, new BlockWidget(
                    outputBlockState,
                    new Vec2(0, -1))
            );
        }
        widgets.addDrawable(90, 25, 0, 0, new BlockWidget(
                workBlockStates,
                workBlockPoses
        ));
        widgets.addDrawable(135, 25, 0, 0, new BlockWidget(
                workBlockStates,
                outputWorkBlockPoses)
        );
    }
}
