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

    @SuppressWarnings("SameParameterValue")
    private static Lazy<KeyMapping> register(String name, KeyConflictContext context, Type type, int key) {
        return Lazy.of(() -> new KeyMapping("key.anvilcraft." + name, context, type, key, "key.categories.anvilcraft"));
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(SWITCH_PHASE.get());
        event.register(TOGGLE_GOGGLE.get());
    }
}
