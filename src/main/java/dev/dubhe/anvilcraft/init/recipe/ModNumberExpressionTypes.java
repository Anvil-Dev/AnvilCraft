package dev.dubhe.anvilcraft.init.recipe;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.number.ArithmeticExpression;
import dev.dubhe.anvilcraft.api.number.ConstantExpression;
import dev.dubhe.anvilcraft.api.number.FlatExpression;
import dev.dubhe.anvilcraft.api.number.FunctionExpression;
import dev.dubhe.anvilcraft.api.number.INumberExpression;
import dev.dubhe.anvilcraft.api.number.InputExpression;
import dev.dubhe.anvilcraft.api.number.NamedExpression;
import dev.dubhe.anvilcraft.init.registry.ModRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModNumberExpressionTypes {
    private static final DeferredRegister<INumberExpression.Type<?>> DF = DeferredRegister
        .create(ModRegistries.NUMBER_EXPRESSION_TYPE, AnvilCraft.MOD_ID);

    public static final DeferredHolder<INumberExpression.Type<?>, ConstantExpression.Type> CONSTANT = DF
        .register("constant", ConstantExpression.Type::new);

    public static final DeferredHolder<INumberExpression.Type<?>, InputExpression.Type> INPUT = DF
        .register("input", InputExpression.Type::new);

    public static final DeferredHolder<INumberExpression.Type<?>, NamedExpression.Type> NAMED = DF
        .register("named", NamedExpression.Type::new);

    public static final DeferredHolder<INumberExpression.Type<?>, ArithmeticExpression.Type> ARITHMETIC = DF
        .register("arithmetic", ArithmeticExpression.Type::new);

    public static final DeferredHolder<INumberExpression.Type<?>, FunctionExpression.Type> FUNCTION = DF
        .register("function", FunctionExpression.Type::new);

    public static final DeferredHolder<INumberExpression.Type<?>, FlatExpression.Type> FLAT = DF
        .register("flat", FlatExpression.Type::new);

    public static void register(IEventBus bus) {
        DF.register(bus);
    }
}
