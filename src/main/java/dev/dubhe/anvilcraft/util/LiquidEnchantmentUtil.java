package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.init.block.ModFluids;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.fluids.FluidStack;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

public final class LiquidEnchantmentUtil {
    private static final int FNV_OFFSET_BASIS = 0x811C9DC5;
    private static final int FNV_PRIME = 0x01000193;

    private LiquidEnchantmentUtil() {
    }

    public static FluidStack fromBook(ItemStack stack) {
        if (!stack.is(Items.BOOK) && !stack.is(Items.ENCHANTED_BOOK)) return FluidStack.EMPTY;
        FluidStack fluid = new FluidStack(ModFluids.LIQUID_ENCHANTMENT.get(), 1);
        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        for (Holder<Enchantment> enchantment : enchantments.keySet()) {
            if (enchantment.unwrapKey().isEmpty()) continue;
            fluid.set(ModComponents.LIQUID_ENCHANTMENT, enchantment.unwrapKey().orElseThrow());
            break;
        }
        return fluid;
    }

    public static Optional<Holder<Enchantment>> getEnchantment(FluidStack stack) {
        if (!stack.is(ModFluids.LIQUID_ENCHANTMENT.get())) return Optional.empty();
        return Optional.ofNullable(stack.get(ModComponents.LIQUID_ENCHANTMENT))
            .flatMap(key -> Objects.requireNonNull(CommonHooks.resolveLookup(Registries.ENCHANTMENT)).get(key));
    }

    public static boolean isBlank(FluidStack stack) {
        return stack.is(ModFluids.LIQUID_ENCHANTMENT.get()) && getEnchantment(stack).isEmpty();
    }

    public static boolean isEnchanted(FluidStack stack) {
        return getEnchantment(stack).isPresent();
    }

    public static boolean isCursed(FluidStack stack) {
        return getEnchantment(stack).filter(enchantment -> enchantment.is(EnchantmentTags.CURSE)).isPresent();
    }

    /** 根据魔咒完整注册名计算跨存档稳定的 24 位 FNV-1a 颜色。 */
    public static int getColor(Holder<Enchantment> enchantment) {
        ResourceLocation id = enchantment.unwrapKey()
            .orElseThrow(() -> new IllegalArgumentException("Liquid enchantment must be registered"))
            .location();
        int hash = FNV_OFFSET_BASIS;
        for (byte value : id.toString().getBytes(StandardCharsets.UTF_8)) {
            hash ^= value & 0xFF;
            hash *= FNV_PRIME;
        }
        return hash & 0xFFFFFF;
    }

    /** 返回诅咒液态魔咒专用的渲染覆盖层颜色；消失诅咒偏橙红、绑定诅咒偏玫红，其余诅咒回退通用深红。 */
    public static int curseLayer(Holder<Enchantment> enchantment) {
        ResourceLocation id = enchantment.unwrapKey()
            .orElseThrow(() -> new IllegalArgumentException("Liquid enchantment must be registered"))
            .location();
        String path = id.getPath();
        if (path.equals("vanishing_curse")) return 0xF0E83400;
        if (path.equals("binding_curse")) return 0xF0C01050;
        return 0xE8990000;
    }

    public static long amountForLevel(int level) {
        if (level <= 0) return 0;
        if (level >= Long.SIZE) return Long.MAX_VALUE;
        return 1L << (level - 1);
    }

    public static int levelForAmount(long amount) {
        return amount <= 0 ? 0 : Long.SIZE - Long.numberOfLeadingZeros(amount);
    }

}
