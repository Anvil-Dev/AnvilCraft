package dev.dubhe.anvilcraft.item.template.frost;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.List;

public class PermutationTemplateItem extends Item {
    private static final Component MISSING_TOOLTIP = Component.translatable("screen.anvilcraft.frost_smithing.permutation.missing");
    private static final Component MISSING_TOOLS_TOOLTIP = Component.translatable(
        "screen.anvilcraft.frost_smithing.permutation.missing_tools"
    );
    public static final List<Identifier> EMPTY_SLOT_TEXTURES = List.of(
        Identifier.withDefaultNamespace("container/slot/ingot"),
        AnvilCraft.of("item/empty_slot_multiphase_matter"),
        AnvilCraft.of("item/empty_slot_block")
    );
    private static final List<Identifier> EMPTY_TOOL_SLOT_TEXTURES = List.of(
        Identifier.withDefaultNamespace("container/slot/sword"),
        Identifier.withDefaultNamespace("container/slot/axe"),
        Identifier.withDefaultNamespace("container/slot/pickaxe"),
        Identifier.withDefaultNamespace("container/slot/shovel"),
        Identifier.withDefaultNamespace("container/slot/hoe"),
        AnvilCraft.of("item/empty_slot_hammer"),
        AnvilCraft.of("item/empty_slot_heavy_halberd"),
        AnvilCraft.of("item/empty_slot_resonator"),
        AnvilCraft.of("item/empty_slot_amulet")
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

    public Component getInputTooltip() {
        return PermutationTemplateItem.MISSING_TOOLS_TOOLTIP;
    }

    public List<Identifier> getInputSlotTextures() {
        return PermutationTemplateItem.EMPTY_TOOL_SLOT_TEXTURES;
    }
}
