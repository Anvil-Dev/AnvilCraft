package dev.dubhe.anvilcraft.integration.create;

import java.io.InputStream;

public class CreateIntegration {
    /**
     * 應用機械動力集成
     */
    public static void applyIfPossible() {
        if (isClassPresent("com.simibubi.create.content.fluids.tank.BoilerHeaters")) {
            apply("dev.dubhe.anvilcraft.integration.create.BoilerIntegration");
        }
    }

    private static void apply(String name) {
        try {
            Class<Integration> integrationClass = (Class<Integration>) Class.forName(name);
            Integration integration = integrationClass.getDeclaredConstructor().newInstance();
            integration.apply();
        } catch (Exception ex) {
            throw new RuntimeException("Integration apply failed", ex);
        }
    }

    private static boolean isClassPresent(String className) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            InputStream is = classLoader.getResourceAsStream(className.replace(".", "/"));
            if (is != null) {
                is.close();
            }
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
