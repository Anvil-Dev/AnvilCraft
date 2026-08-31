package dev.dubhe.anvilcraft.integration.jei.category;

import dev.anvilcraft.lib.v2.util.Util;
import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.recipe.MobTransformJeiRecipe;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.recipe.transform.TransformResult;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MobTransformCategory implements IRecipeCategory<MobTransformJeiRecipe> {
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
        this.title = Component.translatable(MobTransformCategory.KEY_CATEGORY);

        this.arrowDefault = JeiRenderHelper.getArrowDefault(helper);
    }

    @Override
    public IRecipeType<MobTransformJeiRecipe> getRecipeType() {
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
    public Identifier getIdentifier(MobTransformJeiRecipe recipe) {
        return recipe.id();
    }

    @Override
    public int getWidth() {
        return MobTransformCategory.WIDTH;
    }

    @Override
    public int getHeight() {
        return MobTransformCategory.HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MobTransformJeiRecipe recipe, IFocusGroup focuses) {
        SpawnEggItem spawnEggItemInput = SpawnEggItem.byId(recipe.input())
            .map(Holder::value)
            .map(Util::<SpawnEggItem>cast)
            .orElse(null);
        if (spawnEggItemInput == null) {
            String name = recipe.input().toShortString();
            ItemStack x = Items.BARRIER.getDefaultInstance();
            x.set(DataComponents.CUSTOM_NAME, Component.literal(name));
            builder.addSlot(RecipeIngredientRole.INPUT, 21, 24).add(new ItemStackTemplate(x.typeHolder(), 1, x.getComponentsPatch()));
        } else {
            builder.addSlot(RecipeIngredientRole.INPUT, 21, 24).add(spawnEggItemInput);
        }

        for (int index = 0; index < recipe.inputItems().size(); index++) {
            ItemIngredientPredicate ingredient = recipe.inputItems().get(index);
            JeiSlotUtil.addSlotWithCount(builder, 53 + index * 19, 6, ingredient);
        }
        if (!recipe.outputItem().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 92, 6).add(recipe.outputItem());
        }

        List<ChanceItemStack> outputStacks = new ArrayList<>();
        for (TransformResult result : recipe.results()) {
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
        MobTransformJeiRecipe recipe,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphicsExtractor graphics,
        double mouseX,
        double mouseY) {
        BlockState block = ModBlocks.CORRUPTED_BEACON
            .get()
            .defaultBlockState()
            .trySetValue(BlockStateProperties.WATERLOGGED, false);

        RenderSupport.renderBlock(
            graphics,
            block,
            71,
            35,
            20);

        this.arrowDefault.draw(graphics, 74, 22);

        JeiSlotUtil.drawInputSlots(graphics, this.slotDefault, 1);
        for (int index = 0; index < recipe.inputItems().size(); index++) {
            this.slotDefault.draw(graphics, 52 + index * 19, 5);
        }
        if (!recipe.outputItem().isEmpty()) this.slotDefault.draw(graphics, 91, 5);
        if (this.isChance(recipe.results())) {
            JeiSlotUtil.drawOutputSlots(graphics, this.slotChoice, recipe.results().size());
        } else {
            JeiSlotUtil.drawOutputSlots(graphics, this.slotProbability, recipe.results().size());
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        List<MobTransformJeiRecipe> recipes = new ArrayList<>();
        JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.MOB_TRANSFORM.get())
            .stream()
            .map(MobTransformJeiRecipe::ofStandard)
            .forEach(recipes::add);
        JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.MOB_TRANSFORM_WITH_ITEM.get())
            .stream()
            .map(MobTransformJeiRecipe::ofWithItem)
            .forEach(recipes::add);
        registration.addRecipes(AnvilCraftJeiPlugin.MOB_TRANSFORM, recipes);
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
