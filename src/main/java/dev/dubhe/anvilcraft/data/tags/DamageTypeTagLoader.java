package dev.dubhe.anvilcraft.data.tags;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumTagsProvider;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypeTags;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.neoforge.common.Tags;

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
            .addOptional(ResourceLocation.fromNamespaceAndPath("immersiveengineering", "wire_shock"));

        provider.addTag(ModDamageTypeTags.RUBY_AMULET_VALID)
            .addTag(DamageTypeTags.IS_FIRE);

        provider.addTag(ModDamageTypeTags.SAPPHIRE_AMULET_VALID)
            .addTag(DamageTypeTags.IS_DROWNING)
            .add(DamageTypes.DRY_OUT);

        provider.addTag(ModDamageTypeTags.ANVIL_AMULET_VALID)
            .add(DamageTypes.FALLING_ANVIL)
            .addTag(ModDamageTypeTags.IS_FALLING_GIANT_ANVIL);

        provider.addTag(ModDamageTypeTags.FEATHER_AMULET_VALID)
            .addTag(DamageTypeTags.IS_FALL);

        provider.addTag(ModDamageTypeTags.ABNORMAL_AMULET_VALID)
            .add(DamageTypes.WITHER);

        provider.addTag(ModDamageTypeTags.IS_FALLING_GIANT_ANVIL)
            .addOptional(ModDamageTypes.FALLING_GIANT_ANVIL.location());

        /// 天体接触伤害换用独立类型以显示专属死亡提示，因此需要各自继承被替换原版类型的标签，
        /// 否则护符（红宝石=火焰、羽毛=摔落）与抗性提升等无法再规避这些伤害。
        provider.addTag(DamageTypeTags.BYPASSES_ARMOR)
            .addOptional(ModDamageTypes.LOST_IN_TIME.location())
            .addOptional(ModDamageTypes.PLANETARY_COLLAPSE.location())
            .addOptional(ModDamageTypes.BLACK_HOLE_CONTACT.location());

        provider.addTag(DamageTypeTags.BYPASSES_RESISTANCE)
            .addOptional(ModDamageTypes.LOST_IN_TIME.location())
            .addOptional(ModDamageTypes.PLANETARY_COLLAPSE.location())
            .addOptional(ModDamageTypes.BLACK_HOLE_CONTACT.location());

        provider.addTag(DamageTypeTags.BYPASSES_SHIELD)
            .addOptional(ModDamageTypes.PLANETARY_COLLAPSE.location());

        provider.addTag(DamageTypeTags.BYPASSES_INVULNERABILITY)
            .addOptional(ModDamageTypes.PLANETARY_COLLAPSE.location());

        provider.addTag(DamageTypeTags.BYPASSES_COOLDOWN)
            .addOptional(ModDamageTypes.PLANETARY_COLLAPSE.location());

        provider.addTag(DamageTypeTags.BYPASSES_EFFECTS)
            .addOptional(ModDamageTypes.PLANETARY_COLLAPSE.location());

        provider.addTag(DamageTypeTags.BYPASSES_ENCHANTMENTS)
            .addOptional(ModDamageTypes.PLANETARY_COLLAPSE.location());

        provider.addTag(DamageTypeTags.NO_KNOCKBACK)
            .addOptional(ModDamageTypes.LOST_IN_TIME.location())
            .addOptional(ModDamageTypes.HEATER_BURN.location())
            .addOptional(ModDamageTypes.PLANETARY_COLLAPSE.location())
            .addOptional(ModDamageTypes.BLACK_HOLE_CONTACT.location());

        provider.addTag(Tags.DamageTypes.IS_MAGIC)
            .addOptional(ModDamageTypes.LOST_IN_TIME.location())
            .addOptional(ModDamageTypes.BLACK_HOLE_CONTACT.location());

        provider.addTag(DamageTypeTags.IS_FIRE)
            .addOptional(ModDamageTypes.HEATER_BURN.location())
            .addOptional(ModDamageTypes.PLASMA_JET.location())
            .addOptional(ModDamageTypes.LASER.location())
            .addOptional(ModDamageTypes.STAR_CONTACT.location());

        provider.addTag(DamageTypeTags.IS_FALL)
            .addOptional(ModDamageTypes.PLANET_CONTACT.location());
    }
}
