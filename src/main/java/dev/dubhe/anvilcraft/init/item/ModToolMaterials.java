package dev.dubhe.anvilcraft.init.item;

import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ToolMaterial;

public class ModToolMaterials {
    public static final ToolMaterial AMETHYST = new ToolMaterial(
        BlockTags.INCORRECT_FOR_STONE_TOOL,
        751,
        4.0f,
        1.0f,
        5,
        ModItemTags.AMETHYST_TOOL_MATERIALS
    );
    public static final ToolMaterial ROYAL_STEEL = new ToolMaterial(
        BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
        1561,
        8.0f,
        3.0f,
        10,
        ModItemTags.ROYAL_STEEL_TOOL_MATERIALS
    );
    public static final ToolMaterial FROST_METAL = new ToolMaterial(
        BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
        2031,
        9.0f,
        4.0f,
        15,
        ModItemTags.FROST_METAL_TOOL_MATERIALS
    );
    public static final ToolMaterial EMBER_METAL = new ToolMaterial(
        ModBlockTags.INCORRECT_FOR_EMBER_TOOL,
        2031,
        10.0f,
        5.0f,
        22,
        ModItemTags.EMBER_METAL_TOOL_MATERIALS
    );
    public static final ToolMaterial TRANSCENDIUM = new ToolMaterial(
        ModBlockTags.INCORRECT_FOR_TRANSCENDIUM_TOOL,
        3156,
        14.0f,
        7.0f,
        28,
        ModItemTags.TRANSCENDIUM_TOOL_MATERIALS
    );
}
