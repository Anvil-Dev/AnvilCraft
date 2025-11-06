// from embeddium project
package dev.dubhe.anvilcraft.api.rendering.util.thirdparty;

import org.lwjgl.system.MemoryUtil;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.Objects;

public class WindowsCommandLine {
    private static CommandLineHook ACTIVE_COMMAND_LINE_HOOK;

    public static void setCommandLine(String modifiedCmdline) {
        if (ACTIVE_COMMAND_LINE_HOOK != null) {
            throw new IllegalStateException("Command line is already modified");
        } else {
            long pCmdline = Kernel32.getCommandLine();
            String cmdline = MemoryUtil.memUTF16(pCmdline);
            int cmdlineLen = MemoryUtil.memLengthUTF16(cmdline, true);
            if (MemoryUtil.memLengthUTF16(modifiedCmdline, true) > cmdlineLen) {
                throw new BufferOverflowException();
            } else {
                ByteBuffer buffer = MemoryUtil.memByteBuffer(pCmdline, cmdlineLen);
                MemoryUtil.memUTF16(modifiedCmdline, true, buffer);
                if (!Objects.equals(modifiedCmdline, MemoryUtil.memUTF16(pCmdline))) {
                    throw new RuntimeException("Sanity check failed, the command line arguments did not appear to change");
                } else {
                    ACTIVE_COMMAND_LINE_HOOK = new CommandLineHook(cmdline, buffer);
                }
            }
        }
    }

    public static void resetCommandLine() {
        if (ACTIVE_COMMAND_LINE_HOOK != null) {
            ACTIVE_COMMAND_LINE_HOOK.uninstall();
            ACTIVE_COMMAND_LINE_HOOK = null;
        }

    }

    private static class CommandLineHook {
        private final String cmdline;
        private final ByteBuffer cmdlineBuf;
        private boolean active = true;

        private CommandLineHook(String cmdline, ByteBuffer cmdlineBuf) {
            this.cmdline = cmdline;
            this.cmdlineBuf = cmdlineBuf;
        }

        public void uninstall() {
            if (!this.active) {
                throw new IllegalStateException("Hook was already uninstalled");
            } else {
                MemoryUtil.memUTF16(this.cmdline, true, this.cmdlineBuf);
                this.active = false;
            }
        }
    }
}
