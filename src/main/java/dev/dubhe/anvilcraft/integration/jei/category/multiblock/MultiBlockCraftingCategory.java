package dev.dubhe.anvilcraft.integration.jei.category.multiblock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.anvilcraft.lib.v2.rendering.gui.GuiRenderExtras;
import dev.anvilcraft.lib.v2.util.MathUtil;
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
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockRecipe;
import dev.dubhe.anvilcraft.util.LevelLike;
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
import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiBlockCraftingCategory implements IRecipeCategory<RecipeHolder<MultiblockRecipe>> {
    private static final Component TITLE = Component.translatable("gui.anvilcraft.category.multiblock");

    private static final Comparator<ItemStack> BY_COUNT_DECREASING =
        Comparator.comparing(ItemStack::getCount).thenComparing(stack -> stack.getItem().getDescriptionId()).reversed();

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
        this.conversion = helper.drawableBuilder(JeiTextures.BLOCK_CRAFTING, 0, 0, 594, 418)
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
        this.renderSwitchOff = helper.drawableBuilder(JeiTextures.LAYER_SWITCH, 0, 0, 10, 10)
            .setTextureSize(10, 20)
            .build();
        this.renderSwitchOn = helper.drawableBuilder(JeiTextures.LAYER_SWITCH, 0, 10, 10, 10)
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
        this.cache.computeIfAbsent(recipe, it -> LevelLikeDisplaySupport.asLevelLike(it.value().getPattern()));
        builder.addSlot(RecipeIngredientRole.OUTPUT, 130, 70).add(recipe.value().getResult().create());

        List<ItemStack> ingredientList = recipe.value().getPattern().toIngredientList();
        ingredientList.sort(BY_COUNT_DECREASING);

        for (int i = 0; i < ingredientList.size(); i++) {
            ItemStack stack = ingredientList.get(i);
            int row = i / 9;
            int col = i % 9;
            builder.addSlot(RecipeIngredientRole.INPUT, col * 18 + 1, START_HEIGHT + row * 18 + 1).add(stack);
        }
    }

    @Override
    public void draw(
        RecipeHolder<MultiblockRecipe> recipe,
        IRecipeSlotsView view,
        GuiGraphicsExtractor graphics,
        double mouseX,
        double mouseY
    ) {
        LevelLike level = this.cache.get(recipe);
        if (level == null) {
            level = LevelLikeDisplaySupport.asLevelLike(recipe.value().pattern);
            this.cache.put(recipe, level);
        }
        final boolean renderAllLayers = level.isAllLayersVisible();
        final int visibleLayer = level.getCurrentVisibleLayer();
        RenderSupport.renderLevelLike(
            level,
            graphics,
            8,
            8,
            80,
            16,
            4.0F,
            false
        );
        final Minecraft minecraft = Minecraft.getInstance();
        int sizeY = level.verticalSize();
        Component component;
        if (renderAllLayers) {
            component = Component.translatable("gui.anvilcraft.category.multiblock.all_layers");
            this.renderSwitchOff.draw(graphics, 125, 10);
        } else {
            component =
                Component.translatable("gui.anvilcraft.category.multiblock.single_layer", visibleLayer + 1, sizeY);
            this.renderSwitchOn.draw(graphics, 125, 10);
            this.layerUpButton(mouseX, mouseY).draw(graphics, 137, 10);
            this.layerDownButton(mouseX, mouseY).draw(graphics, 149, 10);
        }



        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(129, 51);
        pose.scale(0.03F, 0.03F);
        this.conversion.draw(graphics);
        pose.popMatrix();

        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(Axis.XP.rotationDegrees(30));
        poseStack.mulPose(Axis.YP.rotationDegrees(45));
        poseStack.scale(0.3f, 0.3f, 0.3f);
        int anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(this.timer) / 3;
        GuiRenderExtras.tessellateBlock(
            graphics,
            ModBlocks.GIANT_ANVIL.getDefaultState()
                .trySetValue(GiantAnvilBlock.HALF, Cube3x3PartHalf.MID_CENTER)
                .trySetValue(GiantAnvilBlock.CUBE, GiantAnvilCube.CENTER),
            null,
            null,
            122,
            26 + anvilYOffset,
            122 + 32,
            26 + anvilYOffset + 32,
            -1,
            true,
            poseStack.last()
        );

        pose.pushMatrix();
        pose.scale(0.8F, 0.8F);
        int textX = Math.round(WIDTH / 0.8F - minecraft.font.width(component) - 5);
        graphics.text(minecraft.font, component, textX, 0, 0xFF000000, false);
        int size = recipe.value().pattern.getSize();
        graphics.text(
            minecraft.font,
            Component.translatable("gui.anvilcraft.category.multiblock.size", size, size),
            85, 115, 0xFF000000, false
        );
        pose.popMatrix();
        this.arrowOut.draw(graphics, 110, 60);
        this.slot.draw(graphics, 129, 69);

        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < 9; j++) {
                this.slot.draw(graphics, j * 18, START_HEIGHT + i * 18);
            }
        }
    }

    private IDrawable layerUpButton(double mouseX, double mouseY) {
        return MathUtil.isInRange(mouseX, mouseY, 137, 10, 147, 20) ? this.layerUpHovered : this.layerUp;
    }

    private IDrawable layerDownButton(double mouseX, double mouseY) {
        return MathUtil.isInRange(mouseX, mouseY, 149, 10, 159, 20) ? this.layerDownHovered : this.layerDown;
    }

    @Override
    public void createRecipeExtras(
        IRecipeExtrasBuilder builder, RecipeHolder<MultiblockRecipe> recipe, IFocusGroup focuses) {
        builder.addGuiEventListener(new JeiButton<>(
            125,
            10,
            10,
            it -> {
                LevelLike level = this.cache.computeIfAbsent(
                    it,
                    a -> LevelLikeDisplaySupport.asLevelLike(a.value().pattern)
                );
                level.setAllLayersVisible(!level.isAllLayersVisible());
            },
            recipe
        ));

        builder.addGuiEventListener(new JeiButton<>(
            137,
            10,
            10,
            it -> {
                LevelLike level = this.cache.computeIfAbsent(
                    it,
                    a -> LevelLikeDisplaySupport.asLevelLike(a.value().pattern)
                );
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
                LevelLike level = this.cache.computeIfAbsent(
                    it,
                    a -> LevelLikeDisplaySupport.asLevelLike(a.value().pattern)
                );
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
        registration.addCraftingStation(AnvilCraftJeiPlugin.MULTIBLOCK_CRAFTING, ModBlocks.GIANT_ANVIL);
        registration.addCraftingStation(AnvilCraftJeiPlugin.MULTIBLOCK_CRAFTING, ModBlocks.TRANSPARENT_CRAFTING_TABLE);
        registration.addCraftingStation(AnvilCraftJeiPlugin.MULTIBLOCK_CRAFTING, Items.CRAFTING_TABLE);
        registration.addCraftingStation(AnvilCraftJeiPlugin.MULTIBLOCK_CRAFTING, ModBlocks.SPACE_OVERCOMPRESSOR);
    }

}
