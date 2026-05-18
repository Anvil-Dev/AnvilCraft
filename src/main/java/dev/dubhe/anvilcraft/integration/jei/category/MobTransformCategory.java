package dev.dubhe.anvilcraft.integration.jei.category;

import dev.anvilcraft.lib.v2.util.Util;
import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.recipe.transform.MobTransformRecipe;
import dev.dubhe.anvilcraft.recipe.transform.TransformResult;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MobTransformCategory implements IRecipeCategory<RecipeHolder<MobTransformRecipe>> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;

    private final IDrawable icon;
    private final IDrawable slotDefault;
    private final IDrawable slotChoice;
    private final IDrawable slotProbability;
    private final Component title;

    private final IDrawable arrowDefault;

    private static final String KEY_CATEGORY = "gui.anvilcraft.category.mob_transform";

    public MobTransformCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableItemStack(ModBlocks.CORRUPTED_BEACON.asStack());
        this.slotDefault = JeiRenderHelper.getSlotDefault(helper);
        this.slotChoice = JeiRenderHelper.getSlotChoice(helper);
        this.slotProbability = JeiRenderHelper.getSlotProbability(helper);
        this.title = Component.translatable(KEY_CATEGORY);

        this.arrowDefault = JeiRenderHelper.getArrowDefault(helper);
    }

    @Override
    public IRecipeHolderType<MobTransformRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.MOB_TRANSFORM;
    }

    @Override
    public Component getTitle() {
        return this.title;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return this.icon;
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
    public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<MobTransformRecipe> recipe, IFocusGroup focuses) {
        SpawnEggItem spawnEggItemInput = SpawnEggItem.byId(recipe.value().input())
            .map(Holder::value)
            .map(Util::<SpawnEggItem>cast)
            .orElse(null);
        if (spawnEggItemInput == null) {
            String name = recipe.value().input().toShortString();
            ItemStack x = Items.BARRIER.getDefaultInstance();
            x.set(DataComponents.CUSTOM_NAME, Component.literal(name));
            builder.addSlot(RecipeIngredientRole.INPUT, 21, 24).add(new ItemStackTemplate(x.typeHolder(), 1, x.getComponentsPatch()));
        } else {
            builder.addSlot(RecipeIngredientRole.INPUT, 21, 24).add(spawnEggItemInput);
        }

        List<ChanceItemStack> outputStacks = new ArrayList<>();
        for (TransformResult result : recipe.value().results()) {
            SpawnEggItem spawnEggOutput = SpawnEggItem.byId(result.resultEntityType())
                .map(Holder::value)
                .map(Util::<SpawnEggItem>cast)
                .orElse(null);
            if (spawnEggOutput == null) {
                String name = result.resultEntityType().toShortString();
                ItemStack x = Items.BARRIER.getDefaultInstance();
                x.set(DataComponents.CUSTOM_NAME, Component.literal(name));
                outputStacks.add(ChanceItemStack.of(ItemStackTemplate.fromNonEmptyStack(x), (float) result.probability()));
            } else {
                outputStacks.add(ChanceItemStack.of(new ItemStackTemplate(spawnEggOutput, 1), (float) result.probability()));
            }
        }
        JeiSlotUtil.addOutputSlots(builder, outputStacks);
    }

    @Override
    public void draw(
        RecipeHolder<MobTransformRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphicsExtractor graphics,
        double mouseX,
        double mouseY) {
        final MobTransformRecipe recipe = recipeHolder.value();

        BlockState block = ModBlocks.CORRUPTED_BEACON
            .get()
            .defaultBlockState()
            .trySetValue(BlockStateProperties.WATERLOGGED, false);

        RenderSupport.renderBlock(
            graphics,
            block,
            81,
            40,
            12);

        this.arrowDefault.draw(graphics, 74, 22);

        JeiSlotUtil.drawInputSlots(graphics, this.slotDefault, 1);
        if (this.isChance(recipe.results())) {
            JeiSlotUtil.drawOutputSlots(graphics, this.slotChoice, recipe.results().size());
        } else {
            JeiSlotUtil.drawOutputSlots(graphics, this.slotProbability, recipe.results().size());
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.MOB_TRANSFORM,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.MOB_TRANSFORM.get())
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(AnvilCraftJeiPlugin.MOB_TRANSFORM, ModBlocks.CORRUPTED_BEACON);
    }

    private boolean isChance(List<TransformResult> results) {
        for (TransformResult result : results) {
            return result.probability() != 1.0;
        }
        return false;
    }
}
