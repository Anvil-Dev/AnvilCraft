package dev.dubhe.anvilcraft.client.init;

import com.mojang.blaze3d.platform.InputConstants.Type;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.util.Lazy;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = AnvilCraft.MOD_ID, value = Dist.CLIENT)
public class ModKeyMappings {
    public static final KeyMapping.Category ANVILCRAFT_CATEGORY = new KeyMapping.Category(AnvilCraft.of("all"));

    public static final Lazy<KeyMapping> SWITCH_PHASE = ModKeyMappings.register(
        "switch_phase",
        KeyConflictContext.IN_GAME,
        Type.KEYSYM,
        GLFW.GLFW_KEY_X
    );
    public static final Lazy<KeyMapping> TOGGLE_GOGGLE = ModKeyMappings.register(
        "toggle_goggle",
        KeyConflictContext.IN_GAME,
        Type.KEYSYM,
        GLFW.GLFW_KEY_UNKNOWN
    );
    public static final Lazy<KeyMapping> SWITCH_TOOL_MODE = ModKeyMappings.register(
        "switch_tool_mode",
        KeyConflictContext.IN_GAME,
        Type.KEYSYM,
        GLFW.GLFW_KEY_LEFT_ALT
    );
    public static final Lazy<KeyMapping> USE_PILL_BOX = ModKeyMappings.register(
        "use_pill_box",
        KeyConflictContext.IN_GAME,
        Type.KEYSYM,
        GLFW.GLFW_KEY_V
    );
    public static final Lazy<KeyMapping> THOUGHT = ModKeyMappings.register(
        "thought",
        KeyConflictContext.GUI,
        Type.KEYSYM,
        GLFW.GLFW_KEY_A
    );

    @SuppressWarnings("SameParameterValue")
    private static Lazy<KeyMapping> register(String name, KeyConflictContext context, Type type, int key) {
        return Lazy.of(() -> new KeyMapping("key.anvilcraft." + name, context, type, key, ModKeyMappings.ANVILCRAFT_CATEGORY));
    }

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.registerCategory(ModKeyMappings.ANVILCRAFT_CATEGORY);
        event.register(ModKeyMappings.SWITCH_PHASE.get());
        event.register(ModKeyMappings.TOGGLE_GOGGLE.get());
        event.register(ModKeyMappings.SWITCH_TOOL_MODE.get());
        event.register(ModKeyMappings.USE_PILL_BOX.get());
    }
}
