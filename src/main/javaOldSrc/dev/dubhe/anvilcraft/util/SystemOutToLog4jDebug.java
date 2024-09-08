package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.AnvilCraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jetbrains.annotations.Nullable;

import java.io.OutputStream;
import java.io.PrintStream;

public class SystemOutToLog4jDebug extends PrintStream {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnvilCraft.class);
    protected final String name;

    public SystemOutToLog4jDebug(String name, OutputStream out) {
        super(out);
        this.name = name;
    }

    @Override
    public void println(@Nullable String string) {
        this.logLine(string);
    }

    @Override
    public void println(Object object) {
        this.logLine(String.valueOf(object));
    }

    protected void logLine(String string) {
        LOGGER.debug("[{}]: {}", this.name, string);
    }

    /**
     * 将标准输出重定向至log4j2
     */
    public static void redirectStdoutToLog4j() {
        if (System.getProperty("anvilcraft.dev") != null) return;
        System.setOut(new SystemOutToLog4jDebug("STDOUT", System.out));
        System.setErr(new SystemOutToLog4jDebug("STDERR", System.err));
    }
}
