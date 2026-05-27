package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StateUtil {
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

    public static <O, T extends StateHolder<O, T>, E extends Comparable<E>> boolean equalsState(T state1, T state2) {
        Set<Property<?>> properties = new HashSet<>();
        properties.addAll(state1.getProperties());
        properties.addAll(state2.getProperties());
        for (Property<?> property : properties) {
            if (!StateUtil.equalsProperty(property, state1, state2)) return false;
        }
        return true;
    }

    public static <O, T extends StateHolder<O, T>, E extends Comparable<E>> boolean equalsProperty(
        Property<E> property,
        T state1,
        T state2
    ) {
        if (state1.hasProperty(property) != state2.hasProperty(property)) return false;
        E value1 = state1.getValue(property);
        E value2 = state2.getValue(property);
        return value1.compareTo(value2) == 0;
    }

    public static boolean verifyPossibleStatesForProperty(BlockState initialState, BlockState targetState) {
        Property<?> property = AnvilHammerItem.findModifyableProperty(initialState);
        if (property == null || !initialState.is(targetState.getBlock())) return false;
        List<BlockState> stateList = StateUtil.findPossibleStatesForProperty(initialState, property);
        for (BlockState state : stateList) {
            if (StateUtil.equalsState(state, targetState)) {
                return true;
            }
        }
        return false;
    }
}
