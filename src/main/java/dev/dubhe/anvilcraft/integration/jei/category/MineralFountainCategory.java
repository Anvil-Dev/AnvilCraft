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
import mezz.jei.api.gui.builder.IIngredientAcceptor;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
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
        // 底座需求摊到四个相邻面上，哪个面需要什么、需要几个都能直接看出来。
        // 每项需求各自占格，因此「2 熔岩 + 2 熔融宝石」会显示成两个位置各一种。
        List<MineralFountainJeiRecipe.SideRequirement> sideSlots = recipe.sideSlots();
        for (int i = 0; i < SIDE_SLOT_AREAS.length; i++) {
            if (i >= sideSlots.size()) break;
            MineralFountainJeiRecipe.SideRequirement side = sideSlots.get(i);
            int[] area = SIDE_SLOT_AREAS[i];
            JeiBlockIngredientUtil.addSlot(
                builder,
                RecipeIngredientRole.INPUT,
                SIDE_BLOCK_PREFIX + i,
                area[0],
                area[1],
                area[2],
                area[3],
                JeiBlockIngredientUtil.toItemStacks(side.states()),
                JeiBlockIngredientUtil.toFluidStacks(side.states())
            ).addRichTooltipCallback((slot, tooltip) -> tooltip.add(
                Component.translatable(
                    "gui.anvilcraft.category.mineral_fountain.side_count",
                    side.count()
                ).withStyle(ChatFormatting.GRAY)
            ));
        }

        List<ItemStack> fromStacks = JeiBlockIngredientUtil.toItemStacks(recipe.fromBlocks());
        if (!fromStacks.isEmpty()) {
            JeiBlockIngredientUtil.addSlot(
                builder, RecipeIngredientRole.INPUT, FROM_BLOCK, 40, 28, 16, 13, fromStacks
            );
        }

        // 同一格内放入组内全部结果，JEI 会自动轮换显示，从而把概率产出的多条配方合并为一个条目。
        // 流体产出放进流体本身（而非它的桶），这样悬停显示的是流体名；画面仍由 draw() 渲染流体方块。
        List<BlockState> outcomeStates = recipe.outcomeStates();
        JeiBlockIngredientUtil.addSlot(
            builder,
            RecipeIngredientRole.OUTPUT,
            OUTPUT_BLOCK,
            118,
            28,
            16,
            16,
            JeiBlockIngredientUtil.toItemStacks(outcomeStates),
            JeiBlockIngredientUtil.toFluidStacks(outcomeStates)
        ).addRichTooltipCallback((slot, tooltip) -> {
            BlockState displayed = JeiBlockIngredientUtil.toBlockState(
                slot.getDisplayedIngredient().orElse(null), outcomeStates
            );
            tooltip.addAll(getChanceTooltips(
                recipe, displayed == null ? outcomeStates.getFirst() : displayed
            ));
            if (recipe.dimension() != null) {
                tooltip.add(getDimensionName(recipe.dimension()).copy().withStyle(ChatFormatting.GRAY));
            }
        });

        // 流体成分用于支持 JEI 的流体查找与用途跳转，不额外占格
        if (!recipe.fluids().isEmpty()) {
            IIngredientAcceptor<?> invisible = builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT);
            recipe.fluids().forEach(fluid -> invisible.addFluidStack(
                fluid.getFluid(), fluid.getAmount(), fluid.getComponentsPatch()
            ));
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
        // 每个底座位置渲染它自己那一格当前轮换到的方块：位置 0/1 在涌泉之前、2/3 在其之后，
        // 以便与涌泉的 3D 遮挡关系正确。
        List<MineralFountainJeiRecipe.SideRequirement> sideSlots = recipe.sideSlots();
        for (int i = 0; i < Math.min(2, sideSlots.size()); i++) {
            renderSideSlot(guiGraphics, recipeSlotsView, sideSlots, i);
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

        for (int i = 2; i < Math.min(4, sideSlots.size()); i++) {
            renderSideSlot(guiGraphics, recipeSlotsView, sideSlots, i);
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

        // 渲染当前轮换到的结果方块，与输出格显示保持一致。
        // 流体结果的成分是流体本身（不是桶），故这里要同时认物品与流体。
        BlockState resultState = JeiBlockIngredientUtil.toBlockState(
            JeiBlockIngredientUtil.getDisplayedIngredient(recipeSlotsView, OUTPUT_BLOCK).orElse(null),
            recipe.outcomeStates()
        );
        if (resultState == null) resultState = recipe.outcomeStates().getFirst();
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
            if (holder.value().getAround().isEmpty()) continue;
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
                recipe.getToFluid().map(List::of).orElse(List.of()),
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
        List<FluidStack> fluids = new ArrayList<>();
        for (RecipeHolder<MineralFountainActiveRecipe> holder : group) {
            MineralFountainActiveRecipe recipe = holder.value();
            outcomes.add(new MineralFountainJeiRecipe.Outcome(
                recipe.getToBlock().state(),
                shareOf(recipe, weightTotal, holder.value().getToBlock())
            ));
            recipe.getToFluid().ifPresent(fluids::add);
        }
        return new MineralFountainJeiRecipe(
            group.getFirst().id(),
            sideRequirements(first),
            first.getFromBlock().constructStatesForRender(),
            outcomes,
            fluids,
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
        List<FluidStack> fluids = new ArrayList<>();
        for (RecipeHolder<MineralFountainPassiveRecipe> holder : group) {
            MineralFountainPassiveRecipe recipe = holder.value();
            outcomes.add(new MineralFountainJeiRecipe.Outcome(
                recipe.getToBlock().state(),
                shareOf(recipe, weightTotal, recipe.getToBlock())
            ));
            recipe.getToFluid().ifPresent(fluids::add);
        }
        return new MineralFountainJeiRecipe(
            group.getFirst().id(),
            List.of(),
            first.getFromBlock().constructStatesForRender(),
            outcomes,
            fluids,
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
        return passiveGroupKey(recipe) + "#" + aroundKey(recipe.getAround());
    }

    /**
     * 周围需求的结构化键。
     *
     * <p>不能用 {@code toString()}：{@link AroundRequirement} 是 record，
     * 其 {@code block} 字段是未重写 {@code toString()} 的 {@link BlockStatePredicate}，
     * 因此 toString 里只有身份哈希——结构相同的两条配方会得到不同的键，无法分组。
     * 这里改成按内容拼接。</p>
     */
    private static String aroundKey(List<AroundRequirement> around) {
        return around.stream()
            .map(requirement -> requirement.count() + "x" + predicateKey(requirement.block()))
            .collect(Collectors.joining(","));
    }

    /**
     * 方块谓词的结构化键：方块集合（标签或方块列表）、属性匹配器与 NBT 条件。
     *
     * <p>手工拼接而不用 {@code BlockStatePredicate.CODEC}：该 codec 的 {@code blocks} 字段是
     * {@code RegistryCodecs.homogeneousList}，编码需要 registry access，在 JEI 注册阶段编码会失败。</p>
     */
    private static String predicateKey(BlockStatePredicate predicate) {
        String blocks = predicate.getBlocks().unwrap().map(
            tag -> "#" + tag.location(),
            holders -> holders.stream()
                .map(holder -> holder.unwrapKey().map(key -> key.location().toString()).orElse("?"))
                .sorted()
                .collect(Collectors.joining("|"))
        );
        String properties = predicate.getProperties().stream()
            .map(matchers -> matchers.stream()
                .map(matcher -> matcher.name() + "=" + matcher.valueMatcher())
                .sorted()
                .collect(Collectors.joining("&")))
            .sorted()
            .collect(Collectors.joining(";"));
        String nbts = predicate.getNbts().stream()
            .map(nbt -> nbt.tag().toString())
            .sorted()
            .collect(Collectors.joining(";"));
        return blocks + "[" + properties + "]" + "{" + nbts + "}";
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
     *
     * <p>多个主动配方的需求会按方块合并成一项，因此「任意一种粗矿块 ×4」显示为一项，
     * 而不是十几个各自成项的位置。</p>
     */
    private static List<MineralFountainJeiRecipe.SideRequirement> replacementAround(
        MineralFountainReplacementRecipe recipe,
        Map<ResourceLocation, MineralFountainActiveRecipe> activeById
    ) {
        Set<BlockState> states = new LinkedHashSet<>();
        int count = 0;
        for (Map.Entry<ResourceLocation, MineralFountainActiveRecipe> entry : activeById.entrySet()) {
            if (!recipe.appliesTo(entry.getKey())) continue;
            if (!sharesFromBlock(recipe.getFromBlock(), entry.getValue().getFromBlock())) continue;
            for (AroundRequirement requirement : entry.getValue().getAround()) {
                states.addAll(requirement.block().constructStatesForRender());
                count = Math.max(count, requirement.count());
            }
        }
        if (states.isEmpty()) return List.of();
        return List.of(new MineralFountainJeiRecipe.SideRequirement(List.copyOf(states), count));
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
     * 主动配方的周围需求，保留各自的数量。
     */
    private static List<MineralFountainJeiRecipe.SideRequirement> sideRequirements(MineralFountainActiveRecipe recipe) {
        return recipe.getAround().stream()
            .map(requirement -> new MineralFountainJeiRecipe.SideRequirement(
                requirement.block().constructStatesForRender(),
                requirement.count()
            ))
            .toList();
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ModBlocks.MINERAL_FOUNTAIN.asStack(), AnvilCraftJeiPlugin.MINERAL_FOUNTAIN);
    }

    /**
     * 渲染某个底座位置当前轮换到的方块。
     */
    private static void renderSideSlot(
        GuiGraphics guiGraphics,
        IRecipeSlotsView recipeSlotsView,
        List<MineralFountainJeiRecipe.SideRequirement> sideSlots,
        int index
    ) {
        MineralFountainJeiRecipe.SideRequirement side = sideSlots.get(index);
        BlockState state = JeiBlockIngredientUtil.toBlockState(
            JeiBlockIngredientUtil.getDisplayedIngredient(recipeSlotsView, SIDE_BLOCK_PREFIX + index).orElse(null),
            side.states()
        );
        if (state == null) state = side.states().getFirst();
        renderSideBlock(guiGraphics, state, index);
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

    private static Component getDimensionName(ResourceLocation dimension) {
        return Component.translatable("dimension." + dimension.getNamespace() + "." + dimension.getPath());
    }
}
