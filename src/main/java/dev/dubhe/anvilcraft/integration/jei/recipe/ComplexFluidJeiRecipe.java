package dev.dubhe.anvilcraft.integration.jei.recipe;

import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.integration.jei.util.JeiFluidUtil;
import dev.dubhe.anvilcraft.recipe.FluidMixingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SolidLiquidRecipe;
import dev.dubhe.anvilcraft.recipe.component.HasCauldronSimple;
import lombok.Getter;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI 专用的复杂流体反应配方，用于展示同时包含物品和流体的大锅反应。
 */
public final class ComplexFluidJeiRecipe extends FluidMixingRecipe {
    @Getter
    private final List<ItemIngredientPredicate> inputItems;
    private final List<ChanceItemStack> resultItems;
    private final List<List<FluidStack>> displayFluidInputs;
    private final List<List<FluidStack>> displayFluidResults;
    @Getter
    private final boolean heaterRequired;
    private final boolean linkFluidVariants;

    private ComplexFluidJeiRecipe(
        List<ItemIngredientPredicate> inputItems,
        List<ChanceItemStack> resultItems,
        List<List<FluidStack>> fluidInputs,
        List<List<FluidStack>> fluidResults,
        boolean heaterRequired,
        boolean linkFluidVariants
    ) {
        super(
            toSizedIngredients(fluidInputs),
            toItemStacks(resultItems),
            firstFluids(fluidResults),
            false
        );
        this.inputItems = List.copyOf(inputItems);
        this.resultItems = List.copyOf(resultItems);
        this.displayFluidInputs = copyFluidGroups(fluidInputs);
        this.displayFluidResults = copyFluidGroups(fluidResults);
        this.heaterRequired = heaterRequired;
        this.linkFluidVariants = linkFluidVariants;
    }

    public static ComplexFluidJeiRecipe fromSolidLiquid(SolidLiquidRecipe recipe) {
        HasCauldronSimple cauldron = recipe.getHasCauldron();
        List<FluidStack> inputs = JeiFluidUtil.getDisplayFluids(
            cauldron.fluid(),
            displayAmount(cauldron.consume())
        );
        List<List<FluidStack>> results = cauldron.transforms().stream()
            .map(fluid -> JeiFluidUtil.getDisplayFluids(fluid, displayAmount(fluid.getAmount())))
            .filter(group -> !group.isEmpty())
            .toList();
        return new ComplexFluidJeiRecipe(
            recipe.getInputItems(),
            recipe.getResultItems(),
            asGroup(inputs),
            results,
            false,
            false
        );
    }

    public static ComplexFluidJeiRecipe assimilation(List<ResourceKey<Enchantment>> enchantments) {
        List<FluidStack> enchantedInputs = LiquidEnchantmentJeiRecipeUtil.createFluidStacks(enchantments, 8);
        List<FluidStack> enchantedResults = LiquidEnchantmentJeiRecipeUtil.createFluidStacks(enchantments, 9);
        return new ComplexFluidJeiRecipe(
            List.of(ItemIngredientPredicate.of(Items.LAPIS_LAZULI).build()),
            List.of(),
            List.of(
                List.of(new FluidStack(ModFluids.LIQUID_ENCHANTMENT.get(), 1)),
                enchantedInputs
            ),
            List.of(enchantedResults),
            true,
            true
        );
    }

    public static ComplexFluidJeiRecipe cleanse(List<ResourceKey<Enchantment>> enchantments) {
        return new ComplexFluidJeiRecipe(
            List.of(ItemIngredientPredicate.of(ModItemTags.SILVER_NUGGETS).build()),
            List.of(),
            List.of(LiquidEnchantmentJeiRecipeUtil.createFluidStacks(enchantments, 8)),
            List.of(List.of(new FluidStack(ModFluids.LIQUID_ENCHANTMENT.get(), 8))),
            false,
            false
        );
    }

