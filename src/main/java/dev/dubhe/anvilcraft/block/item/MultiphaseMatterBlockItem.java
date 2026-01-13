package dev.dubhe.anvilcraft.block.item;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.item.IPermutationMaterial;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class MultiphaseMatterBlockItem extends BlockItem implements IPermutationMaterial {
    private static final Component MISSING_TOOLTIP =  Component.translatable(
        "screen.anvilcraft.frost_smithing.multiphase_matter_block.missing_tools"
    );
    private static final List<ResourceLocation> EMPTY_SLOT_TEXTURES = List.of(
        AnvilCraft.of("item/empty_slot_block")
    );

    public MultiphaseMatterBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getInputTooltip(ItemStack material) {
        return MISSING_TOOLTIP;
    }

    @Override
    public List<ResourceLocation> getEmptySlotTextures() {
        return EMPTY_SLOT_TEXTURES;
    }
}
