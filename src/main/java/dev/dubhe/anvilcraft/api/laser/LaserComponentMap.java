package dev.dubhe.anvilcraft.api.laser;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

public final class LaserComponentMap {
    private final Map<ILaserComponentType<?, ?>, ILaserComponent> components = new LinkedHashMap<>();
    @Nullable
    private List<ILaserComponent> orderedComponents;

    public <T extends ILaserComponent> void put(ILaserComponentType<T, ?> type, T instance) {
        components.put(type, instance);
        orderedComponents = null;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <T extends ILaserComponent> T get(ILaserComponentType<T, ?> type) {
        return (T) components.get(type);
    }

    public void putAll(LaserComponentMap other) {
        components.putAll(other.components);
        orderedComponents = null;
    }

    public List<ILaserComponent> allComponents() {
        if (orderedComponents != null) return orderedComponents;
        orderedComponents = components.entrySet().stream()
            .sorted(Comparator.comparingInt((Map.Entry<ILaserComponentType<?, ?>, ILaserComponent> entry) -> entry.getKey().priority())
                .reversed())
            .map(Map.Entry::getValue)
            .toList();
        return orderedComponents;
    }

    public void onEmitPre(ILaserComponentOwner owner) {
        for (ILaserComponent component : allComponents()) component.onEmitPre(owner);
    }

    public void onHitBlock(ILaserComponentOwner owner, Level level, BlockPos pos) {
        for (ILaserComponent component : allComponents()) {
            if (!component.onHitBlock(owner, level, pos)) break;
        }
    }

    /** 配置相同的组件保留本地运行状态，不继承上游的照射进度。 */
    public void replaceWith(LaserComponentMap replacement) {
        Map<ILaserComponentType<?, ?>, ILaserComponent> resolved = new LinkedHashMap<>();
        replacement.components.forEach((type, incoming) -> {
            ILaserComponent previous = components.get(type);
            resolved.put(type, previous != null && incoming.equals(previous) ? previous : incoming);
        });
        components.clear();
        components.putAll(resolved);
        orderedComponents = null;
    }

    public static LaserComponentMap mergeIncoming(List<LaserComponentMap> incoming) {
        LaserComponentMap result = new LaserComponentMap();
        for (LaserComponentMap source : incoming) {
            for (ILaserComponentType<?, ?> type : source.components.keySet()) {
                if (!result.components.containsKey(type)) mergeType(result, type, incoming);
            }
        }
        return result;
    }

    private static <T extends ILaserComponent> void mergeType(
        LaserComponentMap result, ILaserComponentType<T, ?> type, List<LaserComponentMap> incoming
    ) {
        List<T> instances = new ArrayList<>();
        for (LaserComponentMap source : incoming) {
            T instance = source.get(type);
            if (instance != null) instances.add(instance);
        }
        result.put(type, type.mergeIncoming(List.copyOf(instances)));
    }
}
