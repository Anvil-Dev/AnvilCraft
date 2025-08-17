package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.api.item.IMultipleMaterial;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class MultiphaseMatterItem extends Item implements IMultipleMaterial {
    private static final Component MISSING_TOOLS_TOOLTIP = Component.translatable(
        "screen.anvilcraft.ember_smithing.multiphase_matter.missing_tools");
    private static final List<ResourceLocation> EMPTY_SLOT_TEXTURES = List.of(
    );

    public MultiphaseMatterItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getInputTooltip(ItemStack template, List<ItemStack> inputs) {
        return MISSING_TOOLS_TOOLTIP;
    }

    @Override
    public List<ResourceLocation> getEmptySlotTextures(ItemStack template, int id, List<ItemStack> inputs) {
        return EMPTY_SLOT_TEXTURES;
    }
}
