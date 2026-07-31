package dev.dubhe.anvilcraft.integration.jei.util;

import dev.dubhe.anvilcraft.constant.SharedTextures;
import net.minecraft.resources.Identifier;

public class JeiTextures {
    // Arrow
    public static final Identifier ARROW_DEFAULT = JeiTextures.texture("arrow_default");
    public static final Identifier ARROW_BLOCK_CONVERSION = JeiTextures.texture("arrow_block_conversion");
    public static final Identifier ARROW_INPUT = JeiTextures.texture("arrow_input");
    public static final Identifier ARROW_LONG = JeiTextures.texture("arrow_long");
    public static final Identifier ARROW_OUTPUT = JeiTextures.texture("arrow_output");
    public static final Identifier ARROW_OUTPUT_FROM_BELOW = JeiTextures.texture("arrow_output_from_below");

    // Slot
    public static final Identifier SLOT_CHOICE = JeiTextures.texture("slot_choice");
    public static final Identifier SLOT_DEFAULT = JeiTextures.texture("slot_default");
    public static final Identifier SLOT_PROBABILITY = JeiTextures.texture("slot_probability");

    // MULTIBLOCK
    public static final Identifier DISPLAY_MODES = JeiTextures.texture("multiblock/display_modes");
    public static final Identifier LAYER_UP = JeiTextures.texture("multiblock/layer_up");
    public static final Identifier LAYER_DOWN = JeiTextures.texture("multiblock/layer_down");
    public static final Identifier LAYER_SWITCH = JeiTextures.texture("multiblock/layer_switch");
    public static final Identifier BLOCK_CONVERSION = JeiTextures.texture("multiblock/multiblock_conversion");
    public static final Identifier BLOCK_CRAFTING = JeiTextures.texture("multiblock/multiblock_crafting");

    // Other
    public static final Identifier EXPLOSION = JeiTextures.texture("explosion");
    public static final Identifier CYCLE = JeiTextures.texture("cycle");
    public static final Identifier PRE_RENDERED_END_PORTAL = JeiTextures.texture("pre_rendered_end_portal");

    public static Identifier texture(String path) {
        return SharedTextures.textureGui("jei/" + path);
    }

    public static Identifier bg(String id) {
        return SharedTextures.bg("jei", id);
    }
}
