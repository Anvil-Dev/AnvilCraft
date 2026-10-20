package dev.dubhe.anvilcraft.client.markdown.recipe;

import dev.anvilcraft.lib.v2.util.NumberProviderUtil;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ChanceBlockState;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDRecipeComponent;
import dev.anvilcraft.resource.ageratum.util.RecipeUtil;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.support.DimensionNameSupport;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.mineral.AroundRequirement;
import dev.dubhe.anvilcraft.recipe.mineral.MineralFountainActiveRecipe;
import dev.dubhe.anvilcraft.recipe.mineral.MineralFountainPassiveRecipe;
import dev.dubhe.anvilcraft.recipe.mineral.MineralFountainReplacementRecipe;
import dev.dubhe.anvilcraft.recipe.sync.RecipesRecord;
import dev.dubhe.anvilcraft.util.AgeratumUtil;
import dev.dubhe.anvilcraft.util.TooltipUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MDMineralFountainRecipeComponent extends MDRecipeComponent {
    public static final Identifier TEXTURE = AnvilCraft.of("textures/gui/ageratum/128back.png");
    public static final int WIDTH = 128;
    public static final int HEIGHT = 64;
    private static final DecimalFormat FORMATTER = new DecimalFormat();

    private static final int INPUT_X = 38;
    private static final int[] SIDE_OFFSETS = {-14, 14};
    private static final int OUTPUT_X = INPUT_X + 60;
    private static final int BLOCK_Y = 16;
    private static final int FOUNTAIN_Y = BLOCK_Y + AgeratumUtil.BLOCK_HEIGHT;
    private final BlockStatePredicate fromBlock;
    private final ChanceBlockState toBlock;
    private final List<BlockState> sideBlocks;
    @Nullable
    private final Identifier dimension;

    @Nullable
    private final Double replaceChance;

    public MDMineralFountainRecipeComponent(MineralFountainActiveRecipe recipe, boolean enableAlignCenter) {
        this(
            recipe.getFromBlock(),
            recipe.getToBlock(),
            aroundBlocks(recipe),
            recipe.getDimension().orElse(null),
            enableAlignCenter
        );
    }

    public MDMineralFountainRecipeComponent(MineralFountainPassiveRecipe recipe, boolean enableAlignCenter) {
        this(
            recipe.getFromBlock(),
            recipe.getToBlock(),
            List.of(),
            recipe.getDimension().orElse(null),
            enableAlignCenter
        );
    }

    public MDMineralFountainRecipeComponent(MineralFountainReplacementRecipe recipe, boolean enableAlignCenter) {
        this(
            recipe.getFromBlock(),
            recipe.getToBlock(),
            replacementAround(recipe),
            recipe.getDimension().orElse(null),
            recipe.getReplaceChance(),
            enableAlignCenter
        );
    }

    private MDMineralFountainRecipeComponent(
        BlockStatePredicate fromBlock,
        ChanceBlockState toBlock,
        List<BlockState> sideBlocks,
        @Nullable Identifier dimension,
        boolean enableAlignCenter
    ) {
        this(fromBlock, toBlock, sideBlocks, dimension, null, enableAlignCenter);
    }

    private MDMineralFountainRecipeComponent(
        BlockStatePredicate fromBlock,
        ChanceBlockState toBlock,
        List<BlockState> sideBlocks,
        @Nullable Identifier dimension,
        @Nullable Double replaceChance,
        boolean enableAlignCenter
    ) {
        super(TEXTURE, WIDTH, HEIGHT, enableAlignCenter);
        this.fromBlock = fromBlock;
        this.toBlock = toBlock;
        this.sideBlocks = sideBlocks;
        this.dimension = dimension;
        this.replaceChance = replaceChance;
    }

    private static List<BlockState> aroundBlocks(MineralFountainActiveRecipe recipe) {
        Set<BlockState> states = new LinkedHashSet<>();
        for (AroundRequirement requirement : recipe.getAround()) {
            states.addAll(requirement.block().constructStatesForRender());
        }
        return List.copyOf(states);
    }

    private static List<BlockState> replacementAround(MineralFountainReplacementRecipe recipe) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || RecipesRecord.CLIENTSIDE == null) return List.of();
        Set<BlockState> states = new LinkedHashSet<>();
        for (RecipeHolder<MineralFountainActiveRecipe> holder :
            RecipesRecord.CLIENTSIDE.byType(ModRecipeTypes.MINERAL_FOUNTAIN_ACTIVE.get())) {
            if (!recipe.appliesTo(holder.id().identifier())) continue;
            if (!sharesFromBlock(recipe.getFromBlock(), holder.value().getFromBlock())) continue;
            states.addAll(aroundBlocks(holder.value()));
        }
        return List.copyOf(states);
    }

    private static boolean sharesFromBlock(BlockStatePredicate first, BlockStatePredicate second) {
        List<BlockState> firstStates = first.constructStatesForRender();
        List<BlockState> secondStates = second.constructStatesForRender();
        if (firstStates.isEmpty() || secondStates.isEmpty()) return true;
        return firstStates.stream().anyMatch(second::testWithoutEntity)
               || secondStates.stream().anyMatch(first::testWithoutEntity);
    }

    @Override
    protected void extractRecipeRenderState(MDRenderContext context, float mouseX, float mouseY) {
        BlockState side = this.sideBlocks.isEmpty() ? null : this.sideBlocks.get(RecipeUtil.getDisplayIndex(this.sideBlocks.size()));
        renderSides(context, mouseX, mouseY, side, -7);
        var fountain = ModBlocks.MINERAL_FOUNTAIN.getDefaultState();
        AgeratumUtil.renderBlock(context, fountain, mouseX, mouseY, INPUT_X, FOUNTAIN_Y);
        AgeratumUtil.renderBlock(context, this.fromBlock, mouseX, mouseY, INPUT_X, BLOCK_Y);
        AgeratumUtil.renderArrow(context.graphics(), INPUT_X + 18, BLOCK_Y);
        AgeratumUtil.renderBlock(context, fountain, mouseX, mouseY, OUTPUT_X, FOUNTAIN_Y);
        this.renderOutput(context, mouseX, mouseY);
        renderSides(context, mouseX, mouseY, side, 7);
        if (this.dimension != null) {
            context.graphics().centeredText(context.minecraft().font, dimensionName(this.dimension), OUTPUT_X - 10, FOUNTAIN_Y + 14, -1);
        }
    }

    private static void renderSides(MDRenderContext context, float mouseX, float mouseY, @Nullable BlockState side, int offsetY) {
        if (side == null) return;
        for (int offsetX : SIDE_OFFSETS) {
            AgeratumUtil.renderBlock(context, side, mouseX, mouseY, INPUT_X + offsetX, FOUNTAIN_Y + offsetY);
        }
    }

    private void renderOutput(MDRenderContext context, float mouseX, float mouseY) {
        BlockState result = this.toBlock.state();
        RenderSupport.renderBlockAt(
            context.graphics(),
            result,
            OUTPUT_X,
            BLOCK_Y,
            AgeratumUtil.BLOCK_SIZE
        );

        if (!AgeratumUtil.isHoverBlock(OUTPUT_X, BLOCK_Y, mouseX, mouseY)) {
            return;
        }

        List<Component> tooltip = new ArrayList<>(TooltipUtil.tooltip(result.getBlock()));
        tooltip.addAll(this.getOutputTooltips());
        tooltip.forEach(context::addTooltip);
    }

    private List<Component> getOutputTooltips() {
        List<Component> tooltips = new ArrayList<>();
        double chance = NumberProviderUtil.expected(this.toBlock.chance());

        if (chance >= 0.0 && Math.round(chance * 100) < 100) {
            tooltips.add(
                Component.translatable("gui.anvilcraft.category.chance", FORMATTER.format(chance * 100))
                    .withStyle(ChatFormatting.GRAY)
            );
        }
        if (this.replaceChance != null) {
            tooltips.add(
                Component.translatable(
                    "gui.anvilcraft.category.mineral_fountain.replace_chance",
                    FORMATTER.format(this.replaceChance * 100)
                ).withStyle(ChatFormatting.GRAY)
            );
        }
        if (this.dimension != null) {
            tooltips.add(dimensionName(this.dimension).copy().withStyle(ChatFormatting.GRAY));
        }
        return tooltips;
    }

    private static Component dimensionName(Identifier dimension) {
        return DimensionNameSupport.getName(dimension);
    }
}
