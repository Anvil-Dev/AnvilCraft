package dev.dubhe.anvilcraft.integration.jei.category;

import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.recipe.BeaconConversionRecipe;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.util.LevelLike;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class BeaconConversionCategory implements IRecipeCategory<BeaconConversionRecipe> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 128;

    private final IDrawable slotDefault;
    private final IDrawable slotChoice;
    private final IDrawable icon;
    private final Component title;
    private final Component activateTooltip;
    private final Component beaconBaseTooltip;
    protected final IDrawable arrowIn;
    protected final IDrawable arrowDefault;

    private final Map<BeaconConversionRecipe, LevelLike> cache = new HashMap<>();

    public BeaconConversionCategory(IGuiHelper helper) {
        this.slotDefault = JeiRenderHelper.getSlotDefault(helper);
        this.slotChoice = JeiRenderHelper.getSlotChoice(helper);
        this.icon = new DrawableBlockStateIcon(
            Blocks.BEACON
                .defaultBlockState()
                .trySetValue(BlockStateProperties.WATERLOGGED, false),
            ModBlocks.CURSED_GOLD_BLOCK.getDefaultState()
        );
        this.title = Component.translatable("gui.anvilcraft.category.beacon_conversion");
        this.activateTooltip = Component.translatable("gui.anvilcraft.category.beacon_conversion.activate")
            .withStyle(ChatFormatting.GOLD);
        this.beaconBaseTooltip = Component.translatable("gui.anvilcraft.category.beacon_conversion.beacon_base")
            .withStyle(ChatFormatting.GOLD);
        this.arrowDefault = JeiRenderHelper.getArrowDefault(helper);
        this.arrowIn = JeiRenderHelper.getArrowInput(helper);
    }

    @Override
    public IRecipeType<BeaconConversionRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.BEACON_CONVERSION;
    }

    @Override
    public Component getTitle() {
        return this.title;
    }

    @Override
    public int getWidth() {
        return BeaconConversionCategory.WIDTH;
    }

    @Override
    public int getHeight() {
        return BeaconConversionCategory.HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, BeaconConversionRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 48, 8).add(ModItems.CURSED_GOLD_INGOT.asStack())
            .addRichTooltipCallback((_, tooltip) -> tooltip.add(this.activateTooltip));
        builder.addSlot(RecipeIngredientRole.CRAFTING_STATION, 10, 110)
            .add(ModBlocks.CURSED_GOLD_BLOCK.asStack(recipe.cursedGoldBlockCount))
            .addRichTooltipCallback((_, tooltip) -> tooltip.add(this.beaconBaseTooltip));
        builder.addSlot(RecipeIngredientRole.INPUT, 10, 92).add(Blocks.BEACON.asItem().getDefaultInstance());
        IRecipeSlotBuilder slot = builder.addSlot(
            RecipeIngredientRole.OUTPUT,
            130,
            96
        ).add(ModBlocks.CORRUPTED_BEACON.asStack());
        JeiRecipeUtil.addTooltips(
            slot,
            recipe.corruptedBeaconOutput.stack().count(),
            recipe.corruptedBeaconOutput.count()
        );
        if (recipe.chance < 1.0F) {
            slot = builder.addSlot(
                RecipeIngredientRole.OUTPUT,
                112,
                96
            ).add(Blocks.BEACON.asItem().getDefaultInstance());
            JeiRecipeUtil.addTooltips(slot, recipe.beaconOutput.stack().count(), recipe.beaconOutput.count());
        }
    }

    @Override
    public void draw(
        BeaconConversionRecipe recipe,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphicsExtractor graphics,
        double mouseX,
        double mouseY
    ) {
        LevelLike level = this.cache.get(recipe);
        int layers = recipe.cursedGoldBlockLayers;
        if (level == null) {
            LevelLike beaconBase = new LevelLike(Objects.requireNonNull(Minecraft.getInstance().level));

            for (int i = 0; i < layers; i++) {
                for (int j = i; j <= 2 * layers - i; j++) {
                    for (int k = i; k <= 2 * layers - i; k++) {
                        beaconBase.setBlockState(
                            new BlockPos(j - layers / 2, i - layers / 2, k - layers / 2),
                            ModBlocks.CURSED_GOLD_BLOCK.getDefaultState()
                        );
                    }
                }
            }

            BlockState block = ModBlocks.CORRUPTED_BEACON
                .get()
                .defaultBlockState()
                .trySetValue(BlockStateProperties.WATERLOGGED, false);
            beaconBase.setBlockState(
                new BlockPos(layers - layers / 2, layers - layers / 2, layers - layers / 2),
                block
            );
            this.cache.put(recipe, beaconBase);
            level = beaconBase;
        }

        RenderSupport.renderLevelLike(level, graphics, 24, 0, 120, layers == 1 ? 15 : 20 / layers, 0, false);

        this.slotDefault.draw(graphics, 47, 7);
        this.slotDefault.draw(graphics, 9, 109);
        this.slotDefault.draw(graphics, 9, 91);
        if (recipe.chance < 1.0F) {
            this.slotChoice.draw(graphics, 111, 95);
            this.slotChoice.draw(graphics, 129, 95);
        } else {
            this.slotDefault.draw(graphics, 129, 95);
        }

        this.arrowIn.draw(graphics, 66, 14);
        this.arrowDefault.draw(graphics, 60, 96);
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(AnvilCraftJeiPlugin.BEACON_CONVERSION, BeaconConversionRecipe.getAllRecipes());
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(AnvilCraftJeiPlugin.BEACON_CONVERSION, ModBlocks.CURSED_GOLD_BLOCK);
    }
}
