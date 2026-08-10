package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.fluids.FluidStack;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

public final class LiquidEnchantmentUtil {
    private static final int FNV_OFFSET_BASIS = 0x811C9DC5;
    private static final int FNV_PRIME = 0x01000193;

    private LiquidEnchantmentUtil() {
    }

    public static Optional<Holder<Enchantment>> getEnchantment(FluidStack stack) {
        if (!stack.is(ModFluids.LIQUID_ENCHANTMENT.get())) return Optional.empty();
        return Optional.ofNullable(stack.get(ModComponents.LIQUID_ENCHANTMENT));
    }

    public static boolean isBlank(FluidStack stack) {
        return stack.is(ModFluids.LIQUID_ENCHANTMENT.get()) && LiquidEnchantmentUtil.getEnchantment(stack).isEmpty();
    }

    public static boolean isEnchanted(FluidStack stack) {
        return LiquidEnchantmentUtil.getEnchantment(stack).isPresent();
    }

    public static boolean isCursed(FluidStack stack) {
        return LiquidEnchantmentUtil.getEnchantment(stack).filter(enchantment -> enchantment.is(EnchantmentTags.CURSE)).isPresent();
    }

    /** 根据魔咒完整注册名计算跨存档稳定的 24 位 FNV-1a 颜色。 */
    public static int getColor(Holder<Enchantment> enchantment) {
        Identifier id = enchantment.unwrapKey()
            .orElseThrow(() -> new IllegalArgumentException("Liquid enchantment must be registered"))
            .identifier();
        int hash = LiquidEnchantmentUtil.FNV_OFFSET_BASIS;
        for (byte value : id.toString().getBytes(StandardCharsets.UTF_8)) {
            hash ^= value & 0xFF;
            hash *= LiquidEnchantmentUtil.FNV_PRIME;
        }
        return hash & 0xFFFFFF;
    }
}
