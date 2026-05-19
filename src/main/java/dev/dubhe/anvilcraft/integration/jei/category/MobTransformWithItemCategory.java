package dev.dubhe.anvilcraft.integration.jei.category;

import dev.anvilcraft.lib.v2.util.Util;
import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.recipe.transform.MobTransformWithItemRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.advancements.criterion.DataComponentMatchers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentExactPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MobTransformWithItemCategory implements IRecipeCategory<RecipeHolder<MobTransformWithItemRecipe>> {
    public static final int WIDTH = 162;
    public static final int HEIGHT = 64;

    private final IDrawable icon;
    private final IDrawable slotDefault;
    private final IDrawable slotProbability;
    private final Component title;

    private final IDrawable arrowDefault;

    private static final String KEY_CATEGORY = "gui.anvilcraft.category.mob_transform_with_item";
    private static final String KEY_CHANCE = "gui.anvilcraft.category.mob_transform_with_item.chance_per_item";

    public MobTransformWithItemCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableItemStack(ModBlocks.CORRUPTED_BEACON.asStack());
        this.slotDefault = JeiRenderHelper.getSlotDefault(helper);
        this.slotProbability = JeiRenderHelper.getSlotProbability(helper);
        this.title = Component.translatable(KEY_CATEGORY);

        this.arrowDefault = JeiRenderHelper.getArrowDefault(helper);
    }

    @Override
    public IRecipeHolderType<MobTransformWithItemRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.MOB_TRANSFORM_WITH_ITEM;
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
    public void setRecipe(
        IRecipeLayoutBuilder builder,
        RecipeHolder<MobTransformWithItemRecipe> recipe,
        IFocusGroup focuses
    ) {
        List<ItemIngredientPredicate> inputIngredients = new ArrayList<>();
        SpawnEggItem spawnEggItemInput = SpawnEggItem.byId(recipe.value().input())
            .map(Holder::value)
            .map(Util::<SpawnEggItem>cast)
            .orElse(null);
        if (spawnEggItemInput == null) {
            inputIngredients.add(
                ItemIngredientPredicate.Builder.item()
                    .of(Items.BARRIER)
                    .hasComponents(
                        DataComponentMatchers.Builder.components()
                            .exact(DataComponentExactPredicate.expect(
                                DataComponents.CUSTOM_NAME,
                                Component.literal(recipe.value().input().toShortString())
                            ))
                            .build()
                    )
                    .build()
            );
        } else {
            inputIngredients.add(ItemIngredientPredicate.Builder.item().of(spawnEggItemInput).build());
        }
        inputIngredients.addAll(recipe.value().itemIngredients());
        JeiSlotUtil.addInputSlots(builder, inputIngredients);

        List<ChanceItemStack> outputStacks = new ArrayList<>();
        SpawnEggItem spawnEggItemOutput = SpawnEggItem.byId(recipe.value().specialResult().resultEntityType())
            .map(Holder::value)
            .map(Util::<SpawnEggItem>cast)
            .orElse(null);
        if (spawnEggItemOutput == null) {
            String name = recipe.value().specialResult().resultEntityType().toShortString();
            ItemStack x = Items.BARRIER.getDefaultInstance();
            x.set(DataComponents.CUSTOM_NAME, Component.literal(name));
            outputStacks.add(ChanceItemStack.of(ItemStackTemplate.fromNonEmptyStack(x).withCount(1)));
        } else {
            outputStacks.add(ChanceItemStack.of(new ItemStackTemplate(spawnEggItemOutput, 1)));
        }
        outputStacks.add(ChanceItemStack.of(recipe.value().itemResult().withCount(1)));
        JeiSlotUtil.addOutputSlots(builder, outputStacks);
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.MOB_TRANSFORM_WITH_ITEM,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.MOB_TRANSFORM_WITH_ITEM.get())
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(AnvilCraftJeiPlugin.MOB_TRANSFORM_WITH_ITEM, ModBlocks.CORRUPTED_BEACON);
    }

    @Override
    public void draw(
        RecipeHolder<MobTransformWithItemRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphicsExtractor graphics,
        double mouseX,
        double mouseY
    ) {
        final MobTransformWithItemRecipe recipe = recipeHolder.value();

        BlockState block = ModBlocks.CORRUPTED_BEACON
            .getDefaultState()
            .trySetValue(BlockStateProperties.WATERLOGGED, false);

        RenderSupport.renderBlock(graphics, block, 81, 40, 20);

        this.arrowDefault.draw(graphics, 74, 22);

        JeiSlotUtil.drawInputSlots(graphics, this.slotDefault, 2);
        if (recipe.chancePercentPerItem() == 0) {
            JeiSlotUtil.drawOutputSlots(graphics, this.slotDefault, 2);
        } else {
            JeiSlotUtil.drawOutputSlots(graphics, this.slotProbability, 2);
        }

        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.scale(0.8F, 0.8F);
        graphics.text(
            Minecraft.getInstance().font,
            Component.translatable(KEY_CHANCE, recipe.chancePercentPerItem()),
            0, 70, 0xFF000000, false
        );
        pose.popMatrix();
    }
}

