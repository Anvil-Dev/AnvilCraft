package dev.dubhe.anvilcraft.integration.emi.recipe;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.data.recipe.anvil.AnvilRecipe;
import dev.dubhe.anvilcraft.data.recipe.anvil.RecipeOutcome;
import dev.dubhe.anvilcraft.data.recipe.anvil.RecipePredicate;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SelectOne;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SetBlock;
import dev.dubhe.anvilcraft.data.recipe.anvil.outcome.SpawnItem;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.HasBlock;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.HasFluidCauldron;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.HasItem;
import dev.dubhe.anvilcraft.data.recipe.anvil.predicate.NotHasBlock;
import dev.dubhe.anvilcraft.integration.emi.stack.BlockStateEmiStack;
import dev.dubhe.anvilcraft.integration.emi.stack.ListBlockStateEmiIngredient;
import dev.dubhe.anvilcraft.integration.emi.stack.SelectOneEmiStack;
import dev.dubhe.anvilcraft.integration.emi.ui.BlockWidget;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.ListEmiIngredient;
import dev.emi.emi.api.stack.TagEmiIngredient;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("SameParameterValue")
public class AnvilProcessEmiRecipe implements EmiRecipe {
    public static final ResourceLocation EMI_GUI_TEXTURES = AnvilCraft.of("textures/gui/container/emi/emi.png");
    protected static final BlockState ANVIL_BLOCK_STATE = Blocks.ANVIL
        .defaultBlockState()
        .setValue(AnvilBlock.FACING, Direction.WEST);
    protected final EmiRecipeCategory category;
    protected final AnvilRecipe recipe;
    protected final List<EmiIngredient> inputs = new ArrayList<>();
    protected final List<EmiStack> outputs = new ArrayList<>();
    protected final NonNullList<BlockStateEmiStack> inputBlocks = NonNullList.withSize(2, BlockStateEmiStack.EMPTY);
    protected final NonNullList<BlockStateEmiStack> outputBlocks = NonNullList.withSize(2, BlockStateEmiStack.EMPTY);
    protected ResourceLocation id;
    protected int width = 246;
    protected int height = 84;

    /**
     * 基础EMI配方
     *
     * @param category 配方类型
     * @param recipe   {@link AnvilRecipe}配方
     */
    @SuppressWarnings({"UnstableApiUsage"})
    public AnvilProcessEmiRecipe(EmiRecipeCategory category, @NotNull AnvilRecipe recipe) {
        this.category = category;
        this.recipe = recipe;
        for (RecipePredicate predicate : recipe.getPredicates()) {
            if (predicate instanceof HasItem item) {
                HasItem.ModItemPredicate matchItem = item.getMatchItem();
                TagKey<Item> tag = matchItem.getTag();
                Integer min = matchItem.getCount().getMin();
                int count = min == null ? 0 : min;
                if (tag != null) {
                    EmiIngredient stack = new TagEmiIngredient(tag, count);
                    this.inputs.add(stack);
                    continue;
                }
                List<EmiStack> items = matchItem.getItems().stream().map(EmiStack::of).toList();
                this.inputs.add(new ListEmiIngredient(items, count));
            } else if (predicate instanceof HasBlock block && !(block instanceof NotHasBlock)) {
                HasBlock.ModBlockPredicate matchBlock = block.getMatchBlock();
                TagKey<Block> tag = matchBlock.getTag();
                Map<String, String> properties = matchBlock.getProperties();
                Set<Block> blockSet = matchBlock.getBlocks();
                if (tag != null) blockSet.addAll(
                    BuiltInRegistries.BLOCK.getOrCreateTag(tag).stream().map(Holder::value).toList()
                );
                List<BlockStateEmiStack> blocks = blockSet.stream()
                    .map(block1 -> {
                        BlockState workBlockState = block1.defaultBlockState();
                        for (BlockState blockState : block1.getStateDefinition().getPossibleStates()) {
                            first:
                            for (Map.Entry<String, String> entry : properties.entrySet()) {
                                for (Map.Entry<Property<?>, Comparable<?>> entry1 : blockState.getValues().entrySet()) {
                                    if (!entry1.getKey().getName().equals(entry.getKey())) continue;
                                    if (!entry1.getValue().toString().equals(entry.getValue())) break first;
                                }
                            }
                            workBlockState = blockState;
                        }
                        return BlockStateEmiStack.of(workBlockState);
                    })
                    .toList();
                ListBlockStateEmiIngredient ingredient = ListBlockStateEmiIngredient.of(blocks);
                if (block.getOffset().equals(new Vec3(0.0d, -1.0d, 0.0d))) {
                    this.inputBlocks.set(0, ingredient.get());
                    continue;
                }
                if (block.getOffset().equals(new Vec3(0.0d, -2.0d, 0.0d))) {
                    this.inputBlocks.set(1, ingredient.get());
                    continue;
                }
                this.inputs.add(ingredient);
            } else if (predicate instanceof HasFluidCauldron cauldron) {
                BlockStateEmiStack ingredient = BlockStateEmiStack.of(cauldron.getMatchBlock().defaultBlockState());
                if (cauldron.getOffset().equals(new Vec3(0.0d, -1.0d, 0.0d))) {
                    this.inputBlocks.set(0, ingredient);
                    continue;
                }
                if (cauldron.getOffset().equals(new Vec3(0.0d, -2.0d, 0.0d))) {
                    this.inputBlocks.set(1, ingredient);
                    continue;
                }
                this.inputs.add(ingredient);
            }
        }
        for (int i = 0; i < this.inputBlocks.size(); i++) {
            if (outputBlocks.get(i).isEmpty()) this.outputBlocks.set(i, this.inputBlocks.get(i));
        }
        for (RecipeOutcome outcome : recipe.getOutcomes()) {
            if (outcome instanceof SpawnItem item) {
                this.outputs.add(EmiStack.of(item.getResult()).setChance((float) item.getChance()));
            }
            if (outcome instanceof SetBlock block) {
                BlockStateEmiStack emiStack = BlockStateEmiStack.of(block.getResult());
                if (block.getOffset().equals(new Vec3(0.0d, -1.0d, 0.0d))) {
                    this.outputBlocks.set(0, emiStack);
                    continue;
                }
                if (block.getOffset().equals(new Vec3(0.0d, -2.0d, 0.0d))) {
                    this.outputBlocks.set(1, emiStack);
                    continue;
                }
                this.outputs.add(emiStack.setChance((float) block.getChance()));
            }
            if (outcome instanceof SelectOne selectOne) {
                this.outputs.add(SelectOneEmiStack.of(selectOne).setChance((float) selectOne.getChance()));
            }
        }
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return this.category;
    }

