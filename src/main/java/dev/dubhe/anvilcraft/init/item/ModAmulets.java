package dev.dubhe.anvilcraft.init.item;

import dev.anvilcraft.lib.v2.math.expression.IExpression;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.amulet.Amulet;
import dev.dubhe.anvilcraft.api.amulet.effect.AttributeAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.DiscountAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.GiveMobEffectAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.IgnoreGravityAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.IgnoreMobTargetAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.ImmuneAbnormalItemAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.ImmuneAnvilDamageAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.ImmuneFriendlyDamageAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.ImmuneHarmfulMobEffectAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.ImmuneKnockbackAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.ImmuneMobEffectAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.ImmuneTypedDamageAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.ImmuneVibrationAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.TameAnimalAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.WrapOtherAmuletEffect;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypeTags;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import net.minecraft.advancements.critereon.EntityTypePredicate;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.advancements.critereon.TagPredicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
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
    private static final ResourceLocation ANVIL_KNOCKBACK_RESISTANCE = AnvilCraft.of("anvil_amulet_knockback_resistance");
    private static final DeferredRegister<Amulet> REGISTER = DeferredRegister.create(
        ModRegistryKeys.AMULET,
        AnvilCraft.MOD_ID
    );

    public static final DeferredHolder<Amulet, Amulet> EMERALD = REGISTER.register(
        "emerald",
        () -> Amulet.of(
            new DiscountAmuletEffect(IExpression.of(0.3)),
            new IgnoreMobTargetAmuletEffect(List.of(EntityTypePredicate.of(EntityType.IRON_GOLEM)))
        )
    );
    public static final DeferredHolder<Amulet, Amulet> TOPAZ = REGISTER.register(
        "topaz",
        () -> Amulet.of(
            new ImmuneTypedDamageAmuletEffect(List.of(TagPredicate.is(ModDamageTypeTags.TOPAZ_AMULET_VALID))),
            GiveMobEffectAmuletEffect.always(MobEffects.DIG_SPEED, 0)
        )
    );
    public static final DeferredHolder<Amulet, Amulet> RUBY = REGISTER.register(
        "ruby",
        () -> Amulet.of(
            GiveMobEffectAmuletEffect.notInLava(
                new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 3, 0, false, false),
                MinMaxBounds.Ints.atMost(3600)
            ),
            GiveMobEffectAmuletEffect.onFire(MobEffects.DAMAGE_BOOST, 1),
            GiveMobEffectAmuletEffect.notOnFire(MobEffects.DAMAGE_BOOST, 0)
        )
    );
    public static final DeferredHolder<Amulet, Amulet> SAPPHIRE = REGISTER.register(
        "sapphire",
        () -> Amulet.of(
            GiveMobEffectAmuletEffect.notInWater(
                new MobEffectInstance(MobEffects.CONDUIT_POWER, 3, 0, false, false),
                MinMaxBounds.Ints.atMost(3600)
            ),
            GiveMobEffectAmuletEffect.inWaterOrBreathing(MobEffects.DAMAGE_RESISTANCE, 0)
        )
    );
    public static final DeferredHolder<Amulet, Amulet> ANVIL = REGISTER.register(
        "anvil",
        () -> Amulet.of(
            new ImmuneAnvilDamageAmuletEffect(),
            ImmuneMobEffectAmuletEffect.of(MobEffects.LEVITATION),
            new AttributeAmuletEffect(
                Attributes.KNOCKBACK_RESISTANCE,
                ModAmulets.ANVIL_KNOCKBACK_RESISTANCE,
                1,
                AttributeModifier.Operation.ADD_VALUE
            ),
            new ImmuneKnockbackAmuletEffect(),
            new IgnoreGravityAmuletEffect()
        )
    );
    public static final DeferredHolder<Amulet, Amulet> COMRADE = REGISTER.register(
        "comrade",
        () -> Amulet.of(new ImmuneFriendlyDamageAmuletEffect())
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
            new IgnoreMobTargetAmuletEffect(List.of(EntityTypePredicate.of(EntityType.SPIDER))),
            GiveMobEffectAmuletEffect.sneaking(MobEffects.DAMAGE_RESISTANCE, 1)
        )
    );
    public static final DeferredHolder<Amulet, Amulet> CAT = REGISTER.register(
        "cat",
        () -> Amulet.of(
            new IgnoreMobTargetAmuletEffect(List.of(
                EntityTypePredicate.of(EntityType.CREEPER),
                EntityTypePredicate.of(EntityType.PHANTOM)
            )),
            new TameAnimalAmuletEffect(List.of(EntityTypePredicate.of(EntityType.CAT)))
        )
    );
    public static final DeferredHolder<Amulet, Amulet> DOG = REGISTER.register(
        "dog",
        () -> Amulet.of(
            new IgnoreMobTargetAmuletEffect(List.of(EntityTypePredicate.of(EntityTypeTags.SKELETONS))),
            new TameAnimalAmuletEffect(List.of(EntityTypePredicate.of(EntityType.WOLF)))
        )
    );
    public static final DeferredHolder<Amulet, Amulet> SILENCE = REGISTER.register(
        "silence",
        () -> Amulet.of(
            ImmuneMobEffectAmuletEffect.of(MobEffects.DARKNESS),
            new ImmuneVibrationAmuletEffect()
        )
    );
    public static final DeferredHolder<Amulet, Amulet> ABNORMAL = REGISTER.register(
        "abnormal",
        () -> Amulet.of(
            new ImmuneHarmfulMobEffectAmuletEffect(),
            new ImmuneAbnormalItemAmuletEffect()
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
