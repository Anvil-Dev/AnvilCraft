package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.GiantAnvilBlock;
import dev.dubhe.anvilcraft.block.container.storage.HyperdimensionStorageStationBlock;
import dev.dubhe.anvilcraft.block.container.storage.LargeCrateBlock;
import dev.dubhe.anvilcraft.block.container.storage.ShulkerContainerBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.block.state.OpenedCube3x3PartHalf;
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
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;

public class ItemInjectCategory implements IRecipeCategory<RecipeHolder<ItemInjectRecipe>> {
    private static final String INPUT_BLOCK = "input_block";
    private static final String TRANSCENDIUM_PREFIX = "item_inject/transcendium_";

    private static final ResourceLocation CRATE_TO_CONTAINER = AnvilCraft.of("item_inject/crate_to_container");
    private static final ResourceLocation CONTAINER_TO_STATION = AnvilCraft.of("item_inject/container_to_station");

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
    public RecipeType<RecipeHolder<ItemInjectRecipe>> getRecipeType() {
        return AnvilCraftJeiPlugin.ITEM_INJECT;
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
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(
        IRecipeLayoutBuilder builder, RecipeHolder<ItemInjectRecipe> recipeHolder, IFocusGroup focuses) {
        ItemInjectRecipe recipe = recipeHolder.value();
        int transcendiumTier = getTranscendiumTier(recipeHolder);
        if (transcendiumTier >= 0) {
            builder.addSlot(RecipeIngredientRole.INPUT, JeiSlotUtil.INPUT_X, JeiSlotUtil.DEFAULT_Y)
                .addIngredients(Ingredient.of(recipe.getInputItems().getFirst().getItems()))
                .addRichTooltipCallback((slotView, tooltip) -> tooltip.add(Component.translatable(
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
        JeiBlockIngredientUtil.addInputSlot(builder, INPUT_BLOCK, 72, 34, 18, 19, recipe.getFirstInputBlock());
        if (recipeHolder.id().equals(ItemInjectCategory.CONTAINER_TO_STATION)) {
            builder.addSlot(RecipeIngredientRole.INPUT, 31, 43)
                .addItemStack(ModBlocks.SPACE_OVERCOMPRESSOR.asStack(4))
                .addRichTooltipCallback((slotView, tooltip) -> tooltip.add(Component.translatable(
                    "gui.anvilcraft.category.container_upgrade.requires_expansion"
                ).withStyle(ChatFormatting.GOLD)));
        }
        if (transcendiumTier >= 0) {
            addTranscendiumOutputSlots(builder, recipe, transcendiumTier);
        } else {
            addOutputSlots(builder, recipe);
        }
        if (!recipe.getResultBlocks().isEmpty()) {
            int y = getOutputBlockSlotY(recipe, transcendiumTier);
            JeiBlockIngredientUtil.addSlot(
                builder,
                RecipeIngredientRole.OUTPUT,
                "output_block",
                124,
                y,
                16,
                18,
                recipe.getFirstResultBlock().state().getBlock()
            );
        }
    }

    @Override
    public void createRecipeExtras(
        IRecipeExtrasBuilder builder, RecipeHolder<ItemInjectRecipe> recipeHolder, IFocusGroup focuses) {
        JeiBlockIngredientUtil.suppressHoverOverlays(builder);
    }

    @Override
    public void draw(
        RecipeHolder<ItemInjectRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY) {
        ItemInjectRecipe recipe = recipeHolder.value();
        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(this.timer);
        RenderSupport.renderBlock(
            guiGraphics,
            Blocks.ANVIL.defaultBlockState(),
            81,
            22 + anvilYOffset,
            20,
            12,
            RenderSupport.SINGLE_BLOCK);

        List<BlockState> input = recipe.getFirstInputBlock().constructStatesForRender();
        if (input.isEmpty()) return;
        // 输入方块同样要取模型承载状态：巨型铁砧可作输入（如增幅器配方），
        // 默认部件状态是空模型，不映射则整个输入方块都不显示
        BlockState renderedState = JeiBlockIngredientUtil.getRenderablePreviewState(
            JeiBlockIngredientUtil.getDisplayedState(recipeSlotsView, INPUT_BLOCK, input)
                .orElse(input.getFirst())
        );
        boolean giantAnvil = renderedState.getBlock() instanceof GiantAnvilBlock;
        boolean containerUpgrade = isContainerUpgrade(recipeHolder);
        if (containerUpgrade) {
            renderedState = getContainerPreviewState(renderedState);
        }
        int inputScale = giantAnvil || containerUpgrade ? 8 : JeiBlockIngredientUtil.getRenderablePreviewScale(renderedState, 12);
        int inputY = giantAnvil || containerUpgrade ? 44 : 40;
        RenderSupport.renderBlock(guiGraphics, renderedState, 81, inputY, 10, inputScale, RenderSupport.SINGLE_BLOCK);

        this.arrowIn.draw(guiGraphics, 54, 30);
        this.arrowOut.draw(guiGraphics, 92, 29);

        JeiSlotUtil.drawDefaultInputSlots(guiGraphics, this.slotDefault, recipe.getInputItems().size());
        if (recipeHolder.id().equals(CONTAINER_TO_STATION)) {
            this.slotDefault.draw(guiGraphics, 30, 42);
        }
        int transcendiumTier = getTranscendiumTier(recipeHolder);
        if (transcendiumTier >= 0) {
            this.drawTranscendiumOutputSlots(guiGraphics, transcendiumTier);
        } else if (!recipe.getResultItems().isEmpty()) {
            List<ChanceItemStack> results = recipe.getResultItems();
            IntFunction<IDrawable> outputSlot = JeiRecipeUtil.outputSlotFor(results, this.slotDefault, this.slotProbability);
            if (recipe.getResultBlocks().isEmpty()) {
                JeiSlotUtil.drawDefaultOutputSlots(guiGraphics, outputSlot, results.size());
            } else {
                for (int i = 0; i < results.size(); i++) {
                    outputSlot.apply(i).draw(guiGraphics, 106 + i * 19, 14);
                }
            }
        }

        if (!recipe.getResultBlocks().isEmpty()) {
            BlockState resultState = JeiBlockIngredientUtil.getRenderablePreviewState(recipe.getFirstResultBlock().state());
            if (containerUpgrade) {
                resultState = getContainerPreviewState(resultState);
            }
            int resultScale = containerUpgrade ? 8 : JeiBlockIngredientUtil.getRenderablePreviewScale(resultState, 12);
            int resultY = transcendiumTier >= 0
                          ? 45
                          : recipe.getResultItems().isEmpty() ? 30 : 49;
            RenderSupport.renderBlock(
                guiGraphics,
                resultState,
                133,
                resultY,
                0,
                resultScale,
                RenderSupport.SINGLE_BLOCK
            );
        }
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        RecipeHolder<ItemInjectRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        double mouseX,
        double mouseY) {
        ItemInjectRecipe recipe = recipeHolder.value();
        ResourceLocation id = getRegistryName(recipeHolder);
        if (mouseX >= 72 && mouseX <= 90) {
            if (mouseY >= 34 && mouseY <= 53) {
                List<BlockState> states = recipe.getFirstInputBlock().constructStatesForRender();
                if (!states.isEmpty()) {
                    tooltip.addAll(TooltipUtil.tooltip(states.getFirst().getBlock()));
                    if (ItemInjectCategory.isContainerUpgrade(recipeHolder)) {
                        tooltip.add(Component.translatable("gui.anvilcraft.category.container_upgrade.drop_on_top")
                            .withStyle(ChatFormatting.GOLD));
                        tooltip.add(Component.translatable("gui.anvilcraft.category.container_upgrade.strike")
                            .withStyle(ChatFormatting.GOLD));
                    }
                    if (recipeHolder.id().equals(ItemInjectCategory.CONTAINER_TO_STATION)) {
                        tooltip.add(Component.translatable("gui.anvilcraft.category.container_upgrade.requires_expansion")
                            .withStyle(ChatFormatting.GOLD));
                    }
                }
            }
        }
        if (mouseX >= 124 && mouseX <= 140) {
            int outputY = getOutputBlockSlotY(recipe, getTranscendiumTier(recipeHolder));
            if (mouseY >= outputY && mouseY <= outputY + 18 && !recipe.getResultBlocks().isEmpty()) {
                Block block = recipe.getFirstResultBlock().state().getBlock();
                if (id != null) {
                    tooltip.addAll(TooltipUtil.recipeIDTooltip(block, id));
                } else {
                    tooltip.addAll(TooltipUtil.tooltip(block));
                }
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
            addOutputSlot(builder, 107 + index * 19, 15, recipe.getResultItems().get(index));
        }
    }

    private static void addTranscendiumOutputSlots(
        IRecipeLayoutBuilder builder,
        ItemInjectRecipe recipe,
        int tier
    ) {
        List<ChanceItemStack> results = recipe.getResultItems();
        switch (tier) {
            case 0 -> addOutputSlot(builder, 125, 24, results.getFirst());
            case 1 -> {
                addOutputSlot(builder, 116, 15, results.get(0))
                    .addRichTooltipCallback((slotView, tooltip) -> tooltip.add(Component.translatable(
                        "gui.anvilcraft.category.item_inject.transcendium.chance"
                    ).withStyle(ChatFormatting.GRAY)));
                addOutputSlot(builder, 134, 15, results.get(1));
                addOutputSlot(builder, 116, 33, results.get(2))
                    .addRichTooltipCallback((slotView, tooltip) -> tooltip.add(Component.translatable(
                        "gui.anvilcraft.category.item_inject.transcendium.amount_x3"
                    ).withStyle(ChatFormatting.GOLD)));
            }
            case 2 -> {
                addOutputSlot(builder, 116, 15, results.get(0));
                addOutputSlot(builder, 134, 15, results.get(1));
                addOutputSlot(builder, 116, 33, results.get(2))
                    .addRichTooltipCallback((slotView, tooltip) -> tooltip.add(Component.translatable(
                        "gui.anvilcraft.category.item_inject.transcendium.amount_x3"
                    ).withStyle(ChatFormatting.GOLD)));
            }
            case 3 -> addOutputSlot(builder, 125, 15, results.getFirst());
            case 4 -> {
                addOutputSlot(builder, 116, 15, results.get(0));
                addOutputSlot(builder, 134, 15, results.get(1))
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
        ItemStack stack = result.stack().copyWithCount(result.getMaxCount());
        IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.OUTPUT, x, y).addItemStack(stack);
        JeiRecipeUtil.addTooltips(slot, result.getMaxCount(), result.count());
        return slot;
    }

    private void drawTranscendiumOutputSlots(GuiGraphics guiGraphics, int tier) {
        switch (tier) {
            case 0 -> this.slotDefault.draw(guiGraphics, 124, 23);
            case 1 -> {
                this.slotProbability.draw(guiGraphics, 115, 14);
                this.slotDefault.draw(guiGraphics, 133, 14);
                this.slotDefault.draw(guiGraphics, 115, 32);
            }
            case 2 -> {
                this.slotDefault.draw(guiGraphics, 115, 14);
                this.slotDefault.draw(guiGraphics, 133, 14);
                this.slotDefault.draw(guiGraphics, 115, 32);
            }
            case 3 -> this.slotDefault.draw(guiGraphics, 124, 14);
            case 4 -> {
                this.slotDefault.draw(guiGraphics, 115, 14);
                this.slotDefault.draw(guiGraphics, 133, 14);
            }
            default -> throw new AssertionError(tier);
        }
    }

    private static int getOutputBlockSlotY(ItemInjectRecipe recipe, int transcendiumTier) {
        if (transcendiumTier >= 0) return 39;
        return recipe.getResultItems().isEmpty() ? 24 : 43;
    }

    private static int getTranscendiumTier(RecipeHolder<ItemInjectRecipe> recipeHolder) {
        String path = recipeHolder.id().getPath();
        if (!path.startsWith(TRANSCENDIUM_PREFIX)) return -1;
        return Integer.parseInt(path.substring(TRANSCENDIUM_PREFIX.length()));
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        List<RecipeHolder<ItemInjectRecipe>> recipes = new ArrayList<>(
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.ITEM_INJECT_TYPE.get())
        );
        recipes.addAll(getTranscendiumRecipes());
        recipes.addAll(getContainerUpgradeRecipes());
        registration.addRecipes(
            AnvilCraftJeiPlugin.ITEM_INJECT,
            recipes);
    }

    private static BlockState getContainerPreviewState(BlockState state) {
        return switch (state.getBlock()) {
            case LargeCrateBlock ignored -> state.setValue(LargeCrateBlock.HALF, Cube3x3PartHalf.MID_CENTER);
            case ShulkerContainerBlock ignored -> state.setValue(ShulkerContainerBlock.HALF, OpenedCube3x3PartHalf.MID_CENTER);
            case HyperdimensionStorageStationBlock ignored -> {
                yield state.setValue(HyperdimensionStorageStationBlock.HALF, Cube3x3PartHalf.MID_CENTER);
            }
            default -> throw new IllegalArgumentException("Unsupported container preview: " + state);
        };
    }

    private static boolean isContainerUpgrade(RecipeHolder<ItemInjectRecipe> recipeHolder) {
        ResourceLocation id = recipeHolder.id();
        return id.equals(ItemInjectCategory.CRATE_TO_CONTAINER) || id.equals(ItemInjectCategory.CONTAINER_TO_STATION);
    }

    private static List<RecipeHolder<ItemInjectRecipe>> getContainerUpgradeRecipes() {
        return List.of(
            new RecipeHolder<>(
                ItemInjectCategory.CRATE_TO_CONTAINER,
                ItemInjectRecipe.builder()
                    .inputBlock(ModBlocks.LARGE_CRATE)
                    .requires(ModBlocks.SPACE_OVERCOMPRESSOR)
                    .requires(Blocks.NETHERITE_BLOCK, 6)
                    .resultBlock(ModBlocks.SHULKER_CONTAINER)
                    .buildRecipe()
            ),
            new RecipeHolder<>(
                ItemInjectCategory.CONTAINER_TO_STATION,
                ItemInjectRecipe.builder()
                    .inputBlock(ModBlocks.SHULKER_CONTAINER)
                    .requires(ModBlocks.SINGULARITY_CRYSTAL)
                    .requires(ModBlocks.HYPERCUBE, 16)
                    .resultBlock(ModBlocks.HYPERDIMENSION_STORAGE_STATION)
                    .buildRecipe()
            )
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
                AnvilCraft.of("item_inject/transcendium_%d".formatted(index)),
                builder.buildRecipe()
            ));
        }
        return recipes;
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilProcessingCatalysts(registration, AnvilCraftJeiPlugin.ITEM_INJECT);
        registration.addRecipeCatalyst(ModBlocks.LARGE_CRATE.asStack(), AnvilCraftJeiPlugin.ITEM_INJECT);
        registration.addRecipeCatalyst(ModBlocks.SPACE_OVERCOMPRESSOR.asStack(), AnvilCraftJeiPlugin.ITEM_INJECT);
        registration.addRecipeCatalyst(Blocks.NETHERITE_BLOCK.asItem().getDefaultInstance(), AnvilCraftJeiPlugin.ITEM_INJECT);
        registration.addRecipeCatalyst(ModBlocks.SHULKER_CONTAINER.asStack(), AnvilCraftJeiPlugin.ITEM_INJECT);
        registration.addRecipeCatalyst(ModBlocks.SINGULARITY_CRYSTAL.asStack(), AnvilCraftJeiPlugin.ITEM_INJECT);
        registration.addRecipeCatalyst(ModBlocks.HYPERCUBE.asStack(), AnvilCraftJeiPlugin.ITEM_INJECT);
    }
}
