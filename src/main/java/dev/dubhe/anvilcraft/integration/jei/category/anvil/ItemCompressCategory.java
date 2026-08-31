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
import dev.dubhe.anvilcraft.integration.jei.util.JeiItemUtil;
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
import mezz.jei.api.recipe.types.IRecipeHolderType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.common.util.RegistryUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
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
    public IRecipeHolderType<ItemCompressRecipe> getRecipeType() {
        return AnvilCraftJeiPlugin.ITEM_COMPRESS;
    }

    @Override
    public void setRecipe(
        IRecipeLayoutBuilder builder,
        RecipeHolder<ItemCompressRecipe> recipeHolder,
        IFocusGroup focuses
    ) {
        ItemCompressRecipe recipe = recipeHolder.value();
        boolean powered = recipeHolder.id().identifier().getPath().equals(ItemCompressCategory.SUPERCAPACITOR);
        boolean normal = recipeHolder.id().identifier().getPath().equals(ItemCompressCategory.EMPTY_SUPERCAPACITOR);
        if (!powered && !normal) {
            super.setRecipe(builder, recipeHolder, focuses);
            return;
        }
        List<ItemIngredientPredicate> inputs = recipe.getInputItems();
        int inputSlotStartX = JeiSlotUtil.INPUT_X - JeiSlotUtil.OFFSET / 2;
        JeiSlotUtil.addSlotWithCount(builder, inputSlotStartX, JeiSlotUtil.DEFAULT_Y, inputs.getFirst());
        builder.addSlot(RecipeIngredientRole.INPUT, inputSlotStartX + JeiSlotUtil.OFFSET, JeiSlotUtil.DEFAULT_Y)
            .add(ItemCompressCategory.resinWithCreeper(powered))
            .addRichTooltipCallback((slotView, tooltip) ->
                tooltip.add(Component.translatable(powered
                                                   ? "gui.anvilcraft.category.item_compress.supercapacitor.resin"
                                                   : "gui.anvilcraft.category.item_compress.supercapacitor_empty.resin")));
        if (powered) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, JeiSlotUtil.OUTPUT_X, JeiSlotUtil.DEFAULT_Y)
                .add(ModItems.SUPER_CAPACITOR.asStack())
                .addRichTooltipCallback((slotView, tooltip) ->
                    tooltip.add(Component.translatable("gui.anvilcraft.category.item_compress.supercapacitor.chance")));
        } else {
            JeiItemUtil.addDefaultOutputSlots(builder, recipe.getResultItems());
        }
    }

    @Override
    public void draw(
        RecipeHolder<ItemCompressRecipe> recipeHolder,
        IRecipeSlotsView view,
        GuiGraphicsExtractor graphics,
        double mouseX,
        double mouseY
    ) {
        final ItemCompressRecipe recipe = recipeHolder.value();
        int anvilYOffset = JeiRenderHelper.getAnvilAnimationOffset(this.timer);
        RenderSupport.renderBlock(graphics, Blocks.CAULDRON.defaultBlockState(), 71, 35, 20);
        RenderSupport.renderBlock(graphics, Blocks.ANVIL.defaultBlockState(), 71, 17 + anvilYOffset, 20);

        this.arrowIn.draw(graphics, 54, 30);
        this.arrowOutFromBelow.draw(graphics, 92, 29);

        JeiSlotUtil.drawDefaultInputSlots(graphics, this.slotDefault, recipe.getInputItems().size());
        if (recipeHolder.id().identifier().getPath().equals(ItemCompressCategory.SUPERCAPACITOR)
            || JeiRecipeUtil.isChance(recipe.getResultItems())) {
            JeiSlotUtil.drawDefaultOutputSlots(graphics, this.slotProbability, recipe.getResultItems().size());
        } else {
            JeiSlotUtil.drawDefaultOutputSlots(graphics, this.slotDefault, recipe.getResultItems().size());
        }
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        List<RecipeHolder<ItemCompressRecipe>> recipes = new ArrayList<>(
            JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.ITEM_COMPRESS.get())
        );
        recipes.add(new RecipeHolder<>(
            ResourceKey.create(Registries.RECIPE, AnvilCraft.of(ItemCompressCategory.SUPERCAPACITOR)),
            ItemCompressCategory.specialSupercapacitorRecipe()
        ));
        registration.addRecipes(
            AnvilCraftJeiPlugin.ITEM_COMPRESS,
            recipes
        );
    }

    private static ItemCompressRecipe specialSupercapacitorRecipe() {
        HolderGetter<Item> items = RegistryUtil.getRegistryAccess().lookupOrThrow(Registries.ITEM);
        return ItemCompressRecipe.builder()
            .requires(items, ModItemTags.IRON_PLATES, 2)
            .requires(ItemStackTemplate.fromNonEmptyStack(ItemCompressCategory.resinWithCreeper(true)))
            .result(ModItems.SUPER_CAPACITOR)
            .buildRecipe();
    }

    private static ItemStack resinWithCreeper(boolean powered) {
        CompoundTag entityTag = new CompoundTag();
        if (powered) entityTag.putBoolean("powered", true);
        ItemStack resin = ModBlocks.RESIN_BLOCK.asStack();
        resin.set(ModComponents.SAVED_ENTITY, new SavedEntity(EntityType.CREEPER, entityTag, true));
        return resin;
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        AnvilCraftJeiPlugin.addAnvilCauldronCatalysts(registration, AnvilCraftJeiPlugin.ITEM_COMPRESS);
    }
}
