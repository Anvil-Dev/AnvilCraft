package dev.dubhe.anvilcraft.constant;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.resources.Identifier;

public class SharedTextures {
    // CRAFTING
    public static final Identifier ERROR_SPRITE = SharedTextures.textureGui("crafting/error");
    public static final Identifier SWITCH_TABLE_BUTTON = SharedTextures.textureGui("crafting/switch_table_button");
    public static final Identifier SWITCH_TABLE_SLIDER = SharedTextures.textureGui("crafting/switch_table_slider");
    public static final Identifier TEXT_FIELD = SharedTextures.textureGui("crafting/text_field");
    public static final Identifier TEXT_FIELD_DISABLE = SharedTextures.textureGui("crafting/text_field_disabled");

    // MACHINE
    public static final Identifier BUTTON_ALL = SharedTextures.textureGui("machine/button_all");
    public static final Identifier BUTTON_ANY = SharedTextures.textureGui("machine/button_any");
    public static final Identifier BUTTON_U = SharedTextures.textureGui("machine/button_u");
    public static final Identifier BUTTON_D = SharedTextures.textureGui("machine/button_d");
    public static final Identifier BUTTON_N = SharedTextures.textureGui("machine/button_n");
    public static final Identifier BUTTON_S = SharedTextures.textureGui("machine/button_s");
    public static final Identifier BUTTON_E = SharedTextures.textureGui("machine/button_e");
    public static final Identifier BUTTON_W = SharedTextures.textureGui("machine/button_w");
    public static final Identifier BUTTON_RISING_EDGE = SharedTextures.textureGui("machine/button_rising_edge");
    public static final Identifier BUTTON_FALLING_EDGE = SharedTextures.textureGui("machine/button_falling_edge");
    public static final Identifier BUTTON_LOOP = SharedTextures.textureGui("machine/button_loop");
    public static final Identifier BUTTON_HYSTERESIS = SharedTextures.textureGui("machine/button_hysteresis");
    public static final Identifier BUTTON_WINDOW = SharedTextures.textureGui("machine/button_window");
    public static final Identifier BUTTON_YES = SharedTextures.textureGui("machine/button_yes");
    public static final Identifier BUTTON_NO = SharedTextures.textureGui("machine/button_no");
    public static final Identifier BUTTON_REDSTONE_CONTROL_ON = SharedTextures.textureGui("machine/button_redstone_control_on");
    public static final Identifier BUTTON_REDSTONE_CONTROL_OFF = SharedTextures.textureGui("machine/button_redstone_control_off");
    public static final Identifier BUTTON_REVERSE_ON = SharedTextures.textureGui("machine/button_reverse_on");
    public static final Identifier BUTTON_REVERSE_OFF = SharedTextures.textureGui("machine/button_reverse_off");
    public static final Identifier CONFIRM = SharedTextures.textureGui("machine/confirm");
    public static final Identifier REDO = SharedTextures.textureGui("machine/redo");
    public static final Identifier STOP = SharedTextures.textureGui("machine/stop");
    public static final Identifier STRUCTURE_TOOL_LOCKED = SharedTextures.textureGui("machine/structure_tool_locked");
    public static final Identifier DISABLED_SLOT = SharedTextures.textureGui("machine/disabled_slot");
    public static final Identifier SMALL_MACHINE_SLIDER = SharedTextures.textureGui("machine/slider");

    // 智能放置器
    public static final Identifier SMART_BLOCK_PLACER_LAYER_1 = SharedTextures.textureGui("machine/smart_block_placer/layer_1");
    public static final Identifier SMART_BLOCK_PLACER_LAYER_2 = SharedTextures.textureGui("machine/smart_block_placer/layer_2");
    public static final Identifier SMART_BLOCK_PLACER_LAYER_3 = SharedTextures.textureGui("machine/smart_block_placer/layer_3");
    public static final Identifier SMART_BLOCK_PLACER_LAYER_4 = SharedTextures.textureGui("machine/smart_block_placer/layer_4");
    public static final Identifier SMART_BLOCK_PLACER_LAYER_5 = SharedTextures.textureGui("machine/smart_block_placer/layer_5");
    public static final Identifier SMART_BLOCK_PLACER_POSITION_SELECT = SharedTextures.textureGui("machine/smart_block_placer/position_select");
    public static final Identifier SMART_BLOCK_PLACER_LAYER_ALL = SharedTextures.textureGui("machine/smart_block_placer/layer_all");
    public static final Identifier SMART_BLOCK_PLACER_LAYER_SINGLE = SharedTextures.textureGui("machine/smart_block_placer/layer_single");
    public static final Identifier SMART_BLOCK_PLACER_PICKUP_MODE = SharedTextures.textureGui("machine/smart_block_placer/pickup_mode");
    public static final Identifier SMART_BLOCK_PLACER_MOVE_MODE = SharedTextures.textureGui("machine/smart_block_placer/move_mode");
    public static final Identifier SMART_BLOCK_PLACER_BLUEPRINT_MODE = SharedTextures.textureGui("machine/smart_block_placer/blueprint_mode");
    public static final Identifier SMART_BLOCK_PLACER_SKIP_MISSING = SharedTextures.textureGui("machine/smart_block_placer/skip_missing");
    public static final Identifier SMART_BLOCK_PLACER_STOP_MISSING = SharedTextures.textureGui("machine/smart_block_placer/stop_missing");

    // MISC
    public static final Identifier BOX_SELECTION = SharedTextures.textureGui("misc/box_selection");

    public static Identifier texture(String path) {
        return AnvilCraft.of("textures/" + path + ".png");
    }

    public static Identifier textureGui(String path) {
        return SharedTextures.texture("gui/" + path);
    }

    public static Identifier sprites(String path) {
        return SharedTextures.textureGui("sprites/" + path);
    }

    public static Identifier bg(String category, String id) {
        return SharedTextures.textureGui(category + "/background/" + id);
    }
}
