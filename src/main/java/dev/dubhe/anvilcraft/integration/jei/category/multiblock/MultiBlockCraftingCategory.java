package dev.dubhe.anvilcraft.integration.jei.category.multiblock;

import dev.dubhe.anvilcraft.block.GiantAnvilBlock;
import dev.dubhe.anvilcraft.block.state.Cube3x3PartHalf;
import dev.dubhe.anvilcraft.block.state.GiantAnvilCube;
import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.recipe.mulitblock.MulitblockRecipe;
import dev.dubhe.anvilcraft.util.LevelLike;
import dev.dubhe.anvilcraft.util.RecipeUtil;

import net.minecraft.MethodsReturnNonnullByDefault;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.util.Lazy;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MultiBlockCraftingCategory implements IRecipeCategory<MulitblockRecipe> {
    private static final Component TITLE = Component.translatable("gui.anvilcraft.category.multiblock");
    private static final RandomSource RANDOM = RandomSource.createNewThreadLocalInstance();
    public static final int WIDTH = 160;
    public static final int HEIGHT = 160;
    public static final int SIZE_X = 3;
    public static final int SIZE_Y = 3;
    public static final int SIZE_Z = 3;
    public static final float SCALE = 21.21320343559642573202536f;
    public static final BlockPos CORNER_BLOCK = new BlockPos(2, 2, 2);

    private final Lazy<IDrawable> background;
    private final IDrawable icon;
    private LevelLike level;
    private boolean renderAllLayers = true;
    private int renderLayer = 0;

    public MultiBlockCraftingCategory(IGuiHelper helper) {
        background = Lazy.of(() -> helper.createBlankDrawable(WIDTH, HEIGHT));
        icon = new DrawableBlockStateIcon(
                Blocks.ANVIL.defaultBlockState(),
                ModBlocks.GIANT_ANVIL
                        .getDefaultState()
                        .setValue(GiantAnvilBlock.HALF, Cube3x3PartHalf.MID_CENTER)
                        .setValue(GiantAnvilBlock.CUBE, GiantAnvilCube.CENTER));
    }

    @Override
    public RecipeType<MulitblockRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.MULTI_BLOCK;
    }

    @Override
    public void draw(
            MulitblockRecipe recipe,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY) {
        if (level == null) {
            return;
        }
        RenderSystem.enableBlend();
        int xPos = 60;
        int yPos = 60;
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel clientLevel = minecraft.level;
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(xPos, yPos, 100);
        pose.scale(-SCALE, -SCALE, -SCALE);
        pose.translate(-(float) SIZE_X / 2, -(float) SIZE_Y / 2, 0);

        pose.mulPose(Axis.XP.rotationDegrees(-30F));

        float offsetX = (float) -SIZE_X / 2;
        float offsetZ = (float) -SIZE_Y / 2 + 1;
        float rotationY = clientLevel.getGameTime() * 2f;

        pose.translate(-offsetX, 0, -offsetZ);
        pose.mulPose(Axis.YP.rotationDegrees(rotationY + 45));

        pose.translate(offsetX, 0, offsetZ);

        Iterable<BlockPos> iter;
        if (renderAllLayers) {
            iter = BlockPos.betweenClosed(BlockPos.ZERO, CORNER_BLOCK);
        } else {
            iter = BlockPos.betweenClosed(BlockPos.ZERO.atY(renderLayer), CORNER_BLOCK.atY(renderLayer));
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
                blockRenderer.renderLiquid(pos, level, vertex, state, fluid);
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
        RenderSystem.enableCull();
    }

    @Override
    public Component getTitle() {
        return TITLE;
    }

    @Override
    public IDrawable getBackground() {
        return background.get();
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MulitblockRecipe recipe, IFocusGroup focuses) {
        this.level = RecipeUtil.asLevelLike(recipe.pattern);
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemStack(recipe.result.copy());
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
                AnvilCraftJeiPlugin.MULTI_BLOCK,
                JeiRecipeUtil.getRecipesFromType(ModRecipeTypes.MULITBLOCK_TYPE.get()));
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.GIANT_ANVIL), AnvilCraftJeiPlugin.MULTI_BLOCK);
    }
}
