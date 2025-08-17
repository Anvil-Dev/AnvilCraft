package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.InstantenousMobEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class ModMobEffects {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, AnvilCraft.MOD_ID);

    public static final DeferredHolder<MobEffect, MobEffect> RAGE = EFFECTS.register("rage", () -> new InstantenousMobEffect(MobEffectCategory.BENEFICIAL, 0xFF0000));
    public static final DeferredHolder<MobEffect, MobEffect> INVULNERABLE = EFFECTS.register("invulnerable", () -> new InstantenousMobEffect(MobEffectCategory.BENEFICIAL, 0xFF0000));

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }
}
