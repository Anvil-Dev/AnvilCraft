package dev.dubhe.anvilcraft.init.enchantment;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.enchantment.Enchantment;

public class ModEnchantmentTags {
    public static final TagKey<Enchantment> MERCILESS_PASSED = bind("merciless_passed");
    public static final TagKey<Enchantment> DISABLED_PASSED = bind("disabled_passed");
    public static final TagKey<Enchantment> PROVIDENCE_BONUS = bind("providence_bonus");

    public static TagKey<Enchantment> bindC(String id) {
        return TagKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath("c", id));
    }

    public static TagKey<Enchantment> bind(String id) {
        return TagKey.create(Registries.ENCHANTMENT, AnvilCraft.of(id));
    }
}
