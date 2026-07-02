package dev.dubhe.anvilcraft.init.item;

import dev.dubhe.anvilcraft.init.entity.ModDamageTypeTags;
import dev.dubhe.anvilcraft.item.property.component.amulet.AnvilAmulet;
import dev.dubhe.anvilcraft.item.property.component.amulet.ComradeAmulet;
import dev.dubhe.anvilcraft.item.property.component.amulet.DiscountAmulet;
import dev.dubhe.anvilcraft.item.property.component.amulet.DoNothingAmulet;
import dev.dubhe.anvilcraft.item.property.component.amulet.GiveEffectAmulet;
import dev.dubhe.anvilcraft.item.property.component.amulet.ImmuneDamageAmulet;
import dev.dubhe.anvilcraft.item.property.component.amulet.WrappedOthersAmulet;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public class ModAmulets {
    public static final DiscountAmulet EMERALD = new DiscountAmulet(0.3F);
    public static final ImmuneDamageAmulet TOPAZ = ImmuneDamageAmulet.builder()
        .immune(ModDamageTypeTags.TOPAZ_AMULET_VALID)
        .build();
    public static final GiveEffectAmulet RUBY = GiveEffectAmulet.inLava(
        new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 3, 0, false, false),
        MinMaxBounds.Ints.atMost(3600)
    );
    public static final GiveEffectAmulet SAPPHIRE = GiveEffectAmulet.inWater(
        new MobEffectInstance(MobEffects.CONDUIT_POWER, 3, 0, false, false),
        MinMaxBounds.Ints.atMost(3600)
    );
    public static final AnvilAmulet ANVIL = new AnvilAmulet();
    public static final ComradeAmulet COMRADE = ComradeAmulet.empty();
    public static final ImmuneDamageAmulet FEATHER = ImmuneDamageAmulet.builder()
        .immune(ModDamageTypeTags.FEATHER_AMULET_VALID)
        .build();
    public static final DoNothingAmulet CAT = new DoNothingAmulet();
    public static final DoNothingAmulet DOG = new DoNothingAmulet();
    public static final DoNothingAmulet SILENCE = new DoNothingAmulet();
    public static final DoNothingAmulet ABNORMAL = new DoNothingAmulet();
    public static final WrappedOthersAmulet GEM = WrappedOthersAmulet.of(
        ModAmulets.EMERALD,
        ModAmulets.TOPAZ,
        ModAmulets.RUBY,
        ModAmulets.SAPPHIRE
    );
    public static final WrappedOthersAmulet NATURE = WrappedOthersAmulet.of(
        ModAmulets.FEATHER,
        ModAmulets.CAT,
        ModAmulets.DOG,
        ModAmulets.SILENCE
    );
}
