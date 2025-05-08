package dev.dubhe.anvilcraft.api.behavior;

import java.util.HashMap;
import java.util.Map;

public class ExecutionContext<T> {
    private final Map<String, Object> attachment = new HashMap<>();
    private final T context;

    public ExecutionContext(T context) {
        this.context = context;
    }


    public void putAttachment(String key, Object item) {
        attachment.put(key, item);
    }

    public <T1> T1 getAttachment(String key, Class<T1> ty) {
        Object item = attachment.get(key);
        if (ty.isInstance(item)) {
            return ty.cast(item);
        }
        throw new IllegalArgumentException(
            "Expected type %s for attachment %s but found %s".formatted(
                ty.getName(),
                key,
                item.getClass().getName()
            )
        );
    }

    public boolean has(String key) {
        return attachment.containsKey(key);
    }

    public T unwrap() {
        return context;
    }
}
