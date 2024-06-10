package dev.dubhe.anvilcraft.integration.emi.recipe;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.GeneratedSlotWidget;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.WidgetHolder;

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
    public static final ResourceLocation EMI_GUI_TEXTURES = AnvilCraft.of("textures/gui/container/emi/emi.png");
    protected static final BlockState ANVIL_BLOCK_STATE = Blocks.ANVIL.defaultBlockState().setValue(
            AnvilBlock.FACING, Direction.WEST);
    protected EmiRecipeCategory category;
    protected final AnvilRecipe recipe;
    protected List<EmiIngredient> inputs = new ArrayList<>();
    protected ArrayList<EmiStack> blockOutputs =  new ArrayList<>();
    protected ArrayList<EmiStack> simpleOutputs =  new ArrayList<>();
    protected ArrayList<List<EmiStack>> selectOneItemList = new ArrayList<>();
    protected ResourceLocation id;
    protected int width;
    protected int height;

    /**
     * 基础EMI配方
     *
     * @param category 配方类型
     * @param recipe {@link AnvilRecipe}配方
     */
    public BaseEmiRecipe(EmiRecipeCategory category, AnvilRecipe recipe, int width, int height) {
        this.category = category;
        this.recipe = recipe;
        this.id = recipe.getId();
        this.width = width;
        this.height = height;
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

    protected void addInputArrow(WidgetHolder widgets, int x, int y) {
        widgets.addTexture(EMI_GUI_TEXTURES, x, y, 16, 8, 0, 31);
    }

    protected void addOutputArrow(WidgetHolder widgets, int x, int y) {
        widgets.addTexture(EMI_GUI_TEXTURES, x, y, 16, 10, 0, 40);
    }

    protected void addStraightArrow(WidgetHolder widgets, int x, int y) {
        widgets.addTexture(EMI_GUI_TEXTURES, x, y, 12, 11, 0, 19);
    }

    protected void addChangeSlot(EmiIngredient ingredient, int x, int y, WidgetHolder widgets) {
        widgets.add(new SlotWidget(ingredient, x, y).recipeContext(this)
                .customBackground(EMI_GUI_TEXTURES, 19, 0, 18, 18));
    }

    protected void addSelectOneSlot(List<EmiStack> emiStacks, int x, int y, WidgetHolder widgets) {
        widgets.add(new GeneratedSlotWidget(random -> emiStacks.get(random.nextInt(emiStacks.size())), 1, x, y));
        widgets.addTexture(EMI_GUI_TEXTURES, x, y, 18, 18, 38, 0);
    }

    protected void addSimpleSlot(EmiIngredient ingredient, int x, int y, WidgetHolder widgets) {
        widgets.add(new SlotWidget(ingredient, x, y)
                .customBackground(EMI_GUI_TEXTURES, 0, 0, 18, 18));
    }

    protected void addSlot(EmiIngredient ingredient, int x, int y, WidgetHolder widgets) {
        if (ingredient.getChance() < 1) {
            addChangeSlot(ingredient, x, y, widgets);
            return;
        }
        addSimpleSlot(ingredient, x, y, widgets);
    }

    @Override
    public List<EmiStack> getOutputs() {
        ArrayList<EmiStack> list = new ArrayList<>(getItemOutputs());
        list.addAll(blockOutputs);
        return list;
    }

    /**
     * 获取物品输出
     */
    public List<EmiStack> getItemOutputs() {
        ArrayList<EmiStack> list = new ArrayList<>();
        selectOneItemList.forEach(list::addAll);
        list.addAll(simpleOutputs);
        return list;
    }
}
