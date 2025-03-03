package dev.dubhe.anvilcraft.api.integration;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Objects;

@Slf4j
public final class IntegrationInstance {
    private final String modid;
    private final String className;
    private Class<?> clazz;
    private Object instance;
    private MethodHandle constructor;
    private MethodHandle loader;
    private MethodHandle clientLoader;

    @SneakyThrows
    public IntegrationInstance(
        String modid,
        String className
    ) {
        this.modid = modid;
        this.className = className;
    }

    @SneakyThrows
    public void newInstance() {
        if (this.clazz == null) {
            this.clazz = Class.forName(className);
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            this.constructor = lookup.findConstructor(clazz, MethodType.methodType(void.class));
            MethodHandle loader;
            try {
                loader = lookup.findVirtual(clazz, "apply", MethodType.methodType(void.class));
            } catch (Throwable e) {
                loader = null;
            }
            this.loader = loader;
            try {
                loader = lookup.findVirtual(clazz, "applyClient", MethodType.methodType(void.class));
            } catch (Throwable e) {
                loader = null;
            }
            this.clientLoader = loader;
            if (this.loader == null && this.clientLoader == null) {
                log.warn("Integration {} does not declare any loader method.", className);
            }
        }
        if (instance == null) {
            instance = constructor.invoke();
        }
    }

    @SneakyThrows
    public void invoke() {
        if (loader != null) {
            loader.invoke(instance);
        }
    }

    @SneakyThrows
    public void invokeClient() {
        if (clientLoader != null) {
            clientLoader.invoke(instance);
        }
    }

    public String modid() {
        return modid;
    }

    public Object instance() {
        return instance;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (IntegrationInstance) obj;
        return Objects.equals(this.modid, that.modid)
            && Objects.equals(this.instance, that.instance)
            && Objects.equals(this.loader, that.loader)
            && Objects.equals(this.clientLoader, that.clientLoader);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modid, instance, loader, clientLoader);
    }

    @Override
    public String toString() {
        return "IntegrationInstance["
            + "modid=" + modid + ", "
            + "instance=" + instance + ", "
            + "loader=" + loader + ", "
            + "clientLoader=" + clientLoader
            + ']';
    }

}
