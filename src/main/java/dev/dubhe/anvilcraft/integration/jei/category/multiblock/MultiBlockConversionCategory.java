package dev.dubhe.anvilcraft.integration.jei.category.multiblock;

import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.block.state.GiantAnvilCube;
import dev.dubhe.anvilcraft.block.workstation.GiantAnvilBlock;
import dev.dubhe.anvilcraft.client.support.LevelLikeDisplaySupport;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.JeiButton;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiTextures;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockConversionRecipe;
import dev.dubhe.anvilcraft.util.LevelLike;
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
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.Nullable;

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
        Comparator.comparing(ItemStack::getCount).thenComparing(stack -> stack.getItem().getDescriptionId()).reversed();

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
        this.icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.GIANT_ANVIL));
        this.arrowOut = JeiRenderHelper.getArrowDefault(helper);
        this.slot = JeiRenderHelper.getSlotDefault(helper);
        this.timer = helper.createTickTimer(30, 60, true);
        this.conversion = helper.drawableBuilder(JeiTextures.BLOCK_CONVERSION, 0, 0, 594, 418)
            .setTextureSize(594, 418)
            .build();
        this.layerUp = helper.drawableBuilder(JeiTextures.LAYER_UP, 0, 0, 10, 10)
            .setTextureSize(10, 20)
            .build();
        this.layerUpHovered = helper.drawableBuilder(JeiTextures.LAYER_UP, 0, 10, 10, 10)
            .setTextureSize(10, 20)
            .build();
        this.layerDown = helper.drawableBuilder(JeiTextures.LAYER_DOWN, 0, 0, 10, 10)
            .setTextureSize(10, 20)
            .build();
        this.layerDownHovered = helper.drawableBuilder(JeiTextures.LAYER_DOWN, 0, 10, 10, 10)
            .setTextureSize(10, 20)
            .build();
        this.modeOverview = helper.drawableBuilder(JeiTextures.DISPLAY_MODES, 0, 0, 10, 10)
            .setTextureSize(30, 20)
            .build();
        this.modeOverviewHovered = helper.drawableBuilder(JeiTextures.DISPLAY_MODES, 0, 10, 10, 10)
            .setTextureSize(30, 20)
            .build();
        this.modeInput = helper.drawableBuilder(JeiTextures.DISPLAY_MODES, 10, 0, 10, 10)
            .setTextureSize(30, 20)
            .build();
        this.modeInputHovered = helper.drawableBuilder(JeiTextures.DISPLAY_MODES, 10, 10, 10, 10)
            .setTextureSize(30, 20)
            .build();
        this.modeOutput = helper.drawableBuilder(JeiTextures.DISPLAY_MODES, 20, 0, 10, 10)
            .setTextureSize(30, 20)
            .build();
        this.modeOutputHovered = helper.drawableBuilder(JeiTextures.DISPLAY_MODES, 20, 10, 10, 10)
            .setTextureSize(30, 20)
            .build();
        this.renderSwitchOff = helper.drawableBuilder(JeiTextures.LAYER_SWITCH, 0, 0, 10, 10)
            .setTextureSize(10, 20)
            .build();
        this.renderSwitchOn = helper.drawableBuilder(JeiTextures.LAYER_SWITCH, 0, 10, 10, 10)
            .setTextureSize(10, 20)
            .build();
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.MULTIBLOCK_CONVERSION,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.MULTIBLOCK_CONVERSION.get())
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(AnvilCraftJeiPlugin.MULTIBLOCK_CONVERSION, ModBlocks.GIANT_ANVIL.asStack());
        registration.addCraftingStation(AnvilCraftJeiPlugin.MULTIBLOCK_CONVERSION, ModBlocks.TRANSPARENT_CRAFTING_TABLE.asStack());
        registration.addCraftingStation(AnvilCraftJeiPlugin.MULTIBLOCK_CONVERSION, new ItemStack(Blocks.CRAFTING_TABLE));
    }

    @Override
    public IRecipeHolderType<MultiblockConversionRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.MULTIBLOCK_CONVERSION;
    }

    @Override
    public Component getTitle() {
        return MultiBlockConversionCategory.TITLE;
    }

    @Override
    public int getWidth() {
        return MultiBlockConversionCategory.WIDTH;
    }

    @Override
    public int getHeight() {
        return MultiBlockConversionCategory.HEIGHT;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(
        IRecipeLayoutBuilder builder,
        RecipeHolder<MultiblockConversionRecipe> recipe,
        IFocusGroup focuses
    ) {
        this.cacheInput.computeIfAbsent(
            recipe,
            it -> LevelLikeDisplaySupport.asLevelLike(it.value().getInputPattern())
        );
        this.cacheOutput.computeIfAbsent(
            recipe,
            it -> LevelLikeDisplaySupport.asLevelLike(it.value().getOutputPattern())
        );

        List<ItemStack> inputItems = recipe.value().getInputPattern().toIngredientList();
        inputItems.sort(MultiBlockConversionCategory.BY_COUNT_DECREASING);

        for (int i = 0; i < inputItems.size(); i++) {
            ItemStack stack = inputItems.get(i);
            builder.addSlot(RecipeIngredientRole.INPUT, this.inputSlotPosX(i) + 1, this.slotPosY(i) + 1).add(stack);
        }

        List<ItemStack> outputItems = recipe.value().getOutputPattern().toIngredientList();
        outputItems.sort(MultiBlockConversionCategory.BY_COUNT_DECREASING);

        for (int i = 0; i < outputItems.size(); i++) {
            ItemStack stack = outputItems.get(i);
            builder.addSlot(RecipeIngredientRole.OUTPUT, this.outputSlotPosX(i) + 1, this.slotPosY(i) + 1).add(stack);
        }
    }

    @Override
    public void draw(
        RecipeHolder<MultiblockConversionRecipe> recipe,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphicsExtractor graphics,
        double mouseX,
        double mouseY
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        Matrix3x2fStack pose = graphics.pose();
        Component currentModeTooltip =
            Component.translatable(
                "gui.anvilcraft.category.multiblock_conversion.current_mode",
                this.displayMode.getDiscription()
            );
        pose.pushMatrix();
        pose.scale(0.8F, 0.8F);
        int textX = Math.round(MultiBlockConversionCategory.WIDTH / 0.8F - minecraft.font.width(currentModeTooltip) - 5);
        graphics.text(minecraft.font, currentModeTooltip, textX, 0, 0xFF000000, false);
        pose.popMatrix();
        this.displayModeButton(mouseX, mouseY).draw(graphics, 149, 10);

        LevelLike input = this.cacheInput.computeIfAbsent(
            recipe,
            it -> LevelLikeDisplaySupport.asLevelLike(it.value().getInputPattern())
        );
        LevelLike output = this.cacheOutput.computeIfAbsent(
            recipe,
            it -> LevelLikeDisplaySupport.asLevelLike(it.value().getOutputPattern())
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
                RenderSupport.renderLevelLike(input, graphics, 8, 16, MultiBlockConversionCategory.SCALE_FAC_OVERVIEW, 8, 2.0F, false);
                RenderSupport.renderLevelLike(output, graphics, 92, 16, MultiBlockConversionCategory.SCALE_FAC_OVERVIEW, 8, 2.0F, false);
                if (modifiedInput) {
                    input.setAllLayersVisible(false);
                }
                if (modifiedOutput) {
                    output.setAllLayersVisible(false);
                }
                for (int i = 0; i < 12; i++) {
                    this.slot.draw(graphics, this.inputSlotPosX(i), this.slotPosY(i));
                    this.slot.draw(graphics, this.outputSlotPosX(i), this.slotPosY(i));
                }
                this.arrowOut.draw(graphics, 73, 40);
                pose.pushMatrix();
                pose.scale(0.03F, 0.03F);
                this.conversion.draw(graphics, 2375, 875);
                pose.popMatrix();
                int anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(this.timer) / 3;
                // FIXME: The giant anvil is rendered behind the conversion graphics
                RenderSupport.render3x3Block(
                    graphics,
                    ModBlocks.GIANT_ANVIL.getDefaultState()
                        .trySetValue(GiantAnvilBlock.HALF, Cube3x3PartHalf.MID_CENTER)
                        .trySetValue(GiantAnvilBlock.CUBE, GiantAnvilCube.CENTER),
                    70,
                    10 + anvilYOffset,
                    20
                );
                pose.pushMatrix();
                pose.scale(0.8F, 0.8F);
                int size = recipe.value().getSize();
                graphics.text(
                    minecraft.font,
                    Component.translatable("gui.anvilcraft.category.multiblock.size", size, size),
                    85,
                    92,
                    0xFF000000,
                    false
                );
                pose.popMatrix();
                return;
            case INPUT:
                break;
            case OUTPUT:
                rendered = output;
                break;
        }

        for (IRecipeSlotView slotView : recipeSlotsView.getSlotViews()) {
            if (slotView instanceof IRecipeSlotDrawable drawable) {
                drawable.setPosition(-1000, -1000);
            }
        }
        RenderSupport.renderLevelLike(rendered, graphics, 32, 38, MultiBlockConversionCategory.SCALE_FAC_LARGE, 14, 2.0F, false);
        Component component = this.layerTooltip(rendered);
        pose.pushMatrix();
        pose.scale(0.8F, 0.8F);
        textX = Math.round(MultiBlockConversionCategory.WIDTH / 0.8F - minecraft.font.width(component) - 5);
        graphics.text(minecraft.font, component, textX, 25, 0xFF000000, false);
        pose.popMatrix();
        this.renderSwitchButton(rendered).draw(graphics, 125, 30);
        if (!rendered.isAllLayersVisible()) {
            this.layerUpButton(mouseX, mouseY).draw(graphics, 137, 30);
            this.layerDownButton(mouseX, mouseY).draw(graphics, 149, 30);
        }
    }

    private IDrawable renderSwitchButton(LevelLike level) {
        return level.isAllLayersVisible() ? this.renderSwitchOff : this.renderSwitchOn;
    }

    private IDrawable layerUpButton(double mouseX, double mouseY) {
        return (mouseX >= 137 && mouseX < 147 && mouseY >= 30 && mouseY < 40) ? this.layerUpHovered : this.layerUp;
    }

    private IDrawable layerDownButton(double mouseX, double mouseY) {
        return (mouseX >= 149 && mouseX < 159 && mouseY >= 30 && mouseY < 40) ? this.layerDownHovered : this.layerDown;
    }

    private IDrawable displayModeButton(double mouseX, double mouseY) {
        boolean hovered = (mouseX >= 149 && mouseX < 159 && mouseY >= 10 && mouseY < 20);
        return switch (this.displayMode) {
            case OVERVIEW -> hovered ? this.modeOverviewHovered : this.modeOverview;
            case INPUT -> hovered ? this.modeInputHovered : this.modeInput;
            case OUTPUT -> hovered ? this.modeOutputHovered : this.modeOutput;
        };
    }

    private Component layerTooltip(LevelLike level) {
        if (level.isAllLayersVisible()) return MultiBlockConversionCategory.ALL_LAYERS;
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
                            a -> LevelLikeDisplaySupport.asLevelLike(a.value().getInputPattern())
                        );
                        inputLevel.setAllLayersVisible(!inputLevel.isAllLayersVisible());
                        break;
                    case OUTPUT:
                        LevelLike outputLevel = this.cacheOutput.computeIfAbsent(
                            it,
                            a -> LevelLikeDisplaySupport.asLevelLike(a.value().getOutputPattern())
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
                            a -> LevelLikeDisplaySupport.asLevelLike(a.value().getInputPattern())
                        );
                        if (!inputLevel.isAllLayersVisible()) inputLevel.nextLayer();
                        break;
                    case OUTPUT:
                        LevelLike outputLevel = this.cacheOutput.computeIfAbsent(
                            it,
                            a -> LevelLikeDisplaySupport.asLevelLike(a.value().getOutputPattern())
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
                            a -> LevelLikeDisplaySupport.asLevelLike(a.value().getInputPattern())
                        );
                        if (!inputLevel.isAllLayersVisible()) inputLevel.previousLayer();
                        break;
                    case OUTPUT:
                        LevelLike outputLevel = this.cacheOutput.computeIfAbsent(
                            it,
                            a -> LevelLikeDisplaySupport.asLevelLike(a.value().getOutputPattern())
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
                case INPUT -> DisplayMode.OUTPUT;
                case OUTPUT -> DisplayMode.OVERVIEW;
                case OVERVIEW -> DisplayMode.INPUT;
            };
        }

        Component getDiscription() {
            return Component.translatable("gui.anvilcraft.category.multiblock_conversion.display_mode."
                                          + this.translationKey);
        }
    }
}
