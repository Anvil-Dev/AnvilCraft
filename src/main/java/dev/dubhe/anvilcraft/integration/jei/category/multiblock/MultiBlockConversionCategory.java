package dev.dubhe.anvilcraft.integration.jei.category.multiblock;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.GiantAnvilBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.block.state.GiantAnvilCube;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.JeiButton;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.TextureConstants;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockConversionRecipe;
import dev.dubhe.anvilcraft.util.LevelLike;
import dev.dubhe.anvilcraft.util.RecipeUtil;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiBlockConversionCategory implements IRecipeCategory<RecipeHolder<MultiblockConversionRecipe>> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 136;
    public static final int SCALE_FAC_OVERVIEW = 55;
    public static final int SCALE_FAC_LARGE = 96;
    private static final Component TITLE = Component.translatable("gui.anvilcraft.category.multiblock_conversion");
    private static final Component ALL_LAYERS =
        Component.translatable("gui.anvilcraft.category.multiblock.all_layers");
    private final Map<RecipeHolder<MultiblockConversionRecipe>, LevelLike> cacheInput = new HashMap<>();
    private final Map<RecipeHolder<MultiblockConversionRecipe>, LevelLike> cacheOutput = new HashMap<>();

    private static final Comparator<ItemStack> BY_COUNT_DECREASING =
        Comparator.comparing(ItemStack::getCount).thenComparing(ItemStack::getDescriptionId).reversed();

    private final IDrawable icon;
    private final IDrawable slot;
    private final IDrawable layerUp;
    private final IDrawable layerUpHovered;
    private final IDrawable layerDown;
    private final IDrawable layerDownHovered;
    private final IDrawable modeOverview;
    private final IDrawable modeInput;
    private final IDrawable modeOutput;
    private final IDrawable modeOverviewHovered;
    private final IDrawable modeInputHovered;
    private final IDrawable modeOutputHovered;
    private final IDrawable renderSwitchOn;
    private final IDrawable renderSwitchOff;
    private final IDrawable arrowOut;
    private final IDrawable conversion;
    private final ITickTimer timer;

    private DisplayMode displayMode = DisplayMode.OVERVIEW;

    public MultiBlockConversionCategory(IGuiHelper helper) {
        icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.GIANT_ANVIL));
        arrowOut = JeiRenderHelper.getArrowDefault(helper);
        slot = JeiRenderHelper.getSlotDefault(helper);
        timer = helper.createTickTimer(30, 60, true);
        conversion = helper.drawableBuilder(TextureConstants.BLOCK_CONVERSION, 0, 0, 594, 418)
            .setTextureSize(594, 418)
            .build();
        layerUp = helper.drawableBuilder(
                AnvilCraft.of("textures/gui/container/insight/insight_layer_up.png"), 0, 0, 10, 10)
            .setTextureSize(10, 20)
            .build();
        layerUpHovered = helper.drawableBuilder(
                AnvilCraft.of("textures/gui/container/insight/insight_layer_up.png"), 0, 10, 10, 10)
            .setTextureSize(10, 20)
            .build();
        layerDown = helper.drawableBuilder(
                AnvilCraft.of("textures/gui/container/insight/insight_layer_down.png"), 0, 0, 10, 10)
            .setTextureSize(10, 20)
            .build();
        layerDownHovered = helper.drawableBuilder(
                AnvilCraft.of("textures/gui/container/insight/insight_layer_down.png"), 0, 10, 10, 10)
            .setTextureSize(10, 20)
            .build();
        modeOverview = helper.drawableBuilder(
                AnvilCraft.of("textures/gui/container/insight/insight_display_modes.png"), 0, 0, 10, 10)
            .setTextureSize(30, 20)
            .build();
        modeOverviewHovered = helper.drawableBuilder(
                AnvilCraft.of("textures/gui/container/insight/insight_display_modes.png"), 0, 10, 10, 10)
            .setTextureSize(30, 20)
            .build();
        modeInput = helper.drawableBuilder(
                AnvilCraft.of("textures/gui/container/insight/insight_display_modes.png"), 10, 0, 10, 10)
            .setTextureSize(30, 20)
            .build();
        modeInputHovered = helper.drawableBuilder(
                AnvilCraft.of("textures/gui/container/insight/insight_display_modes.png"), 10, 10, 10, 10)
            .setTextureSize(30, 20)
            .build();
        modeOutput = helper.drawableBuilder(
                AnvilCraft.of("textures/gui/container/insight/insight_display_modes.png"), 20, 0, 10, 10)
            .setTextureSize(30, 20)
            .build();
        modeOutputHovered = helper.drawableBuilder(
                AnvilCraft.of("textures/gui/container/insight/insight_display_modes.png"), 20, 10, 10, 10)
            .setTextureSize(30, 20)
            .build();
        renderSwitchOff = helper.drawableBuilder(
                AnvilCraft.of("textures/gui/container/insight/insight_layer_switch.png"), 0, 0, 10, 10)
            .setTextureSize(10, 20)
            .build();
        renderSwitchOn = helper.drawableBuilder(
                AnvilCraft.of("textures/gui/container/insight/insight_layer_switch.png"), 0, 10, 10, 10)
            .setTextureSize(10, 20)
            .build();
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.MULTIBLOCK_CONVERSION,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.MULTIBLOCK_CONVERSION_TYPE.get())
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ModBlocks.GIANT_ANVIL.asStack(), AnvilCraftJeiPlugin.MULTIBLOCK_CONVERSION);
        registration.addRecipeCatalyst(ModBlocks.TRANSPARENT_CRAFTING_TABLE.asStack(), AnvilCraftJeiPlugin.MULTIBLOCK_CONVERSION);
        registration.addRecipeCatalyst(new ItemStack(Blocks.CRAFTING_TABLE), AnvilCraftJeiPlugin.MULTIBLOCK_CONVERSION);
    }

    @Override
    public RecipeType<RecipeHolder<MultiblockConversionRecipe>> getRecipeType() {
        return AnvilCraftJeiPlugin.MULTIBLOCK_CONVERSION;
    }

    @Override
    public Component getTitle() {
        return TITLE;
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
        return icon;
    }

    @Override
    public void setRecipe(
        IRecipeLayoutBuilder builder,
        RecipeHolder<MultiblockConversionRecipe> recipe,
        IFocusGroup focuses
    ) {
        cacheInput.computeIfAbsent(
            recipe,
            it -> RecipeUtil.asLevelLike(it.value().getInputPattern())
        );
        cacheOutput.computeIfAbsent(
            recipe,
            it -> RecipeUtil.asLevelLike(it.value().getOutputPattern())
        );

        List<ItemStack> inputItems = recipe.value().getInputPattern().toIngredientList();
        inputItems.sort(BY_COUNT_DECREASING);

        for (int i = 0; i < inputItems.size(); i++) {
            ItemStack stack = inputItems.get(i);
            builder.addSlot(RecipeIngredientRole.INPUT, this.inputSlotPosX(i) + 1, this.slotPosY(i) + 1)
                .addItemStack(stack);
        }

        List<ItemStack> outputItems = recipe.value().getOutputPattern().toIngredientList();
        outputItems.sort(BY_COUNT_DECREASING);

        for (int i = 0; i < outputItems.size(); i++) {
            ItemStack stack = outputItems.get(i);
            builder.addSlot(RecipeIngredientRole.OUTPUT, this.outputSlotPosX(i) + 1, this.slotPosY(i) + 1)
                .addItemStack(stack);
        }
    }

    @Override
    public void draw(
        RecipeHolder<MultiblockConversionRecipe> recipe,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        PoseStack pose = guiGraphics.pose();
        Component currentModeTooltip =
            Component.translatable(
                "gui.anvilcraft.category.multiblock_conversion.current_mode",
                this.displayMode.getDiscription()
            );
        pose.pushPose();
        pose.scale(0.8f, 0.8f, 0.8f);
        int textX = Math.round(WIDTH / 0.8f - minecraft.font.width(currentModeTooltip) - 5);
        guiGraphics.drawString(minecraft.font, currentModeTooltip, textX, 0, 0xFF000000, false);
        pose.popPose();
        this.displayModeButton(mouseX, mouseY).draw(guiGraphics, 149, 10);

        LevelLike input = cacheInput.computeIfAbsent(
            recipe,
            it -> RecipeUtil.asLevelLike(it.value().getInputPattern())
        );
        LevelLike output = cacheOutput.computeIfAbsent(
            recipe,
            it -> RecipeUtil.asLevelLike(it.value().getOutputPattern())
        );
        LevelLike rendered = input;
        switch (this.displayMode) {
            case OVERVIEW:
                List<IRecipeSlotView> inputSlots = recipeSlotsView.getSlotViews(RecipeIngredientRole.INPUT);
                for (int i = 0; i < inputSlots.size(); i++) {
                    if (inputSlots.get(i) instanceof IRecipeSlotDrawable drawable) {
                        drawable.setPosition(this.inputSlotPosX(i) + 1, this.slotPosY(i) + 1);
                    }
                }
                List<IRecipeSlotView> outputSlots = recipeSlotsView.getSlotViews(RecipeIngredientRole.OUTPUT);
                for (int i = 0; i < outputSlots.size(); i++) {
                    if (outputSlots.get(i) instanceof IRecipeSlotDrawable drawable) {
                        drawable.setPosition(this.outputSlotPosX(i) + 1, this.slotPosY(i) + 1);
                    }
                }
                final boolean modifiedInput = !input.isAllLayersVisible();
                final boolean modifiedOutput = !output.isAllLayersVisible();
                input.setAllLayersVisible(true);
                output.setAllLayersVisible(true);
                RenderSupport.renderLevelLike(input, guiGraphics, 36, 44, SCALE_FAC_OVERVIEW, 2.0f);
                RenderSupport.renderLevelLike(output, guiGraphics, 120, 44, SCALE_FAC_OVERVIEW, 2.0f);
                if (modifiedInput) {
                    input.setAllLayersVisible(false);
                }
                if (modifiedOutput) {
                    output.setAllLayersVisible(false);
                }
                for (int i = 0; i < 12; i++) {
                    slot.draw(guiGraphics, this.inputSlotPosX(i), this.slotPosY(i));
                    slot.draw(guiGraphics, this.outputSlotPosX(i), this.slotPosY(i));
                }
                arrowOut.draw(guiGraphics, 73, 40);
                pose.pushPose();
                pose.scale(0.03f, 0.03f, 1.0f);
                conversion.draw(guiGraphics, 2375, 875);
                pose.popPose();
                float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(timer) / 3;
                RenderSupport.renderBlock(
                    guiGraphics,
                    ModBlocks.GIANT_ANVIL.getDefaultState()
                        .trySetValue(GiantAnvilBlock.HALF, Cube3x3PartHalf.MID_CENTER)
                        .trySetValue(GiantAnvilBlock.CUBE, GiantAnvilCube.CENTER),
                    80,
                    19.8f + anvilYOffset,
                    20,
                    5,
                    RenderSupport.SINGLE_BLOCK
                );
                pose.pushPose();
                pose.scale(0.8f, 0.8f, 1.0f);
                int size = recipe.value().getSize();
                guiGraphics.drawString(
                    minecraft.font,
                    Component.translatable("gui.anvilcraft.category.multiblock.size", size, size),
                    85, 92, 0xFF000000, false
                );
                pose.popPose();
                break;
            case INPUT:
                break;
            case OUTPUT:
                rendered = output;
                break;
            default:
        }
        if (this.displayMode == DisplayMode.OVERVIEW) return;

        for (IRecipeSlotView slotView : recipeSlotsView.getSlotViews()) {
            if (slotView instanceof IRecipeSlotDrawable drawable) {
                drawable.setPosition(-1000, -1000);
            }
        }
        RenderSupport.renderLevelLike(rendered, guiGraphics, 80, 86, SCALE_FAC_LARGE, 2.0f);
        Component component = this.layerTooltip(rendered);
        pose.pushPose();
        pose.scale(0.8f, 0.8f, 0.8f);
        textX = Math.round(WIDTH / 0.8f - minecraft.font.width(component) - 5);
        guiGraphics.drawString(minecraft.font, component, textX, 25, 0xFF000000, false);
        pose.popPose();
        this.renderSwitchButton(rendered).draw(guiGraphics, 125, 30);
        if (!rendered.isAllLayersVisible()) {
            this.layerUpButton(mouseX, mouseY).draw(guiGraphics, 137, 30);
            this.layerDownButton(mouseX, mouseY).draw(guiGraphics, 149, 30);
        }
    }

    private IDrawable renderSwitchButton(LevelLike level) {
        return level.isAllLayersVisible() ? renderSwitchOff : renderSwitchOn;
    }

    private IDrawable layerUpButton(double mouseX, double mouseY) {
        return (mouseX >= 137 && mouseX < 147 && mouseY >= 30 && mouseY < 40) ? layerUpHovered : layerUp;
    }

    private IDrawable layerDownButton(double mouseX, double mouseY) {
        return (mouseX >= 149 && mouseX < 159 && mouseY >= 30 && mouseY < 40) ? layerDownHovered : layerDown;
    }

    private IDrawable displayModeButton(double mouseX, double mouseY) {
        boolean hovered = (mouseX >= 149 && mouseX < 159 && mouseY >= 10 && mouseY < 20);
        return switch (this.displayMode) {
            case OVERVIEW -> hovered ? modeOverviewHovered : modeOverview;
            case INPUT -> hovered ? modeInputHovered : modeInput;
            case OUTPUT -> hovered ? modeOutputHovered : modeOutput;
        };
    }

    private Component layerTooltip(LevelLike level) {
        if (level.isAllLayersVisible()) return ALL_LAYERS;
        return Component.translatable(
            "gui.anvilcraft.category.multiblock.single_layer",
            level.getCurrentVisibleLayer() + 1,
            level.verticalSize()
        );
    }

    private int inputSlotPosX(int i) {
        return (i % 4) * 18;
    }

    private int outputSlotPosX(int i) {
        return (i % 4) * 18 + 88;
    }

    private int slotPosY(int i) {
        return (i / 4) * 18 + 82;
    }

    @Override
    public void createRecipeExtras(
        IRecipeExtrasBuilder builder, RecipeHolder<MultiblockConversionRecipe> recipe, IFocusGroup focuses) {
        builder.addGuiEventListener(new JeiButton<>(
            125,
            30,
            10,
            it -> {
                switch (this.displayMode) {
                    case INPUT:
                        LevelLike inputLevel = this.cacheInput.computeIfAbsent(
                            it,
                            a -> RecipeUtil.asLevelLike(a.value().getInputPattern())
                        );
                        inputLevel.setAllLayersVisible(!inputLevel.isAllLayersVisible());
                        break;
                    case OUTPUT:
                        LevelLike outputLevel = this.cacheOutput.computeIfAbsent(
                            it,
                            a -> RecipeUtil.asLevelLike(a.value().getOutputPattern())
                        );
                        outputLevel.setAllLayersVisible(!outputLevel.isAllLayersVisible());
                        break;
                    default:
                }
            },
            recipe
        ));

        builder.addGuiEventListener(new JeiButton<>(
            137,
            30,
            10,
            it -> {
                switch (this.displayMode) {
                    case INPUT:
                        LevelLike inputLevel = this.cacheInput.computeIfAbsent(
                            it,
                            a -> RecipeUtil.asLevelLike(a.value().getInputPattern())
                        );
                        if (!inputLevel.isAllLayersVisible()) inputLevel.nextLayer();
                        break;
                    case OUTPUT:
                        LevelLike outputLevel = this.cacheOutput.computeIfAbsent(
                            it,
                            a -> RecipeUtil.asLevelLike(a.value().getOutputPattern())
                        );
                        if (!outputLevel.isAllLayersVisible()) outputLevel.nextLayer();
                        break;
                    default:
                }
            },
            recipe
        ));

        builder.addGuiEventListener(new JeiButton<>(
            149,
            30,
            10,
            it -> {
                switch (this.displayMode) {
                    case INPUT:
                        LevelLike inputLevel = this.cacheInput.computeIfAbsent(
                            it,
                            a -> RecipeUtil.asLevelLike(a.value().getInputPattern())
                        );
                        if (!inputLevel.isAllLayersVisible()) inputLevel.previousLayer();
                        break;
                    case OUTPUT:
                        LevelLike outputLevel = this.cacheOutput.computeIfAbsent(
                            it,
                            a -> RecipeUtil.asLevelLike(a.value().getOutputPattern())
                        );
                        if (!outputLevel.isAllLayersVisible()) outputLevel.previousLayer();
                        break;
                    default:
                }
            },
            recipe
        ));

        builder.addGuiEventListener(new JeiButton<>(
            149,
            10,
            10,
            MultiBlockConversionCategory::cycleDisplayMode,
            this
        ));
    }

    private void cycleDisplayMode() {
        this.displayMode = this.displayMode.next();
    }

    private enum DisplayMode {
        OVERVIEW("overview"),
        INPUT("input"),
        OUTPUT("output");

        public final String translationKey;

        DisplayMode(String translationKey) {
            this.translationKey = translationKey;
        }

        DisplayMode next() {
            return switch (this) {
                case INPUT -> OUTPUT;
                case OUTPUT -> OVERVIEW;
                case OVERVIEW -> INPUT;
            };
        }

        Component getDiscription() {
            return Component.translatable("gui.anvilcraft.category.multiblock_conversion.display_mode."
                                          + this.translationKey);
        }
    }
}
