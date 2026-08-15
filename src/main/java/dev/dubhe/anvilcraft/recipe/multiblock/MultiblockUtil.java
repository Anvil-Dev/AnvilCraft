package dev.dubhe.anvilcraft.recipe.multiblock;

import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.DefinitionSerialization;
import dev.anvilcraft.lib.v2.multiblock.dynamic.definition.MultiblockDefinition;
import dev.anvilcraft.lib.v2.util.predicate.BlockStatePredicate;
import dev.dubhe.anvilcraft.api.block.BlockPlacementRules;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.common.util.ItemStackMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MultiblockUtil {
    private static final Rotation[] ROTATIONS = {
        Rotation.NONE,
        Rotation.CLOCKWISE_90,
        Rotation.CLOCKWISE_180,
        Rotation.COUNTERCLOCKWISE_90
    };

    /**
     * 尝试用给定定义匹配输入立方体区域。定义转换后的网格尺寸必须与输入区域完全一致
     * （层数、每层行数、每行列数均等于 input.size()），结构恰好填满输入立方体（即超算
     * 正下方），并依次尝试四种水平旋转。网格中的空格位置必须为空气。
     *
     * @param def   多方块定义
     * @param input 输入区域
     * @param level 维度，用于匹配 NBT 谓词
     * @return 匹配成功的旋转
     */
    public static Optional<Rotation> match(MultiblockDefinition def, MultiblockInput input, Level level) {
        int size = input.size();
        DefinitionSerialization serialization = DefinitionSerialization.fromDefinition(def);
        String[][] grid = serialization.grid();
        if (MultiblockUtil.sizeMismatch(grid, size)) {
            return Optional.empty();
        }
        BlockPos inputCorner = input.centerPos().offset(-size / 2, -size, -size / 2);
        int[] offsets = MultiblockUtil.offsets(size, grid);
        for (Rotation rotation : ROTATIONS) {
            if (MultiblockUtil.matchesGrid(grid, serialization, input, size, rotation, offsets, level, inputCorner)) {
                return Optional.of(rotation);
            }
        }
        return Optional.empty();
    }

    private static boolean sizeMismatch(String[][] grid, int size) {
        if (grid.length != size) {
            return true;
        }
        for (String[] layer : grid) {
            if (layer.length != size) {
                return true;
            }
            for (String line : layer) {
                if (line.length() != size) {
                    return true;
                }
            }
        }
        return false;
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
        if (MultiblockUtil.sizeMismatch(grid, size)) {
            return drops;
        }
        boolean server = level instanceof ServerLevel;
        int[] offsets = MultiblockUtil.offsets(size, grid);
        for (int gy = 0; gy < grid.length; gy++) {
            String[] layer = grid[gy];
            for (int gz = 0; gz < layer.length; gz++) {
                String line = layer[gz];
                for (int gx = 0; gx < line.length(); gx++) {
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
     * 匹配要求网格尺寸与输入区域完全一致，因此偏移恒为零。
     */
    static int[] offsets(int size, String[][] grid) {
        int sizeY = grid.length;
        int sizeZ = sizeY == 0 ? 0 : grid[0].length;
        int sizeX = sizeZ == 0 ? 0 : grid[0][0].length();
        return new int[]{(size - sizeX) / 2, size - sizeY, (size - sizeZ) / 2};
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
        Rotation rotation,
        int[] offsets,
        Level level,
        BlockPos inputCorner
    ) {
        for (int gy = 0; gy < grid.length; gy++) {
            String[] layer = grid[gy];
            for (int gz = 0; gz < layer.length; gz++) {
                String line = layer[gz];
                for (int gx = 0; gx < line.length(); gx++) {
                    char symbol = line.charAt(gx);
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
                        if (predicate == null
                            || !MultiblockUtil.testPredicate(predicate, level, inputCorner, state, world, rotation)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private static boolean testPredicate(
        BlockStatePredicate predicate,
        Level level,
        BlockPos inputCorner,
        BlockState state,
        BlockPos world,
        Rotation rotation
    ) {
        @SuppressWarnings("deprecation")
        BlockState rotated = state.rotate(MultiblockUtil.reverseRotation(rotation));
        if (!predicate.requiresBlockEntity()) {
            return predicate.testWithoutEntity(rotated);
        }
        BlockEntity entity = level.getBlockEntity(inputCorner.offset(world));
        return predicate.test(level, rotated, entity);
    }

    /**
     * 获取 {@link BlockStatePredicate} 对应的默认方块状态：第一个方块（或标签中的第一个方块）的默认状态，
     * 并应用第一组属性匹配器中的精确属性。
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static BlockState getDefaultState(BlockStatePredicate predicate) {
        Block block = predicate.getBlocks().stream()
            .findFirst()
            .map(Holder::value)
            .orElse(Blocks.AIR);
        BlockState state = block.defaultBlockState();
        if (predicate.getProperties().isEmpty()) {
            return state;
        }
        for (BlockStatePredicate.PropertyMatcher matcher : predicate.getProperties().getFirst()) {
            if (!(matcher.valueMatcher() instanceof BlockStatePredicate.ExactMatcher(String value))) {
                continue;
            }
            Property property = block.getStateDefinition().getProperty(matcher.name());
            if (property == null) continue;
            Optional optional = property.getValue(value);
            if (optional.isPresent()) {
                state = state.setValue(property, (Comparable) optional.get());
            }
        }
        return state;
    }

    /**
     * 将 {@link MultiblockDefinition} 转换为所需材料清单，供 JEI 展示使用。
     */
    public static List<ItemStack> ingredientList(
        MultiblockDefinition definition,
        @Nullable HolderLookup.Provider registries
    ) {
        Object2IntMap<BlockState> states = new Object2IntOpenHashMap<>();
        for (Map.Entry<Vec3i, BlockStatePredicate> entry : definition.definition().entrySet()) {
            BlockStatePredicate predicate = entry.getValue();
            if (predicate.getBlocks() instanceof HolderSet.Named) {
                continue;
            }
            states.mergeInt(MultiblockUtil.getDefaultState(predicate), 1, Integer::sum);
        }
        Map<ItemStack, Integer> ingredients = ItemStackMap.createTypeAndTagMap();
        states.forEach((state, stateCount) -> BlockPlacementRules.getPlacementIngredients(registries, state)
            .forEach(stack -> {
                int stackCount = stack.getCount();
                if (stackCount <= 0) return;
                stack.setCount(1);
                Integer totalCount = ingredients.computeIfAbsent(stack, ignore -> 0);
                ingredients.put(stack, totalCount + stateCount * stackCount);
            }));
        List<ItemStack> resultList = new ArrayList<>();
        ingredients.forEach((stack, count) -> {
            stack.setCount(count);
            resultList.add(stack);
        });
        return resultList;
    }

    /**
     * 获取 {@link MultiblockDefinition} 中标签谓词的计数映射。
     *
     * @return TagKey 到在模式中出现的次数的映射
     */
    @SuppressWarnings("unchecked")
    public static Map<TagKey<Block>, Integer> tagIngredientCounts(MultiblockDefinition definition) {
        Object2IntMap<TagKey<Block>> tagCounts = new Object2IntOpenHashMap<>();
        for (Map.Entry<Vec3i, BlockStatePredicate> entry : definition.definition().entrySet()) {
            if (entry.getValue().getBlocks() instanceof HolderSet.Named<?> named) {
                tagCounts.mergeInt((TagKey<Block>) named.key(), 1, Integer::sum);
            }
        }
        return tagCounts;
    }

    /**
     * 获取 {@link MultiblockDefinition} 对应的网格尺寸（层数）。
     */
    public static int size(MultiblockDefinition definition) {
        return DefinitionSerialization.fromDefinition(definition).grid().length;
    }
}
