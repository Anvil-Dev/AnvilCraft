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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

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

        JeiBlockIngredientUtil.addSlot(
            builder,
            RecipeIngredientRole.OUTPUT,
            OUTPUT_BLOCK,
            118,
            28,
            16,
            16,
            getIngredientStacks(List.of(recipe.result().state()))
        ).addRichTooltipCallback((slot, tooltip) -> {
            tooltip.addAll(JeiRecipeUtil.getTooltips(recipe.result().chance()));
            if (recipe.dimension() != null) {
                tooltip.add(getDimensionName(recipe.dimension()).copy().withStyle(ChatFormatting.GRAY));
            }
        });

        if (recipe.sideBlocks().stream().anyMatch(state -> state.is(Blocks.LAVA))) {
            builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addFluidStack(Fluids.LAVA, 1000);
        }
        if (recipe.result().state().is(Blocks.LAVA)) {
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

        BlockState resultState = JeiBlockIngredientUtil.getRenderablePreviewState(recipe.result().state());
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
            List<BlockState> sides = recipe.getNeedBlock().constructStatesForRender();
            List<BlockState> from = recipe.getFromBlock().constructStatesForRender();
            if (sides.isEmpty() || from.isEmpty()) continue;
            displays.add(new MineralFountainJeiRecipe(holder.id(), sides, from, recipe.getToBlock(), null));
        }

        for (RecipeHolder<MineralFountainChanceRecipe> holder :
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.MINERAL_FOUNTAIN_CHANCE.get())) {
            MineralFountainChanceRecipe recipe = holder.value();
            List<BlockState> from = recipe.getFromBlock().constructStatesForRender();
            Set<BlockState> sides = new LinkedHashSet<>();
            for (RecipeHolder<MineralFountainRecipe> normalHolder : normalRecipes) {
                MineralFountainRecipe normalRecipe = normalHolder.value();
                if (sharesBlock(from, normalRecipe.getFromBlock().constructStatesForRender())) {
                    sides.addAll(normalRecipe.getNeedBlock().constructStatesForRender());
                }
            }
            if (sides.isEmpty() || from.isEmpty()) continue;
            displays.add(new MineralFountainJeiRecipe(
                holder.id(), List.copyOf(sides), from, recipe.getToBlock(), recipe.getDimension()
            ));
        }

        registration.addRecipes(AnvilCraftJeiPlugin.MINERAL_FOUNTAIN, displays);
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
