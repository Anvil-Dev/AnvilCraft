package dev.dubhe.anvilcraft.item.template;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class FourToOneTemplateItem extends BaseMultipleToOneTemplateItem {
    public static final Component MISSING_TOOLTIP = Component.translatable(
        "screen.anvilcraft.ember_smithing.four.missing");
    public static final List<ResourceLocation> EMPTY_SLOT_TEXTURES = List.of(
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
    public List<ResourceLocation> getEmptySlotTextures() {
        return EMPTY_SLOT_TEXTURES;
    }
}
