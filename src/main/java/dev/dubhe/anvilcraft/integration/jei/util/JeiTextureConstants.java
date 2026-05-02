package dev.dubhe.anvilcraft.integration.jei.util;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.resources.Identifier;

public class JeiTextureConstants {
    // Arrow
    public static final Identifier ARROW_DEFAULT = texture("arrow_default");
    public static final Identifier ARROW_BLOCK_CONVERSION = texture("arrow_block_conversion");
    public static final Identifier ARROW_INPUT = texture("arrow_input");
    public static final Identifier ARROW_OUTPUT = texture("arrow_output");
    public static final Identifier ARROW_OUTPUT_FROM_BELOW = texture("arrow_output_from_below");

    // Slot
    public static final Identifier SLOT_CHOICE = texture("slot_choice");
    public static final Identifier SLOT_DEFAULT = texture("slot_default");
    public static final Identifier SLOT_PROBABILITY = texture("slot_probability");

    // MULTIBLOCK
    public static final Identifier DISPLAY_MODES = texture("multiblock/display_modes");
    public static final Identifier LAYER_UP = texture("multiblock/layer_up");
    public static final Identifier LAYER_DOWN = texture("multiblock/layer_down");
    public static final Identifier LAYER_SWITCH = texture("multiblock/layer_switch");

    // Other
    public static final Identifier EXPLOSION = texture("explosion");
    public static final Identifier PRE_RENDERED_END_PORTAL = texture("pre_rendered_end_portal");
    public static final Identifier BLOCK_CONVERSION = texture("multiblock/multiblock_conversion");
    public static final Identifier BLOCK_CRAFTING = texture("multiblock/multiblock_crafting");

    private static Identifier texture(String path) {
        return AnvilCraft.of("textures/gui/jei/" + path + ".png");
    }
}
