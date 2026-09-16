package dev.dubhe.anvilcraft.integration.jei.category;

import dev.anvilcraft.lib.v2.math.expression.IExpression;
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
    /**
     * 附属模组为自己的 {@link IFrostMaterialPredicate.Type} 注册的展示用材料。
     *
     * <p>{@link #materialItems} 先处理内置的几种类型，这里只兜底注册表里的其他类型——没有登记的类型
     * 在 JEI 里材料槽会是空的。用 {@link #registerMaterialItems} 登记。</p>
     *
     * <p>本表<b>不随数据包重载清空</b>：条目是插件的静态登记，只会被更晚的登记覆盖，而清空会误伤
     * 顺序在自己之后的插件。真正随重载失效的是 {@link #REPAIR_MATERIAL_CACHE}。</p>
     */
    private static final Map<IFrostMaterialPredicate.Type<?>, MaterialItems<?>> CUSTOM_MATERIAL_ITEMS = new HashMap<>();

    private final IDrawable icon;
    private final IDrawable arrow;

    public FrostSmithingCategory(IGuiHelper helper) {
        this.icon = helper.createDrawableItemStack(ModBlocks.FROST_SMITHING_TABLE.asStack());
        this.arrow = JeiRenderHelper.getArrowDefault(helper);
    }

    /**
     * 供附属模组在自己的 {@code IModPlugin} 里登记某种材料谓词在 JEI 中展示的物品。
     *
     * <p>调用时机不敏感，只要在同一次 JEI 装载期间调用过就行；重复登记以后一次为准。</p>
     *
     * @param type          要登记的材料谓词类型，通常是自己的 {@code DeferredHolder}
     * @param materialItems 给出该类型实例在给定配方与装备下要展示的材料
     */
    public static <T extends IFrostMaterialPredicate> void registerMaterialItems(
        IFrostMaterialPredicate.Type<T> type,
        MaterialItems<T> materialItems
    ) {
        CUSTOM_MATERIAL_ITEMS.put(type, materialItems);
    }

    /**
     * 某种材料谓词在 JEI 材料槽里展示的物品。
     */
    @FunctionalInterface
    public interface MaterialItems<T extends IFrostMaterialPredicate> {
        /**
         * 获取该材料谓词在 JEI 材料槽里展示的物品
         *
         * @param recipe   该页对应的浮霜锻造配方
         * @param input    该页对应的装备
         * @param material 该页对应的材料谓词实例
         * @return 材料槽要展示的物品，空列表表示该页不画材料槽
         */
        List<ItemStack> get(IFrostSmithingRecipe recipe, ItemStack input, T material);
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        // 维修材料来自 ARMOR_MATERIAL 数据包注册表，/reload 后同会话内会过期；JEI 每次重载都会调到这里
        REPAIR_MATERIAL_CACHE.clear();
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
     *
     * <p>{@code CustomFrostMaterialPredicate} 也在这里处理，不走登记表——它是本模组自己的类型，
     * 不该依赖 JEI 插件的装载顺序。</p>
     */
    private static List<ItemStack> materialItems(
        IFrostMaterialPredicate material,
        IFrostSmithingRecipe recipe,
        ItemStack input
    ) {
        switch (material) {
            case EmptyFrostMaterialPredicate ignored -> {
                return List.of();
            }
            case CustomFrostMaterialPredicate custom -> {
                return custom.items();
            }
            case RepairMaterialFrostMaterialPredicate(int cost, boolean allowUniversal, IExpression ignored) -> {
                List<ItemStack> items = new ArrayList<>(
                    FrostSmithingCategory.withCount(FrostSmithingCategory.repairMaterialItems(input), cost)
                );
                if (allowUniversal) {
                    items.addAll(FrostSmithingCategory.withCount(FrostSmithingCategory.universalMaterialItems(), cost));
                }
                return FrostSmithingCategory.distinctByItem(items);
            }
            case OrFrostMaterialPredicate(List<IFrostMaterialPredicate> predicates) -> {
                return FrostSmithingCategory.distinctByItem(
                    predicates
                        .stream()
                        .flatMap(predicate -> FrostSmithingCategory.materialItems(predicate, recipe, input).stream())
                        .toList()
                );
            }
            case AndFrostMaterialPredicate(List<IFrostMaterialPredicate> predicates) -> {
                List<ItemStack> result = null;
                for (IFrostMaterialPredicate predicate : predicates) {
                    List<ItemStack> items = FrostSmithingCategory.materialItems(predicate, recipe, input);
                    if (result == null) {
                        result = new ArrayList<>(items);
                        continue;
                    }
                    result.removeIf(stack -> items.stream().noneMatch(other -> other.is(stack.getItem())));
                }
                return result == null ? List.of() : result;
            }
            default -> {
            }
        }
        return FrostSmithingCategory.registeredMaterialItems(recipe, input, material);
    }

    /**
     * 走附属模组登记的处理函数。实参的类型与登记时声明的类型一致，所以这里的强转是安全的。
     */
    @SuppressWarnings("unchecked")
    private static <T extends IFrostMaterialPredicate> List<ItemStack> registeredMaterialItems(
        IFrostSmithingRecipe recipe,
        ItemStack input,
        T material
    ) {
        MaterialItems<T> registered = (MaterialItems<T>) CUSTOM_MATERIAL_ITEMS.get(material.type());
        return registered == null ? List.of() : registered.get(recipe, input, material);
    }

    /**
     * 把展示用的物品改成一页配方实际会消耗的数量。
     *
     * <p>材料消耗量取自谓词（护甲 2、通用材料按其表达式算出的值），只画 1 个会看不出要几个。
     * 数量上限压到该物品的最大堆叠数，避免 {@link ItemStack#copyWithCount} 断言失败。</p>
     */
    private static List<ItemStack> withCount(List<ItemStack> stacks, int count) {
        if (count <= 1) return stacks;
        return stacks.stream()
            .map(stack -> stack.copyWithCount(Math.min(count, stack.getMaxStackSize())))
            .toList();
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
        List<ItemStack> materials = FrostSmithingCategory.materialItems(
            display.material(),
            display.recipe(),
            display.input()
        );
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
