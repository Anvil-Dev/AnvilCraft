package dev.dubhe.anvilcraft.api.event;

import dev.dubhe.anvilcraft.AnvilCraft;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class EventManager {
    public final Map<Class<?>, List<Consumer<?>>> EVENT_LISTENER = new ConcurrentHashMap<>();

    @SuppressWarnings("unused")
    public <E> void listen(Class<E> event, Consumer<E> trigger) {
        List<Consumer<?>> triggers = EVENT_LISTENER.getOrDefault(event, new Vector<>());
        triggers.add(trigger);
        EVENT_LISTENER.putIfAbsent(event, triggers);
    }

    @SuppressWarnings("all")
    public <E> void post(@NotNull E event) {
        List<Consumer<?>> triggers = new ArrayList<>();
        EVENT_LISTENER.entrySet().stream().filter((k) -> event.getClass().isAssignableFrom(k.getKey())).map(Map.Entry::getValue).forEach(triggers::addAll);
        for (Consumer<?> trigger : triggers) {
            ((Consumer<E>) trigger).accept(event);
        }
    }

    public void register(@NotNull Object object) {
        for (Method method : object.getClass().getMethods()) {
            if (method.getParameterCount() != 1) continue;
            SubscribeEvent annotation = method.getAnnotation(SubscribeEvent.class);
            if (null == annotation) continue;
            Class<?> event = method.getParameterTypes()[0];
            Consumer<?> trigger = (obj) -> {
                try {
                    method.invoke(object, obj);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    AnvilCraft.LOGGER.error(e.getMessage(), e);
                }
            };
            List<Consumer<?>> triggers = EVENT_LISTENER.getOrDefault(event, new Vector<>());
            triggers.add(trigger);
            EVENT_LISTENER.putIfAbsent(event, triggers);
        }
    }
}
