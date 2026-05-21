package dev.dubhe.anvilcraft.client.init;

import com.mojang.blaze3d.platform.InputConstants.Type;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.util.Lazy;
import org.lwjgl.glfw.GLFW;

public class ModKeyMappings {
    public static final Lazy<KeyMapping> SWITCH_PHASE = register(
        "switch_phase",
        KeyConflictContext.IN_GAME,
        Type.KEYSYM,
        GLFW.GLFW_KEY_X
    );
    public static final Lazy<KeyMapping> TOGGLE_GOGGLE = register(
        "toggle_goggle",
        KeyConflictContext.IN_GAME,
        Type.KEYSYM,
        GLFW.GLFW_KEY_UNKNOWN
    );
    public static final Lazy<KeyMapping> SWITCH_TOOL_MODE = register(
        "switch_tool_mode",
        KeyConflictContext.IN_GAME,
        Type.KEYSYM,
        GLFW.GLFW_KEY_LEFT_ALT
    );
    public static final Lazy<KeyMapping> USE_PILL_BOX = register(
        "use_pill_box",
        KeyConflictContext.IN_GAME,
        Type.KEYSYM,
        GLFW.GLFW_KEY_V
    );
    public static final Lazy<KeyMapping> THOUGHT = register(
        "thought",
        KeyConflictContext.GUI,
        Type.KEYSYM,
        GLFW.GLFW_KEY_W
    );

    @SuppressWarnings("SameParameterValue")
    private static Lazy<KeyMapping> register(String name, KeyConflictContext context, Type type, int key) {
        return Lazy.of(() -> new KeyMapping("key.anvilcraft." + name, context, type, key, "key.categories.anvilcraft"));
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(SWITCH_PHASE.get());
        event.register(TOGGLE_GOGGLE.get());
        event.register(SWITCH_TOOL_MODE.get());
        event.register(USE_PILL_BOX.get());
    }
}
