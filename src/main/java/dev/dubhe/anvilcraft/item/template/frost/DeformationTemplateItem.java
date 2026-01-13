package dev.dubhe.anvilcraft.item.template.frost;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.List;

public class DeformationTemplateItem extends Item {
    private static final Component MISSING_TOOLTIP = Component.translatable("screen.anvilcraft.frost_smithing.deformation.missing_tools");
    private static final List<ResourceLocation> EMPTY_SLOT_TEXTURES = List.of(
        ResourceLocation.withDefaultNamespace("item/empty_slot_sword"),
        ResourceLocation.withDefaultNamespace("item/empty_slot_axe"),
        ResourceLocation.withDefaultNamespace("item/empty_slot_pickaxe"),
        ResourceLocation.withDefaultNamespace("item/empty_slot_shovel"),
        ResourceLocation.withDefaultNamespace("item/empty_slot_hoe"),
        AnvilCraft.of("item/empty_slot_helmet"),
        AnvilCraft.of("item/empty_slot_chestplate"),
        AnvilCraft.of("item/empty_slot_leggings"),
        AnvilCraft.of("item/empty_slot_boots"),
        AnvilCraft.of("item/empty_slot_bow"),
        AnvilCraft.of("item/empty_slot_crossbow")
    );

    public DeformationTemplateItem(Properties properties) {
        super(properties);
    }

    public Component getInputTooltip() {
        return DeformationTemplateItem.MISSING_TOOLTIP;
    }

    public List<ResourceLocation> getEmptySlotTextures() {
        return DeformationTemplateItem.EMPTY_SLOT_TEXTURES;
    }
}
