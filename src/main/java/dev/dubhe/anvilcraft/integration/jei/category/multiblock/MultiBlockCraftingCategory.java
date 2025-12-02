package dev.dubhe.anvilcraft.integration.jei.category.multiblock;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.block.GiantAnvilBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.block.state.GiantAnvilCube;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.reicpe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.JeiButton;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.TextureConstants;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockRecipe;
import dev.dubhe.anvilcraft.util.LevelLike;
import dev.dubhe.anvilcraft.util.RecipeUtil;
import dev.dubhe.anvilcraft.util.VertexConsumerWithPose;
import mezz.jei.api.gui.ITickTimer;
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
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiBlockCraftingCategory implements IRecipeCategory<RecipeHolder<MultiblockRecipe>> {
    private static final Component TITLE = Component.translatable("gui.anvilcraft.category.multiblock");
    private static final RandomSource RANDOM = RandomSource.createNewThreadLocalInstance();

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
        icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.GIANT_ANVIL));
        arrowOut = JeiRenderHelper.getArrowInput(helper);
        slot = JeiRenderHelper.getSlotDefault(helper);
        timer = helper.createTickTimer(30, 60, true);
        conversion = helper.drawableBuilder(TextureConstants.BLOCK_CRAFTING, 0, 0, 594, 418)
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
        renderSwitchOff = helper.drawableBuilder(
                AnvilCraft.of("textures/gui/container/insight/insight_layer_switch.png"), 0, 0, 10, 10)
            .setTextureSize(10, 20)
            .build();
        renderSwitchOn = helper.drawableBuilder(
                AnvilCraft.of("textures/gui/container/insight/insight_layer_switch.png"), 0, 10, 10, 10)
            .setTextureSize(10, 20)
            .build();
    }

    @Override
    public RecipeType<RecipeHolder<MultiblockRecipe>> getRecipeType() {
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
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<MultiblockRecipe> recipe, IFocusGroup focuses) {
        cache.computeIfAbsent(recipe, it -> RecipeUtil.asLevelLike(it.value().getPattern()));
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
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY
    ) {
        LevelLike level = cache.get(recipe);
        if (level == null) {
            level = RecipeUtil.asLevelLike(recipe.value().pattern);
            cache.put(recipe, level);
        }
        final boolean renderAllLayers = level.isAllLayersVisible();
        final int visibleLayer = level.getCurrentVisibleLayer();
        RenderSystem.enableBlend();
        final int xPos = 45;
        final int yPos = 50;
        Minecraft minecraft = Minecraft.getInstance();
        DeltaTracker tracker = minecraft.getTimer();
        ClientLevel clientLevel = minecraft.level;
        PoseStack pose = guiGraphics.pose();
        int sizeX = level.horizontalSize();
        int sizeY = level.verticalSize();

        float scaleX = SCALE_FAC / (sizeX * Mth.SQRT_OF_TWO);
        float scaleY = SCALE_FAC / (float) sizeY;
        float scale = Math.min(scaleY, scaleX);

        pose.pushPose();
        pose.translate(xPos, yPos, 100);

        pose.scale(-scale, -scale, -scale);

        pose.translate(-(float) sizeX / 2, -(float) sizeY / 2, 0);
        pose.mulPose(Axis.XP.rotationDegrees(-30));

        float offsetX = (float) -sizeX / 2;
        float offsetZ = (float) -sizeX / 2 + 1;
        float rotationY = (clientLevel.getGameTime() + tracker.getGameTimeDeltaPartialTick(true)) * 2f;

        pose.translate(-offsetX, 0, -offsetZ);
        pose.mulPose(Axis.YP.rotationDegrees(rotationY + 45));

        pose.translate(offsetX, 0, offsetZ);

        Iterable<BlockPos> iter;
        if (renderAllLayers) {
            iter = BlockPos.betweenClosed(BlockPos.ZERO, new BlockPos(sizeX - 1, sizeY - 1, sizeX - 1));
        } else {
            iter = BlockPos.betweenClosed(
                BlockPos.ZERO.atY(visibleLayer), new BlockPos(sizeX - 1, visibleLayer, sizeX - 1));
        }
        pose.pushPose();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        pose.translate(0, 0, -1);
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        BlockRenderDispatcher blockRenderer = minecraft.getBlockRenderer();
        for (BlockPos pos : iter) {
            BlockState state = level.getBlockState(pos);
            pose.pushPose();
            pose.translate(pos.getX(), pos.getY(), pos.getZ());
            FluidState fluid = state.getFluidState();
            if (!fluid.isEmpty()) {
                RenderType renderType = ItemBlockRenderTypes.getRenderLayer(fluid);
                VertexConsumer vertex = buffers.getBuffer(renderType);
                blockRenderer.renderLiquid(pos, level, new VertexConsumerWithPose(vertex, pose.last(), pos), state, fluid);
            }
            if (state.getRenderShape() != RenderShape.INVISIBLE) {
                BakedModel bakedModel = blockRenderer.getBlockModel(state);
                for (RenderType type : bakedModel.getRenderTypes(state, RANDOM, ModelData.EMPTY)) {
                    VertexConsumer vertex = buffers.getBuffer(type);
                    blockRenderer.renderBatched(state, pos, level, pose, vertex, false, RANDOM, ModelData.EMPTY, type);
                }
            }
            pose.popPose();
        }
        buffers.endBatch();
        pose.popPose();
        pose.popPose();
        Component component;
        if (renderAllLayers) {
            component = Component.translatable("gui.anvilcraft.category.multiblock.all_layers");
            renderSwitchOff.draw(guiGraphics, 125, 10);
        } else {
            component =
                Component.translatable("gui.anvilcraft.category.multiblock.single_layer", visibleLayer + 1, sizeY);
            renderSwitchOn.draw(guiGraphics, 125, 10);
            this.layerUpButton(mouseX, mouseY).draw(guiGraphics, 137, 10);
            this.layerDownButton(mouseX, mouseY).draw(guiGraphics, 149, 10);
        }
        pose.pushPose();
        pose.scale(0.03f, 0.03f, 1.0f);
        conversion.draw(guiGraphics, 4300, 1700);
        pose.popPose();
        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(timer) / 3;
        RenderSupport.renderBlock(
            guiGraphics,
            ModBlocks.GIANT_ANVIL.getDefaultState()
                .trySetValue(GiantAnvilBlock.HALF, Cube3x3PartHalf.MID_CENTER)
                .trySetValue(GiantAnvilBlock.CUBE, GiantAnvilCube.CENTER),
            138,
            44.8f + anvilYOffset,
            20,
            5,
            RenderSupport.SINGLE_BLOCK
        );
        pose.pushPose();
        pose.scale(0.8f, 0.8f, 0.8f);
        int textX = Math.round(WIDTH / 0.8f - minecraft.font.width(component) - 5);
        guiGraphics.drawString(minecraft.font, component, textX, 0, 0xFF000000, false);
        int size = recipe.value().pattern.getSize();
        guiGraphics.drawString(
            minecraft.font,
            Component.translatable("gui.anvilcraft.category.multiblock.size", size, size),
            85, 115, 0xFF000000, false
        );
        pose.popPose();
        arrowOut.draw(guiGraphics, 110, 60);
        slot.draw(guiGraphics, 129, 69);

        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < 9; j++) {
                slot.draw(guiGraphics, j * 18, START_HEIGHT + i * 18);
            }
        }
    }

    private IDrawable layerUpButton(double mouseX, double mouseY) {
        return (mouseX >= 137 && mouseX < 147 && mouseY >= 10 && mouseY < 20) ? layerUpHovered : layerUp;
    }

    private IDrawable layerDownButton(double mouseX, double mouseY) {
        return (mouseX >= 149 && mouseX < 159 && mouseY >= 10 && mouseY < 20) ? layerDownHovered : layerDown;
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
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.MULTIBLOCK_TYPE.get())
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ModBlocks.GIANT_ANVIL.asStack(), AnvilCraftJeiPlugin.MULTIBLOCK_CRAFTING);
        registration.addRecipeCatalyst(ModBlocks.TRANSPARENT_CRAFTING_TABLE.asStack(), AnvilCraftJeiPlugin.MULTIBLOCK_CRAFTING);
        registration.addRecipeCatalyst(ModBlocks.SPACE_OVERCOMPRESSOR.asStack(), AnvilCraftJeiPlugin.MULTIBLOCK_CRAFTING);
    }

}
