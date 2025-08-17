package dev.dubhe.anvilcraft.api.heat;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

public record HeaterInfo<T>(
    BiFunction<Level, BlockPos, Optional<T>> getter,
    Function<T, Set<BlockPos>> posesGetter,
    HeatTierLine line,
    ToIntFunction<T> countGetter) {
    public static <T extends BlockEntity> HeaterInfo<T> blockEntity(
        Supplier<BlockEntityType<T>> type,
        Function<T, Set<BlockPos>> posesGetter,
        HeatTierLine line,
        ToIntFunction<T> countGetter
    ) {
        return new HeaterInfo<>(
            (level, pos) -> level.getBlockEntity(pos, type.get()),
            posesGetter, line, countGetter
        );
    }

    public static <T extends BlockEntity> HeaterInfo<T> blockEntity(
        BiFunction<Level, BlockPos, Optional<T>> getter,
        Function<T, Set<BlockPos>> posesGetter,
        HeatTierLine line,
        ToIntFunction<T> countGetter
    ) {
        return new HeaterInfo<>(getter, posesGetter, line, countGetter);
    }

    public static <T extends BlockEntity> HeaterInfo<T> blockEntity(
        Supplier<BlockEntityType<T>> type,
        Function<T, Set<BlockPos>> posesGetter,
        HeatTierLine line
    ) {
        return new HeaterInfo<>(
            (level, pos) -> level.getBlockEntity(pos, type.get()),
            posesGetter, line, o -> 1
        );
    }

    public static <T extends BlockEntity> HeaterInfo<T> blockEntity(
        BiFunction<Level, BlockPos, Optional<T>> getter,
        Function<T, Set<BlockPos>> posesGetter,
        HeatTierLine line
    ) {
        return new HeaterInfo<>(getter, posesGetter, line, o -> 1);
    }
}
