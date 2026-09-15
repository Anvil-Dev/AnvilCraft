package dev.dubhe.anvilcraft.api.number;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 表达式求解时的传入值，既可以按下标引用，也可以按名字引用。
 *
 * @param values 按下标引用的传入值，越界时取 0
 * @param named  按名字引用的传入值，未绑定时取 0
 */
public record NumberArguments(List<Double> values, Map<String, Double> named) {
    public NumberArguments {
        values = List.copyOf(values);
        named = Map.copyOf(named);
    }

    public static NumberArguments of(double... values) {
        List<Double> list = new ArrayList<>(values.length);
        for (double value : values) {
            list.add(value);
        }
        return new NumberArguments(list, Map.of());
    }

    public double value(int index) {
        return index >= 0 && index < this.values.size() ? this.values.get(index) : 0;
    }

    public double value(String name) {
        return this.named.getOrDefault(name, 0.0);
    }

    /**
     * 绑定一个具名传入值，用于 {@code $(name)} 引用。
     */
    public NumberArguments with(String name, double value) {
        Map<String, Double> extended = new LinkedHashMap<>(this.named);
        extended.put(name, value);
        return new NumberArguments(this.values, extended);
    }
}
