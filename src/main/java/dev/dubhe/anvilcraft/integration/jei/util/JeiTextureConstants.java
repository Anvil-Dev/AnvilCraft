package dev.dubhe.anvilcraft.integration.jei.util;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.resources.ResourceLocation;

public class JeiTextureConstants {
    // Arrow
    public static final ResourceLocation ARROW_DEFAULT = texture("arrow_default");
    public static final ResourceLocation ARROW_BLOCK_CONVERSION = texture("arrow_block_conversion");
    public static final ResourceLocation ARROW_INPUT = texture("arrow_input");
    public static final ResourceLocation ARROW_OUTPUT = texture("arrow_output");
    public static final ResourceLocation ARROW_OUTPUT_FROM_BELOW = texture("arrow_output_from_below");

    // Slot
    public static final ResourceLocation SLOT_CHOICE = texture("slot_choice");
    public static final ResourceLocation SLOT_DEFAULT = texture("slot_default");
    public static final ResourceLocation SLOT_PROBABILITY = texture("slot_probability");

    // MULTIBLOCK
    public static final ResourceLocation DISPLAY_MODES = texture("multiblock/display_modes");
    public static final ResourceLocation LAYER_UP = texture("multiblock/layer_up");
    public static final ResourceLocation LAYER_DOWN = texture("multiblock/layer_down");
    public static final ResourceLocation LAYER_SWITCH = texture("multiblock/layer_switch");

    // Other
    public static final ResourceLocation EXPLOSION = texture("explosion");
    public static final ResourceLocation PRE_RENDERED_END_PORTAL = texture("pre_rendered_end_portal");
    public static final ResourceLocation BLOCK_CONVERSION = texture("multiblock/multiblock_conversion");
    public static final ResourceLocation BLOCK_CRAFTING = texture("multiblock/multiblock_crafting");

    public static ResourceLocation texture(String path) {
        return AnvilCraft.of("textures/gui/jei/" + path + ".png");
    }
}
