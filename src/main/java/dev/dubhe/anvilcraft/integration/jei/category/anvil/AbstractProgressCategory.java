package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.anvilcraft.lib.recipe.component.BlockStatePredicate;
import dev.anvilcraft.lib.recipe.component.ChanceItemStack;
import dev.anvilcraft.lib.recipe.component.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.AbstractProcessRecipe;
import dev.dubhe.anvilcraft.recipe.component.HasCauldronSimple;
import dev.dubhe.anvilcraft.util.CauldronUtil;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import lombok.Getter;
import lombok.Setter;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractProgressCategory<T extends AbstractProcessRecipe<?>> implements IRecipeCategory<RecipeHolder<T>> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;

    protected final IDrawable icon;
    protected final IDrawable slotDefault;
    protected final IDrawable slotProbability;
    protected final Component title;
    protected final ITickTimer timer;

    protected final IDrawable arrowIn;
    protected final IDrawable arrowOut;
    protected final IDrawable arrowDefault;
    protected final IDrawable arrowOutputFromBelow;
    @Getter
    @Setter
    protected List<BlockState> workingBlocks = new ArrayList<>();
    @Setter
    @Getter
    protected Map<ResourceLocation, List<BlockStatePredicate>> testBlockMap = new HashMap<>();

    public AbstractProgressCategory(IGuiHelper helper, IDrawable icon, Component title) {
        this.icon = icon;
        this.slotDefault = JeiRenderHelper.getSlotDefault(helper);
        this.slotProbability = JeiRenderHelper.getSlotProbability(helper);
        this.title = title;
        this.timer = helper.createTickTimer(30, 60, true);

        this.arrowIn = JeiRenderHelper.getArrowInput(helper);
        this.arrowOut = JeiRenderHelper.getArrowOutput(helper);
        this.arrowDefault = JeiRenderHelper.getArrowDefault(helper);
        this.arrowOutputFromBelow = JeiRenderHelper.getArrowOutputFromBelow(helper);
    }

    @Override
    public Component getTitle() {
        return this.title;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<T> recipeHolder, IFocusGroup focuses) {
        T recipe = recipeHolder.value();
        if (!recipe.getInputItems().isEmpty()) JeiSlotUtil.addInputSlots(builder, recipe.getInputItems());
        if (!recipe.getResultItems().isEmpty()) JeiSlotUtil.addOutputSlots(builder, recipe.getResultItems());

        // noway
        if (!recipe.getInputBlocks().isEmpty() && recipe.getHasCauldron() != null) {
            List<BlockStatePredicate> list = new ArrayList<>();
            list.add(BlockStatePredicate.builder().of(Blocks.ANVIL).build());
            list.add(BlockStatePredicate.builder().of(recipe.getHasCauldron().getFluidCauldron()).build());
            list.addAll(recipe.getInputBlocks());

            ResourceLocation recipeID = recipeHolder.id();
            Map<ResourceLocation, List<BlockStatePredicate>> map = new HashMap<>();
            map.put(recipeID, list);
            this.testBlockMap = map;
        }
    }

    protected List<ChanceItemStack> getResults(T recipe) {
        List<ChanceItemStack> results = new ArrayList<>(recipe.getResultItems());
        Object2IntMap<Item> remains = new Object2IntArrayMap<>();
        for (ItemIngredientPredicate ingredient : recipe.getInputItems()) {
            for (ItemStack stack : ingredient.getItems()) {
                if (stack.hasCraftingRemainingItem()) {
                    ItemStack remain = stack.getCraftingRemainingItem();
                    remains.mergeInt(remain.getItem(), remain.getCount(), Integer::sum);
                }
            }
        }
        remains.object2IntEntrySet().forEach(entry ->
            results.add(ChanceItemStack.of(new ItemStack(entry.getKey(), entry.getIntValue()), 1))
        );
        return results;
    }

    protected List<BlockStatePredicate> createCommonWorkingList(
        T recipe
    ) {
        List<BlockStatePredicate> list = new ArrayList<>();
        list.add(BlockStatePredicate.builder().of(Blocks.ANVIL).build());

        HasCauldronSimple cauldronSimple = recipe.getHasCauldron();
        if (cauldronSimple != null) {
            Block cauldron = cauldronSimple.getFluidCauldron();
            BlockState cauldronState = CauldronUtil.fullState(cauldron);
            if (cauldronState.hasProperty(CauldronUtil.LEVEL_3)) {
                list.add(BlockStatePredicate.builder().of(cauldron).with(CauldronUtil.LEVEL_3, 3).build());
            } else if (cauldronState.hasProperty(CauldronUtil.LEVEL_4)) {
                list.add(BlockStatePredicate.builder().of(cauldron).with(CauldronUtil.LEVEL_4, 4).build());
            }
        }

        List<BlockStatePredicate> inputBlocks = recipe.getInputBlocks();
        if (!inputBlocks.isEmpty()) {
            list.addAll(inputBlocks);
        }

        return list;
    }

    protected void renderWorkingBlocks(
        GuiGraphics guiGraphics,
        List<BlockStatePredicate> list
    ) {
        if (!list.isEmpty()) {
            int size = list.size();

            float x = (float) getWidth() / 2;
            float y = (float) getHeight() / 2;
            float z = 10;

            float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(timer);

            boolean isFirstAnvil = list.getFirst().getBlocks().stream().anyMatch(
                blockHolder -> blockHolder.is(Blocks.ANVIL.defaultBlockState().getBlockHolder())
            );

            for (int i = 0; i < size; i++) {
                float addYOffset;
                float addZOffset;
                float extraAnvilOffset = 0;

                if (size == 1) {
                    addYOffset = 0;
                    addZOffset = 0;
                } else if (size == 2) {
                    if (i == 0) {
                        addYOffset = -5;
                        addZOffset = 10;
                    } else {
                        addYOffset = 5;
                        addZOffset = 0;
                    }
                } else {
                    addYOffset = (i - 1) * 10;
                    addZOffset = -(i - 1) * 10;
                }

                if (isFirstAnvil && i == 0) {
                    extraAnvilOffset = -8 + anvilYOffset;
                }

                List<BlockState> input = list.get(i).constructStatesForRender();
                BlockState renderedState = input.get((int) ((System.currentTimeMillis() / 1000) % input.size()));

                RenderSupport.renderBlock(
                    guiGraphics,
                    renderedState,
                    x,
                    y + addYOffset + extraAnvilOffset,
                    z + addZOffset,
                    12,
                    RenderSupport.SINGLE_BLOCK
                );
            }
        }
    }

    // 渲染一般配方页面中输入输出箭头
    public void renderArrow(
        GuiGraphics guiGraphics,
        RenderArrowType arrowType
    ) {
        switch (arrowType) {
            case ALL_DEFAULT -> {
                arrowDefault.draw(guiGraphics, 54, 20);
                arrowDefault.draw(guiGraphics, 92, 20);
            }

            case IO_PUT -> {
                arrowIn.draw(guiGraphics, 54, 20);
                arrowOut.draw(guiGraphics, 92, 19);
            }

            case IDEFAULT_OBELOW -> {
                arrowDefault.draw(guiGraphics, 54, 20);
                arrowOutputFromBelow.draw(guiGraphics, 86, 30);
            }

            case IPUT_OBELOW -> {
                arrowIn.draw(guiGraphics, 54, 20);
                arrowOutputFromBelow.draw(guiGraphics, 86, 30);
            }

            default -> throw new IllegalStateException("Unexpected value: " + arrowType);
        }
    }

    public enum RenderArrowType {
        ALL_DEFAULT,
        IO_PUT,
        IDEFAULT_OBELOW,
        IPUT_OBELOW
    }
}
