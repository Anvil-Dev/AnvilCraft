package dev.dubhe.anvilcraft.integration.jei.category;

import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.recipe.DecayRecipe;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.util.LevelLike;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawablesView;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.placement.HorizontalAlignment;
import mezz.jei.api.gui.placement.VerticalAlignment;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.gui.widgets.IScrollGridWidget;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.common.util.RegistryUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DecayCategory implements IRecipeCategory<DecayRecipe> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 128;
    public static final int MAX_SHOWN_ROW = 7;
    public static final int MAX_SHOWN_COLUMN = 5;

    private static final String CENTER_SLOT = "center";
    private static final BlockPos CENTER_POS = new BlockPos(1, 1, 1);

    private final IDrawable slot;
    private final IDrawable icon;
    private final Component title;
    private final IDrawable arrowDefault;
    private final Component randomTickTooltip;
    private final Component centerTooltip;
    private final Component aroundTooltip;
    private final Component notConsumedTooltip;
    private final Map<PreviewKey, LevelLike> previewCache = new HashMap<>();

    public DecayCategory(IGuiHelper helper) {
        this.slot = JeiRenderHelper.getSlotChoice(helper);
        this.icon = helper.createDrawableItemLike(ModBlocks.URANIUM_BLOCK);
        this.title = Component.translatable("gui.anvilcraft.category.decay");
        this.randomTickTooltip = Component.translatable("gui.anvilcraft.category.decay.random_tick");
        this.centerTooltip = Component.translatable("gui.anvilcraft.category.decay.center")
            .withStyle(ChatFormatting.GOLD);
        this.aroundTooltip = Component.translatable("gui.anvilcraft.category.decay.around")
            .withStyle(ChatFormatting.GOLD);
        this.notConsumedTooltip = Component.translatable("gui.anvilcraft.category.decay.not_consumed")
            .withStyle(ChatFormatting.GOLD);
        this.arrowDefault = JeiRenderHelper.getArrowDefault(helper);
    }

    @Override
    public IRecipeType<DecayRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.DECAY;
    }

    @Override
    public Component getTitle() {
        return this.title;
    }

    @Override
    public int getWidth() {
        return DecayCategory.WIDTH;
    }

    @Override
    public int getHeight() {
        return DecayCategory.HEIGHT;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, DecayRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 8, 84)
            .setSlotName(DecayCategory.CENTER_SLOT)
            .addItemStacks(recipe.centers().stream().map(Block::asItem).map(ItemStack::new).toList())
            .addRichTooltipCallback((recipeSlotView, tooltip) -> tooltip.add(this.centerTooltip));

        if (!recipe.matchingNeighbors().isEmpty()) {
            int count = recipe.matchingNeighbors().size();
            builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 8, 102)
                .addItemStacks(recipe.centers().stream().map(block -> new ItemStack(block, count)).toList())
                .addRichTooltipCallback((recipeSlotView, tooltip) ->
                    tooltip.addAll(List.of(this.aroundTooltip, this.notConsumedTooltip)));
        }
        if (!recipe.fixedNeighbors().isEmpty()) {
            recipe.fixedNeighbors().values().stream().distinct().findFirst().ifPresent(block -> {
                // 封存室会被消耗，作为输入展示；其余固定邻居只是催化剂
                RecipeIngredientRole role = block == ModBlocks.CONFINEMENT_CHAMBER.get()
                                            ? RecipeIngredientRole.INPUT
                                            : RecipeIngredientRole.CRAFTING_STATION;
                builder.addSlot(role, 27, 102)
                    .add(new ItemStack(block, DecayCategory.countFixedNeighbors(recipe, block)))
                    .addRichTooltipCallback((recipeSlotView, tooltip) -> {
                        tooltip.add(this.aroundTooltip);
                        if (block != ModBlocks.CONFINEMENT_CHAMBER.get()) {
                            tooltip.add(this.notConsumedTooltip);
                        }
                    });
            });
        }

        if (recipe.resultTag() != null) {
            for (var holder : RegistryUtil.getRegistry(Registries.BLOCK).getTagOrEmpty(recipe.resultTag())) {
                builder.addOutputSlot().add(holder.value().asItem().getDefaultInstance());
            }
        } else if (recipe.centers().size() == 1 && recipe.results().size() > 1) {
            // 单一中心方块可以衰变出多种产物，逐个占一格便于滚动查看
            recipe.results().stream()
                .map(DecayCategory::getResultStack)
                .forEach(stack -> builder.addOutputSlot().add(stack));
        } else {
            builder.addOutputSlot()
                .addItemStacks(recipe.results().stream().map(DecayCategory::getResultStack).toList());
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, DecayRecipe recipe, IFocusGroup focuses) {
        IRecipeSlotDrawablesView recipeSlots = builder.getRecipeSlots();
        List<IRecipeSlotDrawable> outputSlots = recipeSlots.getSlots(RecipeIngredientRole.OUTPUT);
        IScrollGridWidget scrollGridWidget =
            builder.addScrollGridWidget(outputSlots, DecayCategory.MAX_SHOWN_COLUMN, DecayCategory.MAX_SHOWN_ROW);
        scrollGridWidget.setPosition(
            60,
            4,
            this.getWidth(),
            this.getHeight(),
            HorizontalAlignment.LEFT,
            VerticalAlignment.TOP
        );
    }

    @Override
    public void draw(
        DecayRecipe recipe,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphicsExtractor guiGraphics,
        double mouseX,
        double mouseY
    ) {
        Block center = DecayCategory.getDisplayedCenter(recipe, recipeSlotsView);
        PreviewKey key = new PreviewKey(recipe, center);
        LevelLike level = this.previewCache.computeIfAbsent(key, ignored -> DecayCategory.createPreview(recipe, center));
        RenderSupport.renderLevelLike(level, guiGraphics, 0, 6, 60, 12, 0.5f, false);

        this.slot.draw(guiGraphics, 7, 83);
        if (!recipe.matchingNeighbors().isEmpty()) this.slot.draw(guiGraphics, 7, 101);
        if (!recipe.fixedNeighbors().isEmpty()) this.slot.draw(guiGraphics, 26, 101);
        this.arrowDefault.draw(guiGraphics, 35, 87);
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        DecayRecipe recipe,
        IRecipeSlotsView recipeSlotsView,
        double mouseX,
        double mouseY
    ) {
        if (!DecayCategory.isImmediateDecay(recipe)
            && mouseX >= 5 && mouseX <= 45
            && mouseY >= 15 && mouseY <= 65) {
            tooltip.add(this.randomTickTooltip);
        }
    }

    @Override
    public @Nullable Identifier getIdentifier(DecayRecipe recipe) {
        return recipe.id();
    }

    private static LevelLike createPreview(DecayRecipe recipe, Block center) {
        LevelLike preview = new LevelLike(Minecraft.getInstance().level);
        recipe.matchingNeighbors().forEach(pos -> preview.setBlockState(pos, center.defaultBlockState()));
        recipe.fixedNeighbors().forEach((pos, block) -> preview.setBlockState(pos, block.defaultBlockState()));
        preview.setBlockState(DecayCategory.CENTER_POS, center.defaultBlockState());
        return preview;
    }

    private static Block getDisplayedCenter(DecayRecipe recipe, IRecipeSlotsView recipeSlotsView) {
        return recipeSlotsView.findSlotByName(DecayCategory.CENTER_SLOT)
            .flatMap(IRecipeSlotView::getDisplayedItemStack)
            .map(ItemStack::getItem)
            .filter(BlockItem.class::isInstance)
            .map(BlockItem.class::cast)
            .map(BlockItem::getBlock)
            .orElse(recipe.centers().getFirst());
    }

    private static int countFixedNeighbors(DecayRecipe recipe, Block block) {
        return (int) recipe.fixedNeighbors().values().stream().filter(block::equals).count();
    }

    private static boolean isImmediateDecay(DecayRecipe recipe) {
        Block excitedVoidMatter = ModBlocks.EXCITED_STATE_VOID_MATTER_BLOCK.get();
        return recipe.centers().contains(excitedVoidMatter)
               || recipe.fixedNeighbors().containsValue(excitedVoidMatter);
    }

    private static ItemStack getResultStack(Block block) {
        if (block == Blocks.LAVA) return Items.LAVA_BUCKET.getDefaultInstance();
        return new ItemStack(block);
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(AnvilCraftJeiPlugin.DECAY, DecayRecipe.getAllRecipes());
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(
            AnvilCraftJeiPlugin.DECAY,
            ModBlocks.VOID_MATTER_BLOCK,
            ModBlocks.EXCITED_STATE_VOID_MATTER_BLOCK,
            ModBlocks.CONFINEMENT_CHAMBER,
            ModBlocks.PLUTONIUM_BLOCK,
            ModBlocks.URANIUM_BLOCK,
            ModBlocks.LEAD_BLOCK
        );
    }

    private record PreviewKey(DecayRecipe recipe, Block center) {
    }
}
