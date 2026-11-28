package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.api.hammer.IHammerChangeable;
import dev.dubhe.anvilcraft.block.state.FacingWithAxis;
import dev.dubhe.anvilcraft.block.state.Orientation;
import dev.dubhe.anvilcraft.block.utility.redstone.BlockComparatorBlock;
import dev.dubhe.anvilcraft.block.utility.redstone.MountedRedstone;
import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import dev.dubhe.anvilcraft.item.tool.AnvilHammerItem;
import net.minecraft.core.Direction;
import net.minecraft.core.FrontAndTop;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StateUtil {
    public static @Nullable Property<?> hammerPageProperty(BlockState state, Property<?> property) {
        if (property != BlockStateProperties.HORIZONTAL_FACING) {
            return null;
        }
        if (state.getBlock() instanceof MountedRedstone) {
            return MountedRedstone.ATTACHMENT;
        }
        if (state.hasProperty(BlockStateProperties.HALF)) {
            return BlockStateProperties.HALF;
        }
        var flip = state.getBlock().getStateDefinition().getProperty("upside_down");
        return flip instanceof BooleanProperty ? flip : null;
    }

    public static boolean hasHammerPages(BlockState state, Property<?> property) {
        return hammerPageProperty(state, property) != null || property == BlockComparatorBlock.FACING_WITH_AXIS
            || property.getValueClass() == Orientation.class || property.getValueClass() == FrontAndTop.class;
    }

    public static List<BlockState> hammerStates(BlockState initial, Property<?> property) {
        Property<?> page = hammerPageProperty(initial, property);
        if (page != null) {
            return horizontalPages(initial, page);
        }
        if (property == BlockComparatorBlock.FACING_WITH_AXIS) {
            List<BlockState> states = new ArrayList<>();
            for (Direction.Axis axis : Direction.Axis.values()) {
                Direction output = axis == Direction.Axis.Y ? Direction.NORTH : Direction.UP;
                for (int slot = 0; slot < 4; slot++) {
                    states.add(initial.setValue(BlockComparatorBlock.FACING_WITH_AXIS, FacingWithAxis.of(output.getOpposite(), axis)));
                    output = output.getClockWise(axis);
                }
            }
            return states;
        }
        if (property.getValueClass() == Orientation.class || property.getValueClass() == FrontAndTop.class) {
            return orientationStates(initial, property);
        }
        return findPossibleStatesForProperty(initial, property);
    }

    private static <T extends Comparable<T>> List<BlockState> horizontalPages(BlockState initial, Property<T> property) {
        List<T> pages = new ArrayList<>(property.getPossibleValues());
        if (property instanceof BooleanProperty) {
            pages.sort(Comparator.naturalOrder());
        } else if (property == BlockStateProperties.HALF) {
            pages.sort(Comparator.comparing(value -> !Half.BOTTOM.equals(value)));
        }
        List<BlockState> states = new ArrayList<>();
        for (T value : pages) {
            BlockState page = initial.setValue(property, value);
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                states.add(page.setValue(BlockStateProperties.HORIZONTAL_FACING, direction));
            }
        }
        return states;
    }

    private static <T extends Comparable<T>> List<BlockState> orientationStates(BlockState initial, Property<T> property) {
        List<T> values = new ArrayList<>(property.getPossibleValues());
        values.sort(Comparator.comparingInt(value -> {
            FrontAndTop orientation = value instanceof Orientation custom ? FrontAndTop.valueOf(custom.name()) : (FrontAndTop) value;
            Direction front = orientation.front();
            int page = front == Direction.UP ? 1 : front == Direction.DOWN ? 2 : 0;
            Direction horizontal = page == 0 ? front : orientation.top();
            return 4 * page + (horizontal.get2DDataValue() + 2) % 4;
        }));
        return values.stream().map(value -> initial.setValue(property, value)).toList();
    }

    private static <T extends Comparable<T>> BlockState copyValue(BlockState live, BlockState selected, Property<T> property) {
        return live.setValue(property, selected.getValue(property));
    }

    public static BlockState applyHammerProperties(BlockState live, BlockState selected) {
        Property<?> property = AnvilHammerItem.findModifyableProperty(live);
        if (property == null || !live.is(selected.getBlock())) {
            return selected;
        }
        BlockState result = copyValue(live, selected, property);
        Property<?> page = hammerPageProperty(live, property);
        return page == null ? result : copyValue(result, selected, page);
    }

    public static <O, T extends StateHolder<O, T>, E extends Comparable<E>> List<T> findPossibleStatesForProperty(
        T initialState,
        Property<E> property
    ) {
        List<T> result = new ArrayList<>();
        T currentIterating = initialState;
        while (!result.contains(currentIterating)) {
            result.add(currentIterating);
            currentIterating = currentIterating.cycle(property);
        }
        result.sort(Comparator.<T, E>comparing(it -> it.getValue(property)).reversed());
        return result;
    }

    public static <O, T extends StateHolder<O, T>> boolean equalsState(T state1, T state2) {
        Set<Property<?>> properties = new HashSet<>();
        properties.addAll(state1.getProperties());
        properties.addAll(state2.getProperties());
        for (Property<?> property : properties) {
            if (!StateUtil.equalsProperty(property, state1, state2)) {
                return false;
            }
        }
        return true;
    }

    public static <O, T extends StateHolder<O, T>, E extends Comparable<E>> boolean equalsProperty(
        Property<E> property,
        T state1,
        T state2
    ) {
        if (state1.hasProperty(property) != state2.hasProperty(property)) {
            return false;
        }
        E value1 = state1.getValue(property);
        E value2 = state2.getValue(property);
        return value1.compareTo(value2) == 0;
    }

    public static boolean verifyPossibleStatesForProperty(BlockState initialState, BlockState targetState) {
        Property<?> property = AnvilHammerItem.findModifyableProperty(initialState);
        if (property == null || initialState.is(ModBlockTags.ANVIL_HAMMER_DENYLIST) || !initialState.is(targetState.getBlock())
            || initialState.getBlock() instanceof IHammerChangeable changeable && !changeable.checkBlockState(initialState)) {
            return false;
        }
        return hammerStates(initialState, property).contains(targetState);
    }
}
