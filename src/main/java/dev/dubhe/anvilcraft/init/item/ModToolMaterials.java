package dev.dubhe.anvilcraft.init.item;

import dev.dubhe.anvilcraft.init.block.ModBlockTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ToolMaterial;

public class ModToolMaterials {
    public static final ToolMaterial AMETHYST = new ToolMaterial(
        BlockTags.INCORRECT_FOR_STONE_TOOL,
        751,
        4.0F,
        1.0F,
        5,
        ModItemTags.AMETHYST_TOOL_MATERIALS
    );
    public static final ToolMaterial ROYAL_STEEL = new ToolMaterial(
        BlockTags.INCORRECT_FOR_DIAMOND_TOOL,
        1561,
        8.0F,
        3.0F,
        10,
        ModItemTags.ROYAL_STEEL_TOOL_MATERIALS
    );
    public static final ToolMaterial FROST_METAL = new ToolMaterial(
        BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
        2031,
        9.0F,
        4.0F,
        15,
        ModItemTags.FROST_METAL_TOOL_MATERIALS
    );
    public static final ToolMaterial EMBER_METAL = new ToolMaterial(
        ModBlockTags.INCORRECT_FOR_EMBER_TOOL,
        2031,
        10.0F,
        5.0F,
        22,
        ModItemTags.EMBER_METAL_TOOL_MATERIALS
    );
    public static final ToolMaterial TRANSCENDIUM = new ToolMaterial(
        ModBlockTags.INCORRECT_FOR_TRANSCENDIUM_TOOL,
        3156,
        14.0F,
        7.0F,
        28,
        ModItemTags.TRANSCENDIUM_TOOL_MATERIALS
    );
}
