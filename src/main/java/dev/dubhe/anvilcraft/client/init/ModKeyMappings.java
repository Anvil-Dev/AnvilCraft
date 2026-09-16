package dev.dubhe.anvilcraft.client.init;

import com.mojang.blaze3d.platform.InputConstants.Type;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.common.util.Lazy;
import org.lwjgl.glfw.GLFW;

public class ModKeyMappings {
    public static final Lazy<KeyMapping> BUILDING_ROD_FORWARD = register("building_rod_forward", KeyConflictContext.IN_GAME,
        Type.KEYSYM, GLFW.GLFW_KEY_UP);
    public static final Lazy<KeyMapping> BUILDING_ROD_BACK = register("building_rod_back", KeyConflictContext.IN_GAME,
        Type.KEYSYM, GLFW.GLFW_KEY_DOWN);
    public static final Lazy<KeyMapping> BUILDING_ROD_LEFT = register("building_rod_left", KeyConflictContext.IN_GAME,
        Type.KEYSYM, GLFW.GLFW_KEY_LEFT);
    public static final Lazy<KeyMapping> BUILDING_ROD_RIGHT = register("building_rod_right", KeyConflictContext.IN_GAME,
        Type.KEYSYM, GLFW.GLFW_KEY_RIGHT);
    public static final Lazy<KeyMapping> BUILDING_ROD_UP = register("building_rod_up", KeyConflictContext.IN_GAME,
        Type.KEYSYM, GLFW.GLFW_KEY_PAGE_UP);
    public static final Lazy<KeyMapping> BUILDING_ROD_DOWN = register("building_rod_down", KeyConflictContext.IN_GAME,
        Type.KEYSYM, GLFW.GLFW_KEY_PAGE_DOWN);
    public static final Lazy<KeyMapping> BUILDING_ROD_CLOCKWISE = register("building_rod_clockwise",
        KeyConflictContext.IN_GAME, Type.KEYSYM, GLFW.GLFW_KEY_EQUAL);
    public static final Lazy<KeyMapping> BUILDING_ROD_COUNTERCLOCKWISE = register("building_rod_counterclockwise",
        KeyConflictContext.IN_GAME, Type.KEYSYM, GLFW.GLFW_KEY_MINUS);
    public static final Lazy<KeyMapping> BUILDING_ROD_MIRROR = register("building_rod_mirror", KeyConflictContext.IN_GAME,
        Type.KEYSYM, GLFW.GLFW_KEY_BACKSLASH);
    public static final Lazy<KeyMapping> BUILDING_ROD_TOOL = register("building_rod_tool", KeyConflictContext.IN_GAME,
        Type.KEYSYM, GLFW.GLFW_KEY_LEFT_CONTROL);
    public static final Lazy<KeyMapping> BUILDING_ROD_ADJUST = register("building_rod_adjust", KeyConflictContext.IN_GAME,
        Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT);
    public static final Lazy<KeyMapping> BUILDING_ROD_LAYER = register("building_rod_layer", KeyConflictContext.IN_GAME,
        Type.KEYSYM, GLFW.GLFW_KEY_LEFT_SHIFT);

    public static final Lazy<KeyMapping> POCKETS = Lazy.of(() -> new KeyMapping(
        "key.anvilcraft.pockets", KeyConflictContext.IN_GAME, KeyModifier.CONTROL,
        Type.KEYSYM, GLFW.GLFW_KEY_F, "key.categories.anvilcraft"));

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
    public static final Lazy<KeyMapping> OPEN_TERMINAL = register(
        "open_terminal",
        KeyConflictContext.UNIVERSAL,
        Type.KEYSYM,
        GLFW.GLFW_KEY_UNKNOWN
    );
    public static final Lazy<KeyMapping> THOUGHT = register(
        "thought",
        KeyConflictContext.GUI,
        Type.KEYSYM,
        GLFW.GLFW_KEY_A
    );

    @SuppressWarnings("SameParameterValue")
    private static Lazy<KeyMapping> register(String name, KeyConflictContext context, Type type, int key) {
        return Lazy.of(() -> new KeyMapping("key.anvilcraft." + name, context, type, key, "key.categories.anvilcraft"));
    }

    public static void register(RegisterKeyMappingsEvent event) {
        event.register(BUILDING_ROD_FORWARD.get());
        event.register(BUILDING_ROD_BACK.get());
        event.register(BUILDING_ROD_LEFT.get());
        event.register(BUILDING_ROD_RIGHT.get());
        event.register(BUILDING_ROD_UP.get());
        event.register(BUILDING_ROD_DOWN.get());
        event.register(BUILDING_ROD_CLOCKWISE.get());
        event.register(BUILDING_ROD_COUNTERCLOCKWISE.get());
        event.register(BUILDING_ROD_MIRROR.get());
        event.register(BUILDING_ROD_TOOL.get());
        event.register(BUILDING_ROD_ADJUST.get());
        event.register(BUILDING_ROD_LAYER.get());

        event.register(POCKETS.get());
        event.register(SWITCH_PHASE.get());
        event.register(TOGGLE_GOGGLE.get());
        event.register(SWITCH_TOOL_MODE.get());
        event.register(USE_PILL_BOX.get());
        event.register(OPEN_TERMINAL.get());
    }
}
