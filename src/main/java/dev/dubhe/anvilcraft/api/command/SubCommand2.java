package dev.dubhe.anvilcraft.api.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.function.Function;

public interface SubCommand2<T, U> {
    int run(T value, U value2) throws CommandSyntaxException;

    default <S> Command<S> toCommand(
        Function<S, T> transformer,
        Function<S, U> transformer2
    ) {
        return ctx -> this.run(
            transformer.apply(ctx.getSource()),
            transformer2.apply(ctx.getSource())
        );
    }
}
