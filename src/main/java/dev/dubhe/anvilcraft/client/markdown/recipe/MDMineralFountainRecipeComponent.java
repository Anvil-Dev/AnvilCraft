package dev.dubhe.anvilcraft.client.markdown.recipe;

import dev.anvilcraft.lib.v2.util.NumberProviderUtil;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.anvilcraft.lib.v2.util.predicate.ChanceBlockState;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.MDRenderContext;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.extend.MDRecipeComponent;
import dev.anvilcraft.resource.ageratum.util.RecipeUtil;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.recipe.mineral.MineralFountainChanceRecipe;
import dev.dubhe.anvilcraft.recipe.mineral.MineralFountainRecipe;
import dev.dubhe.anvilcraft.recipe.sync.RecipesRecord;
import dev.dubhe.anvilcraft.util.AgeratumUtil;
import dev.dubhe.anvilcraft.util.TooltipUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

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
    private final @Nullable Identifier dimension;

    public MDMineralFountainRecipeComponent(MineralFountainRecipe recipe, boolean enableAlignCenter) {
        this(recipe.fromBlock(), recipe.toBlock(), recipe.needBlock().constructStatesForRender(), null, enableAlignCenter);
    }

    public MDMineralFountainRecipeComponent(MineralFountainChanceRecipe recipe, boolean enableAlignCenter) {
        this(recipe.fromBlock(), recipe.toBlock(), findSideBlocks(recipe.fromBlock()), recipe.dimension(), enableAlignCenter);
    }

    private MDMineralFountainRecipeComponent(
        BlockStatePredicate fromBlock, ChanceBlockState toBlock, List<BlockState> sideBlocks,
        @Nullable Identifier dimension, boolean enableAlignCenter
    ) {
        super(TEXTURE, WIDTH, HEIGHT, enableAlignCenter);
        this.fromBlock = fromBlock;
        this.toBlock = toBlock;
        this.sideBlocks = sideBlocks;
        this.dimension = dimension;
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
        RenderSupport.renderBlockAt(context.graphics(), result, OUTPUT_X, BLOCK_Y, AgeratumUtil.BLOCK_SIZE);
        if (!AgeratumUtil.isHoverBlock(OUTPUT_X, BLOCK_Y, mouseX, mouseY)) return;
        List<Component> lines = new ArrayList<>(TooltipUtil.tooltip(result.getBlock()));
        double chance = NumberProviderUtil.expected(this.toBlock.chance());
        if (chance >= 0 && chance != 1) {
            lines.add(Component.translatable("gui.anvilcraft.category.chance", FORMATTER.format(chance * 100))
                .withStyle(ChatFormatting.GRAY));
        }
        if (this.dimension != null) lines.add(dimensionName(this.dimension).copy().withStyle(ChatFormatting.GRAY));
        context.tooltips().add(new MDRenderContext.Tooltip(lines, Optional.empty()));
    }

    private static List<BlockState> findSideBlocks(BlockStatePredicate fromBlock) {
        if (Minecraft.getInstance().level == null || RecipesRecord.CLIENTSIDE == null) return List.of();
        var fromStates = fromBlock.constructStatesForRender();
        var result = new LinkedHashSet<BlockState>();
        for (var holder : RecipesRecord.CLIENTSIDE.byType(ModRecipeTypes.MINERAL_FOUNTAIN.get())) {
            var recipe = holder.value();
            if (fromStates.stream().anyMatch(first -> recipe.fromBlock().constructStatesForRender().stream()
                .anyMatch(second -> first.is(second.getBlock())))) {
                result.addAll(recipe.needBlock().constructStatesForRender());
            }
        }
        return List.copyOf(result);
    }

    private static Component dimensionName(Identifier dimension) {
        return Component.translatable("dimension." + dimension.getNamespace() + "." + dimension.getPath());
    }
}
