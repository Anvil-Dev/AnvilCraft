package dev.dubhe.anvilcraft.integration.jei.category;

import dev.dubhe.anvilcraft.init.ModBlocks;
import dev.dubhe.anvilcraft.init.ModItems;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.recipe.BeaconConversionRecipe;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.TextureConstants;
import dev.dubhe.anvilcraft.util.LevelLike;
import dev.dubhe.anvilcraft.util.RenderHelper;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class BeaconConversionCategory implements IRecipeCategory<BeaconConversionRecipe> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 128;

    private final IDrawable slot;
    private final IDrawable progressArrow;
    private final Component title;
    private final Component activateTooltip;
    private final Component beaconBaseTooltip;
    private final IDrawable arrowIn;

    private final Map<BeaconConversionRecipe, LevelLike> cache = new HashMap<>();

    public BeaconConversionCategory(IGuiHelper helper) {
        slot = helper.getSlotDrawable();
        title = Component.translatable("gui.anvilcraft.category.beacon_conversion");
        activateTooltip = Component.translatable("gui.anvilcraft.category.beacon_conversion.activate")
            .withStyle(ChatFormatting.GOLD);
        beaconBaseTooltip = Component.translatable("gui.anvilcraft.category.beacon_conversion.beacon_base")
            .withStyle(ChatFormatting.GOLD);
        progressArrow = helper.drawableBuilder(TextureConstants.PROGRESS, 0, 0, 24, 16)
            .setTextureSize(24, 16)
            .build();
        arrowIn = helper.createDrawable(TextureConstants.ANVIL_CRAFT_SPRITES, 0, 31, 16, 8);
    }

    @Override
    public RecipeType<BeaconConversionRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.BEACON_CONVERSION;
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
        return new DrawableBlockStateIcon(
            Blocks.BEACON.defaultBlockState(),
            ModBlocks.CURSED_GOLD_BLOCK.getDefaultState()
        );
    }

    @Override
    public void setRecipe(
        IRecipeLayoutBuilder builder, BeaconConversionRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 48, 8)
            .addItemStack(ModItems.CURSED_GOLD_INGOT.asStack())
            .addRichTooltipCallback((recipeSlotView, tooltip) ->
                tooltip.add(activateTooltip));
        builder.addSlot(RecipeIngredientRole.CATALYST, 10, 110)
            .addItemStack(ModBlocks.CURSED_GOLD_BLOCK.asStack(recipe.cursedGoldBlockCount))
            .addRichTooltipCallback((recipeSlotView, tooltip) ->
                tooltip.add(beaconBaseTooltip));
        builder.addSlot(RecipeIngredientRole.INPUT, 10, 92)
            .addItemStack(Blocks.BEACON.asItem().getDefaultInstance());
        IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.OUTPUT, 130, 96)
            .addItemStack(ModBlocks.CORRUPTED_BEACON.asStack());
        JeiRecipeUtil.addTooltips(slot, recipe.corruptedBeaconOutput.getStack().getCount(), recipe.corruptedBeaconOutput.getCount());
        if (recipe.chance < 1.0f) {
            slot = builder.addSlot(RecipeIngredientRole.OUTPUT, 112, 96)
                .addItemStack(Blocks.BEACON.asItem().getDefaultInstance());
            JeiRecipeUtil.addTooltips(slot, recipe.beaconOutput.getStack().getCount(), recipe.beaconOutput.getCount());
        }
    }

    @Override
    public void draw(
        BeaconConversionRecipe recipe,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY) {
        LevelLike level = cache.get(recipe);
        if (level == null) {
            LevelLike beaconBase = new LevelLike(Objects.requireNonNull(Minecraft.getInstance().level));
            int layers = recipe.cursedGoldBlockLayers;
            for (int i = 0; i < layers; i++) {
                for (int j = i; j <= 2 * layers - i; j++) {
                    for (int k = i; k <= 2 * layers - i; k++) {
                        beaconBase.setBlockState(new BlockPos(j, i, k), ModBlocks.CURSED_GOLD_BLOCK.getDefaultState());
                    }
                }
            }
            beaconBase.setBlockState(new BlockPos(layers, layers, layers), Blocks.BEACON.defaultBlockState());
            cache.put(recipe, beaconBase);
            level = beaconBase;
        }

        RenderHelper.renderLevelLike(level, guiGraphics, 84, 54, 90);

        slot.draw(guiGraphics, 47, 7);
        slot.draw(guiGraphics, 9, 109);
        slot.draw(guiGraphics, 9, 91);
        if (recipe.chance < 1.0f) slot.draw(guiGraphics, 111, 95);
        slot.draw(guiGraphics, 129, 95);

        arrowIn.draw(guiGraphics, 66, 14);
        progressArrow.draw(guiGraphics, 60, 96);
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.BEACON_CONVERSION,
            BeaconConversionRecipe.getAllRecipes());
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ModBlocks.CURSED_GOLD_BLOCK.asStack(), AnvilCraftJeiPlugin.BEACON_CONVERSION);
    }
}