    @Override
    public @Nullable ResourceLocation getId() {
        return this.recipe.getId();
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return this.inputs;
    }

    @Override
    public List<EmiStack> getOutputs() {
        return this.outputs;
    }

    @Override
    public int getDisplayWidth() {
        return this.width;
    }

    @Override
    public int getDisplayHeight() {
        return this.height;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        this.addStraightArrow(widgets, 120, 41);
        if (!this.inputs.isEmpty()) {
            this.addInputArrow(widgets, 72, 32);
            this.addInputSlots(widgets);
        }
        if (!this.outputs.isEmpty()) {
            this.addOutputArrow(widgets, 163, 30);
            this.addOutputs(widgets);
        }
        widgets.addDrawable(90, 25, 0, 0, new BlockWidget(ANVIL_BLOCK_STATE, 1));
        widgets.addDrawable(90, 25, 0, 0, new BlockWidget(this.inputBlocks.get(0).getState(), -1));
        widgets.addDrawable(90, 25, 0, 0, new BlockWidget(this.outputBlocks.get(1).getState(), -2));
        widgets.addDrawable(135, 25, 0, 0, new BlockWidget(ANVIL_BLOCK_STATE, 0));
        widgets.addDrawable(135, 25, 0, 0, new BlockWidget(this.outputBlocks.get(0).getState(), -1));
        widgets.addDrawable(135, 25, 0, 0, new BlockWidget(this.outputBlocks.get(1).getState(), -2));
    }

    protected void addStraightArrow(@NotNull WidgetHolder widgets, int x, int y) {
        widgets.addTexture(EMI_GUI_TEXTURES, x, y, 12, 11, 0, 19);
    }

    protected void addInputArrow(@NotNull WidgetHolder widgets, int x, int y) {
        widgets.addTexture(EMI_GUI_TEXTURES, x, y, 16, 8, 0, 31);
    }

    protected void addOutputArrow(@NotNull WidgetHolder widgets, int x, int y) {
        widgets.addTexture(EMI_GUI_TEXTURES, x, y, 16, 10, 0, 40);
    }

    protected void addInputSlots(WidgetHolder widgets) {
        List<Vec2> posLis = this.getSlotsPosList(inputs.size());
        Vec2 size = this.getSlotsComposeSize(inputs.size());
        int x = this.inputs.size() == 1 ? 40 : (int) ((26 - size.x / 2) + 10);
        int y = (int) ((26 - size.y / 2) + 15);
        Iterator<EmiIngredient> ingredientIterator = inputs.iterator();
        for (Vec2 vec2 : posLis) {
            addSlot(ingredientIterator.next(), (int) (x + vec2.x), (int) (y + vec2.y), widgets);
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

    protected void addSlot(@NotNull EmiIngredient ingredient, int x, int y, WidgetHolder widgets) {
        if (ingredient.getChance() < 1) {
            addChangeSlot(ingredient, x, y, widgets);
            return;
        }
        addSimpleSlot(ingredient, x, y, widgets);
    }

    protected void addSimpleSlot(EmiIngredient ingredient, int x, int y, @NotNull WidgetHolder widgets) {
        widgets.add(new SlotWidget(ingredient, x, y)
            .customBackground(EMI_GUI_TEXTURES, 0, 0, 18, 18));
    }

    protected void addChangeSlot(EmiIngredient ingredient, int x, int y, @NotNull WidgetHolder widgets) {
        widgets.add(new SlotWidget(ingredient, x, y).recipeContext(this)
            .customBackground(EMI_GUI_TEXTURES, 19, 0, 18, 18));
    }

    protected void addOutputs(WidgetHolder widgets) {
        int outputSize = outputs.size();
        List<Vec2> posLis = getSlotsPosList(outputSize);
        Vec2 composeSize = getSlotsComposeSize(outputSize);
        int x = outputSize == 1 ? 190 : (int) ((26 - composeSize.x / 2) + 190);
        int y = (int) ((26 - composeSize.y / 2) + 15);
        Iterator<EmiStack> emiStackIterator = outputs.iterator();
        for (Vec2 vec2 : posLis) {
            if (emiStackIterator.hasNext()) {
                addSlot(emiStackIterator.next(), (int) (x + vec2.x), (int) (y + vec2.y), widgets);
            }
        }
    }
}
