package dev.dubhe.anvilcraft.data.recipe.anvil.predicate.multiblock;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
     * 将方块pattern映射
     */
    public <T> List<List<T>> map(Map<Character, T> map) {
        List<List<T>> parResult = new ArrayList<>();
        for (String p : pattern) {
            List<T> l = new ArrayList<>();
            for (char c : p.toCharArray()) {
                T obj = map.get(c);
                if (obj == null) throw new RuntimeException(
                        "found no matching for pattern %c, candidates are: %s".formatted(
                                c,
                                Arrays.deepToString(map.keySet().toArray())
                        ));
                l.add(obj);
            }
            parResult.add(l);
        }
        return parResult;
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
