package dev.dubhe.anvilcraft.init.recipe;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.recipe.number.BinomialDistributionGenerator;
import dev.dubhe.anvilcraft.api.recipe.number.ConstantValue;
import dev.dubhe.anvilcraft.api.recipe.number.EnchantmentLevelProvider;
import dev.dubhe.anvilcraft.api.recipe.number.INumberProvider;
import dev.dubhe.anvilcraft.api.recipe.number.UniformGenerator;
import dev.dubhe.anvilcraft.init.ModRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModNumberProviderTypes {
    private static final DeferredRegister<INumberProvider.Type<?>> DF = DeferredRegister
        .create(ModRegistries.NUMBER_PROVIDER_TYPE_REGISTRY, AnvilCraft.MOD_ID);

    public static final DeferredHolder<INumberProvider.Type<?>, ConstantValue.Type> CONSTANT = DF
        .register("constant", ConstantValue.Type::new);

    public static final DeferredHolder<INumberProvider.Type<?>, BinomialDistributionGenerator.Type> BINOMIAL = DF
        .register("binomial", BinomialDistributionGenerator.Type::new);

    public static final DeferredHolder<INumberProvider.Type<?>, UniformGenerator.Type> UNIFORM = DF
        .register("uniform", UniformGenerator.Type::new);

    public static final DeferredHolder<INumberProvider.Type<?>, EnchantmentLevelProvider.Type> ENCHANTMENT_LEVEL = DF
        .register("enchantment_level", EnchantmentLevelProvider.Type::new);

    public static void register(IEventBus bus) {
        DF.register(bus);
    }
}
