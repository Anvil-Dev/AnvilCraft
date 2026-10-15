package dev.dubhe.anvilcraft.integration.jei.category;

import dev.anvilcraft.lib.v2.util.NumberProviderUtil;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ChanceBlockState;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.recipe.MineralFountainJeiRecipe;
import dev.dubhe.anvilcraft.integration.jei.util.JeiBlockIngredientUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.recipe.mineral.AbstractMineralFountainRecipe;
import dev.dubhe.anvilcraft.recipe.mineral.AroundRequirement;
import dev.dubhe.anvilcraft.recipe.mineral.MineralFountainActiveRecipe;
import dev.dubhe.anvilcraft.recipe.mineral.MineralFountainPassiveRecipe;
import dev.dubhe.anvilcraft.recipe.mineral.MineralFountainReplacementRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
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
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MineralFountainCategory implements IRecipeCategory<MineralFountainJeiRecipe> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 78;

    private static final DecimalFormat FORMATTER = new DecimalFormat("0.#");

    private static final String SIDE_BLOCK_PREFIX = "side_block_";
    private static final String FROM_BLOCK = "from_block";
    private static final String OUTPUT_BLOCK = "output_block";
    private static final int BLOCK_SCALE = 12;

    private static final int[][] SIDE_POSITIONS = {
        {40, 46, 3},
        {56, 46, 3},
        {40, 54, 17},
        {56, 54, 17}
    };
    private static final int[][] SIDE_SLOT_AREAS = {
        {31, 38, 9, 14},
        {56, 38, 9, 14},
        {31, 50, 9, 14},
        {56, 50, 9, 14}
    };

    private final IDrawable arrow;
    private final IDrawable icon;
    private final Component title;

    public MineralFountainCategory(IGuiHelper helper) {
        this.arrow = JeiRenderHelper.getArrowDefault(helper);
        this.icon = helper.createDrawableItemLike(ModBlocks.MINERAL_FOUNTAIN);
        this.title = ModBlocks.MINERAL_FOUNTAIN.get().getName();
    }

    @Override
    public RecipeType<MineralFountainJeiRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.MINERAL_FOUNTAIN;
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
    public void setRecipe(IRecipeLayoutBuilder builder, MineralFountainJeiRecipe recipe, IFocusGroup focuses) {
        List<ItemStack> sideStacks = getIngredientStacks(recipe.sideBlocks());
        for (int i = 0; i < SIDE_SLOT_AREAS.length && !sideStacks.isEmpty(); i++) {
            int[] area = SIDE_SLOT_AREAS[i];
            JeiBlockIngredientUtil.addSlot(
                builder,
                RecipeIngredientRole.INPUT,
                SIDE_BLOCK_PREFIX + i,
                area[0],
                area[1],
                area[2],
                area[3],
                sideStacks
            );
        }

        List<ItemStack> fromStacks = getIngredientStacks(recipe.fromBlocks());
        if (!fromStacks.isEmpty()) {
            JeiBlockIngredientUtil.addSlot(
                builder, RecipeIngredientRole.INPUT, FROM_BLOCK, 40, 28, 16, 13, fromStacks
            );
        }

        // 同一格内放入组内全部结果，JEI 会自动轮换显示，从而把概率产出的多条配方合并为一个条目
        List<BlockState> outcomeStates = recipe.outcomeStates();
        JeiBlockIngredientUtil.addSlot(
            builder,
            RecipeIngredientRole.OUTPUT,
            OUTPUT_BLOCK,
            118,
            28,
            16,
            16,
            getIngredientStacks(outcomeStates)
        ).addRichTooltipCallback((slot, tooltip) -> {
            BlockState displayed = slot.getDisplayedItemStack()
                .map(ItemStack::getItem)
                .filter(BlockItem.class::isInstance)
                .map(BlockItem.class::cast)
                .map(BlockItem::getBlock)
                .flatMap(block -> outcomeStates.stream().filter(state -> state.is(block)).findFirst())
                .orElse(null);
            tooltip.addAll(getChanceTooltips(recipe, displayed));
            if (recipe.dimension() != null) {
                tooltip.add(getDimensionName(recipe.dimension()).copy().withStyle(ChatFormatting.GRAY));
            }
        });

        if (recipe.sideBlocks().stream().anyMatch(state -> state.is(Blocks.LAVA))) {
            builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addFluidStack(Fluids.LAVA, 1000);
        }
        if (outcomeStates.stream().anyMatch(state -> state.is(Blocks.LAVA))) {
            builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addFluidStack(Fluids.LAVA, 1000);
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, MineralFountainJeiRecipe recipe, IFocusGroup focuses) {
        JeiBlockIngredientUtil.suppressHoverOverlays(builder);
    }

    @Override
    public void draw(
        MineralFountainJeiRecipe recipe,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY
    ) {
        List<BlockState> sideBlocks = recipe.sideBlocks();
        BlockState sideState = null;
        if (!sideBlocks.isEmpty()) {
            sideState = JeiBlockIngredientUtil
                .getDisplayedState(recipeSlotsView, SIDE_BLOCK_PREFIX + 0, sideBlocks)
                .orElse(sideBlocks.getFirst());
            renderSideBlock(guiGraphics, sideState, 0);
            renderSideBlock(guiGraphics, sideState, 1);
        }

        RenderSupport.renderBlock(
            guiGraphics,
            ModBlocks.MINERAL_FOUNTAIN.getDefaultState(),
            48,
            50,
            10,
            BLOCK_SCALE,
            RenderSupport.SINGLE_BLOCK
        );

        if (sideState != null) {
            renderSideBlock(guiGraphics, sideState, 2);
            renderSideBlock(guiGraphics, sideState, 3);
        }

        if (!recipe.fromBlocks().isEmpty()) {
            JeiBlockIngredientUtil.getDisplayedState(recipeSlotsView, FROM_BLOCK, recipe.fromBlocks()).ifPresent(state ->
                RenderSupport.renderBlock(guiGraphics, state, 48, 39.5f, 16, BLOCK_SCALE, RenderSupport.SINGLE_BLOCK)
            );
        }

        RenderSupport.renderBlock(
            guiGraphics,
            ModBlocks.MINERAL_FOUNTAIN.getDefaultState(),
            126,
            50,
            10,
            BLOCK_SCALE,
            RenderSupport.SINGLE_BLOCK
        );

        // 渲染当前轮换到的结果方块，与输出格显示的物品保持一致
        BlockState resultState = JeiBlockIngredientUtil
            .getDisplayedState(recipeSlotsView, OUTPUT_BLOCK, recipe.outcomeStates())
            .orElse(recipe.outcomeStates().getFirst());
        RenderSupport.renderBlock(guiGraphics, resultState, 126, 39.5f, 16, BLOCK_SCALE, RenderSupport.SINGLE_BLOCK);

        this.arrow.draw(guiGraphics, 82, 37);

        if (recipe.dimension() != null) {
            Component dimensionName = getDimensionName(recipe.dimension()).copy().withStyle(ChatFormatting.WHITE);
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, dimensionName, 126, 65, 0xFFFFFFFF);
        }
    }

    @Override
    public @Nullable ResourceLocation getRegistryName(MineralFountainJeiRecipe recipe) {
        return recipe.id();
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        List<RecipeHolder<MineralFountainActiveRecipe>> activeRecipes =
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.MINERAL_FOUNTAIN_ACTIVE.get());
        List<RecipeHolder<MineralFountainPassiveRecipe>> passiveRecipes =
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.MINERAL_FOUNTAIN_PASSIVE.get());
        final List<RecipeHolder<MineralFountainReplacementRecipe>> replacementRecipes =
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.MINERAL_FOUNTAIN_REPLACEMENT.get());
        List<MineralFountainJeiRecipe> displays = new ArrayList<>();

        // 主动与被动在不同阶段执行，故各自分组；每组内并列的配方按权重瓜分产出，合并为一个条目。
        // 替换生产逐个改写主动配方结果，语义不同，保持独立条目。
        Map<String, List<RecipeHolder<MineralFountainActiveRecipe>>> activeGroups = new LinkedHashMap<>();
        for (RecipeHolder<MineralFountainActiveRecipe> holder : activeRecipes) {
            if (aroundBlocks(holder.value()).isEmpty()) continue;
            activeGroups.computeIfAbsent(activeGroupKey(holder.value()), key -> new ArrayList<>()).add(holder);
        }
        for (List<RecipeHolder<MineralFountainActiveRecipe>> group : activeGroups.values()) {
            displays.add(mergeActive(group));
        }

        Map<String, List<RecipeHolder<MineralFountainPassiveRecipe>>> passiveGroups = new LinkedHashMap<>();
        for (RecipeHolder<MineralFountainPassiveRecipe> holder : passiveRecipes) {
            passiveGroups.computeIfAbsent(passiveGroupKey(holder.value()), key -> new ArrayList<>()).add(holder);
        }
        for (List<RecipeHolder<MineralFountainPassiveRecipe>> group : passiveGroups.values()) {
            displays.add(mergePassive(group));
        }

        // 替换配方只在对应主动配方匹配时才会改写其结果，所以它的周围需求取决于被替换的主动配方。
        // 例如地核碎片要求周围摆上任意四个同种粗矿块——这正是粗矿主动配方的需求。
        Map<ResourceLocation, MineralFountainActiveRecipe> activeById = new LinkedHashMap<>();
        for (RecipeHolder<MineralFountainActiveRecipe> holder : activeRecipes) {
            activeById.put(holder.id(), holder.value());
        }

        for (RecipeHolder<MineralFountainReplacementRecipe> holder : replacementRecipes) {
            MineralFountainReplacementRecipe recipe = holder.value();
            displays.add(new MineralFountainJeiRecipe(
                holder.id(),
                replacementAround(recipe, activeById),
                recipe.getFromBlock().constructStatesForRender(),
                List.of(new MineralFountainJeiRecipe.Outcome(
                    recipe.getToBlock().state(), NumberProviderUtil.expected(recipe.getToBlock().chance())
                )),
                recipe.getDimension().orElse(null),
                recipe.getReplaceChance(),
                replacementTarget(recipe)
            ));
        }

        registration.addRecipes(AnvilCraftJeiPlugin.MINERAL_FOUNTAIN, displays);
    }

    /**
     * 把一组并列的主动配方合并为一个条目，结果按权重占比分摊概率。
     */
    private static MineralFountainJeiRecipe mergeActive(List<RecipeHolder<MineralFountainActiveRecipe>> group) {
        MineralFountainActiveRecipe first = group.getFirst().value();
        int weightTotal = group.stream().mapToInt(holder -> holder.value().getWeight()).sum();
        List<MineralFountainJeiRecipe.Outcome> outcomes = new ArrayList<>();
        for (RecipeHolder<MineralFountainActiveRecipe> holder : group) {
            MineralFountainActiveRecipe recipe = holder.value();
            outcomes.add(new MineralFountainJeiRecipe.Outcome(
                recipe.getToBlock().state(),
                shareOf(recipe, weightTotal, holder.value().getToBlock())
            ));
        }
        return new MineralFountainJeiRecipe(
            group.getFirst().id(),
            aroundBlocks(first),
            first.getFromBlock().constructStatesForRender(),
            outcomes,
            first.getDimension().orElse(null),
            null,
            null
        );
    }

    /**
     * 把一组并列的被动配方合并为一个条目，结果按权重占比分摊概率。
     */
    private static MineralFountainJeiRecipe mergePassive(List<RecipeHolder<MineralFountainPassiveRecipe>> group) {
        MineralFountainPassiveRecipe first = group.getFirst().value();
        int weightTotal = group.stream().mapToInt(holder -> holder.value().getWeight()).sum();
        List<MineralFountainJeiRecipe.Outcome> outcomes = new ArrayList<>();
        for (RecipeHolder<MineralFountainPassiveRecipe> holder : group) {
            MineralFountainPassiveRecipe recipe = holder.value();
            outcomes.add(new MineralFountainJeiRecipe.Outcome(
                recipe.getToBlock().state(),
                shareOf(recipe, weightTotal, recipe.getToBlock())
            ));
        }
        return new MineralFountainJeiRecipe(
            group.getFirst().id(),
            List.of(),
            first.getFromBlock().constructStatesForRender(),
            outcomes,
            first.getDimension().orElse(null),
            null,
            null
        );
    }

    /**
     * 单条配方的最终产出概率：结果方块概率 × 组内权重占比。
     */
    private static double shareOf(
        AbstractMineralFountainRecipe recipe,
        int weightTotal,
        ChanceBlockState toBlock
    ) {
        double own = NumberProviderUtil.expected(toBlock.chance());
        if (own < 0) return own;
        if (weightTotal <= 0) return own;
        return own * recipe.getWeight() / (double) weightTotal;
    }

    /**
     * 主动配方的并列分组键。
     *
     * <p>只有「维度 + 优先级 + 材料方块 + 周围需求」都相同的配方才会同时匹配，
     * 因而才会按权重瓜分产出；否则它们之间是靠优先级或匹配条件区分的，不构成竞争。</p>
     */
    private static String activeGroupKey(MineralFountainActiveRecipe recipe) {
        return passiveGroupKey(recipe) + "#" + recipe.getAround();
    }

    /**
     * 被动配方的并列分组键：维度 + 优先级 + 材料方块。
     */
    private static String passiveGroupKey(AbstractMineralFountainRecipe recipe) {
        return recipe.getDimension().map(ResourceLocation::toString).orElse("*")
               + "#" + recipe.getPriority()
               + "#" + recipe.getFromBlock().getKey();
    }

    /**
     * 被替换的主动配方名；为空表示可替换任意主动配方。
     */
    @Nullable
    private static Component replacementTarget(MineralFountainReplacementRecipe recipe) {
        List<ResourceLocation> replace = recipe.getReplace();
        if (replace.isEmpty()) return null;
        return Component.literal(replace.stream()
            .map(ResourceLocation::toString)
            .collect(Collectors.joining(", ")));
    }

    /**
     * 产出概率提示。
     *
     * <p>同一格内会轮换显示组内各个结果，因此这里按**当前显示**的方块给出其实际概率，
     * 而不是只显示一个笼统值。概率为 100% 时不显示，没有信息量。
     * 替换生产的改写概率单独列出。</p>
     *
     * @param displayed 当前显示的方块状态；为空时退回显示整体概率
     */
    private static List<Component> getChanceTooltips(MineralFountainJeiRecipe recipe, @Nullable BlockState displayed) {
        List<Component> tooltips = new ArrayList<>();
        double chance = displayed == null ? recipe.totalChance() : recipe.chanceOf(displayed);
        // 按显示时的取整判断，避免 99.96% 被格式化成 100% 却仍然显示
        if (chance >= 0 && Math.round(chance * 100) < 100) {
            tooltips.add(
                Component.translatable(
                    "gui.anvilcraft.category.chance",
                    FORMATTER.format(chance * 100)
                ).withStyle(ChatFormatting.GRAY)
            );
        }
        if (recipe.replacementChance() != null) {
            tooltips.add(
                Component.translatable(
                    "gui.anvilcraft.category.mineral_fountain.replace_chance",
                    FORMATTER.format(recipe.replacementChance() * 100)
                ).withStyle(ChatFormatting.GRAY)
            );
            if (recipe.replacementTarget() != null) {
                tooltips.add(
                    Component.translatable(
                        "gui.anvilcraft.category.mineral_fountain.replace_target",
                        recipe.replacementTarget()
                    ).withStyle(ChatFormatting.DARK_GRAY)
                );
            }
        }
        return tooltips;
    }

    /**
     * 替换配方应展示的周围需求。
     *
     * <p>替换只会在某个主动配方匹配时生效，所以它的周围需求就是被替换的主动配方的需求。
     * {@code replace} 为空表示可替换任意主动配方，此时并入所有主动配方的需求；
     * 若指定了配方 id，则只取这些配方的需求。</p>
     *
     * <p>此外要求两者能匹配同一个上方方块：替换与主动必须同时匹配才会发生，
     * 材料方块不相容的配方（如熔岩配方的 {@code from_block} 是空气）永远不会共存，
     * 不应把它的周围需求画进来。</p>
     */
    private static List<BlockState> replacementAround(
        MineralFountainReplacementRecipe recipe,
        Map<ResourceLocation, MineralFountainActiveRecipe> activeById
    ) {
        Set<BlockState> states = new LinkedHashSet<>();
        for (Map.Entry<ResourceLocation, MineralFountainActiveRecipe> entry : activeById.entrySet()) {
            if (!recipe.appliesTo(entry.getKey())) continue;
            if (!sharesFromBlock(recipe.getFromBlock(), entry.getValue().getFromBlock())) continue;
            states.addAll(aroundBlocks(entry.getValue()));
        }
        return List.copyOf(states);
    }

    /**
     * 两个材料方块谓词是否存在交集，即能否匹配同一个上方方块。
     *
     * <p>用 {@code constructStatesForRender()} 取候选状态再互相匹配；
     * 谓词为空（不限制方块）时没有候选状态，视为可与任意方块共存。</p>
     */
    private static boolean sharesFromBlock(BlockStatePredicate first, BlockStatePredicate second) {
        List<BlockState> firstStates = first.constructStatesForRender();
        List<BlockState> secondStates = second.constructStatesForRender();
        if (firstStates.isEmpty() || secondStates.isEmpty()) return true;
        return firstStates.stream().anyMatch(second::testWithoutEntity)
               || secondStates.stream().anyMatch(first::testWithoutEntity);
    }

    /**
     * 主动配方需求表中出现的方块，用于侧边展示。
     */
    private static List<BlockState> aroundBlocks(MineralFountainActiveRecipe recipe) {
        Set<BlockState> states = new LinkedHashSet<>();
        for (AroundRequirement requirement : recipe.getAround()) {
            states.addAll(requirement.block().constructStatesForRender());
        }
        return List.copyOf(states);
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ModBlocks.MINERAL_FOUNTAIN.asStack(), AnvilCraftJeiPlugin.MINERAL_FOUNTAIN);
    }

    private static void renderSideBlock(GuiGraphics guiGraphics, BlockState state, int index) {
        int[] position = SIDE_POSITIONS[index];
        RenderSupport.renderBlock(
            guiGraphics,
            state,
            position[0],
            position[1],
            position[2],
            BLOCK_SCALE,
            RenderSupport.SINGLE_BLOCK
        );
    }

    private static List<ItemStack> getIngredientStacks(List<BlockState> states) {
        return states.stream()
            .map(state -> state.is(Blocks.LAVA) ? new ItemStack(Items.LAVA_BUCKET) : new ItemStack(state.getBlock()))
            .filter(stack -> !stack.isEmpty())
            .toList();
    }

    private static boolean sharesBlock(List<BlockState> first, List<BlockState> second) {
        return first.stream().anyMatch(firstState ->
            second.stream().anyMatch(secondState -> firstState.is(secondState.getBlock()))
        );
    }

    private static Component getDimensionName(ResourceLocation dimension) {
        return Component.translatable("dimension." + dimension.getNamespace() + "." + dimension.getPath());
    }
}
