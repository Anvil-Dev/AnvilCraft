package dev.dubhe.anvilcraft.integration.emi.recipe;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SpawnItem;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.HasItem;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.emi.emi.registry.EmiTags;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import lombok.Getter;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;


@Getter
public abstract class BaseEmiRecipe implements EmiRecipe {
    public final ResourceLocation EMI_GUI_TEXTURES = AnvilCraft.of("textures/gui/container/emi/emi.png");

    protected final List<Vec2> inputVec2s = new ArrayList<>(

    );
    protected final BlockState ANVIL_BLOCK_STATE = Blocks.ANVIL.defaultBlockState().setValue(
            AnvilBlock.FACING, Direction.WEST);
    protected EmiRecipeCategory category;
    protected final AnvilRecipe recipe;
    protected List<EmiIngredient> inputs = new ArrayList<>();
    protected List<EmiStack> outputs = new ArrayList<>();
    protected ResourceLocation id;
    protected int width;
    protected int height;

    public BaseEmiRecipe(EmiRecipeCategory category, AnvilRecipe recipe, int width, int height) {
        this.category = category;
        this.recipe = recipe;
        this.id = recipe.getId();
        this.width = width;
        this.height = height;
        recipe.getPredicates().forEach(ingredient -> {
            if (ingredient instanceof HasItem hasItem) {
                hasItem.getMatchItem().getItems().forEach(item -> inputs.add(EmiStack.of(item)));
                if (hasItem.getMatchItem().getTag() != null)
                    inputs.addAll(EmiTags.getValues(hasItem.getMatchItem().getTag()));
            }
        });
        recipe.getOutcomes().forEach(recipeOutcome -> {
            if (recipeOutcome instanceof SpawnItem spawnItem) {
                outputs.add(EmiStack.of(spawnItem.getResult()));
            }
        });
    }

    @Override
    public int getDisplayWidth() {
        return width;
    }

    @Override
    public int getDisplayHeight() {
        return height;
    }

    @Override
    public abstract void addWidgets(WidgetHolder widgets);

    protected void addBendArrow(WidgetHolder widgets, int x, int y) {
        widgets.addTexture(EMI_GUI_TEXTURES, x, y, 17, 9, 0, 29);
    }

    protected void addStraightArrow(WidgetHolder widgets, int x, int y) {
        widgets.addTexture(EMI_GUI_TEXTURES, x, y, 9, 9, 10, 19);
    }

    protected void addChangeSlot(WidgetHolder widgets, int x, int y) {
        widgets.addTexture(EMI_GUI_TEXTURES, x, y, 18, 18, 19, 0);
    }

    protected void addSelectSlot(WidgetHolder widgets, int x, int y) {
        widgets.addTexture(EMI_GUI_TEXTURES, x, y, 18, 18, 38, 0);
    }

    protected void addSimpleSlot(EmiIngredient ingredient, int x, int y, WidgetHolder widgets) {
        widgets.addTexture(EMI_GUI_TEXTURES, x, y, 18, 18, 0, 0);
        widgets.addSlot(ingredient, x, y);
    }

    protected void addInputSlots(List<EmiIngredient> ingredients, WidgetHolder widgets) {
        List<Vec2> posLis = getSlotsPosLis(ingredients.size());
        Vec2 size = getSlotsComposeSize(ingredients.size());
        int x = ingredients.size() == 1 ? 41 : (int) ((26 - size.x / 2) + 11);
        int y = (int) ((26 - size.y / 2) + 11);
        Iterator<EmiIngredient> ingredientIterator = ingredients.iterator();
        for (Vec2 vec2 : posLis) {
            addSimpleSlot(ingredientIterator.next(), (int) (x + vec2.x), (int) (y + vec2.y), widgets);
        }
    }

    protected void addOutputs(List<EmiStack> emiStacks, WidgetHolder widgets) {
        List<Vec2> posLis = getSlotsPosLis(emiStacks.size());
        Vec2 size = getSlotsComposeSize(emiStacks.size());
        int x = emiStacks.size() == 1 ? 179 : (int) ((26 - size.x / 2) + 169);
        int y = (int) ((26 - size.y / 2) + 11);
        Iterator<EmiStack> emiStackIterator = emiStacks.iterator();
        for (Vec2 vec2 : posLis) {
            addSimpleSlot(emiStackIterator.next(), (int) (x + vec2.x), (int) (y + vec2.y), widgets);
        }
    }

    protected void addPlus(WidgetHolder widgets, int x, int y) {
        widgets.addTexture(EMI_GUI_TEXTURES, x, y, 9, 9, 0, 19);
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
}
