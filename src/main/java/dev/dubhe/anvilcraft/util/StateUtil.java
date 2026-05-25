package dev.dubhe.anvilcraft.util;

import dev.anvilcraft.lib.v2.util.Util;
import dev.dubhe.anvilcraft.item.AnvilHammerItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
        for (Property<?> property : state1.getProperties()) {
            E value1 = Util.cast(state1.getValue(property));
            E value2 = Util.cast(state2.getValue(property));
            // noinspection ConstantValue
            if (value1 == null || value2 == null || value1.compareTo(value2) != 0) {
                return false;
            }
        }
        return true;
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
