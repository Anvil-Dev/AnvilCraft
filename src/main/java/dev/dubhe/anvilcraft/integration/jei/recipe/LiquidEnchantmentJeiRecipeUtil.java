package dev.dubhe.anvilcraft.integration.jei.recipe;

import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class LiquidEnchantmentJeiRecipeUtil {
    private LiquidEnchantmentJeiRecipeUtil() {
    }

    public static List<ResourceKey<Enchantment>> getEnchantments(boolean cursesOnly) {
        var connection = Minecraft.getInstance().getConnection();
        if (connection == null) return List.of();
        return connection.registryAccess()
            .lookupOrThrow(Registries.ENCHANTMENT)
            .listElements()
            .filter(enchantment -> !cursesOnly || enchantment.is(EnchantmentTags.CURSE))
            .map(Holder.Reference::getKey)
            .map(Objects::requireNonNull)
            .sorted(Comparator.comparing(key -> key.location().toString()))
            .toList();
    }

    public static List<FluidStack> createFluidStacks(List<ResourceKey<Enchantment>> enchantments, int amount) {
        List<FluidStack> fluids = new ArrayList<>(enchantments.size());
        for (ResourceKey<Enchantment> enchantment : enchantments) {
            FluidStack fluid = new FluidStack(ModFluids.LIQUID_ENCHANTMENT.get(), amount);
            fluid.set(ModComponents.LIQUID_ENCHANTMENT, enchantment);
            fluids.add(fluid);
        }
        return List.copyOf(fluids);
    }
}
