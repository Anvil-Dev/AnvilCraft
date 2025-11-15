// from embeddium project
package dev.dubhe.anvilcraft.api.rendering.util.thirdparty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;

public class NvidiaWorkarounds {
    private static final Logger LOGGER = LoggerFactory.getLogger("AnvilCraft");

    public static void install() {
        LOGGER.warn("Applying workaround: Prevent the NVIDIA OpenGL driver from using broken optimizations (NVIDIA_THREADED_OPTIMIZATIONS)");

        try {
            String s = System.getProperty("os.name").toLowerCase(Locale.ROOT);
            if (s.contains("win")) {
                WindowsCommandLine.setCommandLine("net.caffeinemc.sodium");
                Kernel32.setEnvironmentVariable("SHIM_MCCOMPAT", "0x800000001");
            } else {
                if (!s.contains("linux") && !s.contains("unix")) return;
                Libc.setEnvironmentVariable("__GL_THREADED_OPTIMIZATIONS", "0");
            }
        } catch (Throwable t) {
        }

    }

    public static void uninstall() {
        String s = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        if (s.contains("win")) {
            WindowsCommandLine.setCommandLine("net.caffeinemc.sodium");
        }
    }
}
