package dev.dubhe.anvilcraft.item.ingredients;

import dev.anvilcraft.lib.v2.util.ListUtil;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.item.IMultipleMaterial;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;

import java.util.List;

public class HeavyHalberdCoreItem extends Item implements IMultipleMaterial {
    private static final Identifier EMPTY_SLOT_SWORD =
        Identifier.withDefaultNamespace("container/slot/sword");
    private static final Identifier EMPTY_SLOT_SPEAR =
        Identifier.withDefaultNamespace("container/slot/spear");
    private static final Identifier EMPTY_SLOT_TRIDENT =
        AnvilCraft.of("item/empty_slot_trident");
    private static final Identifier EMPTY_SLOT_MACE =
        AnvilCraft.of("item/empty_slot_mace");
    private static final Component MISSING_TOOLS_TOOLTIP = Component.translatable(
        "screen.anvilcraft.ember_smithing.heavy_halberd_core.missing_tools");
    private static final List<Identifier> EMPTY_SLOT_TEXTURES = List.of(
        HeavyHalberdCoreItem.EMPTY_SLOT_SWORD, HeavyHalberdCoreItem.EMPTY_SLOT_SPEAR, HeavyHalberdCoreItem.EMPTY_SLOT_TRIDENT,
        HeavyHalberdCoreItem.EMPTY_SLOT_MACE
    );

    public HeavyHalberdCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getInputTooltip(ItemStack template, List<ItemStack> inputs) {
        return HeavyHalberdCoreItem.MISSING_TOOLS_TOOLTIP;
    }

    @Override
    public List<Identifier> getEmptySlotTextures(ItemStack template, int id, List<ItemStack> inputs) {
        List<Identifier> textures = ListUtil.cycle(HeavyHalberdCoreItem.EMPTY_SLOT_TEXTURES, id);
        for (ItemStack input : inputs) {
            if (input.is(ItemTags.SWORDS)) {
                textures.remove(HeavyHalberdCoreItem.EMPTY_SLOT_SWORD);
            } else if (input.is(ItemTags.SPEARS)) {
                textures.remove(HeavyHalberdCoreItem.EMPTY_SLOT_SPEAR);
            } else if (input.is(Items.TRIDENT)) {
                textures.remove(HeavyHalberdCoreItem.EMPTY_SLOT_TRIDENT);
            } else if (input.is(Tags.Items.TOOLS_MACE)) {
                textures.remove(HeavyHalberdCoreItem.EMPTY_SLOT_MACE);
            }
        }
        return textures;
    }
}
