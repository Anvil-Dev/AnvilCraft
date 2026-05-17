package dev.dubhe.anvilcraft.integration.jei.category.multiblock;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.block.state.GiantAnvilCube;
import dev.dubhe.anvilcraft.block.workstation.GiantAnvilBlock;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.JeiButton;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiTextureConstants;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockRecipe;
import dev.dubhe.anvilcraft.util.LevelLike;
import dev.dubhe.anvilcraft.util.RecipeUtil;
import mezz.jei.api.gui.ITickTimer;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiBlockCraftingCategory implements IRecipeCategory<RecipeHolder<MultiblockRecipe>> {
    private static final Component TITLE = Component.translatable("gui.anvilcraft.category.multiblock");

    private static final Comparator<ItemStack> BY_COUNT_DECREASING =
        Comparator.comparing(ItemStack::getCount).thenComparing(ItemStack::getDescriptionId).reversed();

    public static final int WIDTH = 162;
    public static final int START_HEIGHT = 100;
    public static final int ROWS = 2;

    public static final int SCALE_FAC = 80;
    private final Map<RecipeHolder<MultiblockRecipe>, LevelLike> cache = new HashMap<>();

    private final IDrawable icon;
    private final IDrawable slot;
    private final IDrawable layerUp;
    private final IDrawable layerUpHovered;
    private final IDrawable layerDown;
    private final IDrawable layerDownHovered;
    private final IDrawable renderSwitchOn;
    private final IDrawable renderSwitchOff;
    private final IDrawable arrowOut;
    private final IDrawable conversion;
    private final ITickTimer timer;

    public MultiBlockCraftingCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.GIANT_ANVIL));
        this.arrowOut = JeiRenderHelper.getArrowInput(helper);
        this.slot = JeiRenderHelper.getSlotDefault(helper);
        this.timer = helper.createTickTimer(30, 60, true);
        this.conversion = helper.drawableBuilder(JeiTextureConstants.BLOCK_CRAFTING, 0, 0, 594, 418)
            .setTextureSize(594, 418)
            .build();
        this.layerUp = helper.drawableBuilder(JeiTextureConstants.LAYER_UP, 0, 0, 10, 10)
            .setTextureSize(10, 20)
            .build();
        this.layerUpHovered = helper.drawableBuilder(JeiTextureConstants.LAYER_UP, 0, 10, 10, 10)
            .setTextureSize(10, 20)
            .build();
        this.layerDown = helper.drawableBuilder(JeiTextureConstants.LAYER_DOWN, 0, 0, 10, 10)
            .setTextureSize(10, 20)
            .build();
        this.layerDownHovered = helper.drawableBuilder(JeiTextureConstants.LAYER_DOWN, 0, 10, 10, 10)
            .setTextureSize(10, 20)
            .build();
        this.renderSwitchOff = helper.drawableBuilder(JeiTextureConstants.LAYER_SWITCH, 0, 0, 10, 10)
            .setTextureSize(10, 20)
            .build();
        this.renderSwitchOn = helper.drawableBuilder(JeiTextureConstants.LAYER_SWITCH, 0, 10, 10, 10)
            .setTextureSize(10, 20)
            .build();
    }

    @Override
    public IRecipeHolderType<MultiblockRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.MULTIBLOCK_CRAFTING;
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
        return START_HEIGHT + ROWS * 18;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<MultiblockRecipe> recipe, IFocusGroup focuses) {
        this.cache.computeIfAbsent(recipe, it -> RecipeUtil.asLevelLike(it.value().getPattern()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 130, 70)
            .addItemStack(recipe.value().getResult().copy());

        List<ItemStack> ingredientList = recipe.value().getPattern().toIngredientList();
        ingredientList.sort(BY_COUNT_DECREASING);

        for (int i = 0; i < ingredientList.size(); i++) {
            ItemStack stack = ingredientList.get(i);
            int row = i / 9;
            int col = i % 9;
            builder.addSlot(RecipeIngredientRole.INPUT, col * 18 + 1, START_HEIGHT + row * 18 + 1)
                .addItemStack(stack);
        }
    }

    @Override
    public void draw(
        RecipeHolder<MultiblockRecipe> recipe,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphicsExtractor guiGraphics,
        double mouseX,
        double mouseY
    ) {
        LevelLike level = this.cache.get(recipe);
        if (level == null) {
            level = RecipeUtil.asLevelLike(recipe.value().pattern);
            this.cache.put(recipe, level);
        }
        final boolean renderAllLayers = level.isAllLayersVisible();
        final int visibleLayer = level.getCurrentVisibleLayer();
        RenderSupport.renderLevelLike(level, guiGraphics, 45, 50, SCALE_FAC, 2.0F);
        final Minecraft minecraft = Minecraft.getInstance();
        PoseStack pose = guiGraphics.pose();
        int sizeY = level.verticalSize();
        Component component;
        if (renderAllLayers) {
            component = Component.translatable("gui.anvilcraft.category.multiblock.all_layers");
            this.renderSwitchOff.draw(guiGraphics, 125, 10);
        } else {
            component =
                Component.translatable("gui.anvilcraft.category.multiblock.single_layer", visibleLayer + 1, sizeY);
            this.renderSwitchOn.draw(guiGraphics, 125, 10);
            this.layerUpButton(mouseX, mouseY).draw(guiGraphics, 137, 10);
            this.layerDownButton(mouseX, mouseY).draw(guiGraphics, 149, 10);
        }
        pose.pushPose();
        pose.scale(0.03F, 0.03F, 1.0F);
        this.conversion.draw(guiGraphics, 4300, 1700);
        pose.popPose();
        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(this.timer) / 3;
        RenderSupport.renderBlock(
            guiGraphics,
            ModBlocks.GIANT_ANVIL.getDefaultState()
                .trySetValue(GiantAnvilBlock.HALF, Cube3x3PartHalf.MID_CENTER)
                .trySetValue(GiantAnvilBlock.CUBE, GiantAnvilCube.CENTER),
            138,
            44.8F + anvilYOffset,
            20,
            5,
            RenderSupport.SINGLE_BLOCK
        );
        pose.pushPose();
        pose.scale(0.8F, 0.8F, 0.8F);
        int textX = Math.round(WIDTH / 0.8F - minecraft.font.width(component) - 5);
        guiGraphics.drawString(minecraft.font, component, textX, 0, 0xFF000000, false);
        int size = recipe.value().pattern.getSize();
        guiGraphics.drawString(
            minecraft.font,
            Component.translatable("gui.anvilcraft.category.multiblock.size", size, size),
            85, 115, 0xFF000000, false
        );
        pose.popPose();
        this.arrowOut.draw(guiGraphics, 110, 60);
        this.slot.draw(guiGraphics, 129, 69);

        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < 9; j++) {
                this.slot.draw(guiGraphics, j * 18, START_HEIGHT + i * 18);
            }
        }
    }

    private IDrawable layerUpButton(double mouseX, double mouseY) {
        return (mouseX >= 137 && mouseX < 147 && mouseY >= 10 && mouseY < 20) ? this.layerUpHovered : this.layerUp;
    }

    private IDrawable layerDownButton(double mouseX, double mouseY) {
        return (mouseX >= 149 && mouseX < 159 && mouseY >= 10 && mouseY < 20) ? this.layerDownHovered : this.layerDown;
    }

    @Override
    public void createRecipeExtras(
        IRecipeExtrasBuilder builder, RecipeHolder<MultiblockRecipe> recipe, IFocusGroup focuses) {
        builder.addGuiEventListener(new JeiButton<>(
            125,
            10,
            10,
            it -> {
                LevelLike level = this.cache.computeIfAbsent(it, a -> RecipeUtil.asLevelLike(a.value().pattern));
                level.setAllLayersVisible(!level.isAllLayersVisible());
            },
            recipe
        ));

        builder.addGuiEventListener(new JeiButton<>(
            137,
            10,
            10,
            it -> {
                LevelLike level = this.cache.computeIfAbsent(it, a -> RecipeUtil.asLevelLike(a.value().pattern));
                if (level.isAllLayersVisible()) return;
                level.nextLayer();
            },
            recipe
        ));

        builder.addGuiEventListener(new JeiButton<>(
            149,
            10,
            10,
            it -> {
                LevelLike level = this.cache.computeIfAbsent(it, a -> RecipeUtil.asLevelLike(a.value().pattern));
                if (level.isAllLayersVisible()) return;
                level.previousLayer();
            },
            recipe
        ));
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.MULTIBLOCK_CRAFTING,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.MULTIBLOCK.get())
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(AnvilCraftJeiPlugin.MULTIBLOCK_CRAFTING, ModBlocks.GIANT_ANVIL.asStack());
        registration.addCraftingStation(AnvilCraftJeiPlugin.MULTIBLOCK_CRAFTING, ModBlocks.TRANSPARENT_CRAFTING_TABLE.asStack());
        registration.addCraftingStation(AnvilCraftJeiPlugin.MULTIBLOCK_CRAFTING, Items.CRAFTING_TABLE.getDefaultInstance());
        registration.addCraftingStation(AnvilCraftJeiPlugin.MULTIBLOCK_CRAFTING, ModBlocks.SPACE_OVERCOMPRESSOR.asStack());
    }

}
