package dev.dubhe.anvilcraft.data.tags;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumTagsProvider;
import dev.dubhe.anvilcraft.init.enchantment.ModEnchantmentTags;
import dev.dubhe.anvilcraft.init.enchantment.ModEnchantments;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.common.Tags;

public class EnchantmentTagLoader {
    /**
     * 魔咒标签生成器初始化
     *
     * @param provider 提供器
     */
    public static void init(RegistrumTagsProvider<Enchantment> provider) {
        provider.addTag(Tags.Enchantments.INCREASE_BLOCK_DROPS)
            .addOptional(ModEnchantments.HARVEST_KEY.location());

        provider.addTag(EnchantmentTags.IN_ENCHANTING_TABLE)
            .addOptional(ModEnchantments.SMELTING_KEY.location())
            .addOptional(ModEnchantments.DISINTEGRATION_KEY.location());

        provider.addTag(ModEnchantmentTags.DISABLED_PASSED)
            .add(Enchantments.MENDING);

        provider.addTag(ModEnchantmentTags.PROVIDENCE_BONUS)
            .add(Enchantments.FORTUNE)
            .add(Enchantments.LOOTING)
            .addOptional(ModEnchantments.BEHEADING_KEY.location())
            .add(Enchantments.THORNS)
            .add(Enchantments.LUCK_OF_THE_SEA)
            .addOptional(ModEnchantments.DISINTEGRATION_KEY.location());
    }
}
