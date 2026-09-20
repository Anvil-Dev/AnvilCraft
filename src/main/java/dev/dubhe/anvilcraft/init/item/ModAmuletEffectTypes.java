package dev.dubhe.anvilcraft.init.item;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.amulet.effect.AttributeAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.ConditionalMobEffectAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.DiscountAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.GiveEffectAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.IAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.IgnoreGravityAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.IgnoreMobTargetAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.ImmuneAbnormalItemAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.ImmuneAnvilDamageAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.ImmuneFriendlyDamageAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.ImmuneHarmfulMobEffectAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.ImmuneKnockbackAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.ImmuneMobEffectAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.ImmuneMurdererDamageAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.ImmuneTypedDamageAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.ImmuneVibrationAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.TameAnimalAmuletEffect;
import dev.dubhe.anvilcraft.api.amulet.effect.WrapOtherAmuletEffect;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModAmuletEffectTypes {
    private static final DeferredRegister<IAmuletEffect.Type<?>> REGISTER = DeferredRegister.create(
        ModRegistryKeys.AMULET_EFFECT_TYPE,
        AnvilCraft.MOD_ID
    );

    public static final DeferredHolder<IAmuletEffect.Type<?>, DiscountAmuletEffect.Type> DISCOUNT = REGISTER.register(
        "discount",
        DiscountAmuletEffect.Type::new
    );
    public static final DeferredHolder<IAmuletEffect.Type<?>, GiveEffectAmuletEffect.Type> GIVE_EFFECT = REGISTER.register(
        "give_effect",
        GiveEffectAmuletEffect.Type::new
    );
    public static final DeferredHolder<IAmuletEffect.Type<?>, ImmuneTypedDamageAmuletEffect.Type> IMMUNE_TYPED_DAMAGE = REGISTER.register(
        "immune_typed_damage",
        ImmuneTypedDamageAmuletEffect.Type::new
    );
    public static final DeferredHolder<IAmuletEffect.Type<?>, ImmuneMurdererDamageAmuletEffect.Type> IMMUNE_MURDERER_DAMAGE =
        REGISTER.register("immune_murderer_damage", ImmuneMurdererDamageAmuletEffect.Type::new);
    public static final DeferredHolder<IAmuletEffect.Type<?>, ImmuneFriendlyDamageAmuletEffect.Type> IMMUNE_FRIENDLY_DAMAGE =
        REGISTER.register("immune_friendly_damage", ImmuneFriendlyDamageAmuletEffect.Type::new);
    public static final DeferredHolder<IAmuletEffect.Type<?>, ImmuneAnvilDamageAmuletEffect.Type> IMMUNE_ANVIL_DAMAGE = REGISTER.register(
        "immune_anvil_damage",
        ImmuneAnvilDamageAmuletEffect.Type::new
    );
    public static final DeferredHolder<IAmuletEffect.Type<?>, ImmuneMobEffectAmuletEffect.Type> IMMUNE_MOB_EFFECT = REGISTER.register(
        "immune_mob_effect",
        ImmuneMobEffectAmuletEffect.Type::new
    );
    public static final DeferredHolder<IAmuletEffect.Type<?>, ImmuneHarmfulMobEffectAmuletEffect.Type> IMMUNE_HARMFUL_MOB_EFFECT =
        REGISTER.register("immune_harmful_mob_effect", ImmuneHarmfulMobEffectAmuletEffect.Type::new);
    public static final DeferredHolder<IAmuletEffect.Type<?>, ConditionalMobEffectAmuletEffect.Type> CONDITIONAL_MOB_EFFECT =
        REGISTER.register("conditional_mob_effect", ConditionalMobEffectAmuletEffect.Type::new);
    public static final DeferredHolder<IAmuletEffect.Type<?>, AttributeAmuletEffect.Type> ATTRIBUTE = REGISTER.register(
        "attribute",
        AttributeAmuletEffect.Type::new
    );
    public static final DeferredHolder<IAmuletEffect.Type<?>, ImmuneKnockbackAmuletEffect.Type> IMMUNE_KNOCKBACK = REGISTER.register(
        "immune_knockback",
        ImmuneKnockbackAmuletEffect.Type::new
    );
    public static final DeferredHolder<IAmuletEffect.Type<?>, IgnoreGravityAmuletEffect.Type> IGNORE_GRAVITY = REGISTER.register(
        "ignore_gravity",
        IgnoreGravityAmuletEffect.Type::new
    );
    public static final DeferredHolder<IAmuletEffect.Type<?>, ImmuneVibrationAmuletEffect.Type> IMMUNE_VIBRATION = REGISTER.register(
        "immune_vibration",
        ImmuneVibrationAmuletEffect.Type::new
    );
    public static final DeferredHolder<IAmuletEffect.Type<?>, IgnoreMobTargetAmuletEffect.Type> IGNORE_MOB_TARGET = REGISTER.register(
        "ignore_mob_target",
        IgnoreMobTargetAmuletEffect.Type::new
    );
    public static final DeferredHolder<IAmuletEffect.Type<?>, TameAnimalAmuletEffect.Type> TAME_ANIMAL = REGISTER.register(
        "tame_animal",
        TameAnimalAmuletEffect.Type::new
    );
    public static final DeferredHolder<IAmuletEffect.Type<?>, ImmuneAbnormalItemAmuletEffect.Type> IMMUNE_ABNORMAL_ITEM =
        REGISTER.register("immune_abnormal_item", ImmuneAbnormalItemAmuletEffect.Type::new);
    public static final DeferredHolder<IAmuletEffect.Type<?>, WrapOtherAmuletEffect.Type> WRAP_OTHER = REGISTER.register(
        "wrap_other",
        WrapOtherAmuletEffect.Type::new
    );

    public static void register(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
    }
}
