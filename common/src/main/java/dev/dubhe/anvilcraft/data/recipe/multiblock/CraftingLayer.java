package dev.dubhe.anvilcraft.data.recipe.multiblock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class CraftingLayer {

    public static final Codec<CraftingLayer> CODEC = Codec.STRING.listOf().comapFlatMap(
            CraftingLayer::read,
            (s) -> s.pattern
    ).stable();

    private final List<String> pattern;

    CraftingLayer(List<String> location) {
        if (location.size() != 3) throw new IllegalArgumentException("There are and can only be 3 rows");
        for (String s : location) {
            if (s.length() != 3) throw new IllegalArgumentException(
                    "Each row has and can only have 3 elements, consider filling empty spaces with ' '"
            );
        }
        pattern = location;
    }

    /**
     * 将方块pattern重映射
     */
    public <T, V> List<List<T>> map(Map<Character, V> map, Function<V, T> remapper) {
        List<List<V>> parResult = new ArrayList<>();
        for (String p : pattern) {
            List<V> l = new ArrayList<>();
            for (char c : p.toCharArray()) {
                V obj = map.get(c);
                if (obj == null)throw new RuntimeException(
                        "found no matching for pattern %c, candidates are: %s".formatted(
                                c,
                                Arrays.deepToString(map.keySet().toArray())
                        ));
                l.add(obj);
            }
            parResult.add(l);
        }
        List<List<T>> result = new ArrayList<>();
        for (List<V> vs : parResult) {
            List<T> ts = new ArrayList<>();
            for (V v : vs) {
                ts.add(remapper.apply(v));
            }
            result.add(ts);
        }
        return result;
    }


    static DataResult<CraftingLayer> read(List<String> location) {
        try {
            return DataResult.success(new CraftingLayer(location));
        } catch (Exception e) {
            return DataResult.error(() -> "Not a valid CraftingLayer: " + e.getMessage());
        }
    }

    public static CraftingLayer of(String... vararg) {
        return new CraftingLayer(Arrays.stream(vararg).toList());
    }
}
