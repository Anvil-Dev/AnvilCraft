package dev.dubhe.anvilcraft.init.enchantment;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.enchantment.FellingEffect;
import dev.dubhe.anvilcraft.enchantment.HarvestLeftClickEffect;
import dev.dubhe.anvilcraft.enchantment.HarvestRightClickEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEnchantmentEffects {
    public static final DeferredRegister<MapCodec<? extends EnchantmentEntityEffect>> REGISTER =
        DeferredRegister.create(Registries.ENCHANTMENT_ENTITY_EFFECT_TYPE, AnvilCraft.MOD_ID);

    static {
        REGISTER.register(
            "haevest_left_click",
            () -> HarvestLeftClickEffect.CODEC
        );
        REGISTER.register(
            "harvest_right_click",
            () -> HarvestRightClickEffect.CODEC
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
