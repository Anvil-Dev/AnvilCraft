package dev.dubhe.anvilcraft.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.neoforged.fml.ModList;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Util {
    public static final Lazy<Boolean> jadePresent = new Lazy<>(() -> isLoaded("jade") || isLoaded("wthit"));
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
        {Direction.EAST, Direction.NORTH}, {Direction.EAST, Direction.SOUTH},
        {Direction.WEST, Direction.NORTH}, {Direction.WEST, Direction.SOUTH},
    };
    /**
     * @return 模组是否加载
     */
    public static boolean isLoaded(String modid) {
        return ModList.get().isLoaded(modid);
    }

    /**
     *
     */
    public static Function<InteractionResult, ItemInteractionResult> interactionResultConverter() {
        return it -> switch (it) {
            case SUCCESS, SUCCESS_NO_ITEM_USED -> ItemInteractionResult.SUCCESS;
            case CONSUME -> ItemInteractionResult.CONSUME;
            case CONSUME_PARTIAL -> ItemInteractionResult.CONSUME_PARTIAL;
            case PASS -> ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            case FAIL -> ItemInteractionResult.FAIL;
        };
    }

    public static <E> Optional<List<E>> intoOptional(List<E> collection) {
        if (collection.isEmpty()) return Optional.empty();
        return Optional.of(collection);
    }

    public static @NotNull String generateUniqueRecipeSuffix() {
        return "_generated_" + generateRandomString(8, true, false);
    }

    public static @NotNull String generateRandomString(int len) {
        return generateRandomString(len, true, true);
    }

    public static @NotNull String generateRandomString(int len, boolean hasInteger, boolean hasUpperLetter) {
        String ch = "abcdefghijklmnopqrstuvwxyz" + (hasUpperLetter ? "ABCDEFGHIGKLMNOPQRSTUVWXYZ" : "")
                + (hasInteger ? "0123456789" : "");
        StringBuilder stringBuffer = new StringBuilder();
        for (int i = 0; i < len; i++) {
            Random random = new Random(System.nanoTime());
            int num = random.nextInt(ch.length() - 1);
            stringBuffer.append(ch.charAt(num));
        }
        return stringBuffer.toString();
    }

    public static int comparingIntReversed(int x, int y) {
        return Integer.compare(y, x);
    }

    public static boolean findCaller(String caller) {
        return STACK_WALKER.walk(
                it -> it.anyMatch(frame -> frame.getMethodName().equals(caller)));
    }

    public static <K, V> Collector<Map.Entry<K, V>, ?, Map<K, V>> toMapCollector() {
        return Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue);
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
}
