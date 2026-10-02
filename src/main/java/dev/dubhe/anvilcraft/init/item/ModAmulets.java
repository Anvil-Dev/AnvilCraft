package dev.dubhe.anvilcraft.init.item;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.amulet.Amulet;
import dev.dubhe.anvilcraft.api.amulet.effect.ActAsScarecrowAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.AttributeAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.DiscountAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.GiveMobEffectAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.IgnoreGravityAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.IgnoreMobTargetAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.ImmuneAbnormalItemAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.ImmuneEatEffectAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.ImmuneEntityAnvilDamageAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.ImmuneFriendlyDamageAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.ImmuneKnockbackAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.ImmuneMobEffectAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.ImmuneTypedDamageAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.ImmuneVibrationAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.ScarePhantomAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.TameAnimalAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.WrapOtherAmuletEffect;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypeTags;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import dev.dubhe.anvilcraft.util.Impersonators;
import net.minecraft.advancements.criterion.EntityTypePredicate;
import net.minecraft.advancements.criterion.TagPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public class ModAmulets {
    public static final int SMALL_AMULET_WEIGHT = 6;
    public static final int BIG_AMULET_WEIGHT = 9;
    private static final Identifier ANVIL_KNOCKBACK_RESISTANCE = AnvilCraft.of("anvil_amulet_knockback_resistance");
    private static final DeferredRegister<Amulet> REGISTER = DeferredRegister.create(
        ModRegistryKeys.AMULET,
        AnvilCraft.MOD_ID
    );

    public static final DeferredHolder<Amulet, Amulet> EMERALD = REGISTER.register(
        "emerald",
        () -> Amulet.of(
            new DiscountAmuletEffect(0.3F),
            new IgnoreMobTargetAmuletEffect(List.of(EntityTypePredicate.of(BuiltInRegistries.ENTITY_TYPE, EntityType.IRON_GOLEM)))
        )
    );
    public static final DeferredHolder<Amulet, Amulet> TOPAZ = REGISTER.register(
        "topaz",
        () -> Amulet.of(
            new ImmuneTypedDamageAmuletEffect(List.of(TagPredicate.is(ModDamageTypeTags.TOPAZ_AMULET_VALID))),
            GiveMobEffectAmuletEffect.always(MobEffects.HASTE, 0)
        )
    );
    public static final DeferredHolder<Amulet, Amulet> RUBY = REGISTER.register(
        "ruby",
        () -> Amulet.of(
            GiveMobEffectAmuletEffect.always(MobEffects.FIRE_RESISTANCE, 0),
            GiveMobEffectAmuletEffect.onFire(MobEffects.STRENGTH, 1),
            GiveMobEffectAmuletEffect.notOnFire(MobEffects.STRENGTH, 0)
        )
    );
    public static final DeferredHolder<Amulet, Amulet> SAPPHIRE = REGISTER.register(
        "sapphire",
        () -> Amulet.of(
            GiveMobEffectAmuletEffect.always(MobEffects.CONDUIT_POWER, 0),
            GiveMobEffectAmuletEffect.inWaterOrBreathing(MobEffects.RESISTANCE, 0)
        )
    );
    public static final DeferredHolder<Amulet, Amulet> ANVIL = REGISTER.register(
        "anvil",
        () -> Amulet.of(
            ImmuneTypedDamageAmuletEffect.of(ModDamageTypeTags.ANVIL_AMULET_VALID),
            ImmuneEntityAnvilDamageAmuletEffect.INSTANCE,
            ImmuneMobEffectAmuletEffect.of(MobEffects.LEVITATION),
            new AttributeAmuletEffect(
                Attributes.KNOCKBACK_RESISTANCE,
                ModAmulets.ANVIL_KNOCKBACK_RESISTANCE,
                1,
                AttributeModifier.Operation.ADD_VALUE
            ),
            ImmuneKnockbackAmuletEffect.INSTANCE,
            IgnoreGravityAmuletEffect.INSTANCE
        )
    );
    public static final DeferredHolder<Amulet, Amulet> COMRADE = REGISTER.register(
        "comrade",
        () -> Amulet.of(ImmuneFriendlyDamageAmuletEffect.INSTANCE)
    );
    public static final DeferredHolder<Amulet, Amulet> FEATHER = REGISTER.register(
        "feather",
        () -> Amulet.of(
            new ImmuneTypedDamageAmuletEffect(List.of(TagPredicate.is(ModDamageTypeTags.FEATHER_AMULET_VALID))),
            ImmuneMobEffectAmuletEffect.whileSneaking(MobEffects.SLOW_FALLING),
            GiveMobEffectAmuletEffect.notSneaking(MobEffects.SLOW_FALLING, 0)
        )
    );
    public static final DeferredHolder<Amulet, Amulet> ARMADILLO = REGISTER.register(
        "armadillo",
        () -> Amulet.of(
            new ActAsScarecrowAmuletEffect<>(Impersonators.ARMADILLO),
            GiveMobEffectAmuletEffect.sneaking(MobEffects.RESISTANCE, 1)
        )
    );
    public static final DeferredHolder<Amulet, Amulet> CAT = REGISTER.register(
        "cat",
        () -> Amulet.of(
            new ActAsScarecrowAmuletEffect<>(Impersonators.CAT),
            ScarePhantomAmuletEffect.INSTANCE,
            new TameAnimalAmuletEffect(List.of(EntityTypePredicate.of(BuiltInRegistries.ENTITY_TYPE, EntityType.CAT)))
        )
    );
    public static final DeferredHolder<Amulet, Amulet> DOG = REGISTER.register(
        "dog",
        () -> Amulet.of(
            new ActAsScarecrowAmuletEffect<>(Impersonators.WOLF),
            new TameAnimalAmuletEffect(List.of(EntityTypePredicate.of(BuiltInRegistries.ENTITY_TYPE, EntityType.WOLF)))
        )
    );
    public static final DeferredHolder<Amulet, Amulet> SILENCE = REGISTER.register(
        "silence",
        () -> Amulet.of(
            ImmuneMobEffectAmuletEffect.of(MobEffects.DARKNESS),
            ImmuneVibrationAmuletEffect.INSTANCE
        )
    );
    public static final DeferredHolder<Amulet, Amulet> ABNORMAL = REGISTER.register(
        "abnormal",
        () -> Amulet.of(
            new ImmuneEatEffectAmuletEffect(MobEffectCategory.HARMFUL),
            ImmuneAbnormalItemAmuletEffect.INSTANCE
        )
    );
    public static final DeferredHolder<Amulet, Amulet> GEM = REGISTER.register(
        "gem",
        () -> Amulet.of(new WrapOtherAmuletEffect(List.of(
            ModAmulets.EMERALD.getKey(),
            ModAmulets.TOPAZ.getKey(),
            ModAmulets.RUBY.getKey(),
            ModAmulets.SAPPHIRE.getKey()
        )))
    );
    public static final DeferredHolder<Amulet, Amulet> NATURE = REGISTER.register(
        "nature",
        () -> Amulet.of(new WrapOtherAmuletEffect(List.of(
            ModAmulets.ARMADILLO.getKey(),
            ModAmulets.CAT.getKey(),
            ModAmulets.DOG.getKey(),
            ModAmulets.SILENCE.getKey()
        )))
    );

    public static void register(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
    }
}
