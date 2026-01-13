package dev.dubhe.anvilcraft.item.template.frost;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.List;

public class PermutationTemplateItem extends Item {
    private static final Component MISSING_TOOLTIP = Component.translatable("screen.anvilcraft.frost_smithing.permutation.missing");
    public static final List<ResourceLocation> EMPTY_SLOT_TEXTURES = List.of(
        ResourceLocation.withDefaultNamespace("item/empty_slot_ingot"),
        AnvilCraft.of("item/empty_slot_multiphase_matter"),
        AnvilCraft.of("item/empty_slot_block")
    );

    public PermutationTemplateItem(Properties properties) {
        super(properties);
    }

    public Component getMaterialTooltip() {
        return PermutationTemplateItem.MISSING_TOOLTIP;
    }

    public List<ResourceLocation> getEmptySlotTextures() {
        return PermutationTemplateItem.EMPTY_SLOT_TEXTURES;
    }
}
