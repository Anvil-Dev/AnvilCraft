package dev.dubhe.anvilcraft.integration.jei.category.anvil;

import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.client.support.RenderSupport;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.drawable.DrawableBlockStateIcon;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.integration.jei.util.JeiSlotUtil;
import dev.dubhe.anvilcraft.item.property.component.SavedEntity;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.ItemCompressRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;

public class ItemCompressCategory extends AbstractProgressCategory<ItemCompressRecipe> {
    private static final String SUPERCAPACITOR = "item_compress/supercapacitor";
    private static final String EMPTY_SUPERCAPACITOR = "item_compress/supercapacitor_empty";

    public ItemCompressCategory(IGuiHelper helper) {
        super(
            helper,
            new DrawableBlockStateIcon(Blocks.ANVIL.defaultBlockState(), Blocks.CAULDRON.defaultBlockState()),
            Component.translatable("gui.anvilcraft.category.item_compress")
        );
    }

    @Override
    public RecipeType<RecipeHolder<ItemCompressRecipe>> getRecipeType() {
        return AnvilCraftJeiPlugin.ITEM_COMPRESS;
    }

    @Override
    public void setRecipe(
        IRecipeLayoutBuilder builder,
        RecipeHolder<ItemCompressRecipe> recipeHolder,
        IFocusGroup focuses
    ) {
        ItemCompressRecipe recipe = recipeHolder.value();
        boolean powered = recipeHolder.id().getPath().equals(SUPERCAPACITOR);
        boolean normal = recipeHolder.id().getPath().equals(EMPTY_SUPERCAPACITOR);
        if (!powered && !normal) {
            super.setRecipe(builder, recipeHolder, focuses);
            return;
        }
        List<ItemIngredientPredicate> inputs = recipe.getInputItems();
        builder.addSlot(RecipeIngredientRole.INPUT, 11, 15).addIngredients(Ingredient.of(inputs.get(0).getItems()));
        builder.addSlot(RecipeIngredientRole.INPUT, 30, 15)
            .addItemStack(resinWithCreeper(powered))
            .addRichTooltipCallback((slotView, tooltip) ->
                tooltip.add(Component.translatable(powered
                    ? "gui.anvilcraft.category.item_compress.supercapacitor.resin"
                    : "gui.anvilcraft.category.item_compress.supercapacitor_empty.resin")));
        if (powered) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 125, 24)
                .addItemStack(ModItems.SUPER_CAPACITOR.asStack())
                .addRichTooltipCallback((slotView, tooltip) ->
                    tooltip.add(Component.translatable("gui.anvilcraft.category.item_compress.supercapacitor.chance")));
        } else {
            JeiSlotUtil.addOutputSlots(builder, recipe.getResultItems());
        }
    }

    @Override
    public void draw(
        RecipeHolder<ItemCompressRecipe> recipeHolder,
        IRecipeSlotsView recipeSlotsView,
        GuiGraphics guiGraphics,
        double mouseX,
        double mouseY) {
        final ItemCompressRecipe recipe = recipeHolder.value();
        float anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(timer);
        RenderSupport.renderBlock(
            guiGraphics,
            Blocks.ANVIL.defaultBlockState(),
            81,
            22 + anvilYOffset,
            20,
            12,
            RenderSupport.SINGLE_BLOCK);
        RenderSupport.renderBlock(
            guiGraphics, Blocks.CAULDRON.defaultBlockState(), 81, 40, 10, 12, RenderSupport.SINGLE_BLOCK);

        arrowIn.draw(guiGraphics, 54, 30);
        arrowOut.draw(guiGraphics, 92, 29);

        JeiSlotUtil.drawInputSlots(guiGraphics, slotDefault, recipe.getInputItems().size());
        if (recipeHolder.id().getPath().equals(SUPERCAPACITOR)
            || JeiRecipeUtil.isChance(recipe.getResultItems())) {
            JeiSlotUtil.drawOutputSlots(guiGraphics, slotProbability, recipe.getResultItems().size());
        } else {
            JeiSlotUtil.drawOutputSlots(guiGraphics, slotDefault, recipe.getResultItems().size());
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        List<RecipeHolder<ItemCompressRecipe>> recipes = new ArrayList<>(
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.ITEM_COMPRESS_TYPE.get())
        );
        recipes.add(new RecipeHolder<>(
            AnvilCraft.of("item_compress/supercapacitor"),
            specialSupercapacitorRecipe()
        ));
        registration.addRecipes(
            AnvilCraftJeiPlugin.ITEM_COMPRESS, recipes);
    }

    private static ItemCompressRecipe specialSupercapacitorRecipe() {
        return ItemCompressRecipe.builder()
            .requires(ModItemTags.IRON_PLATES, 2)
            .requires(ItemIngredientPredicate.Builder.item().of(resinWithCreeper(true)).build())
            .result(ModItems.SUPER_CAPACITOR)
            .buildRecipe();
    }

    private static ItemStack resinWithCreeper(boolean powered) {
        CompoundTag entityTag = new CompoundTag();
        entityTag.putString("id", "minecraft:creeper");
        if (powered) entityTag.putBoolean("powered", true);
        ItemStack resin = ModBlocks.RESIN_BLOCK.asStack();
        resin.set(ModComponents.SAVED_ENTITY, new SavedEntity(entityTag, true));
        return resin;
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilProcessingCatalysts(registration, AnvilCraftJeiPlugin.ITEM_COMPRESS);
        registration.addRecipeCatalyst(new ItemStack(Items.CAULDRON), AnvilCraftJeiPlugin.ITEM_COMPRESS);
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.FISH_TANK), AnvilCraftJeiPlugin.ITEM_COMPRESS);
    }
}
