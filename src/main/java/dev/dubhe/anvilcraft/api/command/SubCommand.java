package dev.dubhe.anvilcraft.api.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import java.util.function.Function;

public interface SubCommand<T> {
    int run(T value) throws CommandSyntaxException;

    default <S> Command<S> toCommand(Function<S, T> transformer) {
        return ctx -> this.run(transformer.apply(ctx.getSource()));
    }
}
