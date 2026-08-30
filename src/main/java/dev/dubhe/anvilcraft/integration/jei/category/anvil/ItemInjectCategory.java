package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.workstation.GiantAnvilBlock;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.util.JeiBlockIngredientUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiItemUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemInjectRecipe;
import dev.dubhe.anvilcraft.util.TooltipUtil;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ItemInjectCategory implements IRecipeCategory<RecipeHolder<ItemInjectRecipe>> {
    private static final String INPUT_BLOCK = "input_block";
    private static final String TRANSCENDIUM_PREFIX = "item_inject/transcendium_";

    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;

    private final IDrawable icon;
    private final IDrawable slotDefault;
    private final IDrawable slotProbability;
    private final Component title;
    private final ITickTimer timer;

    private final IDrawable arrowIn;
    private final IDrawable arrowOut;

    public ItemInjectCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableItemStack(new ItemStack(Items.ANVIL));
        this.slotDefault = JeiRenderHelper.getSlotDefault(helper);
        this.slotProbability = JeiRenderHelper.getSlotProbability(helper);
        this.title = Component.translatable("gui.anvilcraft.category.item_inject");
        this.timer = helper.createTickTimer(30, 60, true);

        this.arrowIn = JeiRenderHelper.getArrowInput(helper);
        this.arrowOut = JeiRenderHelper.getArrowOutput(helper);
    }

    @Override
    public IRecipeHolderType<ItemInjectRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.ITEM_INJECT;
    }

    @Override
    public Component getTitle() {
        return this.title;
    }

    @Override
    public int getWidth() {
        return ItemInjectCategory.WIDTH;
    }

    @Override
    public int getHeight() {
        return ItemInjectCategory.HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<ItemInjectRecipe> recipeHolder, IFocusGroup focuses) {
        ItemInjectRecipe recipe = recipeHolder.value();
        int transcendiumTier = ItemInjectCategory.getTranscendiumTier(recipeHolder);
        if (transcendiumTier >= 0) {
            IRecipeSlotBuilder inputSlot = builder.addSlot(
                RecipeIngredientRole.INPUT,
                JeiSlotUtil.INPUT_X,
                JeiSlotUtil.DEFAULT_Y
            )
                .add(Ingredient.of(Arrays.stream(recipe.getInputItems().getFirst().getItems())
                    .map(template -> template.item().value())));
            inputSlot.addRichTooltipCallback((slotView, tooltip) -> tooltip.add(Component.translatable(
                "gui.anvilcraft.category.item_inject.transcendium.enchantments",
                switch (transcendiumTier) {
                    case 0 -> "0";
                    case 1 -> "1-10";
                    case 2 -> "11-14";
                    case 3 -> "15";
                    default -> "16+";
                }
            ).withStyle(ChatFormatting.GOLD)));
        } else {
            JeiItemUtil.addDefaultInputSlots(builder, recipe.getInputItems());
        }
        JeiBlockIngredientUtil.addInputSlot(builder, ItemInjectCategory.INPUT_BLOCK, 72, 34, 18, 19, recipe.getFirstInputBlock());
        if (transcendiumTier >= 0) {
            ItemInjectCategory.addTranscendiumOutputSlots(builder, recipe, transcendiumTier);
        } else {
            ItemInjectCategory.addOutputSlots(builder, recipe);
        }
        if (!recipe.getResultBlocks().isEmpty()) {
            JeiBlockIngredientUtil.addSlot(
                builder,
                RecipeIngredientRole.OUTPUT,
                "output_block",
                124,
                ItemInjectCategory.getOutputBlockSlotY(recipe, transcendiumTier),
                16,
                18,
                recipe.getFirstResultBlock().state().getBlock()
            );
        }
    }

    @Override
    public void createRecipeExtras(
        IRecipeExtrasBuilder builder, RecipeHolder<ItemInjectRecipe> recipeHolder, IFocusGroup focuses
    ) {
        JeiBlockIngredientUtil.suppressHoverOverlays(builder);
    }

    @Override
    public void draw(
        RecipeHolder<ItemInjectRecipe> recipeHolder,
        IRecipeSlotsView view,
        GuiGraphicsExtractor graphics,
        double mouseX,
        double mouseY
    ) {
        ItemInjectRecipe recipe = recipeHolder.value();

        List<BlockState> input = recipe.getFirstInputBlock().constructStatesForRender();
        if (input.isEmpty()) return;
        BlockState renderedState = JeiBlockIngredientUtil.getDisplayedState(view, ItemInjectCategory.INPUT_BLOCK, input)
            .orElse(input.getFirst());
        if (renderedState.getBlock() instanceof GiantAnvilBlock) {
            RenderSupport.render3x3Block(graphics, renderedState, 61, 24, 40);
        } else {
            int inputScale = JeiBlockIngredientUtil.getRenderablePreviewScale(renderedState, 20);
            RenderSupport.renderBlock(graphics, renderedState, 71, 35, inputScale);
        }
        
        int anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(this.timer);
        RenderSupport.renderBlock(graphics, Blocks.ANVIL.defaultBlockState(), 71, 17 + anvilYOffset, 20);

        this.arrowIn.draw(graphics, 54, 30);
        this.arrowOut.draw(graphics, 92, 29);

        JeiSlotUtil.drawDefaultInputSlots(graphics, this.slotDefault, recipe.getInputItems().size());
        int transcendiumTier = ItemInjectCategory.getTranscendiumTier(recipeHolder);
        if (transcendiumTier >= 0) {
            this.drawTranscendiumOutputSlots(graphics, transcendiumTier);
        } else if (!recipe.getResultItems().isEmpty()) {
            IDrawable outputSlot = JeiRecipeUtil.isChance(recipe.getResultItems())
                                   ? this.slotProbability
                                   : this.slotDefault;
            if (recipe.getResultBlocks().isEmpty()) {
                JeiSlotUtil.drawDefaultOutputSlots(graphics, outputSlot, recipe.getResultItems().size());
            } else {
                for (int i = 0; i < recipe.getResultItems().size(); i++) {
                    outputSlot.draw(graphics, 106 + i * 19, 14);
                }
            }
        }

        if (!recipe.getResultBlocks().isEmpty()) {
            BlockState resultState = JeiBlockIngredientUtil.getRenderablePreviewState(recipe.getFirstResultBlock().state());
            int resultScale = JeiBlockIngredientUtil.getRenderablePreviewScale(resultState, 20);
            int resultY = transcendiumTier >= 0
                          ? 39
                          : recipe.getResultItems().isEmpty() ? 24 : 43;
            RenderSupport.renderBlock(graphics, resultState, 122, resultY, resultScale);
        }
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        RecipeHolder<ItemInjectRecipe> recipeHolder,
        IRecipeSlotsView view,
        double mouseX,
        double mouseY
    ) {
        ItemInjectRecipe recipe = recipeHolder.value();
        Identifier id = this.getIdentifier(recipeHolder);
        if (MathUtil.isInRange(mouseX, mouseY, 72, 34, 90, 53)) {
            List<BlockState> states = recipe.getFirstInputBlock().constructStatesForRender();
            if (!states.isEmpty()) {
                tooltip.addAll(TooltipUtil.tooltip(states.getFirst().getBlock()));
            }
        }
        int outputY = ItemInjectCategory.getOutputBlockSlotY(recipe, ItemInjectCategory.getTranscendiumTier(recipeHolder));
        if (!recipe.getResultBlocks().isEmpty()
            && MathUtil.isInRange(mouseX, mouseY, 124, outputY, 140, outputY + 18)) {
            Block block = recipe.getFirstResultBlock().state().getBlock();
            if (id != null) {
                tooltip.addAll(TooltipUtil.recipeIDTooltip(block, id));
            } else {
                tooltip.addAll(TooltipUtil.tooltip(block));
            }
        }
    }

    private static void addOutputSlots(IRecipeLayoutBuilder builder, ItemInjectRecipe recipe) {
        if (recipe.getResultItems().isEmpty()) return;
        if (recipe.getResultBlocks().isEmpty()) {
            JeiItemUtil.addDefaultOutputSlots(builder, recipe.getResultItems());
            return;
        }
        for (int index = 0; index < recipe.getResultItems().size(); index++) {
            ItemInjectCategory.addOutputSlot(builder, 107 + index * 19, 15, recipe.getResultItems().get(index));
        }
    }

    private static void addTranscendiumOutputSlots(
        IRecipeLayoutBuilder builder,
        ItemInjectRecipe recipe,
        int tier
    ) {
        List<ChanceItemStack> results = recipe.getResultItems();
        switch (tier) {
            case 0 -> ItemInjectCategory.addOutputSlot(builder, 125, 24, results.getFirst());
            case 1 -> {
                ItemInjectCategory.addOutputSlot(builder, 116, 15, results.get(0))
                    .addRichTooltipCallback((slotView, tooltip) -> tooltip.add(Component.translatable(
                        "gui.anvilcraft.category.item_inject.transcendium.chance"
                    ).withStyle(ChatFormatting.GRAY)));
                ItemInjectCategory.addOutputSlot(builder, 134, 15, results.get(1));
                ItemInjectCategory.addOutputSlot(builder, 116, 33, results.get(2))
                    .addRichTooltipCallback((slotView, tooltip) -> tooltip.add(Component.translatable(
                        "gui.anvilcraft.category.item_inject.transcendium.amount_x3"
                    ).withStyle(ChatFormatting.GOLD)));
            }
            case 2 -> {
                ItemInjectCategory.addOutputSlot(builder, 116, 15, results.get(0));
                ItemInjectCategory.addOutputSlot(builder, 134, 15, results.get(1));
                ItemInjectCategory.addOutputSlot(builder, 116, 33, results.get(2))
                    .addRichTooltipCallback((slotView, tooltip) -> tooltip.add(Component.translatable(
                        "gui.anvilcraft.category.item_inject.transcendium.amount_x3"
                    ).withStyle(ChatFormatting.GOLD)));
            }
            case 3 -> ItemInjectCategory.addOutputSlot(builder, 125, 15, results.getFirst());
            case 4 -> {
                ItemInjectCategory.addOutputSlot(builder, 116, 15, results.get(0));
                ItemInjectCategory.addOutputSlot(builder, 134, 15, results.get(1))
                    .addRichTooltipCallback((slotView, tooltip) -> tooltip.add(Component.translatable(
                        "gui.anvilcraft.category.item_inject.transcendium.amount_x1"
                    ).withStyle(ChatFormatting.GOLD)));
            }
            default -> throw new AssertionError(tier);
        }
    }

    private static IRecipeSlotBuilder addOutputSlot(
        IRecipeLayoutBuilder builder,
        int x,
        int y,
        ChanceItemStack result
    ) {
        ItemStackTemplate template = result.stack().withCount(result.getMaxCount());
        IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.OUTPUT, x, y).add(template);
        JeiRecipeUtil.addTooltips(slot, result.getMaxCount(), result.count());
        return slot;
    }

    private void drawTranscendiumOutputSlots(GuiGraphicsExtractor graphics, int tier) {
        switch (tier) {
            case 0 -> this.slotDefault.draw(graphics, 124, 23);
            case 1 -> {
                this.slotProbability.draw(graphics, 115, 14);
                this.slotDefault.draw(graphics, 133, 14);
                this.slotDefault.draw(graphics, 115, 32);
            }
            case 2 -> {
                this.slotDefault.draw(graphics, 115, 14);
                this.slotDefault.draw(graphics, 133, 14);
                this.slotDefault.draw(graphics, 115, 32);
            }
            case 3 -> this.slotDefault.draw(graphics, 124, 14);
            case 4 -> {
                this.slotDefault.draw(graphics, 115, 14);
                this.slotDefault.draw(graphics, 133, 14);
            }
            default -> throw new AssertionError(tier);
        }
    }

    private static int getOutputBlockSlotY(ItemInjectRecipe recipe, int transcendiumTier) {
        if (transcendiumTier >= 0) return 39;
        return recipe.getResultItems().isEmpty() ? 24 : 43;
    }

    private static int getTranscendiumTier(RecipeHolder<ItemInjectRecipe> recipeHolder) {
        String path = recipeHolder.id().identifier().getPath();
        if (!path.startsWith(ItemInjectCategory.TRANSCENDIUM_PREFIX)) return -1;
        return Integer.parseInt(path.substring(ItemInjectCategory.TRANSCENDIUM_PREFIX.length()));
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        List<RecipeHolder<ItemInjectRecipe>> recipes = new ArrayList<>(
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.ITEM_INJECT.get())
        );
        recipes.addAll(ItemInjectCategory.getTranscendiumRecipes());
        registration.addRecipes(
            AnvilCraftJeiPlugin.ITEM_INJECT,
            recipes
        );
    }

    private static List<RecipeHolder<ItemInjectRecipe>> getTranscendiumRecipes() {
        List<RecipeHolder<ItemInjectRecipe>> recipes = new ArrayList<>();
        for (int index = 0; index < 5; index++) {
            ItemInjectRecipe.Builder builder = ItemInjectRecipe.builder()
                .inputBlock(ModBlocks.OVERHEATED_EMBER_METAL_BLOCK.get())
                .requires(ModItems.CHARGED_NEUTRONIUM_INGOT);
            switch (index) {
                case 0 -> builder.result(ModItems.TRANSCENDIUM_INGOT, 4);
                case 1 -> builder.result(ModItems.NEUTRONIUM_INGOT)
                    .result(ModItems.TRANSCENDIUM_INGOT, 4)
                    .result(ModItems.TRANSCENDIUM_NUGGET, 3);
                case 2 -> builder.result(ModItems.NEUTRONIUM_INGOT)
                    .result(ModItems.TRANSCENDIUM_INGOT, 4)
                    .result(ModItems.TRANSCENDIUM_NUGGET, 33);
                case 3 -> builder.result(ModItems.NEUTRONIUM_INGOT)
                    .resultBlock(ModBlocks.TRANSCENDIUM_BLOCK);
                case 4 -> builder.result(ModItems.NEUTRONIUM_INGOT)
                    .result(ModItems.TRANSCENDIUM_NUGGET, 16)
                    .resultBlock(ModBlocks.TRANSCENDIUM_BLOCK);
                default -> throw new AssertionError(index);
            }
            recipes.add(new RecipeHolder<>(
                ResourceKey.create(
                    Registries.RECIPE,
                    AnvilCraft.of("item_inject/transcendium_%d".formatted(index))
                ),
                builder.buildRecipe()
            ));
        }
        return recipes;
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilProcessingCatalysts(registration, AnvilCraftJeiPlugin.ITEM_INJECT);
    }
}
