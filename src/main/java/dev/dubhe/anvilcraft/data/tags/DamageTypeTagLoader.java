package dev.dubhe.anvilcraft.data.tags;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumTagsProvider;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypeTags;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.neoforge.common.Tags;

public class DamageTypeTagLoader {
    /// 初始化伤害类型标签
    ///
    /// @param provider 提供器
    public static void init(RegistrumTagsProvider<DamageType> provider) {
        provider.rawBuilder(DamageTypeTags.BYPASSES_ARMOR)
            .addOptionalElement(ModDamageTypes.LOST_IN_TIME.identifier());

        provider.rawBuilder(DamageTypeTags.BYPASSES_RESISTANCE)
            .addOptionalElement(ModDamageTypes.LOST_IN_TIME.identifier());

        provider.rawBuilder(DamageTypeTags.NO_KNOCKBACK)
            .addOptionalElement(ModDamageTypes.LOST_IN_TIME.identifier())
            .addOptionalElement(ModDamageTypes.HEATER_BURN.identifier());

        provider.rawBuilder(DamageTypeTags.IS_FIRE)
            .addOptionalElement(ModDamageTypes.HEATER_BURN.identifier())
            .addOptionalElement(ModDamageTypes.PLASMA_JET.identifier())
            .addOptionalElement(ModDamageTypes.LASER.identifier());

        provider.rawBuilder(Tags.DamageTypes.IS_MAGIC)
            .addOptionalElement(ModDamageTypes.LOST_IN_TIME.identifier());

        provider.rawBuilder(ModDamageTypeTags.AMULET_VALID)
            .addOptionalTag(ModDamageTypeTags.TOPAZ_AMULET_VALID.location())
            .addOptionalTag(ModDamageTypeTags.RUBY_AMULET_VALID.location())
            .addOptionalTag(ModDamageTypeTags.SAPPHIRE_AMULET_VALID.location())
            .addOptionalTag(ModDamageTypeTags.ANVIL_AMULET_VALID.location())
            .addOptionalTag(ModDamageTypeTags.FEATHER_AMULET_VALID.location())
            .addOptionalTag(ModDamageTypeTags.ABNORMAL_AMULET_VALID.location());

        provider.rawBuilder(ModDamageTypeTags.TOPAZ_AMULET_VALID)
            .addTag(DamageTypeTags.IS_LIGHTNING.location())
            .addOptionalElement(Identifier.fromNamespaceAndPath("immersiveengineering", "wire_shock"));

        provider.rawBuilder(ModDamageTypeTags.RUBY_AMULET_VALID)
            .addTag(DamageTypeTags.IS_FIRE.location())
            .addOptionalElement(ModDamageTypes.LASER.identifier());

        provider.rawBuilder(ModDamageTypeTags.SAPPHIRE_AMULET_VALID)
            .addTag(DamageTypeTags.IS_DROWNING.location())
            .addElement(DamageTypes.DRY_OUT.identifier());

        provider.rawBuilder(ModDamageTypeTags.ANVIL_AMULET_VALID)
            .addElement(DamageTypes.FALLING_ANVIL.identifier())
            .addTag(ModDamageTypeTags.IS_FALLING_GIANT_ANVIL.location());

        provider.rawBuilder(ModDamageTypeTags.FEATHER_AMULET_VALID)
            .addTag(DamageTypeTags.IS_FALL.location());

        provider.rawBuilder(ModDamageTypeTags.ABNORMAL_AMULET_VALID)
            .addElement(DamageTypes.WITHER.identifier());

        provider.rawBuilder(ModDamageTypeTags.IS_FALLING_GIANT_ANVIL)
            .addOptionalElement(ModDamageTypes.FALLING_GIANT_ANVIL.identifier());
    }
}
