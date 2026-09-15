package dev.dubhe.anvilcraft.integration.jei.category;

import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.recipe.ModRecipeTypes;
import dev.dubhe.anvilcraft.integration.jei.AnvilCraftJeiPlugin;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRecipeUtil;
import dev.dubhe.anvilcraft.integration.jei.util.JeiRenderHelper;
import dev.dubhe.anvilcraft.recipe.frost.AndFrostMaterialPredicate;
import dev.dubhe.anvilcraft.recipe.frost.CustomFrostMaterialPredicate;
import dev.dubhe.anvilcraft.recipe.frost.DeformationRecipe;
import dev.dubhe.anvilcraft.recipe.frost.EmptyFrostMaterialPredicate;
import dev.dubhe.anvilcraft.recipe.frost.FrostSmithingOption;
import dev.dubhe.anvilcraft.recipe.frost.IFrostMaterialPredicate;
import dev.dubhe.anvilcraft.recipe.frost.IFrostSmithingRecipe;
import dev.dubhe.anvilcraft.recipe.frost.OrFrostMaterialPredicate;
import dev.dubhe.anvilcraft.recipe.frost.PermutationRecipe;
import dev.dubhe.anvilcraft.recipe.frost.RepairMaterialFrostMaterialPredicate;
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
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FrostSmithingCategory implements IRecipeCategory<FrostSmithingCategory.Display> {
    private static final Map<Item, List<ItemStack>> REPAIR_MATERIAL_CACHE = new HashMap<>();

    private final IDrawable icon;
    private final IDrawable arrow;

    public FrostSmithingCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableItemStack(ModBlocks.FROST_SMITHING_TABLE.asStack());
        this.arrow = JeiRenderHelper.getArrowDefault(helper);
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        List<IFrostSmithingRecipe> recipes = new ArrayList<>();
        JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.PERMUTATION_TYPE.get())
            .forEach(holder -> recipes.add(holder.value()));
        JeiRecipeUtil.getRecipeHoldersFromType(ModRecipeTypes.DEFORMATION_TYPE.get())
            .forEach(holder -> recipes.add(holder.value()));
        List<Display> displays = new ArrayList<>();
        for (IFrostSmithingRecipe recipe : recipes) {
            for (ItemStack input : recipe.possibleInputs()) {
                FrostSmithingCategory.groupByMaterial(recipe.options(input))
                    .forEach((material, options) -> displays.add(new Display(recipe, input, material, options)));
            }
        }
        registration.addRecipes(AnvilCraftJeiPlugin.FROST_SMITHING, displays);
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ModBlocks.FROST_SMITHING_TABLE.asStack(), AnvilCraftJeiPlugin.FROST_SMITHING);
        registration.addRecipeCatalyst(ModBlocks.TRANSCENDENCE_SMITHING_TABLE.asStack(), AnvilCraftJeiPlugin.FROST_SMITHING);
    }

    /**
     * 材料需求相同的锻造结果归入同一页展示。
     */
    private static Map<IFrostMaterialPredicate, List<FrostSmithingOption>> groupByMaterial(
        List<FrostSmithingOption> options) {
        Map<IFrostMaterialPredicate, List<FrostSmithingOption>> grouped = new LinkedHashMap<>();
        for (FrostSmithingOption option : options) {
            grouped.computeIfAbsent(option.material(), key -> new ArrayList<>()).add(option);
        }
        return grouped;
    }

    private static ItemIngredientPredicate templateOf(IFrostSmithingRecipe recipe) {
        if (recipe instanceof PermutationRecipe permutation) return permutation.template();
        if (recipe instanceof DeformationRecipe deformation) return deformation.template();
        throw new IllegalArgumentException("Unknown frost smithing recipe: " + recipe);
    }

    /**
     * 材料槽可以放入的物品，只在 JEI 中用于展示。
     */
    private static List<ItemStack> materialItems(IFrostMaterialPredicate material, ItemStack input) {
        switch (material) {
            case EmptyFrostMaterialPredicate ignored -> {
                return List.of();
            }
            case CustomFrostMaterialPredicate(ItemIngredientPredicate predicate) -> {
                return List.of(predicate.getItems());
            }
            case RepairMaterialFrostMaterialPredicate ignored -> {
                List<ItemStack> items = new ArrayList<>(FrostSmithingCategory.repairMaterialItems(input));
                items.addAll(FrostSmithingCategory.universalMaterialItems());
                return FrostSmithingCategory.distinctByItem(items);
            }
            case OrFrostMaterialPredicate(List<IFrostMaterialPredicate> predicates) -> {
                return FrostSmithingCategory.distinctByItem(
                    predicates
                        .stream()
                        .flatMap(predicate -> FrostSmithingCategory.materialItems(predicate, input).stream())
                        .toList()
                );
            }
            case AndFrostMaterialPredicate(List<IFrostMaterialPredicate> predicates) -> {
                List<ItemStack> result = new ArrayList<>();
                for (IFrostMaterialPredicate predicate : predicates) {
                    List<ItemStack> items = FrostSmithingCategory.materialItems(predicate, input);
                    if (result.isEmpty()) {
                        result.addAll(items);
                        continue;
                    }
                    result.removeIf(stack -> items.stream().noneMatch(other -> other.is(stack.getItem())));
                }
                return result;
            }
            default -> {
            }
        }
        return List.of();
    }

    /**
     * 该装备的维修材料，通过逐个物品校验得到。
     */
    private static List<ItemStack> repairMaterialItems(ItemStack equipment) {
        if (equipment.isEmpty()) return List.of();
        return REPAIR_MATERIAL_CACHE.computeIfAbsent(equipment.getItem(), item -> {
            List<ItemStack> items = new ArrayList<>();
            for (Item candidate : BuiltInRegistries.ITEM) {
                ItemStack stack = candidate.getDefaultInstance();
                if (item.isValidRepairItem(equipment, stack)) items.add(stack);
            }
            return List.copyOf(items);
        });
    }

    private static List<ItemStack> universalMaterialItems() {
        return BuiltInRegistries.ITEM.getOrCreateTag(ModItemTags.UNIVERSAL_REPAIR_MATERIALS)
            .stream()
            .map(holder -> holder.value().getDefaultInstance())
            .toList();
    }

    private static List<ItemStack> distinctByItem(List<ItemStack> stacks) {
        Map<Item, ItemStack> distinct = new LinkedHashMap<>();
        for (ItemStack stack : stacks) {
            distinct.putIfAbsent(stack.getItem(), stack);
        }
        return List.copyOf(distinct.values());
    }

    @Override
    public RecipeType<Display> getRecipeType() {
        return AnvilCraftJeiPlugin.FROST_SMITHING;
    }

    @Override
    public Component getTitle() {
        return ModBlocks.FROST_SMITHING_TABLE.get().getName();
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public int getWidth() {
        return 144;
    }

    @Override
    public int getHeight() {
        return 36;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Display display, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.CATALYST, 1, 10)
            .setStandardSlotBackground()
            .addIngredients(Ingredient.of(FrostSmithingCategory.templateOf(display.recipe()).getItems()))
            .addRichTooltipCallback((slot, tooltip) -> tooltip.add(
                Component.translatable("jei.anvilcraft.tooltip.not_consumed").withStyle(ChatFormatting.GOLD)
            ));
        builder.addSlot(RecipeIngredientRole.INPUT, 37, 10)
            .setStandardSlotBackground()
            .addItemStack(display.input());
        List<ItemStack> materials = FrostSmithingCategory.materialItems(display.material(), display.input());
        if (!materials.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, 55, 10)
                .setStandardSlotBackground()
                .addItemStacks(materials);
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 123, 10)
            .setStandardSlotBackground()
            .addItemStacks(display.options().stream().map(option -> option.result().result().getDefaultInstance()).toList());
    }

    @Override
    public void draw(Display recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        this.arrow.draw(graphics, 88, 9);
    }

    public record Display(
        IFrostSmithingRecipe recipe,
        ItemStack input,
        IFrostMaterialPredicate material,
        List<FrostSmithingOption> options
    ) {
    }
}
