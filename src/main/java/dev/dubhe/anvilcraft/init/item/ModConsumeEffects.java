package dev.dubhe.anvilcraft.init.item;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.item.property.consume.PreventShrinkingConsumeEffect;
import dev.dubhe.anvilcraft.item.property.consume.SetFoodLevelConsumeEffect;
import dev.dubhe.anvilcraft.item.property.consume.SetRagedConsumeEffect;
import dev.dubhe.anvilcraft.item.property.consume.TeleportToRespawnPointConsumeEffect;
import dev.dubhe.anvilcraft.item.property.consume.TryTotemsInBoxConsumeEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.consume_effects.ConsumeEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModConsumeEffects {
    public static final DeferredRegister<ConsumeEffect.Type<?>> DF = DeferredRegister.create(
        Registries.CONSUME_EFFECT_TYPE,
        AnvilCraft.MOD_ID
    );

    public static final DeferredHolder<ConsumeEffect.Type<?>, ConsumeEffect.Type<TeleportToRespawnPointConsumeEffect>> TP_TO_RESPAWN = DF
        .register("tp_to_respawn", () -> new ConsumeEffect.Type<>(
            TeleportToRespawnPointConsumeEffect.CODEC,
            TeleportToRespawnPointConsumeEffect.STREAM_CODEC.cast()
        ));

    public static final DeferredHolder<ConsumeEffect.Type<?>, ConsumeEffect.Type<SetFoodLevelConsumeEffect>> SET_FOOD_LEVEL = DF
        .register("set_food_level", () -> new ConsumeEffect.Type<>(
            SetFoodLevelConsumeEffect.CODEC,
            SetFoodLevelConsumeEffect.STREAM_CODEC.cast()
        ));

    public static final DeferredHolder<ConsumeEffect.Type<?>, ConsumeEffect.Type<TryTotemsInBoxConsumeEffect>> TRY_TOTEMS_IN_BOX = DF
        .register("try_totems_in_box", () -> new ConsumeEffect.Type<>(
            TryTotemsInBoxConsumeEffect.CODEC,
            TryTotemsInBoxConsumeEffect.STREAM_CODEC.cast()
        ));

    public static final DeferredHolder<ConsumeEffect.Type<?>, ConsumeEffect.Type<PreventShrinkingConsumeEffect>> PREVENT_SHRINKING = DF
        .register("prevent_shrinking", () -> new ConsumeEffect.Type<>(
            PreventShrinkingConsumeEffect.CODEC,
            PreventShrinkingConsumeEffect.STREAM_CODEC.cast()
        ));

    public static final DeferredHolder<ConsumeEffect.Type<?>, ConsumeEffect.Type<SetRagedConsumeEffect>> SET_RAGED = DF
        .register("set_raged", () -> new ConsumeEffect.Type<>(
            SetRagedConsumeEffect.CODEC,
            SetRagedConsumeEffect.STREAM_CODEC.cast()
        ));

    public static void register(IEventBus bus) {
        DF.register(bus);
    }
}
