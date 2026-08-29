package dev.dubhe.anvilcraft.item.ingredients;

import dev.dubhe.anvilcraft.api.item.IPermutationMaterial;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class EmberMetalIngotItem extends Item implements IPermutationMaterial {
    private static final Component MISSING_TOOLTIP = Component.translatable(
        "screen.anvilcraft.frost_smithing.ember_metal_ingot.missing_tools"
    );
    private static final List<Identifier> EMPTY_SLOT_TEXTURES = List.of(
        Identifier.withDefaultNamespace("container/slot/sword"),
        Identifier.withDefaultNamespace("container/slot/axe"),
        Identifier.withDefaultNamespace("container/slot/pickaxe"),
        Identifier.withDefaultNamespace("container/slot/shovel"),
        Identifier.withDefaultNamespace("container/slot/hoe")
    );

    public EmberMetalIngotItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getInputTooltip(ItemStack material) {
        return EmberMetalIngotItem.MISSING_TOOLTIP;
    }

    @Override
    public List<Identifier> getEmptySlotTextures() {
        return EmberMetalIngotItem.EMPTY_SLOT_TEXTURES;
    }
}
