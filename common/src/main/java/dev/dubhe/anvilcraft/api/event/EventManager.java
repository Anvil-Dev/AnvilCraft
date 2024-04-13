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
    public final Map<Class<?>, List<Consumer<?>>> eventListener = new ConcurrentHashMap<>();

    /**
     * 侦听事件
     *
     * @param event   事件
     * @param trigger 处理器
     * @param <E>     事件
     */
    @SuppressWarnings("unused")
    public <E> void listen(Class<E> event, Consumer<E> trigger) {
        List<Consumer<?>> triggers = eventListener.getOrDefault(event, new Vector<>());
        triggers.add(trigger);
        eventListener.putIfAbsent(event, triggers);
    }

    @SuppressWarnings("all")
    public <E> void post(@NotNull E event) {
        List<Consumer<?>> triggers = new ArrayList<>();
        eventListener.entrySet().stream().filter((k) -> event.getClass().isAssignableFrom(k.getKey())).map(Map.Entry::getValue).forEach(triggers::addAll);
        for (Consumer<?> trigger : triggers) {
            ((Consumer<E>) trigger).accept(event);
        }
    }

    /**
     * 注册侦听器
     *
     * @param object 侦听器类
     */
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
            List<Consumer<?>> triggers = eventListener.getOrDefault(event, new Vector<>());
            triggers.add(trigger);
            eventListener.putIfAbsent(event, triggers);
        }
    }
}
