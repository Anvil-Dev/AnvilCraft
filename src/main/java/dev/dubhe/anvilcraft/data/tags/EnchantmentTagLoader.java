package dev.dubhe.anvilcraft.data.tags;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumTagsProvider;
import dev.dubhe.anvilcraft.init.enchantment.ModEnchantmentTags;
import dev.dubhe.anvilcraft.init.enchantment.ModEnchantments;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.common.Tags;

public class EnchantmentTagLoader {
    /// 魔咒标签生成器初始化
    ///
    /// @param provider 提供器
    public static void init(RegistrumTagsProvider<Enchantment> provider) {
        provider.rawBuilder(Tags.Enchantments.INCREASE_BLOCK_DROPS)
            .addOptionalElement(ModEnchantments.HARVEST_KEY.identifier());

        provider.rawBuilder(ModEnchantmentTags.DISABLED_PASSED)
            .addElement(Enchantments.MENDING.identifier());

        provider.rawBuilder(ModEnchantmentTags.PROVIDENCE_BONUS)
            .addElement(Enchantments.FORTUNE.identifier())
            .addElement(Enchantments.LOOTING.identifier())
            .addOptionalElement(ModEnchantments.BEHEADING_KEY.identifier())
            .addElement(Enchantments.THORNS.identifier())
            .addElement(Enchantments.LUCK_OF_THE_SEA.identifier())
            .addOptionalElement(ModEnchantments.DISINTEGRATION_KEY.identifier());
    }
}
