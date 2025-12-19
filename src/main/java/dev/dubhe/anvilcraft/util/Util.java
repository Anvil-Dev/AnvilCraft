package dev.dubhe.anvilcraft.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.neoforged.fml.ModList;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Util {
    public static final Lazy<Boolean> jadePresent = new Lazy<>(() -> Util.isLoaded("jade") || Util.isLoaded("wthit"));
    private static final StackWalker STACK_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
    public static final Direction[] HORIZONTAL_DIRECTIONS = new Direction[] {
        Direction.SOUTH,
        Direction.WEST,
        Direction.EAST,
        Direction.NORTH
    };
    public static final Direction[] VERTICAL_DIRECTIONS = new Direction[] {
        Direction.UP,
        Direction.DOWN
    };
    public static final Direction[][] CORNER_DIRECTIONS = new Direction[][] {
        {Direction.EAST, Direction.NORTH},
        {Direction.EAST, Direction.SOUTH},
        {Direction.WEST, Direction.NORTH},
        {Direction.WEST, Direction.SOUTH},
    };

    /**
     * 判断给定的 {@code modId} 对应的模组是否加载
     *
     * @return 若模组加载，返回 {@code true} 。反之则返回 {@code false}
     */
    public static boolean isLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    public static Function<InteractionResult, ItemInteractionResult> interactionResultConverter() {
        return it -> switch (it) {
            case SUCCESS, SUCCESS_NO_ITEM_USED -> ItemInteractionResult.SUCCESS;
            case CONSUME -> ItemInteractionResult.CONSUME;
            case CONSUME_PARTIAL -> ItemInteractionResult.CONSUME_PARTIAL;
            case PASS -> ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            case FAIL -> ItemInteractionResult.FAIL;
        };
    }

    public static boolean findCaller(String caller) {
        return STACK_WALKER.walk(it -> it.anyMatch(frame -> frame.getMethodName().equals(caller)));
    }

    public static void acceptDirections(BlockPos blockPos, Consumer<BlockPos> blockPosConsumer) {
        for (Direction direction : Direction.values()) {
            blockPosConsumer.accept(blockPos.relative(direction));
        }
        for (Direction horizontal : HORIZONTAL_DIRECTIONS) {
            for (Direction vertical : VERTICAL_DIRECTIONS) {
                blockPosConsumer.accept(blockPos.relative(horizontal).relative(vertical));
            }
        }
        for (Direction[] corner : CORNER_DIRECTIONS) {
            BlockPos pos1 = blockPos;
            for (Direction direction : corner) {
                pos1 = pos1.relative(direction);
            }
            for (Direction verticalDirection : VERTICAL_DIRECTIONS) {
                pos1 = pos1.relative(verticalDirection);
                blockPosConsumer.accept(pos1);
            }
        }
    }

    public static void acceptHorizontalDirections(BlockPos blockPos, Consumer<BlockPos> blockPosConsumer) {
        for (Direction direction : HORIZONTAL_DIRECTIONS) {
            blockPosConsumer.accept(blockPos.relative(direction));
        }
    }

    public static boolean isClient() {
        return Thread.currentThread().getThreadGroup() != SidedThreadGroups.SERVER;
    }

    public static boolean isServer() {
        return Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER;
    }

    /**
     * 将传入的值强转为{@code T}类型
     *
     * @param <T> 想要转为的类型
     * @param o   一个值
     *
     * @return 传入的值，但是类型为{@code T}
     * @throws ClassCastException 当无法将传入的值强转时抛出
     */
    @SuppressWarnings("unchecked")
    public static <T> T cast(@NotNull Object o) {
        return (T) o;
    }

    /**
     * 将传入的值强转为{@code T}类型
     *
     * @param <T>              想要转为的类型
     * @param <E>              当无法将传入的值强转时抛出的异常类型
     * @param o                一个值
     * @param exceptionFactory 用于创建异常的工厂
     *
     * @return 传入的值，但是类型为{@code T}
     * @throws E 当无法将传入的值强转时抛出
     */
    @SuppressWarnings("unchecked")
    public static <T, E extends Exception> T cast(@NotNull Object o, Supplier<E> exceptionFactory) throws E {
        try {
            return (T) o;
        } catch (ClassCastException ignored) {
            throw exceptionFactory.get();
        }
    }

    /**
     * 若传入的值可被强转为{@code T}类型，则返回包含传入的值的{@link Optional}
     *
     * @param <T> 想要转为的类型
     * @param o   一个值，可为null
     *
     * @return 一个可能包含传入的值的{@link Optional}
     */
    public static <T> Optional<T> castSafely(@Nullable Object o, Class<T> clazz) {
        return Optional.ofNullable(o)
            .filter(clazz::isInstance)
            .map(Util::cast);
    }

    /**
     * 若传入的值可被强转为传入的任意类型，则返回true
     *
     * @param o 一个值，可为null
     *
     * @return 传入的值，但是类型为{@code T}
     */
    @SuppressWarnings("TypeParameterExplicitlyExtendsObject")
    @SafeVarargs
    public static boolean instanceOfAny(@Nullable Object o, Class<? extends Object>... classes) {
        Optional<Object> op = Optional.empty();
        for (Class<?> clazz : classes) {
            op = op.or(() -> Util.castSafely(o, clazz));
        }
        return op.isPresent();
    }

    /**
     * 一个用于解决以下情景的方法：
     * <pre>{@code
     *     public AClassConstructor(A value) {
     *         // 这两个参数都需传入 value
     *         this(value.voidMethod(), value.returnBMethod());
     *     }
     * }</pre>
     *
     * @param value 原参数
     * @param consumer 需要在传入前调用的方法
     * @param <T> 原参数的类型
     * @return 原参数
     */
    public static <T> T run(T value, Consumer<T> consumer) {
        consumer.accept(value);
        return value;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static <T1, T2> void ifAllPresent(Optional<T1> op1, Supplier<Optional<T2>> op2Getter, BiConsumer<T1, T2> runnable) {
        if (op1.isEmpty()) return;
        var op2 = op2Getter.get();
        if (op2.isEmpty()) return;
        runnable.accept(op1.get(), op2.get());
    }

    /**
     * 使用传入的参数运行代码，并返回原参数
     *
     * @param value 原参数
     * @param consumer 需要在传入前调用的方法
     * @param <T> 原参数的类型
     * @return 原参数
     */
    public static <T> T run(T value, Consumer<T> consumer) {
        consumer.accept(value);
        return value;
    }
}
