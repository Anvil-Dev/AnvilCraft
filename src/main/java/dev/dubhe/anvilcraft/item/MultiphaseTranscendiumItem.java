package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.item.IMultipleMaterial;
import dev.dubhe.anvilcraft.api.item.property.Eternal;
import dev.dubhe.anvilcraft.init.ModComponents;
import dev.dubhe.anvilcraft.init.ModItemTags;
import dev.dubhe.anvilcraft.util.ListUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.TriState;

import java.util.List;

public class MultiphaseTranscendiumItem extends Item implements IMultipleMaterial {
    private static final ResourceLocation EMPTY_SLOT_RESONATOR =
        AnvilCraft.of("item/empty_slot_resonator");
    private static final ResourceLocation EMPTY_SLOT_HEAVY_HALBERD =
        AnvilCraft.of("item/empty_slot_heavy_halberd");
    private static final Component MISSING_TOOLS_TOOLTIP = Component.translatable(
        "screen.anvilcraft.ember_smithing.multiphase_transcendium.missing_tools");
    private static final Component RESONATOR_MISSING_TOOLS_TOOLTIP = Component.translatable(
        "screen.anvilcraft.ember_smithing.multiphase_transcendium.resonator_missing_tools");
    private static final Component HEAVY_HALBERD_MISSING_TOOLS_TOOLTIP = Component.translatable(
        "screen.anvilcraft.ember_smithing.multiphase_transcendium.heavy_halberd_missing_tools");
    private static final List<ResourceLocation> EMPTY_SLOT_TEXTURES = List.of(
        EMPTY_SLOT_RESONATOR,
        EMPTY_SLOT_HEAVY_HALBERD
    );

    public MultiphaseTranscendiumItem(Properties properties) {
        super(properties.component(ModComponents.ETERNAL, Eternal.INSTANCE));
    }

    @Override
    public Component getInputTooltip(ItemStack template, List<ItemStack> inputs) {
        TriState tool = TriState.DEFAULT;
        for (ItemStack input : inputs) {
            if (input.is(ModItemTags.RESONATOR)) {
                tool = TriState.TRUE;
                break;
            }
            if (input.is(ModItemTags.HEAVY_HALBERD)) {
                tool = TriState.FALSE;
                break;
            }
        }
        return switch (tool) {
            case DEFAULT -> MISSING_TOOLS_TOOLTIP;
            case TRUE -> RESONATOR_MISSING_TOOLS_TOOLTIP;
            case FALSE -> HEAVY_HALBERD_MISSING_TOOLS_TOOLTIP;
        };
    }

    @Override
    public List<ResourceLocation> getEmptySlotTextures(ItemStack template, int id, List<ItemStack> inputs) {
        for (ItemStack input : inputs) {
            if (input.is(ModItemTags.RESONATOR)) return List.of(EMPTY_SLOT_RESONATOR);
            if (input.is(ModItemTags.HEAVY_HALBERD)) return List.of(EMPTY_SLOT_HEAVY_HALBERD);
        }
        return ListUtil.cycle(EMPTY_SLOT_TEXTURES, id);
    }
}
