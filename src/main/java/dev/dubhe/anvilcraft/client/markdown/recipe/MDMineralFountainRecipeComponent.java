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
import dev.dubhe.anvilcraft.recipe.mineral.AroundRequirement;
import dev.dubhe.anvilcraft.recipe.mineral.MineralFountainActiveRecipe;
import dev.dubhe.anvilcraft.recipe.mineral.MineralFountainPassiveRecipe;
import dev.dubhe.anvilcraft.recipe.mineral.MineralFountainReplacementRecipe;
import dev.dubhe.anvilcraft.util.AgeratumUtil;
import dev.dubhe.anvilcraft.util.TooltipUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

public class MDMineralFountainRecipeComponent extends MDRecipeComponent {
    public static final ResourceLocation TEXTURE = AnvilCraft.of("textures/gui/ageratum/128back.png");
    public static final int WIDTH = 128;
    public static final int HEIGHT = 64;
    private static final DecimalFormat FORMATTER = new DecimalFormat();

    private static final int INPUT_X = 38;
    private static final int ARROW_X = INPUT_X + 18;
    private static final int OUTPUT_X = INPUT_X + 60;
    private static final int BLOCK_Y = 16;
    private static final int FOUNTAIN_Y = BLOCK_Y + AgeratumUtil.BLOCK_HEIGHT;

    private static final int DIMENSION_X = OUTPUT_X - 10;
    private static final int DIMENSION_Y = FOUNTAIN_Y + 14;

    private static final int[][] SIDE_POSITIONS = {
        {
            INPUT_X - 14,
            FOUNTAIN_Y - 7,
            0
        },
        {
            INPUT_X + 14,
            FOUNTAIN_Y - 7,
            0
        },
        {
            INPUT_X - 14,
            FOUNTAIN_Y + 7,
            20
        },
        {
            INPUT_X + 14,
            FOUNTAIN_Y + 7,
            20
        }
    };

    private final BlockStatePredicate fromBlock;
    private final ChanceBlockState toBlock;
    private final List<BlockState> sideBlocks;
    @Nullable
    private final ResourceLocation dimension;

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
            List.of(),
            recipe.getDimension().orElse(null),
            enableAlignCenter
        );
    }

    private MDMineralFountainRecipeComponent(
        BlockStatePredicate fromBlock,
        ChanceBlockState toBlock,
        List<BlockState> sideBlocks,
        @Nullable ResourceLocation dimension,
        boolean enableAlignCenter
    ) {
        super(TEXTURE, WIDTH, HEIGHT, enableAlignCenter);
        this.fromBlock = fromBlock;
        this.toBlock = toBlock;
        this.sideBlocks = sideBlocks;
        this.dimension = dimension;
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

    @Override
    protected void renderRecipe(MDRenderContext context, float mouseX, float mouseY) {

        if (!this.sideBlocks.isEmpty()) {
            BlockState sideBlock = this.sideBlocks.get(RecipeUtil.getDisplayIndex(this.sideBlocks.size()));
            for (int[] position : SIDE_POSITIONS) {
                AgeratumUtil.renderBlock(context, sideBlock, mouseX, mouseY, position[0], position[1], position[2]);
            }
        }

        AgeratumUtil.renderBlock(context, this.fromBlock, mouseX, mouseY, INPUT_X, BLOCK_Y, 16);

        BlockState fountain = ModBlocks.MINERAL_FOUNTAIN.getDefaultState();
        AgeratumUtil.renderBlock(context, fountain, mouseX, mouseY, INPUT_X, FOUNTAIN_Y, 10);

        GuiGraphics graphics = context.graphics();
        AgeratumUtil.renderArrow(graphics, ARROW_X, BLOCK_Y);

        AgeratumUtil.renderBlock(context, fountain, mouseX, mouseY, OUTPUT_X, FOUNTAIN_Y, 10);
        this.renderOutput(context, mouseX, mouseY);

        if (this.dimension != null) {
            graphics.drawCenteredString(
                Minecraft.getInstance().font,
                getDimensionName(this.dimension),
                DIMENSION_X,
                DIMENSION_Y,
                0xFFFFFFFF
            );
        }
    }

    private void renderOutput(MDRenderContext context, float mouseX, float mouseY) {
        BlockState result = this.toBlock.state();
        RenderSupport.renderBlock(
            context.graphics(),
            result,
            OUTPUT_X,
            BLOCK_Y,
            16,
            AgeratumUtil.BLOCK_SIZE,
            RenderSupport.SINGLE_BLOCK
        );

        if (!AgeratumUtil.isHoverBlock(OUTPUT_X, BLOCK_Y, mouseX, mouseY)) {
            return;
        }

        List<Component> tooltip = new ArrayList<>(TooltipUtil.tooltip(result.getBlock()));
        tooltip.addAll(this.getOutputTooltips());
        context.addTooltip(tooltip);
    }

    private List<Component> getOutputTooltips() {
        List<Component> tooltips = new ArrayList<>();
        double chance = NumberProviderUtil.expected(this.toBlock.chance());
        if (chance >= 0.0 && chance != 1.0) {
            tooltips.add(
                Component.translatable("gui.anvilcraft.category.chance", FORMATTER.format(chance * 100))
                    .withStyle(ChatFormatting.GRAY)
            );
        }
        if (this.dimension != null) {
            tooltips.add(getDimensionName(this.dimension).copy().withStyle(ChatFormatting.GRAY));
        }
        return tooltips;
    }

    private static Component getDimensionName(ResourceLocation dimension) {
        return Component.translatable("dimension." + dimension.getNamespace() + "." + dimension.getPath());
    }
}
