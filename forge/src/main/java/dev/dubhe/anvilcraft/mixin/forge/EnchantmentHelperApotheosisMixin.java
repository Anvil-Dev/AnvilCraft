package dev.dubhe.anvilcraft.mixin.forge;

import dev.dubhe.anvilcraft.item.enchantment.BeheadingEnchantment;
import dev.dubhe.anvilcraft.item.enchantment.FellingEnchantment;
import dev.dubhe.anvilcraft.item.enchantment.HarvestEnchantment;
import dev.shadowsoffire.apotheosis.ench.table.RealEnchantmentHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mixin(RealEnchantmentHelper.class)
public class EnchantmentHelperApotheosisMixin {

    @Unique
    private static final List<Class<? extends Enchantment>> DISABLED_ENCHANTMENTS = List.of(
        HarvestEnchantment.class,
        BeheadingEnchantment.class,
        FellingEnchantment.class
    );

    @Inject(
        method = "getAvailableEnchantmentResults",
        at = @At(
            value = "RETURN"
        ),
        remap = false,
        cancellable = true
    )
    private static <E> void getAvailableEnchantmentResults(
        int power,
        ItemStack stack,
        boolean allowTreasure,
        Set<Enchantment> blacklist,
        CallbackInfoReturnable<List<EnchantmentInstance>> cir
    ) {
        List<EnchantmentInstance> instances = cir.getReturnValue();
        if (instances.stream().anyMatch(it -> DISABLED_ENCHANTMENTS.contains(it.enchantment.getClass()))) {
            cir.setReturnValue(
                new ArrayList<>(
                    instances.stream()
                        .filter(it -> !DISABLED_ENCHANTMENTS.contains(it.enchantment.getClass()))
                        .toList()
                )// a mutable list is required
            );
        }

    }
}
