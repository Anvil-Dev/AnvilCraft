package dev.dubhe.anvilcraft.integration.jei.util;

import dev.dubhe.anvilcraft.AnvilCraft;

import net.minecraft.resources.ResourceLocation;

public class TextureConstants {
    public static final String BASE_PATH = "textures/gui/";

    public static final ResourceLocation PROGRESS =
        ResourceLocation.parse(BASE_PATH + "sprites/container/furnace/burn_progress.png");

    public static final ResourceLocation ANVIL_CRAFT_SPRITES = AnvilCraft.of(BASE_PATH + "sprites/jei.png");

    public static final ResourceLocation PRE_RENDERED_END_PORTAL =
        AnvilCraft.of(BASE_PATH + "pre_rendered_end_portal.png");
}
