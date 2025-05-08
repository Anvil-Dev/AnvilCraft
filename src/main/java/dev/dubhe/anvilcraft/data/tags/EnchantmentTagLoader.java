package dev.dubhe.anvilcraft.data.tags;

import com.tterrag.registrate.providers.RegistrateTagsProvider;
import dev.dubhe.anvilcraft.init.ModEnchantmentTags;
import dev.dubhe.anvilcraft.init.ModEnchantments;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.NotNull;

public class EnchantmentTagLoader {
    /**
     * 魔咒标签生成器初始化
     *
     * @param provider 提供器
     */
    public static void init(@NotNull RegistrateTagsProvider<Enchantment> provider) {
        provider.addTag(ModEnchantmentTags.MERCILESS_DAMAGE_PASSED)
            .add(Enchantments.SWEEPING_EDGE);

        provider.addTag(ModEnchantmentTags.MERCILESS_PASSED)
            .addTag(ModEnchantmentTags.MERCILESS_DAMAGE_PASSED)
            .addTag(EnchantmentTags.CURSE)
            .add(Enchantments.UNBREAKING)
            .add(Enchantments.MENDING)
            .add(Enchantments.EFFICIENCY);

        provider.addTag(Tags.Enchantments.INCREASE_BLOCK_DROPS)
            .addOptional(ModEnchantments.HARVEST_KEY.location());

        provider.addTag(ModEnchantmentTags.MODIFY_BLOCK_DROPS)
            .addTag(Tags.Enchantments.INCREASE_BLOCK_DROPS)
            .add(Enchantments.SILK_TOUCH);

        provider.addTag(ModEnchantmentTags.MODIFY_ENTITY_DROPS)
            .addTag(Tags.Enchantments.INCREASE_ENTITY_DROPS)
            .addOptional(ModEnchantments.BEHEADING_KEY.location());
    }
}
