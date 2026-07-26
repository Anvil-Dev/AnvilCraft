package dev.dubhe.anvilcraft.integration.jei.recipe;

import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SolidLiquidRecipe;
import dev.dubhe.anvilcraft.recipe.component.HasCauldronSimple;
import mezz.jei.common.util.RegistryUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public final class LiquidEnchantmentSolidLiquidRecipe extends SolidLiquidRecipe {
    private final List<FluidStack> inputFluids;
    private final List<FluidStack> outputFluids;

    private LiquidEnchantmentSolidLiquidRecipe(
        List<ItemIngredientPredicate> itemIngredients,
        List<ChanceItemStack> itemResults,
        HasCauldronSimple cauldron,
        List<FluidStack> inputFluids,
        List<FluidStack> outputFluids
    ) {
        super(itemIngredients, itemResults, cauldron);
        this.inputFluids = copyFluids(inputFluids);
        this.outputFluids = copyFluids(outputFluids);
    }

    public static LiquidEnchantmentSolidLiquidRecipe cleanse(List<FluidStack> enchantedFluids) {
        return new LiquidEnchantmentSolidLiquidRecipe(
            List.of(ItemIngredientPredicate.of(
                RegistryUtil.getRegistryAccess().lookupOrThrow(Registries.ITEM),
                ModItemTags.SILVER_NUGGETS
            ).build()),
            List.of(),
            cauldron(8, 8),
            enchantedFluids,
            List.of(new FluidStack(ModFluids.LIQUID_ENCHANTMENT.get(), 8))
        );
    }

    public static LiquidEnchantmentSolidLiquidRecipe curseGoldIngot(List<FluidStack> cursedFluids) {
        return new LiquidEnchantmentSolidLiquidRecipe(
            List.of(ItemIngredientPredicate.of(Items.GOLD_INGOT).withCount(16).build()),
            List.of(ChanceItemStack.of(ModItems.CURSED_GOLD_INGOT, 16)),
            cauldron(1, 0),
            cursedFluids,
            List.of()
        );
    }

    public static LiquidEnchantmentSolidLiquidRecipe curseGoldBlock(List<FluidStack> cursedFluids) {
        return new LiquidEnchantmentSolidLiquidRecipe(
            List.of(ItemIngredientPredicate.of(Items.GOLD_BLOCK).withCount(16).build()),
            List.of(ChanceItemStack.of(ModBlocks.CURSED_GOLD_BLOCK, 16)),
            cauldron(9, 0),
            cursedFluids,
            List.of()
        );
    }

    public List<FluidStack> getInputFluids() {
        return copyFluids(this.inputFluids);
    }

    public List<FluidStack> getOutputFluids() {
        return copyFluids(this.outputFluids);
    }

    private static HasCauldronSimple cauldron(int consume, int produce) {
        HasCauldronSimple.Builder cauldron = HasCauldronSimple.fluid(ModFluids.LIQUID_ENCHANTMENT.getId())
            .consume(consume);
        if (produce > 0) {
            cauldron.transform(ModFluids.LIQUID_ENCHANTMENT.getId()).produce(produce);
        }
        return cauldron.build();
    }

    private static List<FluidStack> copyFluids(List<FluidStack> fluids) {
        return fluids.stream().map(FluidStack::copy).toList();
    }
}
