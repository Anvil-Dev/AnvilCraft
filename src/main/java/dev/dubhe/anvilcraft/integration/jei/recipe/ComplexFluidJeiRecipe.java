package dev.dubhe.anvilcraft.integration.jei.recipe;

import dev.anvilcraft.lib.v2.util.predicate.ChanceItemStack;
import dev.anvilcraft.lib.v2.util.predicate.ItemIngredientPredicate;
import dev.dubhe.anvilcraft.init.block.ModBlocks;
import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.init.item.ModItemTags;
import dev.dubhe.anvilcraft.init.item.ModItems;
import dev.dubhe.anvilcraft.recipe.FluidMixingRecipe;
import dev.dubhe.anvilcraft.recipe.anvil.predicate.block.HasCauldron;
import dev.dubhe.anvilcraft.recipe.anvil.wrap.SolidLiquidRecipe;
import dev.dubhe.anvilcraft.recipe.component.HasCauldronSimple;
import mezz.jei.common.util.RegistryUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** JEI 专用的复杂流体反应配方，用于展示同时包含物品和流体的大锅反应。 */
public final class ComplexFluidJeiRecipe extends FluidMixingRecipe {
    private final List<ItemIngredientPredicate> inputItems;
    private final List<ChanceItemStack> resultItems;
    private final List<List<FluidStack>> displayFluidInputs;
    private final List<List<FluidStack>> displayFluidResults;
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

    public static boolean isComplex(SolidLiquidRecipe recipe) {
        HasCauldronSimple cauldron = recipe.getHasCauldron();
        return cauldron.consume() > FluidType.BUCKET_VOLUME
               || cauldron.produce() > FluidType.BUCKET_VOLUME;
    }

    public static ComplexFluidJeiRecipe fromSolidLiquid(SolidLiquidRecipe recipe) {
        HasCauldronSimple cauldron = recipe.getHasCauldron();
        List<FluidStack> inputs = createFluidStacks(
            cauldron.fluid(),
            cauldron.fluidTag(),
            displayAmount(cauldron.consume())
        );
        List<FluidStack> results = createFluidStacks(
            cauldron.transform(),
            null,
            displayAmount(cauldron.produce())
        );
        return new ComplexFluidJeiRecipe(
            recipe.getInputItems(),
            recipe.getResultItems(),
            asGroup(inputs),
            asGroup(results),
            false,
            false
        );
    }

    public static ComplexFluidJeiRecipe assimilation(List<? extends Holder<Enchantment>> enchantments) {
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

    public static ComplexFluidJeiRecipe cleanse(List<? extends Holder<Enchantment>> enchantments) {
        return new ComplexFluidJeiRecipe(
            List.of(ItemIngredientPredicate.of(
                RegistryUtil.getRegistryAccess().lookupOrThrow(Registries.ITEM),
                ModItemTags.SILVER_NUGGETS
            ).build()),
            List.of(),
            List.of(LiquidEnchantmentJeiRecipeUtil.createFluidStacks(enchantments, 8)),
            List.of(List.of(new FluidStack(ModFluids.LIQUID_ENCHANTMENT.get(), 8))),
            false,
            false
        );
    }

    public static ComplexFluidJeiRecipe curseGoldIngot(List<? extends Holder<Enchantment>> curses) {
        return new ComplexFluidJeiRecipe(
            List.of(ItemIngredientPredicate.of(Items.GOLD_INGOT).withCount(16).build()),
            List.of(ChanceItemStack.of(ModItems.CURSED_GOLD_INGOT, 16)),
            List.of(LiquidEnchantmentJeiRecipeUtil.createFluidStacks(curses, 1)),
            List.of(),
            false,
            false
        );
    }

    public static ComplexFluidJeiRecipe curseGoldBlock(List<? extends Holder<Enchantment>> curses) {
        return new ComplexFluidJeiRecipe(
            List.of(ItemIngredientPredicate.of(Items.GOLD_BLOCK).withCount(16).build()),
            List.of(ChanceItemStack.of(ModBlocks.CURSED_GOLD_BLOCK, 16)),
            List.of(LiquidEnchantmentJeiRecipeUtil.createFluidStacks(curses, 9)),
            List.of(),
            false,
            false
        );
    }

    public List<ItemIngredientPredicate> getInputItems() {
        return this.inputItems;
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

    public boolean isHeaterRequired() {
        return this.heaterRequired;
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

    private static List<ItemStackTemplate> toItemStacks(List<ChanceItemStack> results) {
        return results.stream()
            .map(result -> result.stack().withCount(result.getMaxCount()))
            .toList();
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

    private static List<FluidStack> createFluidStacks(
        Identifier fluidId,
        @Nullable Identifier fluidTag,
        int amount
    ) {
        if (fluidTag != null) {
            TagKey<Fluid> tag = TagKey.create(Registries.FLUID, fluidTag);
            List<Fluid> tagged = new ArrayList<>();
            for (Holder<Fluid> holder : BuiltInRegistries.FLUID.getTagOrEmpty(tag)) {
                Fluid fluid = holder.value();
                if (fluid.defaultFluidState().isSource() && !tagged.contains(fluid)) tagged.add(fluid);
            }
            return tagged.stream()
                .map(fluid -> new FluidStack(fluid, amount))
                .toList();
        }
        if (!HasCauldron.isNotEmpty(fluidId)) return List.of();
        return BuiltInRegistries.FLUID.get(fluidId)
            .stream()
            .map(Holder::value)
            .filter(fluid -> fluid.defaultFluidState().isSource())
            .map(fluid -> new FluidStack(fluid, amount))
            .toList();
    }
}
