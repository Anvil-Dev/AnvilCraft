package dev.dubhe.anvilcraft.integration.jei.category;

import dev.anvilcraft.lib.v2.util.predicate.ChanceBlockState;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.recipe.MineralFountainJeiRecipe;
import dev.dubhe.anvilcraft.integration.jei.util.JeiBlockIngredientUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.recipe.mineral.MineralFountainChanceRecipe;
import dev.dubhe.anvilcraft.recipe.mineral.MineralFountainRecipe;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MineralFountainCategory implements IRecipeCategory<MineralFountainJeiRecipe> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 78;

    private static final String SIDE_BLOCK_PREFIX = "side_block_";
    private static final String FROM_BLOCK = "from_block";
    private static final String OUTPUT_BLOCK = "output_block";
    private static final int BLOCK_SCALE = 20;
    private static final int[][] SIDE_POSITIONS = {
        {30, 36},
        {46, 36},
        {30, 44},
        {46, 44}
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
        return MineralFountainCategory.WIDTH;
    }

    @Override
    public int getHeight() {
        return MineralFountainCategory.HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MineralFountainJeiRecipe recipe, IFocusGroup focuses) {
        List<ItemStack> sideStacks = MineralFountainCategory.getIngredientStacks(recipe.sideBlocks());
        for (int i = 0; i < MineralFountainCategory.SIDE_SLOT_AREAS.length && !sideStacks.isEmpty(); i++) {
            int[] area = MineralFountainCategory.SIDE_SLOT_AREAS[i];
            JeiBlockIngredientUtil.addSlot(
                builder,
                RecipeIngredientRole.INPUT,
                MineralFountainCategory.SIDE_BLOCK_PREFIX + i,
                area[0],
                area[1],
                area[2],
                area[3],
                sideStacks
            );
        }

        List<ItemStack> fromStacks = MineralFountainCategory.getIngredientStacks(recipe.fromBlocks());
        if (!fromStacks.isEmpty()) {
            JeiBlockIngredientUtil.addSlot(
                builder, RecipeIngredientRole.INPUT, MineralFountainCategory.FROM_BLOCK, 40, 28, 16, 13, fromStacks
            );
        }

        JeiBlockIngredientUtil.addSlot(
            builder,
            RecipeIngredientRole.OUTPUT,
            MineralFountainCategory.OUTPUT_BLOCK,
            118,
            28,
            16,
            16,
            MineralFountainCategory.getIngredientStacks(List.of(recipe.result().state()))
        ).addRichTooltipCallback((slot, tooltip) -> {
            tooltip.addAll(JeiRecipeUtil.getTooltips(recipe.result().chance()));
            if (recipe.dimension() != null) {
                tooltip.add(MineralFountainCategory.getDimensionName(recipe.dimension()).copy().withStyle(ChatFormatting.GRAY));
            }
        });

        if (recipe.sideBlocks().stream().anyMatch(state -> state.is(Blocks.LAVA))) {
            builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).add(Fluids.LAVA, 1000);
        }
        if (recipe.result().state().is(Blocks.LAVA)) {
            builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).add(Fluids.LAVA, 1000);
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
        GuiGraphicsExtractor graphics,
        double mouseX,
        double mouseY
    ) {
        List<BlockState> sideBlocks = recipe.sideBlocks();
        BlockState sideState = null;
        if (!sideBlocks.isEmpty()) {
            sideState = JeiBlockIngredientUtil
                .getDisplayedState(recipeSlotsView, MineralFountainCategory.SIDE_BLOCK_PREFIX + 0, sideBlocks)
                .orElse(sideBlocks.getFirst());
            MineralFountainCategory.renderSideBlock(graphics, sideState, 0);
            MineralFountainCategory.renderSideBlock(graphics, sideState, 1);
        }

        RenderSupport.renderBlock(graphics, ModBlocks.MINERAL_FOUNTAIN.getDefaultState(), 38, 40, MineralFountainCategory.BLOCK_SCALE);

        if (sideState != null) {
            MineralFountainCategory.renderSideBlock(graphics, sideState, 2);
            MineralFountainCategory.renderSideBlock(graphics, sideState, 3);
        }

        if (!recipe.fromBlocks().isEmpty()) {
            JeiBlockIngredientUtil.getDisplayedState(recipeSlotsView, MineralFountainCategory.FROM_BLOCK, recipe.fromBlocks()).ifPresent(
                state ->
                    RenderSupport.renderBlock(graphics, state, 38, 30f, MineralFountainCategory.BLOCK_SCALE)
            );
        }

        RenderSupport.renderBlock(graphics, ModBlocks.MINERAL_FOUNTAIN.getDefaultState(), 116, 40, MineralFountainCategory.BLOCK_SCALE);
        BlockState resultState = JeiBlockIngredientUtil.getRenderablePreviewState(recipe.result().state());
        RenderSupport.renderBlock(graphics, resultState, 116, 30f, MineralFountainCategory.BLOCK_SCALE);
        this.arrow.draw(graphics, 82, 37);

        if (recipe.dimension() != null) {
            Component dimensionName = MineralFountainCategory.getDimensionName(recipe.dimension()).copy().withStyle(ChatFormatting.WHITE);
            graphics.text(Minecraft.getInstance().font, dimensionName, 126, 65, 0xFFFFFFFF, true);
        }
    }

    @Override
    public @Nullable Identifier getIdentifier(MineralFountainJeiRecipe recipe) {
        return recipe.id();
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        List<RecipeHolder<MineralFountainRecipe>> normalRecipes =
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.MINERAL_FOUNTAIN.get());
        List<MineralFountainJeiRecipe> displays = new ArrayList<>();

        displays.add(new MineralFountainJeiRecipe(
            AnvilCraft.of("mineral_fountain/cinerite_from_air"),
            List.of(),
            List.of(),
            new ChanceBlockState(ModBlocks.CINERITE.getDefaultState(), 1.0f),
            null
        ));
        displays.add(new MineralFountainJeiRecipe(
            AnvilCraft.of("mineral_fountain/lava_from_air"),
            List.of(Blocks.LAVA.defaultBlockState()),
            List.of(),
            new ChanceBlockState(Blocks.LAVA.defaultBlockState(), 1.0f),
            null
        ));

        for (RecipeHolder<MineralFountainRecipe> holder : normalRecipes) {
            MineralFountainRecipe recipe = holder.value();
            List<BlockState> sides = recipe.needBlock().constructStatesForRender();
            List<BlockState> from = recipe.fromBlock().constructStatesForRender();
            if (sides.isEmpty() || from.isEmpty()) continue;
            displays.add(new MineralFountainJeiRecipe(
                holder.id().identifier(), sides, from, recipe.toBlock(), null
            ));
        }

        for (RecipeHolder<MineralFountainChanceRecipe> holder :
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.MINERAL_FOUNTAIN_CHANCE.get())) {
            MineralFountainChanceRecipe recipe = holder.value();
            List<BlockState> from = recipe.fromBlock().constructStatesForRender();
            Set<BlockState> sides = new LinkedHashSet<>();
            for (RecipeHolder<MineralFountainRecipe> normalHolder : normalRecipes) {
                MineralFountainRecipe normalRecipe = normalHolder.value();
                if (MineralFountainCategory.sharesBlock(from, normalRecipe.fromBlock().constructStatesForRender())) {
                    sides.addAll(normalRecipe.needBlock().constructStatesForRender());
                }
            }
            if (sides.isEmpty() || from.isEmpty()) continue;
            displays.add(new MineralFountainJeiRecipe(
                holder.id().identifier(), List.copyOf(sides), from, recipe.toBlock(), recipe.dimension()
            ));
        }

        registration.addRecipes(AnvilCraftJeiPlugin.MINERAL_FOUNTAIN, displays);
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(AnvilCraftJeiPlugin.MINERAL_FOUNTAIN, ModBlocks.MINERAL_FOUNTAIN);
    }

    private static void renderSideBlock(GuiGraphicsExtractor graphics, BlockState state, int index) {
        int[] position = MineralFountainCategory.SIDE_POSITIONS[index];
        RenderSupport.renderBlock(graphics, state, position[0], position[1], MineralFountainCategory.BLOCK_SCALE);
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

    private static Component getDimensionName(Identifier dimension) {
        return Component.translatable("dimension." + dimension.getNamespace() + "." + dimension.getPath());
    }
}
