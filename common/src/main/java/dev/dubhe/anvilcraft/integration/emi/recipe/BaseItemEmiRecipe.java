package dev.dubhe.anvilcraft.integration.emi.recipe;

import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SelectOne;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SpawnItem;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.HasItem;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.TagEmiIngredient;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.world.phys.Vec2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class BaseItemEmiRecipe extends BaseEmiRecipe {

    public BaseItemEmiRecipe(EmiRecipeCategory category,
        AnvilRecipe recipe) {
        super(category, recipe, 240, 80);
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
            }
        });
        recipe.getOutcomes().forEach(recipeOutcome -> {
            if (recipeOutcome instanceof SpawnItem spawnItem) {
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
            }
        });
    }

    protected void addInputSlots(WidgetHolder widgets) {
        List<Vec2> posLis = getSlotsPosLis(inputs.size());
        Vec2 size = getSlotsComposeSize(inputs.size());
        int x = inputs.size() == 1 ? 41 : (int) ((26 - size.x / 2) + 11);
        int y = (int) ((26 - size.y / 2) + 11);
        Iterator<EmiIngredient> ingredientIterator = inputs.iterator();
        for (Vec2 vec2 : posLis) {
            addSlot(ingredientIterator.next(), (int) (x + vec2.x), (int) (y + vec2.y), widgets);
        }
    }

    protected void addOutputs(WidgetHolder widgets) {
        int outputSize = simpleOutputs.size() + selectOneItemList.size();
        List<Vec2> posLis = getSlotsPosLis(outputSize);
        Vec2 composeSize = getSlotsComposeSize(outputSize);
        int x = outputSize == 1 ? 179 : (int) ((26 - composeSize.x / 2) + 169);
        int y = (int) ((26 - composeSize.y / 2) + 11);
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
        addBendArrow(widgets, 75, 34);
        addStraightArrow(widgets, 119, 34);
        addPlus(widgets, 154, 34);
        addInputSlots(widgets);
        addOutputs(widgets);
    }
}
