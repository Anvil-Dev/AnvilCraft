package dev.dubhe.anvilcraft.integration.jei.category;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.anvilcraft.lib.v2.util.predicate.ChanceBlockState;
import dev.anvilcraft.resource.ageratum.client.constants.AgeratumConstants;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDImageComponent;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiTextureConstants;
import dev.dubhe.anvilcraft.recipe.PortalConversionRecipe;
import dev.dubhe.anvilcraft.util.TooltipUtil;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PortalConversionCategory implements IRecipeCategory<RecipeHolder<PortalConversionRecipe>> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;
    public static final int PORTAL_WIDTH = 110;
    public static final int PORTAL_HEIGHT = 64;

    private final Component title;
    private final IDrawable slotDefault;
    private final IDrawable slotProbability;

    public PortalConversionCategory(IGuiHelper helper) {
        this.title = Component.translatable("gui.anvilcraft.category.portal_conversion");
        this.slotDefault = JeiRenderHelper.getSlotDefault(helper);
        this.slotProbability = JeiRenderHelper.getSlotProbability(helper);
    }

    @Override
    public RecipeType<RecipeHolder<PortalConversionRecipe>> getRecipeType() {
        return AnvilCraftJeiPlugin.PORTAL_CONVERSION;
    }

    @Override
    public Component getTitle() {
        return title;
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
        return null;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<PortalConversionRecipe> recipeHolder, IFocusGroup focuses) {
        PortalConversionRecipe recipe = recipeHolder.value();
        builder.addInvisibleIngredients(RecipeIngredientRole.INPUT).addItemStacks(
            recipe.getInput().getBlocks().stream().map(holder -> new ItemStack(holder.value())).toList()
        );
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT).addItemLike(recipe.getResult().state().getBlock());
    }

    @Override
    public void draw(
        RecipeHolder<PortalConversionRecipe> holder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY
    ) {
        PortalConversionRecipe recipe = holder.value();
        RENDER_INPUT: {
            List<BlockState> input = recipe.getInput().constructStatesForRender();
            if (input.isEmpty()) break RENDER_INPUT;
            BlockState renderedState = input.get((int) ((System.currentTimeMillis() / 1000) % input.size()));
            if (renderedState == null) break RENDER_INPUT;
            JeiRenderHelper.renderBlockWithSlot(
                guiGraphics,
                this.slotDefault,
                renderedState,
                4,
                4,
                20,
                RenderSupport.SINGLE_BLOCK
            );
        }

        Minecraft minecraft = Minecraft.getInstance();
        ResourceLocation location = PortalConversionCategory.computePortalTexture(recipe.getPortalType().getId());
        MDImageComponent.Size size = PortalConversionCategory.resolveSize(minecraft, location);
        MDImageComponent.Size renderSize = PortalConversionCategory.computeRenderSize(size);
        if (renderSize.width() > 0 && renderSize.height() > 0) {
            PoseStack pose = guiGraphics.pose();
            pose.pushPose();
            int x = 26 + (int) ((float) (PortalConversionCategory.PORTAL_WIDTH - renderSize.width()) / 2);
            int y = (int) ((float) (PortalConversionCategory.PORTAL_HEIGHT - renderSize.height()) / 2);
            pose.translate(x, y, 0);
            PortalConversionCategory.innerBlit(
                guiGraphics,
                location,
                renderSize.width(),
                renderSize.height(),
                renderSize.width(),
                renderSize.height()
            );
            pose.popPose();
        }

        ChanceBlockState result = recipe.getResult();
        JeiRenderHelper.renderBlockWithSlot(
            guiGraphics,
            result.chance() instanceof ConstantValue(float value) && value == 1.0F ? this.slotDefault : this.slotProbability,
            result.state(),
            142,
            4,
            20,
            RenderSupport.SINGLE_BLOCK
        );
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        RecipeHolder<PortalConversionRecipe> recipe,
        IRecipeSlotsView recipeSlotsView,
        double mouseX,
        double mouseY
    ) {
        if (MathUtil.isInRange(mouseX, mouseY, 4, 4, 21, 21)) {
            List<BlockState> input = recipe.value().getInput().constructStatesForRender();
            if (input.isEmpty()) return;
            BlockState renderedState = input.get((int) ((System.currentTimeMillis() / 1000) % input.size()));
            if (renderedState == null) return;
            tooltip.addAll(TooltipUtil.tooltip(renderedState.getBlock()));
            return;
        } else if (MathUtil.isInRange(mouseX, mouseY, 24, 0, 138, 64)) {
            tooltip.add(Component.translatable(
                "gui.anvilcraft.category.portal_conversion.fall_through",
                recipe.value().getPortalType().getPortalName()
            ));
            return;
        }

        ChanceBlockState result = recipe.value().getResult();
        if (!MathUtil.isInRange(mouseX, mouseY, 142, 4, 159, 21)) return;
        List<Component> tooltips = TooltipUtil.recipeIDTooltip(result.state().getBlock(), recipe.id());
        tooltips.addAll(tooltips.size() - 1, JeiRecipeUtil.getTooltips(result.chance()));
        tooltip.addAll(tooltips);

    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.PORTAL_CONVERSION,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.PORTAL_CONVERSION_TYPE.get())
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(Blocks.END_PORTAL_FRAME), AnvilCraftJeiPlugin.PORTAL_CONVERSION);
        registration.addRecipeCatalyst(new ItemStack(Blocks.OBSIDIAN), AnvilCraftJeiPlugin.PORTAL_CONVERSION);
    }

    protected static void innerBlit(
        GuiGraphics guiGraphics,
        ResourceLocation atlasLocation,
        int width,
        int height,
        int textureWidth,
        int textureHeight
    ) {
        final float minU = ((float) 0.0 + 0.0F) / (float) textureWidth;
        final float maxU = ((float) 0.0 + (float) width) / (float) textureWidth;
        final float minV = (0.0F + 0.0F) / (float) textureHeight;
        final float maxV = (0.0F + (float) height) / (float) textureHeight;
        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, atlasLocation);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        Matrix4f matrix4f = guiGraphics.pose().last().pose();
        BufferBuilder bufferbuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferbuilder.addVertex(matrix4f, (float) 0, (float) 0, (float) 0).setUv(minU, minV);
        bufferbuilder.addVertex(matrix4f, (float) 0, (float) height, (float) 0).setUv(minU, maxV);
        bufferbuilder.addVertex(matrix4f, (float) width, (float) height, (float) 0).setUv(maxU, maxV);
        bufferbuilder.addVertex(matrix4f, (float) width, (float) 0, (float) 0).setUv(maxU, minV);
        BufferUploader.drawWithShader(bufferbuilder.buildOrThrow());
        RenderSystem.disableBlend();
    }

    protected static ResourceLocation computePortalTexture(ResourceLocation typeId) {
        return JeiTextureConstants.texture("portal/" + typeId.toShortLanguageKey().replace(':', '_'));
    }

    protected static MDImageComponent.Size computeRenderSize(MDImageComponent.Size source) {
        float scale = PortalConversionCategory.computeScale(source);
        int width = Math.max(1, Math.round(source.width() * scale));
        int height = Math.max(1, Math.round(source.height() * scale));
        return new MDImageComponent.Size(width, height, scale);
    }

    protected static float computeScale(MDImageComponent.Size source) {
        float scale = Math.min(
            (float) PortalConversionCategory.PORTAL_WIDTH / source.width(),
            (float) PortalConversionCategory.PORTAL_HEIGHT / source.height()
        );
        scale = Math.min(1.0F, scale);
        return scale;
    }

    public static final Map<ResourceLocation, MDImageComponent.Size> IMAGE_SIZE_CACHE = new HashMap<>();

    /**
     * 获取图片原始尺寸，缺失时使用缓存或回退默认值。
     */
    protected static MDImageComponent.Size resolveSize(Minecraft minecraft, ResourceLocation location) {
        MDImageComponent.Size cachedSize = IMAGE_SIZE_CACHE.get(location);
        if (cachedSize != null) {
            return cachedSize;
        }
        MDImageComponent.Size size = new MDImageComponent.Size(
            AgeratumConstants.Image.DEFAULT_PLACEHOLDER_WIDTH,
            AgeratumConstants.Image.DEFAULT_PLACEHOLDER_HEIGHT,
            1.0f
        );
        try {
            Resource resource = minecraft.getResourceManager().getResource(location).orElse(null);
            if (resource != null) {
                try (NativeImage image = NativeImage.read(resource.open())) {
                    size = new MDImageComponent.Size(Math.max(1, image.getWidth()), Math.max(1, image.getHeight()), 1.0f);
                }
            }
        } catch (IOException ignored) {
            // Missing or invalid textures fall back to a tiny placeholder size.
        }
        IMAGE_SIZE_CACHE.put(location, size);
        return size;
    }
}