    public static ComplexFluidJeiRecipe curseGoldIngot(List<ResourceKey<Enchantment>> curses) {
        return new ComplexFluidJeiRecipe(
            List.of(ItemIngredientPredicate.of(Items.GOLD_INGOT).withCount(16).build()),
            List.of(ChanceItemStack.of(ModItems.CURSED_GOLD_INGOT, 16)),
            List.of(LiquidEnchantmentJeiRecipeUtil.createFluidStacks(curses, 1)),
            List.of(),
            false,
            false
        );
    }

    public static ComplexFluidJeiRecipe curseGoldBlock(List<ResourceKey<Enchantment>> curses) {
        return new ComplexFluidJeiRecipe(
            List.of(ItemIngredientPredicate.of(Items.GOLD_BLOCK).withCount(16).build()),
            List.of(ChanceItemStack.of(ModBlocks.CURSED_GOLD_BLOCK, 16)),
            List.of(LiquidEnchantmentJeiRecipeUtil.createFluidStacks(curses, 9)),
            List.of(),
            false,
            false
        );
    }

    public static ComplexFluidJeiRecipe enchantGoldIngot() {
        List<FluidStack> fluids = new ArrayList<>();
        fluids.add(new FluidStack(ModFluids.LIQUID_ENCHANTMENT.get(), 16));
        FluidStack mending = new FluidStack(ModFluids.LIQUID_ENCHANTMENT.get(), 4);
        mending.set(ModComponents.LIQUID_ENCHANTMENT, Enchantments.MENDING);
        fluids.add(mending);
        FluidStack fortune = new FluidStack(ModFluids.LIQUID_ENCHANTMENT.get(), 1);
        fortune.set(ModComponents.LIQUID_ENCHANTMENT, Enchantments.FORTUNE);
        fluids.add(fortune);
        return new ComplexFluidJeiRecipe(
            List.of(ItemIngredientPredicate.of(Items.GOLD_INGOT).build()),
            List.of(ChanceItemStack.of(ModItems.ENCHANTED_GOLD_INGOT, 1)),
            List.of(fluids),
            List.of(),
            false,
            false
        );
    }

    public List<ChanceItemStack> getDisplayItemResults() {
        return this.resultItems;
    }

    public List<List<FluidStack>> getDisplayFluidInputs() {
        return copyFluidGroups(this.displayFluidInputs);
    }

    public int getDisplayFluidInputCount() {
        return this.displayFluidInputs.size();
    }

    public List<List<FluidStack>> getDisplayFluidResults() {
        return copyFluidGroups(this.displayFluidResults);
    }

    public int getDisplayFluidResultCount() {
        return this.displayFluidResults.size();
    }

    public boolean shouldLinkFluidVariants() {
        return this.linkFluidVariants;
    }

    private static List<SizedFluidIngredient> toSizedIngredients(List<List<FluidStack>> groups) {
        List<SizedFluidIngredient> ingredients = new ArrayList<>(groups.size());
        for (List<FluidStack> group : groups) {
            FluidStack first = group.getFirst();
            ingredients.add(SizedFluidIngredient.of(first.getFluid(), first.getAmount()));
        }
        return ingredients;
    }

    private static List<ItemStack> toItemStacks(List<ChanceItemStack> results) {
        return results.stream().map(result -> {
            ItemStack stack = result.stack().copy();
            stack.setCount(result.getMaxCount());
            return stack;
        }).toList();
    }

    private static List<FluidStack> firstFluids(List<List<FluidStack>> groups) {
        return groups.stream().map(group -> group.getFirst().copy()).toList();
    }

    private static List<List<FluidStack>> asGroup(List<FluidStack> fluids) {
        return fluids.isEmpty() ? List.of() : List.of(fluids);
    }

    private static List<List<FluidStack>> copyFluidGroups(List<List<FluidStack>> groups) {
        return groups.stream()
            .map(group -> group.stream().map(FluidStack::copy).toList())
            .toList();
    }

    private static int displayAmount(int amount) {
        return amount > 0 ? amount : FluidType.BUCKET_VOLUME;
    }

}
