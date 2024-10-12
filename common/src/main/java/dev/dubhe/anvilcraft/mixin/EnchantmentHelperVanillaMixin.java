package dev.dubhe.anvilcraft.mixin;

import dev.dubhe.anvilcraft.item.enchantment.BeheadingEnchantment;
import dev.dubhe.anvilcraft.item.enchantment.FellingEnchantment;
import dev.dubhe.anvilcraft.item.enchantment.HarvestEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(EnchantmentHelper.class)
public class EnchantmentHelperVanillaMixin {

    @Unique
    private static final List<Class<? extends Enchantment>> DISABLED_ENCHANTMENTS = List.of(
        HarvestEnchantment.class,
        BeheadingEnchantment.class,
        FellingEnchantment.class
    );

    @Redirect(
        method = "getAvailableEnchantmentResults",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;add(Ljava/lang/Object;)Z"
        )
    )
    private static <E> boolean getAvailableEnchantmentResults(List<EnchantmentInstance> instance, E e) {
        EnchantmentInstance entry = (EnchantmentInstance) e;
        if (DISABLED_ENCHANTMENTS.contains(entry.enchantment.getClass())) {
            return false;
        }
        return instance.add(entry);
    }
}
