package dev.dubhe.anvilcraft.integration.jei.category;

import dev.anvilcraft.lib.v2.util.NumberProviderUtil;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ChanceBlockState;
import dev.dubhe.anvilcraft.client.support.DimensionNameSupport;
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
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidStackTemplate;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
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
        {40, 46},
        {56, 46},
        {40, 54},
        {56, 54}
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
    public IRecipeType<MineralFountainJeiRecipe> getRecipeType() {
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
        GuiGraphicsExtractor guiGraphics,
        double mouseX,
        double mouseY
    ) {

        List<MineralFountainJeiRecipe.SideRequirement> sideSlots = recipe.sideSlots();
        for (int i = 0; i < Math.min(2, sideSlots.size()); i++) {
            renderSideSlot(guiGraphics, recipeSlotsView, sideSlots, i);
        }

        RenderSupport.renderBlockAt(
            guiGraphics,
            ModBlocks.MINERAL_FOUNTAIN.getDefaultState(),
            48,
            50,
            BLOCK_SCALE
        );

        for (int i = 2; i < Math.min(4, sideSlots.size()); i++) {
            renderSideSlot(guiGraphics, recipeSlotsView, sideSlots, i);
        }

        if (!recipe.fromBlocks().isEmpty()) {
            JeiBlockIngredientUtil.getDisplayedState(recipeSlotsView, FROM_BLOCK, recipe.fromBlocks()).ifPresent(state ->
                RenderSupport.renderBlockAt(
                    guiGraphics,
                    state,
                    48,
                    39.5f,
                    BLOCK_SCALE
                )
            );
        }

        RenderSupport.renderBlockAt(
            guiGraphics,
            ModBlocks.MINERAL_FOUNTAIN.getDefaultState(),
            126,
            50,
            BLOCK_SCALE
        );

        BlockState resultState = JeiBlockIngredientUtil.toBlockState(
            JeiBlockIngredientUtil.getDisplayedIngredient(recipeSlotsView, OUTPUT_BLOCK).orElse(null),
            recipe.outcomeStates()
        );
        if (resultState == null) resultState = recipe.outcomeStates().getFirst();
        RenderSupport.renderBlockAt(
            guiGraphics,
            resultState,
            126,
            39.5f,
            BLOCK_SCALE
        );

        this.arrow.draw(guiGraphics, 82, 37);

        if (recipe.dimension() != null) {
            Component dimensionName = getDimensionName(recipe.dimension()).copy().withStyle(ChatFormatting.WHITE);
            guiGraphics.centeredText(Minecraft.getInstance().font, dimensionName, 126, 65, 0xFFFFFFFF);
        }
    }

    @Override
    public @Nullable Identifier getRegistryName(MineralFountainJeiRecipe recipe) {
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

        Map<String, List<RecipeHolder<MineralFountainActiveRecipe>>> activeGroups = new LinkedHashMap<>();
        for (RecipeHolder<MineralFountainActiveRecipe> holder : activeRecipes) {
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

        Map<Identifier, MineralFountainActiveRecipe> activeById = new LinkedHashMap<>();
        for (RecipeHolder<MineralFountainActiveRecipe> holder : activeRecipes) {
            activeById.put(holder.id().identifier(), holder.value());
        }

        for (RecipeHolder<MineralFountainReplacementRecipe> holder : replacementRecipes) {
            MineralFountainReplacementRecipe recipe = holder.value();
            displays.add(new MineralFountainJeiRecipe(
                holder.id().identifier(),
                replacementAround(recipe, activeById),
                recipe.getFromBlock().constructStatesForRender(),
                List.of(new MineralFountainJeiRecipe.Outcome(
                    recipe.getToBlock().state(), NumberProviderUtil.expected(recipe.getToBlock().chance())
                )),
                recipe.getToFluid().map(FluidStackTemplate::create).map(List::of).orElse(List.of()),
                recipe.getDimension().orElse(null),
                recipe.getReplaceChance(),
                replacementTarget(recipe)
            ));
        }

        registration.addRecipes(AnvilCraftJeiPlugin.MINERAL_FOUNTAIN, displays);
    }

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
            recipe.getToFluid().map(FluidStackTemplate::create).ifPresent(fluids::add);
        }
        return new MineralFountainJeiRecipe(
            group.getFirst().id().identifier(),
            sideRequirements(first),
            first.getFromBlock().constructStatesForRender(),
            outcomes,
            fluids,
            first.getDimension().orElse(null),
            null,
            null
        );
    }

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
            recipe.getToFluid().map(FluidStackTemplate::create).ifPresent(fluids::add);
        }
        return new MineralFountainJeiRecipe(
            group.getFirst().id().identifier(),
            List.of(),
            first.getFromBlock().constructStatesForRender(),
            outcomes,
            fluids,
            first.getDimension().orElse(null),
            null,
            null
        );
    }

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

    private static String activeGroupKey(MineralFountainActiveRecipe recipe) {
        return passiveGroupKey(recipe) + "#" + aroundKey(recipe.getAround());
    }

    private static String aroundKey(List<AroundRequirement> around) {
        return around.stream()
            .map(requirement -> requirement.count() + "x" + predicateKey(requirement.block()))
            .collect(Collectors.joining(","));
    }

    private static String predicateKey(BlockStatePredicate predicate) {
        String blocks = predicate.getBlocks().unwrap().map(
            tag -> "#" + tag.location(),
            holders -> holders.stream()
                .map(holder -> holder.unwrapKey().map(key -> key.identifier().toString()).orElse("?"))
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

    private static String passiveGroupKey(AbstractMineralFountainRecipe recipe) {
        return recipe.getDimension().map(Identifier::toString).orElse("*")
               + "#" + recipe.getPriority()
               + "#" + recipe.getFromBlock().getKey();
    }

    @Nullable
    private static Component replacementTarget(MineralFountainReplacementRecipe recipe) {
        List<Identifier> replace = recipe.getReplace();
        if (replace.isEmpty()) return null;
        return Component.literal(replace.stream()
            .map(Identifier::toString)
            .collect(Collectors.joining(", ")));
    }

    private static List<Component> getChanceTooltips(MineralFountainJeiRecipe recipe, @Nullable BlockState displayed) {
        List<Component> tooltips = new ArrayList<>();
        double chance = displayed == null ? recipe.totalChance() : recipe.chanceOf(displayed);

        if (chance >= 0 && chance < 1) {
            tooltips.add(
                Component.translatable(
                    "gui.anvilcraft.category.chance",
                    formatPercentage(chance)
                ).withStyle(ChatFormatting.GRAY)
            );
        }
        if (recipe.replacementChance() != null) {
            tooltips.add(
                Component.translatable(
                    "gui.anvilcraft.category.mineral_fountain.replace_chance",
                    formatPercentage(recipe.replacementChance())
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

    private static String formatPercentage(double chance) {
        String percentage = FORMATTER.format(chance * 100);
        if (chance < 1 && percentage.equals(FORMATTER.format(100))) {
            BigDecimal exact = BigDecimal.valueOf(chance).movePointRight(2);
            DecimalFormat localized = (DecimalFormat) FORMATTER.clone();
            localized.setMaximumFractionDigits(Math.max(0, exact.scale()));
            return localized.format(exact);
        }
        return percentage;
    }

    private static List<MineralFountainJeiRecipe.SideRequirement> replacementAround(
        MineralFountainReplacementRecipe recipe,
        Map<Identifier, MineralFountainActiveRecipe> activeById
    ) {
        Set<BlockState> states = new LinkedHashSet<>();
        int count = 0;
        for (Map.Entry<Identifier, MineralFountainActiveRecipe> entry : activeById.entrySet()) {
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

    private static boolean sharesFromBlock(BlockStatePredicate first, BlockStatePredicate second) {
        List<BlockState> firstStates = first.constructStatesForRender();
        List<BlockState> secondStates = second.constructStatesForRender();
        if (firstStates.isEmpty() || secondStates.isEmpty()) return true;
        return firstStates.stream().anyMatch(second::testWithoutEntity)
               || secondStates.stream().anyMatch(first::testWithoutEntity);
    }

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

    private static void renderSideSlot(
        GuiGraphicsExtractor guiGraphics,
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

    private static void renderSideBlock(GuiGraphicsExtractor guiGraphics, BlockState state, int index) {
        int[] position = SIDE_POSITIONS[index];
        RenderSupport.renderBlockAt(
            guiGraphics,
            state,
            position[0],
            position[1],
            BLOCK_SCALE
        );
    }

    private static Component getDimensionName(Identifier dimension) {
        return DimensionNameSupport.getName(dimension);
    }
}
