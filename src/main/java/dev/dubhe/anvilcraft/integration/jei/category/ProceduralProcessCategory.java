package dev.dubhe.anvilcraft.integration.jei.category;

import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.block.WipBlock;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.util.JeiBlockIngredientUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.recipe.anvil.procedural.ProceduralProcessRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.procedural.ProceduralProcessStep;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.AbstractProcessRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProceduralProcessCategory implements IRecipeCategory<RecipeHolder<ProceduralProcessRecipe>> {
    private static final String INITIAL_BLOCK = "initial_block";
    public static final int WIDTH = 162;
    /** 步骤图区域的高度，至少容纳到流程行（长箭头与循环图标）的底边 */
    public static final int CONTENT_HEIGHT = 82;
    /** 材料清单相对步骤图底边上移的像素数。标签在流程行左侧、槽位在流程行下方，上移后仍不相撞。 */
    public static final int MATERIALS_RAISE = 10;
    public static final int MATERIALS_LABEL_Y = CONTENT_HEIGHT - MATERIALS_RAISE;
    public static final int MATERIALS_Y = MATERIALS_LABEL_Y + 12;
    /** 材料清单单行最多展示的材料种类数 */
    public static final int MATERIALS_SLOTS = 9;
    public static final int HEIGHT = MATERIALS_Y + 18 + 4;

    public static final int STEPS_LENGTH = 120;
    public static final int STEP_X = (WIDTH - STEPS_LENGTH) / 2 + 10;
    public static final int STEP_LENGTH = 20;

    /** 铁砧顶端。整体下移，使铁砧贴图完整落在类目内，不再溢到标题区白占高度。 */
    public static final int ANVIL_Y = 10;
    /**
     * 物品槽顶端，紧贴铁砧下方。
     *
     * <p>物品槽与主线上方那行必然互斥：只有物品注入配方带物品输入，而它的输入方块恒为一个、
     * 主体就落在主线上，会占上方那行的是方块压缩与方块处理（其配方不含物品）。因此两者可以
     * 重叠，无需为其预留独立高度。</p>
     */
    public static final int ITEM_Y = 23;
    /** 显示主线：主体方块固定画在这一行，上方一行留「从上面砸进去」的方块，下方一行留垫底方块。 */
    public static final int BLOCK_Y = 48;
    /** 相邻输入方块之间的行距。 */
    public static final int BLOCK_ROW_DY = 10;
    public static final int FLOW_Y = 69;
    private static final long LOOP_CYCLE_MILLIS = 1500L;
    private static final int CYCLE_SIZE = 16;

    private final IDrawable slotDefault;
    private final IDrawable cycle;
    private final IDrawable arrowLong;
    private final IDrawable icon;
    private final Component title;

    public ProceduralProcessCategory(IGuiHelper helper) {
        this.slotDefault = JeiRenderHelper.getSlotDefault(helper);
        this.cycle = JeiRenderHelper.getCycle(helper);
        this.arrowLong = JeiRenderHelper.getArrowLong(helper);
        this.icon = helper.createDrawableItemLike(Blocks.ANVIL);
        this.title = Component.translatable("gui.anvilcraft.category.procedural_process");
    }

    @Override
    public RecipeType<RecipeHolder<ProceduralProcessRecipe>> getRecipeType() {
        return AnvilCraftJeiPlugin.PROCEDURAL_PROCESS;
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<ProceduralProcessRecipe> recipeHolder, IFocusGroup focuses) {
        ProceduralProcessRecipe recipe = recipeHolder.value();

        // input
        JeiBlockIngredientUtil.addInputSlot(
            builder, INITIAL_BLOCK, STEP_X - 29, BLOCK_Y - 6, 18, 18, recipe.getInitialBlock()
        );

        // step
        int size = Math.clamp(recipe.getSteps().size(), 1, 5);
        int gap = STEPS_LENGTH / size - STEP_LENGTH;
        int stepX = STEP_X + gap / 2;
        int stepDx = STEP_LENGTH + gap;

        for (int i = 0; i < size; i++) {
            ProceduralProcessStep step = recipe.getSteps().get(i);
            if (!(step.getContent() instanceof AbstractProcessRecipe<?> stepRecipe)) continue;

            if (!stepRecipe.getInputItems().isEmpty()) {
                ItemIngredientPredicate ingredient = stepRecipe.getInputItems().getFirst();
                IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.INPUT, stepX + i * stepDx - 8, ITEM_Y + 1);
                slot.addIngredients(Ingredient.of(ingredient.getItems()));
            }

            // 槽位跟随主线之外的偏移，与 draw 中的方块渲染保持一致
            int anchor = anchorIndex(stepRecipe);
            for (int j = 0; j < stepRecipe.getInputBlocks().size(); j++) {
                int blockY = BLOCK_Y + BLOCK_ROW_DY * (j - anchor);
                int y = blockY + (j == 0 ? -6 : 2);
                int height = j == 0 ? 18 : 10;
                JeiBlockIngredientUtil.addInputSlot(
                    builder,
                    stepBlockSlotName(i, j),
                    stepX + i * stepDx - 9,
                    y,
                    18,
                    height,
                    stepRecipe.getInputBlocks().get(j)
                );
            }
        }

        // output
        JeiBlockIngredientUtil.addSlot(
            builder,
            RecipeIngredientRole.OUTPUT,
            "output_block",
            STEP_X + STEPS_LENGTH - 9,
            BLOCK_Y - 6,
            18,
            18,
            recipe.getResultBlock().state().getBlock()
        );

        // 材料清单：整个加工流程实际消耗的材料
        List<ItemStack> materials = collectRequiredMaterials(recipe);
        int materialCount = Math.min(materials.size(), MATERIALS_SLOTS);
        for (int i = 0; i < materialCount; i++) {
            builder.addSlot(RecipeIngredientRole.INPUT, i * 18 + 1, MATERIALS_Y + 1)
                .addItemStack(materials.get(i));
        }
    }

    @Override
    public void createRecipeExtras(
        IRecipeExtrasBuilder builder, RecipeHolder<ProceduralProcessRecipe> recipeHolder, IFocusGroup focuses) {
        JeiBlockIngredientUtil.suppressHoverOverlays(builder);
    }

    @Override
    public void draw(
        RecipeHolder<ProceduralProcessRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY
    ) {
        ProceduralProcessRecipe recipe = recipeHolder.value();

        // input
        List<BlockState> initialStates = recipe.getInitialBlock().constructStatesForRender();
        JeiBlockIngredientUtil.getDisplayedState(recipeSlotsView, INITIAL_BLOCK, initialStates).ifPresent(blockState ->
            RenderSupport.renderBlock(
                guiGraphics, blockState, STEP_X - 20, BLOCK_Y, 10, 12, RenderSupport.SINGLE_BLOCK
            )
        );


        // step
        int size = Math.clamp(recipe.getSteps().size(), 1, 5);
        int gap = STEPS_LENGTH / size - STEP_LENGTH;
        int stepX = STEP_X + gap / 2;
        int stepDx = STEP_LENGTH + gap;
        int displayedLoop = getDisplayedLoop(recipe);

        for (int i = 0; i < size; i++) {
            ProceduralProcessStep step = getDisplayedStep(recipe, i, displayedLoop);
            if (!(step.getContent() instanceof AbstractProcessRecipe<?> stepRecipe)) continue;

            // anvil
            RenderSupport.renderBlock(
                guiGraphics,
                Blocks.ANVIL.defaultBlockState(),
                stepX + i * stepDx,
                ANVIL_Y,
                20,
                12,
                RenderSupport.SINGLE_BLOCK
            );

            // item

            if (!stepRecipe.getInputItems().isEmpty()) {
                this.slotDefault.draw(guiGraphics, stepX + i * stepDx - 9, ITEM_Y);
            }

            // block
            // 主体（产出方块落点所在的那个输入）固定占主线行，其余输入按与它的世界上下
            // 关系向上/下偏移，使各步的主线始终对齐。
            int anchor = anchorIndex(stepRecipe);
            for (int j = stepRecipe.getInputBlocks().size() - 1; j >= 0; j--) {
                List<BlockState> input = stepRecipe.getInputBlocks().get(j).constructStatesForRender();
                if (input.isEmpty()) continue;
                BlockState renderedState = JeiBlockIngredientUtil
                    .getDisplayedState(recipeSlotsView, stepBlockSlotName(i, j), input)
                    .orElse(input.getFirst());
                int blockY = BLOCK_Y + BLOCK_ROW_DY * (j - anchor);
                if (renderedState.getBlock() instanceof WipBlock) {
                    RenderSupport.renderBlock(
                        guiGraphics,
                        renderedState,
                        stepX + i * stepDx,
                        blockY,
                        10 - 10 * j,
                        12,
                        RenderSupport.wipDisplay(recipe, displayedLoop * recipe.getSteps().size() + i)
                    );
                }
                RenderSupport.renderBlock(
                    guiGraphics,
                    renderedState,
                    stepX + i * stepDx,
                    blockY,
                    10 - 10 * j,
                    12,
                    RenderSupport.SINGLE_BLOCK
                );
            }
        }

        // loop
        if (recipe.getLoop() > 1) {
            this.cycle.draw(guiGraphics, WIDTH / 2 + 45, FLOW_Y);
            drawLoopCounter(guiGraphics, displayedLoop + 1, recipe.getLoop());
        }
        this.arrowLong.draw(guiGraphics, WIDTH / 2 - 32, FLOW_Y + 4);

        // result
        RenderSupport.renderBlock(
            guiGraphics, recipe.getResultBlock().state(), STEP_X + STEPS_LENGTH, BLOCK_Y, 0, 12, RenderSupport.SINGLE_BLOCK
        );

        // 材料清单：标签 + 槽位底板
        var font = Minecraft.getInstance().font;
        guiGraphics.drawString(
            font,
            Component.translatable("gui.anvilcraft.category.procedural_process.materials"),
            1,
            MATERIALS_LABEL_Y + 2,
            0xFF404040,
            false
        );
        List<ItemStack> materials = collectRequiredMaterials(recipe);
        int materialCount = Math.min(materials.size(), MATERIALS_SLOTS);
        for (int i = 0; i < materialCount; i++) {
            this.slotDefault.draw(guiGraphics, i * 18, MATERIALS_Y);
        }
        // 材料种类超出展示上限时提示，避免数据包配方被无声截断
        if (materials.size() > materialCount) {
            Component overflow = Component.translatable(
                "gui.anvilcraft.category.procedural_process.materials.overflow",
                materials.size()
            );
            guiGraphics.drawString(
                font,
                overflow,
                WIDTH - font.width(overflow) - 1,
                MATERIALS_LABEL_Y + 2,
                0xFFAA0000,
                false
            );
        }
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        RecipeHolder<ProceduralProcessRecipe> recipe,
        IRecipeSlotsView recipeSlotsView,
        double mouseX,
        double mouseY
    ) {
        IRecipeCategory.super.getTooltip(tooltip, recipe, recipeSlotsView, mouseX, mouseY);
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.PROCEDURAL_PROCESS,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.PROCEDURAL_PROCESS.get())
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilProcessingCatalysts(registration, AnvilCraftJeiPlugin.PROCEDURAL_PROCESS);
    }

    private static String stepBlockSlotName(int step, int block) {
        return "step_" + step + "_block_" + block;
    }

    /**
     * 该步骤的显示主线落在第几个输入方块上。
     *
     * <p>主线即该步骤正在被加工的主体，也就是产出方块落点所在的那格输入：下标 {@code i} 的
     * 输入位于 {@code blockInputOffset} 往下 {@code i} 格，产出位于 {@code blockOutputOffset}，
     * 两者重合的那个下标就是主体。方块压缩把两块压成一块、产物落在再下一格（偏移 -2），
     * 主体是下面那块、上面那块是从上面砸进去的；方块处理与物品注入原地成型（偏移 -1），
     * 主体就是铁砧正下方那块。主体固定在 {@link #BLOCK_Y} 上渲染，其余输入按它在世界里的
     * 相对高度向上或向下偏移，避免主体在各步之间上下跳动。</p>
     */
    private static int anchorIndex(AbstractProcessRecipe<?> stepRecipe) {
        double inputY = stepRecipe.getProperty().getBlockInputOffset().y;
        double outputY = stepRecipe.getProperty().getBlockOutputOffset().y;
        int index = (int) Math.round(inputY - outputY);
        return Math.clamp(index, 0, Math.max(stepRecipe.getInputBlocks().size() - 1, 0));
    }

    private static int getDisplayedLoop(ProceduralProcessRecipe recipe) {
        if (recipe.getLoop() <= 1) return 0;
        return (int) ((Util.getMillis() / LOOP_CYCLE_MILLIS) % recipe.getLoop());
    }

    private static ProceduralProcessStep getDisplayedStep(
        ProceduralProcessRecipe recipe,
        int stepIndex,
        int displayedLoop
    ) {
        if (stepIndex == 0 && displayedLoop > 0) {
            return recipe.getMultiLoopFirstStep().orElse(recipe.getSteps().getFirst());
        }
        return recipe.getSteps().get(stepIndex);
    }

    private static void drawLoopCounter(GuiGraphics guiGraphics, int currentLoop, int loopCount) {
        Component text = Component.literal(currentLoop + "/" + loopCount);
        var font = Minecraft.getInstance().font;
        int textX = WIDTH - font.width(text);
        int textY = FLOW_Y + (CYCLE_SIZE - font.lineHeight) / 2 + 2;
        guiGraphics.drawString(font, text, textX, textY, 0xFFFFFFFF, true);
    }

    // region 材料清单

    /**
     * 统计完成整个序列加工所需的全部材料（方块与物品），按配方实际执行顺序展开所有循环。
     *
     * <p>只计入真正会被消耗的材料，并排除两类非材料方块：
     * <ul>
     *   <li>WIP 方块是加工过程的中间态，由机器自身产生，不需要准备；</li>
     *   <li>加热器、中子辐照器等作为反应条件的方块不会被消耗。</li>
     * </ul>
     * 物品输入（如末影珍珠、下界残骸）会被消耗，按其谓词标注的数量计入。
     * 同一材料出现多次时合并计数，保持首次出现的顺序。
     */
    private static List<ItemStack> collectRequiredMaterials(ProceduralProcessRecipe recipe) {
        Map<Item, Integer> counts = new LinkedHashMap<>();
        int loop = Math.max(recipe.getLoop(), 1);
        for (int currentLoop = 0; currentLoop < loop; currentLoop++) {
            for (int stepIndex = 0; stepIndex < recipe.getSteps().size(); stepIndex++) {
                ProceduralProcessStep step = getExecutedStep(recipe, stepIndex, currentLoop);
                if (!(step.getContent() instanceof AbstractProcessRecipe<?> stepRecipe)) continue;
                for (BlockStatePredicate block : consumedBlocks(stepRecipe)) {
                    addBlock(counts, block);
                }
                for (ItemIngredientPredicate item : stepRecipe.getInputItems()) {
                    addItem(counts, item);
                }
            }
        }
        // 初始方块由玩家放置并被第一步加工，正常情况下已经计入；
        // 若某个配方没有把它写进步骤输入，这里补上以免遗漏。
        addInitialBlockIfMissing(counts, recipe.getInitialBlock());
        return counts.entrySet().stream()
            .map(entry -> new ItemStack(entry.getKey(), entry.getValue()))
            .toList();
    }

    /**
     * 取出该步骤中会被消耗的方块谓词。
     *
     * <p>{@code consumeInputBlocks} 为真时（如方块压缩）全部输入方块都会被消耗；
     * 为假时（如方块处理、物品注入）只有首个输入是被加工的主体，其余是同处的
     * 加热器、辐照器等反应条件。
     */
    private static List<BlockStatePredicate> consumedBlocks(AbstractProcessRecipe<?> recipe) {
        List<BlockStatePredicate> inputs = recipe.getInputBlocks();
        if (inputs.isEmpty()) return List.of();
        if (recipe.getProperty().isConsumeInputBlocks()) return inputs;
        return List.of(inputs.getFirst());
    }

    private static void addInitialBlockIfMissing(Map<Item, Integer> counts, BlockStatePredicate initialBlock) {
        Item item = firstMaterialItem(initialBlock);
        if (item != null && !counts.containsKey(item)) {
            counts.put(item, 1);
        }
    }

    private static void addBlock(Map<Item, Integer> counts, BlockStatePredicate predicate) {
        Item item = firstMaterialItem(predicate);
        if (item != null) {
            counts.merge(item, 1, Integer::sum);
        }
    }

    /** 物品输入会被消耗，数量取自谓词的 {@code count}。 */
    private static void addItem(Map<Item, Integer> counts, ItemIngredientPredicate predicate) {
        ItemStack[] items = predicate.getItems();
        if (items.length == 0 || items[0].isEmpty()) return;
        counts.merge(items[0].getItem(), Math.max(predicate.count(), 1), Integer::sum);
    }

    /**
     * 取出该谓词的代表性材料物品；WIP 中间态与无可放置物品的方块返回 {@code null}。
     */
    private static @Nullable Item firstMaterialItem(BlockStatePredicate predicate) {
        return predicate.getBlocks().stream()
            .findFirst()
            .map(Holder::value)
            .filter(block -> !block.defaultBlockState().isAir())
            .filter(block -> !(block instanceof WipBlock))
            .map(Block::asItem)
            .filter(item -> item != Items.AIR)
            .orElse(null);
    }

    /**
     * 取出该循环下实际执行的步骤：多圈配方第一圈之外的第一个步骤会被替换为
     * {@code multiLoopFirstStep}。
     */
    private static ProceduralProcessStep getExecutedStep(
        ProceduralProcessRecipe recipe,
        int stepIndex,
        int currentLoop
    ) {
        if (stepIndex == 0 && currentLoop > 0) {
            return recipe.getMultiLoopFirstStep().orElse(recipe.getSteps().getFirst());
        }
        return recipe.getSteps().get(stepIndex);
    }

    // endregion
}
