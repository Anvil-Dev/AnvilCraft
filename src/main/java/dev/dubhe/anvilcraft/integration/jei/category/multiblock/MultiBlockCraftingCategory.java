package dev.dubhe.anvilcraft.integration.jei.category.multiblock;

import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.TextureConstants;
import dev.dubhe.anvilcraft.recipe.multiblock.MultiblockRecipe;
import dev.dubhe.anvilcraft.util.LevelLike;
import dev.dubhe.anvilcraft.util.RecipeUtil;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.MethodsReturnNonnullByDefault;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
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

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MultiBlockCraftingCategory implements IRecipeCategory<RecipeHolder<MultiblockRecipe>> {
    private static final Component TITLE = Component.translatable("gui.anvilcraft.category.multiblock");
    private static final RandomSource RANDOM = RandomSource.createNewThreadLocalInstance();
    public static final int WIDTH = 160;
    public static final int HEIGHT = 100;
    public static final int SCALE_FAC = 93;
    public static final float SCALE = 21.21320343559642573202536f;
    public static final BlockPos CORNER_BLOCK = new BlockPos(2, 2, 2);
    private static final Object2ObjectMap<RecipeHolder<MultiblockRecipe>, LevelLike> cache = new Object2ObjectOpenHashMap<>();

    private final Lazy<IDrawable> background;
    private final IDrawable icon;
    private final IDrawable slot;
    private final IDrawable arrowOut;
    private boolean renderAllLayers = true;
    private int renderLayer = 0;

    public MultiBlockCraftingCategory(IGuiHelper helper) {
        background = Lazy.of(() -> helper.createBlankDrawable(WIDTH, HEIGHT));
        icon = helper.createDrawableItemStack(new ItemStack(ModBlocks.GIANT_ANVIL));
        arrowOut = helper.createDrawable(TextureConstants.ANVIL_CRAFT_SPRITES, 0, 31, 16, 8);
        slot = helper.getSlotDrawable();
    }

    @Override
    public RecipeType<RecipeHolder<MultiblockRecipe>> getRecipeType() {
        return AnvilCraftJeiPlugin.MULTI_BLOCK;
    }

    @Override
    public void draw(
            RecipeHolder<MultiblockRecipe> recipeHolder,
            IRecipeSlotsView recipeSlotsView,
            GuiGraphics guiGraphics,
            double mouseX,
            double mouseY) {
        LevelLike level = cache.get(recipeHolder);
        if (level == null) {
            level = RecipeUtil.asLevelLike(recipeHolder.value().pattern);
            cache.put(recipeHolder, level);
        }
        RenderSystem.enableBlend();
        int xPos = 55;
        int yPos = 50;
        Minecraft minecraft = Minecraft.getInstance();
        DeltaTracker tracker = minecraft.getTimer();
        ClientLevel clientLevel = minecraft.level;
        PoseStack pose = guiGraphics.pose();
        int sizeX = level.horizontalSize();
        int sizeY = level.verticalSize();

        float scaleX = SCALE_FAC / (float) Math.sqrt(sizeX * sizeX * 2);
        float scaleY = SCALE_FAC / (float) sizeY;
        float scale = Math.min(scaleY, scaleX);

        pose.pushPose();
        pose.translate(xPos, yPos, 100);

        pose.scale(-scale, -scale, -scale);

        pose.translate(-(float) sizeX / 2, -(float) sizeY / 2, 0);
        pose.mulPose(Axis.XP.rotationDegrees(-30F));

        float offsetX = (float) -sizeX / 2;
        float offsetZ = (float) -sizeY / 2 + 1;
        float rotationY = (clientLevel.getGameTime() + tracker.getGameTimeDeltaPartialTick(true)) * 2f;

        pose.translate(-offsetX, 0, -offsetZ);
        pose.mulPose(Axis.YP.rotationDegrees(rotationY + 45));

        pose.translate(offsetX, 0, offsetZ);

        Iterable<BlockPos> iter;
        if (renderAllLayers) {
            iter = BlockPos.betweenClosed(BlockPos.ZERO, new BlockPos(sizeX - 1, sizeY - 1, sizeX - 1));
        } else {
            iter = BlockPos.betweenClosed(
                    BlockPos.ZERO.atY(renderLayer), new BlockPos(sizeX - 1, renderLayer, sizeX - 1));
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
        arrowOut.draw(guiGraphics, 110, 60);
        slot.draw(guiGraphics, 129, 69);
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<MultiblockRecipe> recipeHolder, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.OUTPUT, 130, 70).addItemStack(recipeHolder.value().result.copy());
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
                AnvilCraftJeiPlugin.MULTI_BLOCK,
                JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.MULITBLOCK_TYPE.get()));
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.GIANT_ANVIL), AnvilCraftJeiPlugin.MULTI_BLOCK);
    }
}
