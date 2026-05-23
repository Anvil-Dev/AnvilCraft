package dev.dubhe.anvilcraft.constant;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.resources.ResourceLocation;

public class SharedTextures {
    // CRAFTING
    public static final ResourceLocation ERROR_SPRITE = textureGui("crafting/error");
    public static final ResourceLocation SWITCH_TABLE_BUTTON = textureGui("crafting/switch_table_button");
    public static final ResourceLocation SWITCH_TABLE_SLIDER = textureGui("crafting/switch_table_slider");
    public static final ResourceLocation TEXT_FIELD = textureGui("crafting/text_field");
    public static final ResourceLocation TEXT_FIELD_DISABLE = textureGui("crafting/text_field_disabled");

    // MACHINE
    public static final ResourceLocation BUTTON_ALL = textureGui("machine/button_all");
    public static final ResourceLocation BUTTON_ANY = textureGui("machine/button_any");
    public static final ResourceLocation BUTTON_U = textureGui("machine/button_u");
    public static final ResourceLocation BUTTON_D = textureGui("machine/button_d");
    public static final ResourceLocation BUTTON_N = textureGui("machine/button_n");
    public static final ResourceLocation BUTTON_S = textureGui("machine/button_s");
    public static final ResourceLocation BUTTON_E = textureGui("machine/button_e");
    public static final ResourceLocation BUTTON_W = textureGui("machine/button_w");
    public static final ResourceLocation BUTTON_RISING_EDGE = textureGui("machine/button_rising_edge");
    public static final ResourceLocation BUTTON_FALLING_EDGE = textureGui("machine/button_falling_edge");
    public static final ResourceLocation BUTTON_LOOP = textureGui("machine/button_loop");
    public static final ResourceLocation BUTTON_HYSTERESIS = textureGui("machine/button_hysteresis");
    public static final ResourceLocation BUTTON_WINDOW = textureGui("machine/button_window");
    public static final ResourceLocation BUTTON_YES = textureGui("machine/button_yes");
    public static final ResourceLocation BUTTON_NO = textureGui("machine/button_no");
    public static final ResourceLocation BUTTON_REDSTONE_CONTROL_ON = textureGui("machine/button_redstone_control_on");
    public static final ResourceLocation BUTTON_REDSTONE_CONTROL_OFF = textureGui("machine/button_redstone_control_off");
    public static final ResourceLocation BUTTON_REVERSE_ON = textureGui("machine/button_reverse_on");
    public static final ResourceLocation BUTTON_REVERSE_OFF = textureGui("machine/button_reverse_off");
    public static final ResourceLocation BUTTON_REDO = textureGui("machine/redo");
    public static final ResourceLocation BUTTON_STOP = textureGui("machine/stop");
    public static final ResourceLocation BUTTON_CONFIRM = textureGui("machine/confirm");
    
    /**
     * 已弃用，请使用 {@link #BUTTON_CONFIRM} 代替。
     *
     * @deprecated 使用 {@link #BUTTON_CONFIRM} 代替
     */
    @Deprecated
    @SuppressWarnings("DeprecatedIsStillUsed")
    public static final ResourceLocation CONFIRM = BUTTON_CONFIRM;
    public static final ResourceLocation DISABLED_SLOT = textureGui("machine/disabled_slot");
    public static final ResourceLocation PLAYER_ALLOW = textureGui("machine/player_allow");
    public static final ResourceLocation PLAYER_NOT_ALLOW = textureGui("machine/player_not_allow");
    public static final ResourceLocation VILLAGER_ALLOW = textureGui("machine/villager_allow");
    public static final ResourceLocation VILLAGER_NOT_ALLOW = textureGui("machine/villager_not_allow");
    public static final ResourceLocation SMALL_SLIDER = textureGui("machine/slider");
    public static final ResourceLocation STRUCTURE_TOOL_LOCKED = textureGui("machine/structure_tool_locked");
    
    // SMART_BLOCK_PLACER
    public static final ResourceLocation SMART_BLOCK_PLACER_BACKGROUND = bg("machine", "smart_block_placer");
    public static final ResourceLocation SMART_BLOCK_PLACER_LAYER_1 = textureGui("machine/smart_block_placer/layer_1");
    public static final ResourceLocation SMART_BLOCK_PLACER_LAYER_2 = textureGui("machine/smart_block_placer/layer_2");
    public static final ResourceLocation SMART_BLOCK_PLACER_LAYER_3 = textureGui("machine/smart_block_placer/layer_3");
    public static final ResourceLocation SMART_BLOCK_PLACER_LAYER_4 = textureGui("machine/smart_block_placer/layer_4");
    public static final ResourceLocation SMART_BLOCK_PLACER_LAYER_5 = textureGui("machine/smart_block_placer/layer_5");
    public static final ResourceLocation SMART_BLOCK_PLACER_POSITION_SELECT = textureGui("machine/smart_block_placer/position_select");
    public static final ResourceLocation SMART_BLOCK_PLACER_LAYER_ALL = textureGui("machine/smart_block_placer/layer_all");
    public static final ResourceLocation SMART_BLOCK_PLACER_LAYER_SINGLE = textureGui("machine/smart_block_placer/layer_single");
    public static final ResourceLocation SMART_BLOCK_PLACER_PICKUP_MODE = textureGui("machine/smart_block_placer/pickup_mode");
    public static final ResourceLocation SMART_BLOCK_PLACER_MOVE_MODE = textureGui("machine/smart_block_placer/move_mode");
    public static final ResourceLocation SMART_BLOCK_PLACER_BLUEPRINT_MODE = textureGui("machine/smart_block_placer/blueprint_mode");
    public static final ResourceLocation SMART_BLOCK_PLACER_SKIP_MISSING = textureGui("machine/smart_block_placer/skip_missing");
    public static final ResourceLocation SMART_BLOCK_PLACER_STOP_MISSING = textureGui("machine/smart_block_placer/stop_missing");

    // MISC
    public static final ResourceLocation BOX_SELECTION = textureGui("misc/box_selection");

    public static ResourceLocation texture(String path) {
        return AnvilCraft.of("textures/" + path + ".png");
    }

    public static ResourceLocation textureGui(String path) {
        return texture("gui/" + path);
    }

    public static ResourceLocation bg(String category, String id) {
        return textureGui(category + "/background/" + id);
    }
}
