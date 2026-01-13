package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.item.IMultipleMaterial;
import dev.dubhe.anvilcraft.api.item.IPermutationMaterial;
import dev.dubhe.anvilcraft.init.item.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MultiphaseMatterItem extends Item implements IMultipleMaterial, IPermutationMaterial {
    private static final Component EMBER_MISSING_TOOLTIP = Component.translatable(
        "screen.anvilcraft.ember_smithing.multiphase_matter.missing_tools"
    );
    private static final Component FROST_MISSING_TOOLTIP = Component.translatable(
        "screen.anvilcraft.frost_smithing.multiphase_matter.missing_tools"
    );
    private static final Map<Item, ResourceLocation> EMBER_EMPTY_SLOT_TEXTURES = Map.of(
        Items.SHEARS, AnvilCraft.of("item/empty_slot_shears"),
        Items.FLINT_AND_STEEL, AnvilCraft.of("item/empty_slot_flint_and_steel"),
        Items.BRUSH, AnvilCraft.of("item/empty_slot_brush"),
        Items.SPYGLASS, AnvilCraft.of("item/empty_slot_spyglass"),
        ModItems.MAGNET.get(), AnvilCraft.of("item/empty_slot_magnet"),
        Items.FISHING_ROD, AnvilCraft.of("item/empty_slot_fishing_rod"),
        Items.CARROT_ON_A_STICK, AnvilCraft.of("item/empty_slot_carrot_on_a_stick"),
        Items.WARPED_FUNGUS_ON_A_STICK, AnvilCraft.of("item/empty_slot_warped_fungus_on_a_stick")
    );
    private static final List<ResourceLocation> FROST_EMPTY_SLOT_TEXTURES = List.of(
        ResourceLocation.withDefaultNamespace("item/empty_slot_sword"),
        ResourceLocation.withDefaultNamespace("item/empty_slot_axe"),
        ResourceLocation.withDefaultNamespace("item/empty_slot_pickaxe"),
        ResourceLocation.withDefaultNamespace("item/empty_slot_shovel"),
        ResourceLocation.withDefaultNamespace("item/empty_slot_hoe"),
        AnvilCraft.of("item/empty_slot_heavy_halberd"),
        AnvilCraft.of("item/empty_slot_resonator")
    );

    public MultiphaseMatterItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getInputTooltip(ItemStack template, List<ItemStack> inputs) {
        return EMBER_MISSING_TOOLTIP;
    }

    @Override
    public Component getInputTooltip(ItemStack material) {
        return FROST_MISSING_TOOLTIP;
    }

    @Override
    public List<ResourceLocation> getEmptySlotTextures(ItemStack template, int id, List<ItemStack> inputs) {
        List<ResourceLocation> result = new ArrayList<>();
        EMPTY_SLOTS_TEXTURES_CHECK:
        for (Item item : EMBER_EMPTY_SLOT_TEXTURES.keySet()) {
            for (ItemStack input : inputs) {
                if (input.is(item)) continue EMPTY_SLOTS_TEXTURES_CHECK;
            }
            result.add(EMBER_EMPTY_SLOT_TEXTURES.get(item));
        }
        return result;
    }

    @Override
    public List<ResourceLocation> getEmptySlotTextures() {
        return FROST_EMPTY_SLOT_TEXTURES;
    }
}
