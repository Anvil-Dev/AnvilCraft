package dev.dubhe.anvilcraft.data.tags;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumTagsProvider;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypeTags;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;

public class DamageTypeTagLoader {
    /**
     * 初始化伤害类型标签
     *
     * @param provider 提供器
     */
    public static void init(RegistrumTagsProvider<DamageType> provider) {
        provider.addTag(ModDamageTypeTags.AMULET_VALID)
            .addOptionalTag(ModDamageTypeTags.TOPAZ_AMULET_VALID)
            .addOptionalTag(ModDamageTypeTags.RUBY_AMULET_VALID)
            .addOptionalTag(ModDamageTypeTags.SAPPHIRE_AMULET_VALID)
            .addOptionalTag(ModDamageTypeTags.ANVIL_AMULET_VALID)
            .addOptionalTag(ModDamageTypeTags.FEATHER_AMULET_VALID)
            .addOptionalTag(ModDamageTypeTags.ABNORMAL_AMULET_VALID);

        provider.addTag(ModDamageTypeTags.TOPAZ_AMULET_VALID)
            .addTag(DamageTypeTags.IS_LIGHTNING)
            .addOptional(Identifier.fromNamespaceAndPath("immersiveengineering", "wire_shock"));

        provider.addTag(ModDamageTypeTags.RUBY_AMULET_VALID)
            .addTag(DamageTypeTags.IS_FIRE)
            .addOptional(ModDamageTypes.LASER.location());

        provider.addTag(ModDamageTypeTags.SAPPHIRE_AMULET_VALID)
            .addTag(DamageTypeTags.IS_DROWNING)
            .add(DamageTypes.DRY_OUT);

        provider.addTag(ModDamageTypeTags.ANVIL_AMULET_VALID)
            .add(DamageTypes.FALLING_ANVIL);

        provider.addTag(ModDamageTypeTags.FEATHER_AMULET_VALID)
            .addTag(DamageTypeTags.IS_FALL);

        provider.addTag(ModDamageTypeTags.ABNORMAL_AMULET_VALID)
            .add(DamageTypes.WITHER);
    }
}
