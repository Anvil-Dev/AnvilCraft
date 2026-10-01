package dev.dubhe.anvilcraft.integration.jei.category;

import com.mojang.blaze3d.platform.NativeImage;
import dev.anvilcraft.lib.v2.util.MathUtil;
import dev.anvilcraft.lib.v2.util.predicate.WeightedChanceBlockStates;
import dev.anvilcraft.resource.ageratum.client.constants.AgeratumConstants;
import dev.anvilcraft.resource.ageratum.client.feat.markdown.component.MDImageComponent;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.util.JeiBlockIngredientUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiTextures;
import dev.dubhe.anvilcraft.recipe.PortalConversionRecipe;
import dev.dubhe.anvilcraft.util.TooltipUtil;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PortalConversionCategory implements IRecipeCategory<RecipeHolder<PortalConversionRecipe>> {
    private static final String INPUT_BLOCK = "input_block";
    private static final String OUTPUT_BLOCK = "output_block";

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
    public IRecipeHolderType<PortalConversionRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.PORTAL_CONVERSION;
    }

    @Override
    public Component getTitle() {
        return this.title;
    }

    @Override
    public int getWidth() {
        return PortalConversionCategory.WIDTH;
    }

    @Override
    public int getHeight() {
        return PortalConversionCategory.HEIGHT;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return null;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<PortalConversionRecipe> holder, IFocusGroup focuses) {
        PortalConversionRecipe recipe = holder.value();
        JeiBlockIngredientUtil.addInputSlot(builder, PortalConversionCategory.INPUT_BLOCK, 4, 4, 18, 18, recipe.getInput());
        JeiBlockIngredientUtil.addSlot(
            builder,
            RecipeIngredientRole.OUTPUT,
            PortalConversionCategory.OUTPUT_BLOCK,
            142,
            4,
            18,
            18,
            recipe.getResults().states().stream()
                .map(result -> new ItemStack(result.state().state().getBlock()))
                .toList()
        );
    }

    @Override
    public void createRecipeExtras(
        IRecipeExtrasBuilder builder, RecipeHolder<PortalConversionRecipe> holder, IFocusGroup focuses
    ) {
        JeiBlockIngredientUtil.suppressHoverOverlays(builder);
    }

    @Override
    public void draw(
        RecipeHolder<PortalConversionRecipe> holder,
        IRecipeSlotsView view,
        GuiGraphicsExtractor graphics,
        double mouseX,
        double mouseY
    ) {
        PortalConversionRecipe recipe = holder.value();
        RENDER_INPUT: {
            List<BlockState> input = recipe.getInput().constructStatesForRender();
            if (input.isEmpty()) break RENDER_INPUT;
            BlockState renderedState = JeiBlockIngredientUtil.getDisplayedState(view, PortalConversionCategory.INPUT_BLOCK, input)
                .orElse(input.getFirst());
            JeiRenderHelper.renderBlockWithSlot(
                graphics,
                this.slotDefault,
                renderedState,
                4,
                4
            );
        }

        Identifier location = PortalConversionCategory.computePortalTexture(recipe.getPortalType().getId());
        MDImageComponent.Size size = PortalConversionCategory.resolveSize(Minecraft.getInstance(), location);
        MDImageComponent.Size renderSize = PortalConversionCategory.computeRenderSize(size);
        int x = 26 + (PortalConversionCategory.PORTAL_WIDTH - renderSize.width()) / 2;
        int y = (PortalConversionCategory.PORTAL_HEIGHT - renderSize.height()) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, location, x, y, 0, 0,
            renderSize.width(), renderSize.height(), renderSize.width(), renderSize.height());

        List<WeightedChanceBlockStates.Entry> results = recipe.getResults().states();
        if (!results.isEmpty()) {
            List<BlockState> resultStates = results.stream().map(result -> result.state().state()).toList();
            BlockState displayedState = JeiBlockIngredientUtil.getDisplayedState(view, PortalConversionCategory.OUTPUT_BLOCK, resultStates)
                .orElse(resultStates.getFirst());
            WeightedChanceBlockStates.Entry result = results.stream()
                .filter(entry -> entry.state().state().is(displayedState.getBlock()))
                .findFirst()
                .orElse(results.getFirst());
            JeiRenderHelper.renderBlockWithSlot(
                graphics,
                result.state().chance() instanceof ConstantValue(float value) && value == 1.0F ? this.slotDefault : this.slotProbability,
                displayedState,
                142,
                4
            );
        }
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

        List<WeightedChanceBlockStates.Entry> results = recipe.value().getResults().states();
        if (results.size() == 1) {
            if (!MathUtil.isInRange(mouseX, mouseY, 142, 4, 159, 21)) return;
            WeightedChanceBlockStates.Entry result = results.getFirst();
            List<Component> tooltips = TooltipUtil.recipeIDTooltip(result.state().state().getBlock(), recipe.id().identifier());
            tooltips.addAll(tooltips.size() - 1, JeiRecipeUtil.getTooltips(result.state().chance()));
            tooltip.addAll(tooltips);
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.PORTAL_CONVERSION,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.PORTAL_CONVERSION.get())
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(AnvilCraftJeiPlugin.PORTAL_CONVERSION, Blocks.END_PORTAL_FRAME);
        registration.addCraftingStation(AnvilCraftJeiPlugin.PORTAL_CONVERSION, Blocks.OBSIDIAN);
    }

    protected static Identifier computePortalTexture(Identifier typeId) {
        return JeiTextures.texture("portal/" + typeId.toShortLanguageKey().replace(':', '_'));
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

    public static final Map<Identifier, MDImageComponent.Size> IMAGE_SIZE_CACHE = new HashMap<>();

    /**
     * 获取图片原始尺寸，缺失时使用缓存或回退默认值。
     */
    protected static MDImageComponent.Size resolveSize(Minecraft minecraft, Identifier location) {
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
