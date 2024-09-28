package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.ConditionalEffect;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.function.UnaryOperator;

public class ModEnchantmentEffectComponents {

    private static final DeferredRegister<DataComponentType<?>> REGISTER =
        DeferredRegister.create(Registries.ENCHANTMENT_EFFECT_COMPONENT_TYPE, AnvilCraft.MOD_ID);

    public static final DataComponentType<List<ConditionalEffect<EnchantmentEntityEffect>>> USE_ON_BLOCK = register(
        "use_on_block",
        (it) -> it.persistent(
            ConditionalEffect.codec(
                EnchantmentEntityEffect.CODEC,
                ModLootContextParamSets.USE_ON_ITEM
            ).listOf()
        )
    );

    private static <T> DataComponentType<T> register(String name, UnaryOperator<DataComponentType.Builder<T>> operator) {
        DataComponentType<T> dct = operator.apply(DataComponentType.builder()).build();
        REGISTER.register(name, () -> dct);
        return dct;
    }

    public static void register(IEventBus bus) {
        REGISTER.register(bus);
    }
}
