package dev.dubhe.anvilcraft.block.item;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.item.IMultipleMaterial;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class FrostMetalBlockItem extends BlockItem implements IMultipleMaterial {
    private static final Component MISSING_TOOLTIP = Component.translatable(
        "screen.anvilcraft.ember_smithing.frost_metal_block.missing_tools"
    );
    private static final List<ResourceLocation> EMPTY_SLOT_TEXTURES = List.of(
        AnvilCraft.of("item/empty_slot_amulet")
    );

    public FrostMetalBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getInputTooltip(ItemStack template, List<ItemStack> inputs) {
        return FrostMetalBlockItem.MISSING_TOOLTIP;
    }

    @Override
    public List<ResourceLocation> getEmptySlotTextures(ItemStack template, int id, List<ItemStack> inputs) {
        return FrostMetalBlockItem.EMPTY_SLOT_TEXTURES;
    }
}
