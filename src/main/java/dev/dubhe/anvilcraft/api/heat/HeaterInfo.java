package dev.dubhe.anvilcraft.api.heat;

import dev.dubhe.anvilcraft.util.BlockInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

public record HeaterInfo<T>(
    BiFunction<Level, BlockPos, Optional<T>> getter,
    Function<T, Set<BlockPos>> posesGetter,
    BiFunction<T, BlockPos, Integer> tierOffsetGetter,
    HeatTierLine line,
    ToIntFunction<T> countGetter
) {

    private static <T> BiFunction<T, BlockPos, Integer> noTierOffset() {
        return (_, _) -> 0;
    }

    public static <T extends BlockEntity> HeaterInfo<T> blockEntity(
        Supplier<BlockEntityType<T>> type,
        Function<T, Set<BlockPos>> posesGetter,
        BiFunction<T, BlockPos, Integer> tierOffsetGetter,
        HeatTierLine line
    ) {
        return new HeaterInfo<>(
            (level, pos) -> {
                if (!level.isLoaded(pos)) return Optional.empty();
                return level.getBlockEntity(pos, type.get());
            },
            posesGetter,
            tierOffsetGetter,
            line,
            _ -> 1
        );
    }

    public static <T extends BlockEntity> HeaterInfo<T> blockEntity(
        Supplier<BlockEntityType<T>> type,
        Function<T, Set<BlockPos>> posesGetter,
        HeatTierLine line,
        ToIntFunction<T> countGetter
    ) {
        return new HeaterInfo<>(
            (level, pos) -> {
                if (!level.isLoaded(pos)) return Optional.empty();
                return level.getBlockEntity(pos, type.get());
            },
            posesGetter,
            noTierOffset(),
            line,
            countGetter
        );
    }

    public static <T extends BlockEntity> HeaterInfo<T> blockEntity(
        BiFunction<Level, BlockPos, Optional<T>> getter,
        Function<T, Set<BlockPos>> posesGetter,
        HeatTierLine line,
        ToIntFunction<T> countGetter
    ) {
        return new HeaterInfo<>(getter, posesGetter, noTierOffset(), line, countGetter);
    }

    public static <T extends BlockEntity> HeaterInfo<T> blockEntity(
        Supplier<BlockEntityType<T>> type,
        Function<T, Set<BlockPos>> posesGetter,
        HeatTierLine line
    ) {
        return new HeaterInfo<>(
            (level, pos) -> {
                if (!level.isLoaded(pos)) return Optional.empty();
                return level.getBlockEntity(pos, type.get());
            },
            posesGetter,
            noTierOffset(),
            line,
            _ -> 1
        );
    }

    public static <T extends BlockEntity> HeaterInfo<T> blockEntity(
        BiFunction<Level, BlockPos, Optional<T>> getter,
        Function<T, Set<BlockPos>> posesGetter,
        HeatTierLine line
    ) {
        return new HeaterInfo<>(getter, posesGetter, noTierOffset(), line, _ -> 1);
    }

    public static <T extends Block, P extends Comparable<P>> HeaterInfo<BlockInfo> blockState(
        T block,
        Property<P> property,
        P expectValue,
        Function<BlockInfo, Set<BlockPos>> posesGetter,
        HeatTierLine line,
        ToIntFunction<BlockInfo> countGetter
    ) {
        return new HeaterInfo<>(
            (level, pos) -> {
                if (!level.isLoaded(pos)) return Optional.empty();
                var state = level.getBlockState(pos);
                if (!state.is(block) || state.getOptionalValue(property).filter(expectValue::equals).isEmpty()) return Optional.empty();
                return Optional.of(new BlockInfo(pos, state));
            },
            posesGetter,
            noTierOffset(),
            line,
            countGetter
        );
    }

    public static <P extends Comparable<P>> HeaterInfo<BlockInfo> blockState(
        TagKey<Block> tag,
        Property<P> property,
        P expectValue,
        Function<BlockInfo, Set<BlockPos>> posesGetter,
        HeatTierLine line,
        ToIntFunction<BlockInfo> countGetter
    ) {
        return new HeaterInfo<>(
            (level, pos) -> {
                if (!level.isLoaded(pos)) return Optional.empty();
                var state = level.getBlockState(pos);
                if (!state.is(tag) || state.getOptionalValue(property).filter(expectValue::equals).isEmpty()) return Optional.empty();
                return Optional.of(new BlockInfo(pos, state));
            },
            posesGetter,
            noTierOffset(),
            line,
            countGetter
        );
    }

    public static HeaterInfo<BlockInfo> blockState(
        BiFunction<Level, BlockPos, Optional<BlockInfo>> getter,
        Function<BlockInfo, Set<BlockPos>> posesGetter,
        HeatTierLine line,
        ToIntFunction<BlockInfo> countGetter
    ) {
        return new HeaterInfo<>(getter, posesGetter, noTierOffset(), line, countGetter);
    }

    public static <T extends Block, P extends Comparable<P>> HeaterInfo<BlockInfo> blockState(
        T block,
        Property<P> property,
        P expectValue,
        Function<BlockInfo, Set<BlockPos>> posesGetter,
        HeatTierLine line
    ) {
        return new HeaterInfo<>(
            (level, pos) -> {
                if (!level.isLoaded(pos)) return Optional.empty();
                var state = level.getBlockState(pos);
                if (!state.is(block) || !state.getValue(property).equals(expectValue)) return Optional.empty();
                return Optional.of(new BlockInfo(pos, state));
            },
            posesGetter,
            noTierOffset(),
            line,
            _ -> 1
        );
    }

    public static <P extends Comparable<P>> HeaterInfo<BlockInfo> blockState(
        TagKey<Block> tag,
        Property<P> property,
        P expectValue,
        Function<BlockInfo, Set<BlockPos>> posesGetter,
        HeatTierLine line
    ) {
        return new HeaterInfo<>(
            (level, pos) -> {
                if (!level.isLoaded(pos)) return Optional.empty();
                var state = level.getBlockState(pos);
                if (!state.is(tag) || state.getOptionalValue(property).filter(expectValue::equals).isEmpty()) return Optional.empty();
                return Optional.of(new BlockInfo(pos, state));
            },
            posesGetter,
            noTierOffset(),
            line,
            _ -> 1
        );
    }

    public static HeaterInfo<BlockInfo> blockState(
        BiFunction<Level, BlockPos, Optional<BlockInfo>> getter,
        Function<BlockInfo, Set<BlockPos>> posesGetter,
        HeatTierLine line
    ) {
        return new HeaterInfo<>(getter, posesGetter, noTierOffset(), line, _ -> 1);
    }
}
