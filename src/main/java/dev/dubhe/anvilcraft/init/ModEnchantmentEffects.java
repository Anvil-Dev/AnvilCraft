package dev.dubhe.anvilcraft.init;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.enchantment.FellingEffect;
import dev.dubhe.anvilcraft.enchantment.HarvestEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEnchantmentEffects {
    public static final DeferredRegister<MapCodec<? extends EnchantmentEntityEffect>> REGISTER =
        DeferredRegister.create(Registries.ENCHANTMENT_ENTITY_EFFECT_TYPE, AnvilCraft.MOD_ID);

    static {
        REGISTER.register(
            "harvest",
            () -> HarvestEffect.CODEC
        );
        REGISTER.register(
            "felling",
            () -> FellingEffect.CODEC
        );
    }

    public static void register(IEventBus eventBus) {
        REGISTER.register(eventBus);
    }
}
