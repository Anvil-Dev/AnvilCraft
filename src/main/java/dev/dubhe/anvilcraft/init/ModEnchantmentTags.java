package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.NotNull;

public class ModEnchantmentTags {
    public static final TagKey<Enchantment> MERCILESS_DAMAGE_PASSED = bind("merciless_damage_passed");
    public static final TagKey<Enchantment> MERCILESS_PASSED = bind("merciless_passed");

    public static @NotNull TagKey<Enchantment> bindC(String id) {
        return TagKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("c", id));
    }

    public static @NotNull TagKey<Enchantment> bind(String id) {
        return TagKey.create(Registries.ENCHANTMENT, AnvilCraft.of(id));
    }
}
