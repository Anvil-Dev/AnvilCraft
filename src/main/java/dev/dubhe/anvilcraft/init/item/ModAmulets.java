package dev.dubhe.anvilcraft.init.item;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.entity.ModDamageTypeTags;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import dev.dubhe.anvilcraft.item.property.component.amulet.AnvilAmulet;
import dev.dubhe.anvilcraft.item.property.component.amulet.ComradeAmulet;
import dev.dubhe.anvilcraft.item.property.component.amulet.DiscountAmulet;
import dev.dubhe.anvilcraft.item.property.component.amulet.DoNothingAmulet;
import dev.dubhe.anvilcraft.item.property.component.amulet.GiveEffectAmulet;
import dev.dubhe.anvilcraft.item.property.component.amulet.IAmulet;
import dev.dubhe.anvilcraft.item.property.component.amulet.ImmuneDamageAmulet;
import dev.dubhe.anvilcraft.item.property.component.amulet.WrappedOthersAmulet;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public class ModAmulets {
    private static final DeferredRegister<IAmulet> REGISTER = DeferredRegister.create(
        ModRegistryKeys.AMULET,
        AnvilCraft.MOD_ID
    );

    public static final DeferredHolder<IAmulet, DiscountAmulet> EMERALD = REGISTER.register(
        "emerald",
        () -> new DiscountAmulet(0.3F)
    );
    public static final DeferredHolder<IAmulet, ImmuneDamageAmulet> TOPAZ = REGISTER.register(
        "topaz",
        () -> ImmuneDamageAmulet.builder()
            .immune(ModDamageTypeTags.TOPAZ_AMULET_VALID)
            .build()
    );
    public static final DeferredHolder<IAmulet, GiveEffectAmulet> RUBY = REGISTER.register(
        "ruby",
        () -> GiveEffectAmulet.inLava(
            new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 3, 0, false, false),
            MinMaxBounds.Ints.atMost(3600)
        )
    );
    public static final DeferredHolder<IAmulet, GiveEffectAmulet> SAPPHIRE = REGISTER.register(
        "sapphire",
        () -> GiveEffectAmulet.inWater(
            new MobEffectInstance(MobEffects.CONDUIT_POWER, 3, 0, false, false),
            MinMaxBounds.Ints.atMost(3600)
        )
    );
    public static final DeferredHolder<IAmulet, AnvilAmulet> ANVIL = REGISTER.register("anvil", AnvilAmulet::new);
    public static final DeferredHolder<IAmulet, ComradeAmulet> COMRADE = REGISTER.register("comrade", ComradeAmulet::new);
    public static final DeferredHolder<IAmulet, ImmuneDamageAmulet> FEATHER = REGISTER.register(
        "feather",
        () -> ImmuneDamageAmulet.builder()
            .immune(ModDamageTypeTags.FEATHER_AMULET_VALID)
            .build()
    );
    public static final DeferredHolder<IAmulet, DoNothingAmulet> CAT = REGISTER.register("cat", DoNothingAmulet::new);
    public static final DeferredHolder<IAmulet, DoNothingAmulet> DOG = REGISTER.register("dog", DoNothingAmulet::new);
    public static final DeferredHolder<IAmulet, DoNothingAmulet> SILENCE = REGISTER.register("silence", DoNothingAmulet::new);
    public static final DeferredHolder<IAmulet, DoNothingAmulet> ABNORMAL = REGISTER.register("abnormal", DoNothingAmulet::new);
    public static final DeferredHolder<IAmulet, WrappedOthersAmulet> GEM = REGISTER.register(
        "gem",
        () -> new WrappedOthersAmulet(List.of(
            ModAmulets.EMERALD.getKey(),
            ModAmulets.TOPAZ.getKey(),
            ModAmulets.RUBY.getKey(),
            ModAmulets.SAPPHIRE.getKey()
        ))
    );
    public static final DeferredHolder<IAmulet, WrappedOthersAmulet> NATURE = REGISTER.register(
        "nature",
        () -> new WrappedOthersAmulet(List.of(
            ModAmulets.FEATHER.getKey(),
            ModAmulets.CAT.getKey(),
            ModAmulets.DOG.getKey(),
            ModAmulets.SILENCE.getKey()
        ))
    );

    public static void register(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
    }
}
