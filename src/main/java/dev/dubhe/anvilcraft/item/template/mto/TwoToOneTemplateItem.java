package dev.dubhe.anvilcraft.item.template.mto;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

public class TwoToOneTemplateItem extends BaseMultipleToOneTemplateItem {
    public static final Component MISSING_TOOLTIP = Component.translatable(
        "screen.anvilcraft.ember_smithing.two.missing");
    public static final List<Identifier> EMPTY_SLOT_TEXTURES = List.of(
        AnvilCraft.of("item/empty_slot_multiphase_matter")
    );

    public TwoToOneTemplateItem(Properties properties) {
        super(properties, 2);
    }

    @Override
    public Component getMaterialTooltip() {
        return TwoToOneTemplateItem.MISSING_TOOLTIP;
    }

    @Override
    public List<Identifier> getEmptySlotTextures() {
        return TwoToOneTemplateItem.EMPTY_SLOT_TEXTURES;
    }
}
