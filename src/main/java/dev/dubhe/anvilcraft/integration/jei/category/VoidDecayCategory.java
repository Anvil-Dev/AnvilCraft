package dev.dubhe.anvilcraft.integration.jei.category;

import com.google.common.collect.ImmutableList;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.recipe.VoidDecayRecipe;
import dev.dubhe.anvilcraft.integration.jei.util.BlockTagUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.util.LevelLike;
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
import mezz.jei.common.util.RegistryUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class VoidDecayCategory implements IRecipeCategory<VoidDecayRecipe> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 128;
    public static final int MAX_SHOWN_ROW = 7;
    public static final int MAX_SHOWN_COLUMN = 5;
    public static final int MAX_SHOWN_COUNT = MAX_SHOWN_ROW * MAX_SHOWN_COLUMN;

    private final IDrawable slot;
    private final Component title;
    private final IDrawable arrowDefault;
    private final Component randomTickTooltip;
    private final Component centerTooltip;
    private final Component aroundTooltip;
    private final Component notConsumedTooltip;

    private final Map<VoidDecayRecipe, LevelLike> cache = new HashMap<>();

    private static final ImmutableList<BlockPos> CATALYST_POS = ImmutableList.of(
        new BlockPos(1, 0, 1),
        new BlockPos(1, 1, 0),
        new BlockPos(1, 1, 2),
        new BlockPos(1, 2, 1),
        new BlockPos(0, 1, 1)
    );
    private static final BlockPos CENTER_POS = new BlockPos(1, 1, 1);

    public VoidDecayCategory(IGuiHelper helper) {
        slot = JeiRenderHelper.getSlotChoice(helper);
        title = Component.translatable("gui.anvilcraft.category.void_decay");
        randomTickTooltip = Component.translatable("gui.anvilcraft.category.void_decay.random_tick");
        centerTooltip = Component.translatable("gui.anvilcraft.category.void_decay.center")
            .withStyle(ChatFormatting.GOLD);
        aroundTooltip = Component.translatable("gui.anvilcraft.category.void_decay.around")
            .withStyle(ChatFormatting.GOLD);
        notConsumedTooltip = Component.translatable("gui.anvilcraft.category.void_decay.not_consumed")
            .withStyle(ChatFormatting.GOLD);
        arrowDefault = JeiRenderHelper.getArrowDefault(helper);
    }

    @Override
    public RecipeType<VoidDecayRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.VOID_DECAY;
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
    public void setRecipe(
        IRecipeLayoutBuilder builder, VoidDecayRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 8, 84)
            .addItemStack(recipe.center.asItem().getDefaultInstance())
            .addRichTooltipCallback((recipeSlotView, tooltip) ->
                tooltip.add(centerTooltip));
        builder.addSlot(RecipeIngredientRole.CATALYST, 8, 102)
            .addItemStack(new ItemStack(recipe.catalyst.asItem(), recipe.catalystCount))
            .addRichTooltipCallback((recipeSlotView, tooltip) ->
                tooltip.addAll(List.of(aroundTooltip, notConsumedTooltip)));
        builder.addInvisibleIngredients(RecipeIngredientRole.OUTPUT)
            .addIngredients(BlockTagUtil.toIngredient(recipe.result));
        AtomicInteger slotId = new AtomicInteger(0);
        RegistryUtil.getRegistry(Registries.BLOCK)
            .getTag(recipe.result)
            .stream()
            .flatMap(HolderSet.ListBacked::stream)
            .map(h -> h.value().asItem().getDefaultInstance())
            .limit(MAX_SHOWN_COUNT)
            .forEach(stack -> {
                int id = slotId.getAndAdd(1);
                builder.addSlot(RecipeIngredientRole.RENDER_ONLY,
                        (id % MAX_SHOWN_COLUMN) * 18 + 66,
                        (id / MAX_SHOWN_COLUMN) * 18 + 4)
                    .addItemStack(stack);
            });
    }

    @Override
    public void draw(
        VoidDecayRecipe recipe,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY) {
        LevelLike level = cache.get(recipe);
        if (level == null) {
            LevelLike showCase = new LevelLike(Minecraft.getInstance().level);
            CATALYST_POS.forEach(pos -> showCase.setBlockState(pos, recipe.catalyst.defaultBlockState()));
            showCase.setBlockState(CENTER_POS, recipe.center.defaultBlockState());
            cache.put(recipe, showCase);
            level = showCase;
        }

        RenderSupport.renderLevelLike(level, guiGraphics, 24, 36, 60, 0.5f);

        slot.draw(guiGraphics, 7, 83);
        slot.draw(guiGraphics, 7, 101);
        for (int i = 0; i < MAX_SHOWN_ROW; i++) {
            for (int j = 0; j < MAX_SHOWN_COLUMN; j++) {
                int x = 65 + j * 18;
                int y = 3 + i * 18;
                slot.draw(guiGraphics, x, y);
            }
        }

        arrowDefault.draw(guiGraphics, 35, 87);
    }

    @Override
    public void getTooltip(
        ITooltipBuilder tooltip,
        VoidDecayRecipe recipe,
        IRecipeSlotsView recipeSlotsView,
        double mouseX,
        double mouseY) {
        if (mouseX >= 5 && mouseX <= 45 && mouseY >= 15 && mouseY <= 65) {
            tooltip.add(randomTickTooltip);
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.VOID_DECAY,
            VoidDecayRecipe.getAllRecipes());
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ModBlocks.VOID_MATTER_BLOCK.asStack(), AnvilCraftJeiPlugin.VOID_DECAY);
    }
}
