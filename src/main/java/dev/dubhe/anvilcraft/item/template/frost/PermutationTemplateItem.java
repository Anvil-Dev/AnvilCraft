package dev.dubhe.anvilcraft.item.template.frost;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.List;

public class PermutationTemplateItem extends Item {
    private static final Component MISSING_TOOLTIP = Component.translatable("screen.anvilcraft.frost_smithing.permutation.missing");
    public static final List<Identifier> EMPTY_SLOT_TEXTURES = List.of(
        Identifier.withDefaultNamespace("container/slot/ingot"),
        AnvilCraft.of("item/empty_slot_multiphase_matter"),
        AnvilCraft.of("item/empty_slot_block")
    );

    public PermutationTemplateItem(Properties properties) {
        super(properties);
    }

    public Component getMaterialTooltip() {
        return PermutationTemplateItem.MISSING_TOOLTIP;
    }

    public List<Identifier> getEmptySlotTextures() {
        return PermutationTemplateItem.EMPTY_SLOT_TEXTURES;
    }
}
