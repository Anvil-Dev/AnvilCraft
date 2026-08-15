package dev.dubhe.anvilcraft.recipe.multiblock;

import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.DefinitionSerialization;
import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.MultiblockDefinition;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MultiblockUtil {
    private static final Rotation[] ROTATIONS = {
        Rotation.NONE,
        Rotation.CLOCKWISE_90,
        Rotation.CLOCKWISE_180,
        Rotation.COUNTERCLOCKWISE_90
    };

    /**
     * 尝试用给定定义匹配输入立方体区域。定义转换后的网格尺寸必须不超过输入区域，
     * 结构以输入区域顶部中心对齐（结构顶部紧贴输入区域顶部，即超算正下方），
     * 并依次尝试四种水平旋转。网格未定义的位置必须为空气。
     *
     * @param def   多方块定义
     * @param input 输入区域
     * @return 匹配成功的旋转
     */
    public static Optional<Rotation> match(MultiblockDefinition def, MultiblockInput input) {
        int size = input.size();
        DefinitionSerialization serialization = DefinitionSerialization.fromDefinition(def);
        String[][] grid = serialization.grid();
        int gridSize = grid.length;
        if (gridSize > size) {
            return Optional.empty();
        }
        int[] offsets = MultiblockUtil.offsets(size, gridSize);
        for (Rotation rotation : ROTATIONS) {
            if (MultiblockUtil.matchesGrid(grid, serialization, input, size, gridSize, rotation, offsets)) {
                return Optional.of(rotation);
            }
        }
        return Optional.empty();
    }

    /**
     * 消耗（清除）定义覆盖的方块，使其消失。放置偏移与 {@link #match} 一致。
     *
     * @param level       维度
     * @param def         多方块定义
     * @param input       输入区域
     * @param inputCorner 输入区域的世界坐标角点
     * @param rotation    匹配到的旋转
     * @return 被消耗方块在服务端产生的掉落物（客户端返回空列表）
     */
    public static List<ItemStack> consume(
        Level level,
        MultiblockDefinition def,
        MultiblockInput input,
        BlockPos inputCorner,
        Rotation rotation
    ) {
        List<ItemStack> drops = new ArrayList<>();
        int size = input.size();
        DefinitionSerialization serialization = DefinitionSerialization.fromDefinition(def);
        String[][] grid = serialization.grid();
        int gridSize = grid.length;
        if (gridSize > size) {
            return drops;
        }
        boolean server = level instanceof ServerLevel;
        int[] offsets = MultiblockUtil.offsets(size, gridSize);
        for (int gy = 0; gy < gridSize; gy++) {
            for (int gz = 0; gz < gridSize; gz++) {
                String line = grid[gy][gz];
                for (int gx = 0; gx < gridSize; gx++) {
                    if (line.charAt(gx) == ' ') {
                        continue;
                    }
                    int x = gx + offsets[0];
                    int y = gy + offsets[1];
                    int z = gz + offsets[2];
                    BlockPos world = MultiblockUtil.rotatePatternToWorld(x, y, z, rotation, size);
                    BlockPos absolute = inputCorner.offset(world);
                    BlockState state = level.getBlockState(absolute);
                    if (server) {
                        ServerLevel serverLevel = (ServerLevel) level;
                        if (!state.isAir()) {
                            drops.addAll(Block.getDrops(state, serverLevel, absolute, serverLevel.getBlockEntity(absolute)));
                        }
                    }
                    level.setBlockAndUpdate(absolute, Blocks.AIR.defaultBlockState());
                }
            }
        }
        return drops;
    }

    /**
     * 计算网格结构在输入区域内的放置偏移：{offsetX, offsetY, offsetZ}。
     * 水平居中，垂直贴顶部（结构顶部位于输入区域顶部）。
     */
    private static int[] offsets(int size, int gridSize) {
        int horizontal = (size - gridSize) / 2;
        return new int[]{horizontal, size - gridSize, horizontal};
    }

    /**
     * 将 pattern 网格坐标 (x, y, z) 转换为世界坐标。与 {@code rotatePos} 方向约定一致。
     */
    public static BlockPos rotatePatternToWorld(int x, int y, int z, Rotation rotation, int size) {
        return switch (rotation) {
            case CLOCKWISE_90 -> new BlockPos(size - 1 - z, y, x);
            case CLOCKWISE_180 -> new BlockPos(size - 1 - x, y, size - 1 - z);
            case COUNTERCLOCKWISE_90 -> new BlockPos(z, y, size - 1 - x);
            default -> new BlockPos(x, y, z);
        };
    }

    /**
     * 获取方块需要的反向旋转
     */
    public static Rotation reverseRotation(Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90;
            case COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90;
            default -> rotation;
        };
    }

    private static boolean matchesGrid(
        String[][] grid,
        DefinitionSerialization serialization,
        MultiblockInput input,
        int size,
        int gridSize,
        Rotation rotation,
        int[] offsets
    ) {
        for (int gy = 0; gy < gridSize; gy++) {
            for (int gz = 0; gz < gridSize; gz++) {
                for (int gx = 0; gx < gridSize; gx++) {
                    char symbol = grid[gy][gz].charAt(gx);
                    int x = gx + offsets[0];
                    int y = gy + offsets[1];
                    int z = gz + offsets[2];
                    BlockPos world = MultiblockUtil.rotatePatternToWorld(x, y, z, rotation, size);
                    BlockState state = input.getBlockState(world.getX(), world.getY(), world.getZ());
                    if (symbol == ' ') {
                        if (!state.isAir()) {
                            return false;
                        }
                    } else {
                        BlockStatePredicate predicate = serialization.mapping().get(symbol);
                        // noinspection deprecation
                        if (predicate == null || !predicate.testWithoutEntity(state.rotate(MultiblockUtil.reverseRotation(rotation)))) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * 将 {@link MultiblockDefinition} 转换为可用于 JEI 渲染的 {@link BlockPattern}。
     */
    public static BlockPattern toBlockPattern(MultiblockDefinition definition) {
        BlockPattern pattern = BlockPattern.create();
        DefinitionSerialization serialization = DefinitionSerialization.fromDefinition(definition);
        for (String[] layer : serialization.grid()) {
            pattern.layer(layer);
        }
        serialization.mapping().forEach((symbol, predicate) ->
            pattern.symbol(symbol, MultiblockUtil.toBlockPredicate(predicate)));
        return pattern;
    }

    /**
     * 将 {@link BlockStatePredicate} 转换为 {@link BlockPredicateWithState}，供渲染与数据生成使用。
     * 多方块与 NBT 谓词会被简化：只保留第一个方块或标签，以及第一组属性。
     */
    @SuppressWarnings("unchecked")
    public static BlockPredicateWithState toBlockPredicate(BlockStatePredicate predicate) {
        if (predicate.getBlocks() instanceof HolderSet.Named<?> named) {
            return BlockPredicateWithState.of((TagKey<Block>) named.key());
        }
        Block block = predicate.getBlocks().stream()
            .findFirst()
            .map(Holder::value)
            .orElse(Blocks.AIR);
        BlockPredicateWithState result = BlockPredicateWithState.of(block);
        if (!predicate.getProperties().isEmpty()) {
            for (BlockStatePredicate.PropertyMatcher matcher : predicate.getProperties().getFirst()) {
                if (matcher.valueMatcher() instanceof BlockStatePredicate.ExactMatcher(String value)) {
                    result.hasState(matcher.name(), value);
                }
            }
        }
        return result;
    }

    /**
     * 将 {@link BlockPredicateWithState} 转换为 {@link BlockStatePredicate.Builder}。
     */
    public static BlockStatePredicate.Builder toBlockStatePredicateBuilder(BlockPredicateWithState predicate) {
        BlockStatePredicate.Builder builder = BlockStatePredicate.builder();
        if (predicate.getTag() != null) {
            builder.of(predicate.getTag());
        } else if (predicate.getBlock() != null) {
            builder.of(predicate.getBlock());
        }
        predicate.getProperties().forEach((property, value) ->
            builder.with(property, BlockPredicateWithState.getNameOf(value)));
        return builder;
    }
}
