package dev.dubhe.anvilcraft.item.ingredients;

import dev.anvilcraft.lib.v2.util.ListUtil;
import dev.dubhe.anvilcraft.api.item.IMultipleMaterial;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ResonatorCoreItem extends Item implements IMultipleMaterial {
    private static final Identifier EMPTY_SLOT_AXE =
        Identifier.withDefaultNamespace("container/slot/axe");
    private static final Identifier EMPTY_SLOT_SHOVEL =
        Identifier.withDefaultNamespace("container/slot/shovel");
    private static final Identifier EMPTY_SLOT_HOE =
        Identifier.withDefaultNamespace("container/slot/hoe");
    private static final Identifier EMPTY_SLOT_PICKAXE =
        Identifier.withDefaultNamespace("container/slot/pickaxe");
    private static final Component MISSING_TOOLS_TOOLTIP = Component.translatable(
        "screen.anvilcraft.ember_smithing.resonator_core.missing_tools");
    private static final List<Identifier> EMPTY_SLOT_TEXTURES = List.of(
        ResonatorCoreItem.EMPTY_SLOT_AXE,
        ResonatorCoreItem.EMPTY_SLOT_SHOVEL,
        ResonatorCoreItem.EMPTY_SLOT_HOE,
        ResonatorCoreItem.EMPTY_SLOT_PICKAXE
    );

    public ResonatorCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getInputTooltip(ItemStack template, List<ItemStack> inputs) {
        return ResonatorCoreItem.MISSING_TOOLS_TOOLTIP;
    }

    @Override
    public List<Identifier> getEmptySlotTextures(ItemStack template, int id, List<ItemStack> inputs) {
        List<Identifier> textures = ListUtil.cycle(ResonatorCoreItem.EMPTY_SLOT_TEXTURES, id);
        for (ItemStack input : inputs) {
            if (input.is(ItemTags.AXES)) {
                textures.remove(ResonatorCoreItem.EMPTY_SLOT_AXE);
            } else if (input.is(ItemTags.SHOVELS)) {
                textures.remove(ResonatorCoreItem.EMPTY_SLOT_SHOVEL);
            } else if (input.is(ItemTags.HOES)) {
                textures.remove(ResonatorCoreItem.EMPTY_SLOT_HOE);
            } else if (input.is(ItemTags.PICKAXES)) {
                textures.remove(ResonatorCoreItem.EMPTY_SLOT_PICKAXE);
            }
        }
        return textures;
    }
}
