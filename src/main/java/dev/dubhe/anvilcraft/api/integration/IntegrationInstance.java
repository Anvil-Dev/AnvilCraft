package dev.dubhe.anvilcraft.api.integration;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.neoforged.fml.loading.moddiscovery.ModInfo;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.Objects;

@Slf4j
public final class IntegrationInstance {
    private final String modid;
    private final ModVersionRange versionRange;
    private final String className;
    private final List<IntegrationType> type;
    private Class<?> clazz;
    private Object instance;
    private MethodHandle constructor;
    private MethodHandle loader;
    private MethodHandle clientLoader;
    private MethodHandle dataLoader;

    @SneakyThrows
    public IntegrationInstance(
        String modid,
        ModVersionRange versionRange,
        String className,
        List<IntegrationType> type
    ) {
        this.modid = modid;
        this.versionRange = versionRange;
        this.className = className;
        this.type = type;
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
            try {
                loader = lookup.findVirtual(clazz, "applyData", MethodType.methodType(void.class));
            } catch (Throwable e) {
                loader = null;
            }
            this.dataLoader = loader;
            if (this.loader == null && this.clientLoader == null && this.dataLoader == null) {
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

    @SneakyThrows
    public void invokeData() {
        if (dataLoader != null) {
            dataLoader.invoke(instance);
        }
    }

    public boolean containsType(IntegrationType type) {
        return this.type.contains(type);
    }

    public boolean is(@NotNull ModInfo modInfo) {
        return modid.equals(modInfo.getModId()) && versionRange.containsVersion(modInfo.getVersion());
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
        return Objects.hash(modid, versionRange, className, instance, loader, clientLoader, dataLoader);
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
