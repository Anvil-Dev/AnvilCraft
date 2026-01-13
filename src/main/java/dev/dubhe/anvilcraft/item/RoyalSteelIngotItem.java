package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.api.item.IPermutationMaterial;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class RoyalSteelIngotItem extends Item implements IPermutationMaterial {
    private static final Component MISSING_TOOLTIP = Component.translatable(
        "screen.anvilcraft.frost_smithing.royal_steel_ingot.missing_tools"
    );
    private static final List<ResourceLocation> EMPTY_SLOT_TEXTURES = List.of(
        ResourceLocation.withDefaultNamespace("item/empty_slot_sword"),
        ResourceLocation.withDefaultNamespace("item/empty_slot_axe"),
        ResourceLocation.withDefaultNamespace("item/empty_slot_pickaxe"),
        ResourceLocation.withDefaultNamespace("item/empty_slot_shovel"),
        ResourceLocation.withDefaultNamespace("item/empty_slot_hoe")
    );

    public RoyalSteelIngotItem(Properties properties) {
        super(properties);
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
