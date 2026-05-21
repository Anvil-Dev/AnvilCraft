package dev.dubhe.anvilcraft.constant;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.resources.Identifier;

public class SharedTextures {
    // CRAFTING
    public static final Identifier ERROR_SPRITE = textureGui("crafting/error");
    public static final Identifier SWITCH_TABLE_BUTTON = textureGui("crafting/switch_table_button");
    public static final Identifier SWITCH_TABLE_SLIDER = textureGui("crafting/switch_table_slider");
    public static final Identifier TEXT_FIELD = textureGui("crafting/text_field");
    public static final Identifier TEXT_FIELD_DISABLE = textureGui("crafting/text_field_disabled");

    // MACHINE
    public static final Identifier BUTTON_ALL = textureGui("machine/button_all");
    public static final Identifier BUTTON_ANY = textureGui("machine/button_any");
    public static final Identifier BUTTON_U = textureGui("machine/button_u");
    public static final Identifier BUTTON_D = textureGui("machine/button_d");
    public static final Identifier BUTTON_N = textureGui("machine/button_n");
    public static final Identifier BUTTON_S = textureGui("machine/button_s");
    public static final Identifier BUTTON_E = textureGui("machine/button_e");
    public static final Identifier BUTTON_W = textureGui("machine/button_w");
    public static final Identifier BUTTON_RISING_EDGE = textureGui("machine/button_rising_edge");
    public static final Identifier BUTTON_FALLING_EDGE = textureGui("machine/button_falling_edge");
    public static final Identifier BUTTON_LOOP = textureGui("machine/button_loop");
    public static final Identifier BUTTON_HYSTERESIS = textureGui("machine/button_hysteresis");
    public static final Identifier BUTTON_WINDOW = textureGui("machine/button_window");
    public static final Identifier BUTTON_YES = textureGui("machine/button_yes");
    public static final Identifier BUTTON_NO = textureGui("machine/button_no");
    public static final Identifier BUTTON_REDSTONE_CONTROL_ON = textureGui("machine/button_redstone_control_on");
    public static final Identifier BUTTON_REDSTONE_CONTROL_OFF = textureGui("machine/button_redstone_control_off");
    public static final Identifier BUTTON_REVERSE_ON = textureGui("machine/button_reverse_on");
    public static final Identifier BUTTON_REVERSE_OFF = textureGui("machine/button_reverse_off");
    public static final Identifier CONFIRM = textureGui("machine/confirm");
    public static final Identifier DISABLED_SLOT = textureGui("machine/disabled_slot");
    public static final Identifier PLAYER_ALLOW = textureGui("machine/player_allow");
    public static final Identifier PLAYER_NOT_ALLOW = textureGui("machine/player_not_allow");
    public static final Identifier VILLAGER_ALLOW = textureGui("machine/villager_allow");
    public static final Identifier VILLAGER_NOT_ALLOW = textureGui("machine/villager_not_allow");
    public static final Identifier SMALL_SLIDER = textureGui("machine/slider");

    // MISC
    public static final Identifier BOX_SELECTION = textureGui("misc/box_selection");

    public static Identifier texture(String path) {
        return AnvilCraft.of("textures/" + path + ".png");
    }

    public static Identifier textureGui(String path) {
        return texture("gui/" + path);
    }

    public static Identifier bg(String category, String id) {
        return textureGui(category + "/background/" + id);
    }
}
