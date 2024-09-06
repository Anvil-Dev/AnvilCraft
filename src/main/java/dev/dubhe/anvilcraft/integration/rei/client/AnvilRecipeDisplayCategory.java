package dev.dubhe.anvilcraft.integration.rei.client;

import com.google.common.collect.Lists;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.integration.rei.AnvilCraftCategoryIdentifier;
import dev.dubhe.anvilcraft.integration.rei.AnvilCraftEntryIngredient;
import dev.dubhe.anvilcraft.integration.rei.client.widget.BlockWidget;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Slot;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.Widgets;
import me.shedaniel.rei.api.client.registry.display.DisplayCategory;
import me.shedaniel.rei.api.client.util.ClientEntryStacks;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class AnvilRecipeDisplayCategory implements DisplayCategory<AnvilRecipeDisplay> {
    public static final ResourceLocation REI_GUI_TEXTURES = AnvilCraft.of("textures/gui/container/emi/emi.png");
    protected static final BlockState ANVIL_BLOCK_STATE = Blocks.ANVIL
            .defaultBlockState()
            .setValue(AnvilBlock.FACING, Direction.WEST);
    final AnvilCraftCategoryIdentifier anvilCraftCategoryIdentifier;

    public AnvilRecipeDisplayCategory(AnvilCraftCategoryIdentifier anvilCraftCategoryIdentifier) {
        this.anvilCraftCategoryIdentifier = anvilCraftCategoryIdentifier;
    }

    @Override
    public CategoryIdentifier<? extends AnvilRecipeDisplay> getCategoryIdentifier() {
        return anvilCraftCategoryIdentifier.getCategoryIdentifier();
    }

    @Override
    public Component getTitle() {
        return anvilCraftCategoryIdentifier.getTitle();
    }

    @Override
    public Renderer getIcon() {
        return anvilCraftCategoryIdentifier.getIcon();
    }

    @Override
    public int getDisplayHeight() {
        return 84;
    }

    @Override
    public int getDisplayWidth(AnvilRecipeDisplay display) {
        return 266;
    }

    @Override
    public List<Widget> setupDisplay(@NotNull AnvilRecipeDisplay display, @NotNull Rectangle bounds) {
        Point startPoint = new Point(bounds.getCenterX() - 133, bounds.getCenterY() - 42);
        List<Widget> widgets = Lists.newArrayList();
        widgets.add(Widgets.createRecipeBase(bounds));
        this.addStraightArrow(widgets, startPoint.x + 120, startPoint.y + 41);
        if (!display.inputItems.isEmpty()) {
            this.addInputArrow(widgets, startPoint.x + 72, startPoint.y + 32);
            this.addInputSlots(widgets, startPoint, display.inputItems);
        }
        if (!display.outputItems.isEmpty()) {
            this.addOutputArrow(widgets, startPoint.x + 163, startPoint.y + 30);
            this.addOutputs(widgets, startPoint, display.outputItems);
        }
        widgets.add(new BlockWidget(ANVIL_BLOCK_STATE, 1, startPoint.x + 90, startPoint.y + 25));
        widgets.add(new BlockWidget(display.inputBlockStates.get(1), -2, startPoint.x + 90, startPoint.y + 25));
        widgets.add(new BlockWidget(display.inputBlockStates.get(0), -1, startPoint.x + 90, startPoint.y + 25));
        widgets.add(new BlockWidget(ANVIL_BLOCK_STATE, 0, startPoint.x + 135, startPoint.y + 25));
        widgets.add(new BlockWidget(display.outputBlockStates.get(1), -2, startPoint.x + 135, startPoint.y + 25));
        widgets.add(new BlockWidget(display.outputBlockStates.get(0), -1, startPoint.x + 135, startPoint.y + 25));
        return widgets;
    }

    protected void addStraightArrow(List<Widget> widgets, int x, int y) {
        widgets.add(Widgets.createTexturedWidget(REI_GUI_TEXTURES, x, y, 0, 19, 12, 11));
        widgets.add(Widgets.createTexturedWidget(REI_GUI_TEXTURES, x, y, 0, 19, 12, 11));
    }

    protected void addInputArrow(List<Widget> widgets, int x, int y) {
        widgets.add(Widgets.createTexturedWidget(REI_GUI_TEXTURES, x, y, 0, 31, 16, 8));
    }

    protected void addOutputArrow(List<Widget> widgets, int x, int y) {
        widgets.add(Widgets.createTexturedWidget(REI_GUI_TEXTURES, x, y, 0, 40, 16, 10));
    }

    protected void addInputSlots(List<Widget> widgets, Point startPoint, List<EntryIngredient> inputs) {
        List<Vec2> posList = this.getSlotsPosList(inputs.size());
        Vec2 size = this.getSlotsComposeSize(inputs.size());
        int x = inputs.size() == 1 ? startPoint.x + 40 : startPoint.x + (int) ((26 - size.x / 2) + 10);
        int y = startPoint.y + (int) ((26 - size.y / 2) + 15);
        Iterator<EntryIngredient> ingredientIterator = inputs.iterator();
        for (Vec2 vec2 : posList) {
            this.addSlot(ingredientIterator.next(), (int) (x + vec2.x), (int) (y + vec2.y), widgets);
        }
    }

    protected List<Vec2> getSlotsPosList(int length) {
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

    protected void addSlot(EntryIngredient ingredient, int x, int y, List<Widget> widgets) {
        this.addSimpleSlot(ingredient, x, y, widgets);
    }


    protected void addSlot(AnvilCraftEntryIngredient ingredient, int x, int y, List<Widget> widgets) {
        if (ingredient.isSelectOne()) {
            this.addSelectOneSlot(ingredient, x, y, widgets);
            return;
        }
        if (ingredient.getChance() < 1) {
            this.addChanceSlot(ingredient, x, y, widgets);
            return;
        }
        this.addSimpleSlot(ingredient.getEntryIngredient(), x, y, widgets);
    }

    protected void addSimpleSlot(EntryIngredient ingredient, int x, int y, List<Widget> widgets) {
        widgets.add(Widgets.createTexturedWidget(REI_GUI_TEXTURES, x, y, 0, 0, 18, 18));
        widgets.add(Widgets.createSlot(new Point(x + 1, y + 1))
                .entries(ingredient)
                .disableBackground());
    }

    protected void addChanceSlot(AnvilCraftEntryIngredient ingredient, int x, int y, List<Widget> widgets) {
        widgets.add(Widgets.createTexturedWidget(REI_GUI_TEXTURES, x, y, 19, 0, 18, 18));
        Slot chanceSlot = Widgets.createSlot(new Point(x + 1, y + 1))
                .entries(ingredient.getEntryIngredient())
                .disableBackground();
        ClientEntryStacks.setTooltipProcessor(chanceSlot.getCurrentEntry(), (entryStack, tooltip) -> {
            float chance = ingredient.getChance();
            if (chance != 1)
                tooltip.add(Component.translatable("tooltip.anvilcraft.item.recipe.processing.chance",
                                chance < 0.01 ? "<1" : (int) (chance * 100))
                        .withStyle(ChatFormatting.GOLD));
            return tooltip;
        });
        widgets.add(chanceSlot);
    }

    protected void addSelectOneSlot(AnvilCraftEntryIngredient ingredient, int x, int y, List<Widget> widgets) {
        widgets.add(Widgets.createTexturedWidget(REI_GUI_TEXTURES, x, y, 38, 0, 18, 18));
        Slot selectOneSlot = Widgets.createSlot(new Point(x + 1, y + 1))
                .entries(ingredient.getEntryIngredient())
                .disableBackground();
        ClientEntryStacks.setTooltipProcessor(selectOneSlot.getCurrentEntry(), (entryStack, tooltip) -> {
            if (!ingredient.selectOneChanceMap.containsKey(entryStack)) return tooltip;
            float chance = ingredient.selectOneChanceMap.get(entryStack);
            if (chance != 1)
                tooltip.add(Component.translatable("tooltip.anvilcraft.item.recipe.processing.chance",
                                chance < 0.01 ? "<1" : (int) (chance * 100))
                        .withStyle(ChatFormatting.GOLD));
            return tooltip;
        });
        widgets.add(selectOneSlot);
    }

    protected void addOutputs(List<Widget> widgets, Point startPoint, List<AnvilCraftEntryIngredient> outputs) {
        int outputSize = outputs.size();
        List<Vec2> posList = this.getSlotsPosList(outputSize);
        Vec2 composeSize = this.getSlotsComposeSize(outputSize);
        int x = outputSize == 1 ? startPoint.x + 190 : startPoint.x + (int) ((26 - composeSize.x / 2) + 190);
        int y = startPoint.y + (int) ((26 - composeSize.y / 2) + 15);
        Iterator<AnvilCraftEntryIngredient> entryIngredientIterator = outputs.iterator();
        for (Vec2 vec2 : posList) {
            if (entryIngredientIterator.hasNext()) {
                this.addSlot(entryIngredientIterator.next(), (int) (x + vec2.x), (int) (y + vec2.y), widgets);
            }
        }
    }
}
