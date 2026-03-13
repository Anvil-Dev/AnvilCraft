package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.anvilcraft.lib.recipe.component.BlockStatePredicate;
import dev.anvilcraft.lib.recipe.component.ChanceBlockState;
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
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.builder.IIngredientAcceptor;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 一个抽象类，用来"方便"地写JEI兼容(悲)
 *
 * @apiNote 传入的配方必须继承 {@link AbstractProcessRecipe} 才能继承这个类
 */
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
    public void setRecipe(
        IRecipeLayoutBuilder builder,
        RecipeHolder<T> recipeHolder,
        IFocusGroup focuses
    ) {
        T recipe = recipeHolder.value();

        // Item
        if (!recipe.getInputItems().isEmpty()) JeiSlotUtil.addInputSlots(builder, recipe.getInputItems());
        if (!recipe.getResultItems().isEmpty()) JeiSlotUtil.addOutputSlots(builder, recipe.getResultItems());

        IIngredientAcceptor<?> inputAcceptor = builder.addInvisibleIngredients(RecipeIngredientRole.INPUT);
        IIngredientAcceptor<?> outputAcceptor = builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT);

        // Block 我觉得这个括号很神圣
        List<BlockStatePredicate> inputBlocks = recipe.getInputBlocks();
        if (!inputBlocks.isEmpty()) {
            inputBlocks.forEach(blockStatePredicate -> {
                blockStatePredicate.getBlocks().forEach(blockHolder -> {
                    inputAcceptor.addItemLike(blockHolder.value().asItem());
                });
            });
        }
        List<ChanceBlockState> outputBlocks = recipe.getResultBlocks();
        if (!outputBlocks.isEmpty()) {
            outputBlocks.forEach(chanceBlockState -> outputAcceptor.addItemLike(chanceBlockState.state().getBlock().asItem()));
        }

        // Fluid idea会对这个null产生unhappy.png
        HasCauldronSimple cauldronSimple = recipe.getHasCauldron();
        if (cauldronSimple != null) {
            {
                ResourceLocation rl = cauldronSimple.fluid();
                addAcceptorFromRL(inputAcceptor, rl);
            }

            {
                ResourceLocation rl = cauldronSimple.transform();
                addAcceptorFromRL(outputAcceptor, rl);
            }
        }
    }

    // 如果从rl找不到对应的流体，就尝试寻找方块和物品
    // 但是仍对细雪桶/蜂蜜瓶等物品(方块)无效，特判也懒得搞了
    private void addAcceptorFromRL(
        IIngredientAcceptor<?> inputAcceptor,
        ResourceLocation rl
    ) {
        Fluid fluid = BuiltInRegistries.FLUID.get(rl);
        if (fluid != null) {
            inputAcceptor.addFluidStack(fluid);
        } else {
            Block block = BuiltInRegistries.BLOCK.get(rl);
            if (block != null) {
                inputAcceptor.addItemLike(block.asItem());
            } else {
                Item item = BuiltInRegistries.ITEM.get(rl);
                if (item != null) {
                    inputAcceptor.addItemLike(item);
                }
            }
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
        remains.object2IntEntrySet()
            .forEach(entry -> results.add(ChanceItemStack.of(new ItemStack(entry.getKey(), entry.getIntValue()), 1)));
        return results;
    }

    /**
     * 创建配方所需的工作方块列表
     * 一般顺序: 铁砧，输入的炼药锅(如果有)，输入的方块(如果有)
     *
     * @param recipe 要处理的配方对象
     * @return 包含所有工作方块的 BlockStatePredicate 列表，包含铁砧
     */
    protected List<BlockStatePredicate> getWorkingBlocks(
        T recipe
    ) {
        List<BlockStatePredicate> list = new ArrayList<>();
        list.add(BlockStatePredicate.builder().of(Blocks.ANVIL).build());

        // 如果配方包含炼药锅，添加对应的装满的炼药锅状态
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

        // 添加配方的其他输入方块到工作列表
        List<BlockStatePredicate> inputBlocks = recipe.getInputBlocks();
        if (!inputBlocks.isEmpty()) {
            list.addAll(inputBlocks);
        }

        return list;
    }

    /**
     * 渲染配方中的工作方块，可配合{@link AbstractProgressCategory#getWorkingBlocks(AbstractProcessRecipe)}使用
     *
     * @param guiGraphics 指定的GuiGraphics
     * @param list        指定的工作方块列表
     * @apiNote 如果传入的工作方块列表的首位是铁砧，会在渲染时给这个铁砧{@code anvilYOffset}，让其上下移动
     */
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

            boolean isFirstAnvil = list.getFirst()
                .getBlocks()
                .stream()
                .anyMatch(blockHolder -> blockHolder.is(Blocks.ANVIL.defaultBlockState().getBlockHolder()));

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

    /**
     * 渲染自带的几套箭头
     *
     * @param guiGraphics 指定的GuiGraphics
     * @param arrowType 指定的{@link RenderArrowType}
     */
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
        ALL_DEFAULT, IO_PUT, IDEFAULT_OBELOW, IPUT_OBELOW
    }
}
