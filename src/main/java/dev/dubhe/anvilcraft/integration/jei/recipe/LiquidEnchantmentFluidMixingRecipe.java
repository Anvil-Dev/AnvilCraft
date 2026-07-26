package dev.dubhe.anvilcraft.integration.jei.recipe;

import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.recipe.FluidMixingRecipe;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;

import java.util.List;

public final class LiquidEnchantmentFluidMixingRecipe extends FluidMixingRecipe {
    private final FluidStack blankInput;
    private final List<FluidStack> enchantedInputs;
    private final List<FluidStack> enchantedResults;

    public LiquidEnchantmentFluidMixingRecipe(List<? extends Holder<Enchantment>> enchantments) {
        this(
            LiquidEnchantmentJeiRecipeUtil.createFluidStacks(enchantments, 8),
            LiquidEnchantmentJeiRecipeUtil.createFluidStacks(enchantments, 9)
        );
    }

    private LiquidEnchantmentFluidMixingRecipe(
        List<FluidStack> enchantedInputs,
        List<FluidStack> enchantedResults
    ) {
        super(
            List.of(
                SizedFluidIngredient.of(ModFluids.LIQUID_ENCHANTMENT.get(), 1),
                SizedFluidIngredient.of(ModFluids.LIQUID_ENCHANTMENT.get(), 8)
            ),
            List.of(),
            List.of(enchantedResults.getFirst()),
            false
        );
        this.blankInput = new FluidStack(ModFluids.LIQUID_ENCHANTMENT.get(), 1);
        this.enchantedInputs = copyFluids(enchantedInputs);
        this.enchantedResults = copyFluids(enchantedResults);
    }

    public FluidStack getBlankInput() {
        return this.blankInput.copy();
    }

    public List<FluidStack> getEnchantedInputs() {
        return copyFluids(this.enchantedInputs);
    }

    public List<FluidStack> getEnchantedResults() {
        return copyFluids(this.enchantedResults);
    }

    private static List<FluidStack> copyFluids(List<FluidStack> fluids) {
        return fluids.stream().map(FluidStack::copy).toList();
    }
}
