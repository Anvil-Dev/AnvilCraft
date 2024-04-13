package dev.dubhe.anvilcraft.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.SmithingTemplateItem;

import java.util.List;

public class RoyalUpgradeTemplateItem extends SmithingTemplateItem {

    private static final ChatFormatting TITLE_FORMAT = ChatFormatting.GRAY;
    private static final ChatFormatting DESCRIPTION_FORMAT = ChatFormatting.BLUE;
    private static final Component APPLIES_TO =
        Component.translatable("screen.anvilcraft.smithing_template.royal_steel_upgrade_smithing_template.applies_to")
            .withStyle(DESCRIPTION_FORMAT);
    private static final Component UPGRADE_INGREDIENTS =
        Component.translatable("item.anvilcraft.royal_steel_ingot").withStyle(DESCRIPTION_FORMAT);
    private static final Component UPGRADE =
        Component.translatable("screen.anvilcraft.royal_steel_upgrade_smithing_template").withStyle(TITLE_FORMAT);
    private static final Component UPGRADE_BASE_SLOT_DESCRIPTION =
        Component.translatable(
            "screen.anvilcraft.smithing_template.royal_steel_upgrade_smithing_template.base_slot_description");
    private static final Component UPGRADE_ADDITIONS_SLOT_DESCRIPTION =
        Component.translatable(
            "screen.anvilcraft.smithing_template.royal_steel_upgrade_smithing_template.additions_slot_description");
    private static final ResourceLocation EMPTY_SLOT_PICKAXE = new ResourceLocation("item/empty_slot_pickaxe");
    private static final ResourceLocation EMPTY_SLOT_INGOT = new ResourceLocation("item/empty_slot_ingot");

    public RoyalUpgradeTemplateItem(@SuppressWarnings("unused") Properties properties) {
        super(APPLIES_TO, UPGRADE_INGREDIENTS, UPGRADE, UPGRADE_BASE_SLOT_DESCRIPTION,
            UPGRADE_ADDITIONS_SLOT_DESCRIPTION, List.of(EMPTY_SLOT_PICKAXE), List.of(EMPTY_SLOT_INGOT));
    }
}
