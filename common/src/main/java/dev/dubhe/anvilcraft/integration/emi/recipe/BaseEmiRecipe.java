package dev.dubhe.anvilcraft.integration.emi.recipe;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SpawnItem;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.HasItem;
import dev.dubhe.anvilcraft.integration.emi.ui.BlockWidget;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import dev.emi.emi.registry.EmiTags;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;


@Getter
public abstract class BaseEmiRecipe implements EmiRecipe {
    public final ResourceLocation EMI_GUI_TEXTURES = AnvilCraft.of("textures/gui/container/emi/emi.png");

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

    protected void addSimpleSlot(WidgetHolder widgets, int x, int y) {
        widgets.addTexture(EMI_GUI_TEXTURES, x, y, 18, 18, 0, 0);
    }

    protected void addPlus(WidgetHolder widgets, int x, int y) {
        widgets.addTexture(EMI_GUI_TEXTURES, x, y, 9, 9, 0, 19);
    }
}
