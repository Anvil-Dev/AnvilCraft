package dev.dubhe.anvilcraft.client.init;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.InputConstants.Type;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
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

    /**
     * F3 是否被按住。
     *
     * <p>1.21.1 的 F3 不是 {@link KeyMapping}（原始键码 292），原版 {@code KeyboardHandler} 只在
     * {@code handleDebugKeys} 真正消费了按键时才跳过 {@code KeyMapping.set/click}。因此像 F3+G
     * 这类被原版调试功能接受的组合键，仍会被后续无条件执行的 {@code ClientHooks.onKeyInput}
     * 依次送进各个模组的 {@code InputEvent.Key} 监听器，让模组按键被误触发。模组监听器需要
     * 自行查询 F3 的物理状态并忽略按下事件。</p>
     */
    public static boolean isDebugModifierHeld() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.getWindow() != null
            && InputConstants.isKeyDown(minecraft.getWindow().getWindow(), GLFW.GLFW_KEY_F3);
    }

    /**
     * 是否应忽略本次按下事件：F3 组合键期间只放过 RELEASE，保证已按下的键能正常复位。
     *
     * @param action GLFW 动作（{@link GLFW#GLFW_PRESS} / {@link GLFW#GLFW_REPEAT} / {@link GLFW#GLFW_RELEASE}）
     */
    public static boolean shouldIgnorePress(int action) {
        return action != GLFW.GLFW_RELEASE && isDebugModifierHeld();
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
