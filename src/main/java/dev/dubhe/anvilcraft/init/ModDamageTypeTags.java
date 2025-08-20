package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;

public class ModDamageTypeTags {
    public static final TagKey<DamageType> AMULET_VALID = bind("amulet_valid");
    public static final TagKey<DamageType> TOPAZ_AMULET_VALID = bind("amulet_valid/topaz");
    public static final TagKey<DamageType> RUBY_AMULET_VALID = bind("amulet_valid/ruby");
    public static final TagKey<DamageType> SAPPHIRE_AMULET_VALID = bind("amulet_valid/sapphire");
    public static final TagKey<DamageType> ANVIL_AMULET_VALID = bind("amulet_valid/anvil");
    public static final TagKey<DamageType> FEATHER_AMULET_VALID = bind("amulet_valid/feather");
    public static final TagKey<DamageType> ABNORMAL_AMULET_VALID = bind("amulet_valid/abnormal");

    @SuppressWarnings("unused")
    private static TagKey<DamageType> bindC(String id) {
        return TagKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath("c", id));
    }

    private static TagKey<DamageType> bind(String id) {
        return TagKey.create(Registries.DAMAGE_TYPE, AnvilCraft.of(id));
    }
}
