package dev.dubhe.anvilcraft.init.enchantment;

import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.enchantment.FellingEffect;
import dev.dubhe.anvilcraft.enchantment.HarvestLeftClickEffect;
import dev.dubhe.anvilcraft.enchantment.HarvestRightClickEffect;
import dev.dubhe.anvilcraft.enchantment.InRangeModifyEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentValueEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEnchantmentEffects {
    public static final DeferredRegister<MapCodec<? extends EnchantmentEntityEffect>> ENTITY_REGISTER =
        DeferredRegister.create(Registries.ENCHANTMENT_ENTITY_EFFECT_TYPE, AnvilCraft.MOD_ID);
    public static final DeferredRegister<MapCodec<? extends EnchantmentValueEffect>> VALUE_REGISTER =
        DeferredRegister.create(Registries.ENCHANTMENT_VALUE_EFFECT_TYPE, AnvilCraft.MOD_ID);

    static {
        ModEnchantmentEffects.ENTITY_REGISTER.register(
            "haevest_left_click",
            () -> HarvestLeftClickEffect.CODEC
        );
        ModEnchantmentEffects.ENTITY_REGISTER.register(
            "harvest_right_click",
            () -> HarvestRightClickEffect.CODEC
        );
        ModEnchantmentEffects.ENTITY_REGISTER.register(
            "felling",
            () -> FellingEffect.CODEC
        );
        ModEnchantmentEffects.VALUE_REGISTER.register(
            "in_range_modify",
            () -> InRangeModifyEffect.CODEC
        );
    }

    public static void register(IEventBus eventBus) {
        ModEnchantmentEffects.ENTITY_REGISTER.register(eventBus);
        ModEnchantmentEffects.VALUE_REGISTER.register(eventBus);
    }
}
