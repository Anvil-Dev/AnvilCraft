package dev.dubhe.anvilcraft.item.template.mto;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

public class FourToOneTemplateItem extends BaseMultipleToOneTemplateItem {
    public static final Component MISSING_TOOLTIP = Component.translatable(
        "screen.anvilcraft.ember_smithing.four.missing");
    public static final List<Identifier> EMPTY_SLOT_TEXTURES = List.of(
        AnvilCraft.of("item/empty_slot_multiphase_matter"),
        AnvilCraft.of("item/empty_slot_block")
    );

    public FourToOneTemplateItem(Properties properties) {
        super(properties, 4);
    }

    @Override
    public Component getMaterialTooltip() {
        return MISSING_TOOLTIP;
    }

    @Override
    public List<Identifier> getEmptySlotTextures() {
        return EMPTY_SLOT_TEXTURES;
    }
}
