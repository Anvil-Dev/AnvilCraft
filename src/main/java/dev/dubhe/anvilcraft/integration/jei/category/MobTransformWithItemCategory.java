package dev.dubhe.anvilcraft.integration.jei.category;

import com.mojang.blaze3d.vertex.PoseStack;
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
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

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
        icon = helper.createDrawableItemStack(ModBlocks.CORRUPTED_BEACON.asStack());
        slotDefault = JeiRenderHelper.getSlotDefault(helper);
        slotProbability = JeiRenderHelper.getSlotProbability(helper);
        title = Component.translatable(KEY_CATEGORY);

        arrowDefault = JeiRenderHelper.getArrowDefault(helper);
    }

    @Override
    public RecipeType<RecipeHolder<MobTransformWithItemRecipe>> getRecipeType() {
        return AnvilCraftJeiPlugin.MOB_TRANSFORM_WITH_ITEM;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
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
        SpawnEggItem spawnEggItemInput = SpawnEggItem.byId(recipe.value().input());
        if (spawnEggItemInput == null) {
            inputIngredients.add(
                ItemIngredientPredicate.Builder.item().of(Items.BARRIER)
                    .hasComponents(
                        DataComponentPredicate.builder()
                            .expect(DataComponents.CUSTOM_NAME, Component.literal(recipe.value().input().toShortString()))
                            .build())
                    .build());
        } else {
            inputIngredients.add(ItemIngredientPredicate.Builder.item().of(spawnEggItemInput).build());
        }
        inputIngredients.addAll(recipe.value().itemIngredients());
        JeiSlotUtil.addInputSlots(builder, inputIngredients);

        List<ChanceItemStack> outputStacks = new ArrayList<>();
        SpawnEggItem spawnEggItemOutput = SpawnEggItem.byId(recipe.value().specialResult().resultEntityType());
        if (spawnEggItemOutput == null) {
            String name = recipe.value().specialResult().resultEntityType().toShortString();
            ItemStack x = Items.BARRIER.getDefaultInstance();
            x.set(DataComponents.CUSTOM_NAME, Component.literal(name));
            outputStacks.add(ChanceItemStack.of(x.copyWithCount(1)));
        } else {
            outputStacks.add(ChanceItemStack.of(spawnEggItemOutput.getDefaultInstance().copyWithCount(1)));
        }
        outputStacks.add(ChanceItemStack.of(recipe.value().itemResult().copyWithCount(1)));
        JeiSlotUtil.addOutputSlots(builder, outputStacks);

        builder.addInvisibleIngredients(RecipeIngredientRole.CATALYST)
            .addItemStack(ModBlocks.CORRUPTED_BEACON.asStack());
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(
            AnvilCraftJeiPlugin.MOB_TRANSFORM_WITH_ITEM,
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.MOB_TRANSFORM_WITH_ITEM_TYPE.get())
        );
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.CORRUPTED_BEACON), AnvilCraftJeiPlugin.MOB_TRANSFORM_WITH_ITEM);
    }

    @Override
    public void draw(
        RecipeHolder<MobTransformWithItemRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY
    ) {
        final MobTransformWithItemRecipe recipe = recipeHolder.value();

        BlockState block = ModBlocks.CORRUPTED_BEACON
            .get()
            .defaultBlockState()
            .trySetValue(BlockStateProperties.WATERLOGGED, false);

        RenderSupport.renderBlock(
            guiGraphics,
            block,
            81,
            40,
            10,
            12,
            RenderSupport.SINGLE_BLOCK
        );

        arrowDefault.draw(guiGraphics, 74, 22);

        JeiSlotUtil.drawInputSlots(guiGraphics, slotDefault, 2);
        if (recipe.chancePercentPerItem() == 0) {
            JeiSlotUtil.drawOutputSlots(guiGraphics, slotDefault, 2);
        } else {
            JeiSlotUtil.drawOutputSlots(guiGraphics, slotProbability, 2);
        }

        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.scale(0.8f, 0.8f, 1.0f);
        guiGraphics.drawString(
            Minecraft.getInstance().font,
            Component.translatable(KEY_CHANCE, recipe.chancePercentPerItem()),
            0, 70, 0xFF000000, false
        );
        pose.popPose();
    }
}

