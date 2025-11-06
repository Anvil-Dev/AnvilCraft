// from embeddium project
package dev.dubhe.anvilcraft.api.rendering.util.thirdparty;

import java.nio.ByteBuffer;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.APIUtil;
import org.lwjgl.system.JNI;
import org.lwjgl.system.Library;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.SharedLibrary;

public class Libc {
    private static final SharedLibrary LIBRARY = Library.loadNative("me.jellyquid.mods.sodium", "libc.so.6");
    private static final long PFN_setenv;

    public static void setEnvironmentVariable(String name, @Nullable String value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer nameBuf = stack.UTF8(name);
            ByteBuffer valueBuf = value != null ? stack.UTF8(value) : null;
            JNI.callPPI(MemoryUtil.memAddress(nameBuf), MemoryUtil.memAddressSafe(valueBuf), 1, PFN_setenv);
        }

    }

    static {
        PFN_setenv = APIUtil.apiGetFunctionAddress(LIBRARY, "setenv");
    }
}
