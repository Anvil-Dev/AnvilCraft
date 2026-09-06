package dev.dubhe.anvilcraft.integration.emi;

import com.mojang.logging.LogUtils;
import dev.dubhe.anvilcraft.client.gui.screen.StorageScreen;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * EMI 对仓储界面的适配。
 *
 * <p>EMI 通过 {@code EmiScreenBase} 推断当前容器界面的布局边界：对 {@code HandledScreen}
 * 会退化为使用容器的 {@code leftPos/topPos/背景宽高}。仓储界面（300x222 居中绘制）的
 * leftPos 很大，EMI 会把侧栏/面板按这个偏大的矩形计算，导致侧栏出现在屏幕中部而非
 * 屏幕边缘（见 #4720）。这里在 EMI 存在时为其注册全屏边界，使侧栏按整个屏幕布局。</p>
 *
 * <p>AnvilCraft 不直接依赖 EMI，通过反射调用其 1.1.23+ 的实验性 API
 * {@code EmiScreenBase.addScreenBoundsProvider}。</p>
 */
public final class StorageEmiScreenBoundsRegistrar {
    private static final Logger LOGGER = LogUtils.getLogger();

    private StorageEmiScreenBoundsRegistrar() {
    }

    /**
     * 若 EMI 已加载，为 {@link StorageScreen} 注册全屏边界提供者。
     * 应在客户端初始化阶段调用一次。
     */
    public static void registerIfEmiPresent() {
        if (!ModList.get().isLoaded("emi")) return;
        try {
            Class<?> screenBaseClass = Class.forName("dev.emi.emi.screen.EmiScreenBase");
            Class<?> providerClass = Class.forName("dev.emi.emi.api.EmiScreenBoundsProvider");
            Class<?> boundsClass = Class.forName("dev.emi.emi.api.widget.Bounds");
            java.lang.reflect.Constructor<?> boundsCtor = boundsClass.getConstructor(
                int.class, int.class, int.class, int.class
            );
            Method addProvider = screenBaseClass.getMethod(
                "addScreenBoundsProvider", Class.class, providerClass
            );

            InvocationHandler handler = (proxy, method, args) -> {
                if (method.getName().equals("getBounds") && args.length == 1
                    && args[0] instanceof Screen screen) {
                    // 全屏边界：让 EMI 的面板/侧栏按屏幕边缘布局
                    return boundsCtor.newInstance(0, 0, screen.width, screen.height);
                }
                // hashCode/equals/toString 等 Object 方法
                if (method.getDeclaringClass() == Object.class) {
                    return switch (method.getName()) {
                        case "toString" -> "StorageScreenEmiBoundsProvider";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> null;
                    };
                }
                return null;
            };
            Object provider = Proxy.newProxyInstance(
                StorageEmiScreenBoundsRegistrar.class.getClassLoader(),
                new Class<?>[]{providerClass},
                handler
            );
            addProvider.invoke(null, StorageScreen.class, provider);
            LOGGER.debug("Registered full-screen bounds provider for StorageScreen with EMI");
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.warn("Failed to register EMI screen bounds provider for StorageScreen", e);
        }
    }
}
