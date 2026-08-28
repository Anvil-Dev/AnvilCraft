package dev.dubhe.anvilcraft.item.template;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.SmithingTemplateItem;

import java.util.List;

public class FrostMetalUpgradeTemplateItem extends SmithingTemplateItem {

    private static final ChatFormatting TITLE_FORMAT = ChatFormatting.GRAY;
    private static final ChatFormatting DESCRIPTION_FORMAT = ChatFormatting.BLUE;
    private static final Component APPLIES_TO = Component.translatable(
            "screen.anvilcraft.smithing_template.frost_metal_upgrade_smithing_template.applies_to")
        .withStyle(FrostMetalUpgradeTemplateItem.DESCRIPTION_FORMAT);
    private static final Component UPGRADE_INGREDIENTS =
        Component.translatable("screen.anvilcraft.smithing_template.frost_metal_upgrade_smithing_template"
                + ".upgrade_ingredients")
            .withStyle(FrostMetalUpgradeTemplateItem.DESCRIPTION_FORMAT);
    private static final Component UPGRADE_BASE_SLOT_DESCRIPTION = Component.translatable(
        "screen.anvilcraft.smithing_template.frost_metal_upgrade_smithing_template.base_slot_description");
    private static final Component UPGRADE_ADDITIONS_SLOT_DESCRIPTION = Component.translatable(
        "screen.anvilcraft.smithing_template.frost_metal_upgrade_smithing_template.additions_slot_description");
    private static final Identifier EMPTY_SLOT_PICKAXE =
        Identifier.withDefaultNamespace("container/slot/pickaxe");
    private static final Identifier EMPTY_SLOT_INGOT =
        Identifier.withDefaultNamespace("container/slot/ingot");
    private static final Identifier EMPTY_SLOT_HAMMER = AnvilCraft.of("item/empty_slot_hammer");
    private static final Identifier EMPTY_SLOT_BLOCK = AnvilCraft.of("item/empty_slot_block");

    public FrostMetalUpgradeTemplateItem(Properties properties) {
        super(
            FrostMetalUpgradeTemplateItem.APPLIES_TO,
            FrostMetalUpgradeTemplateItem.UPGRADE_INGREDIENTS,
            FrostMetalUpgradeTemplateItem.UPGRADE_BASE_SLOT_DESCRIPTION,
            FrostMetalUpgradeTemplateItem.UPGRADE_ADDITIONS_SLOT_DESCRIPTION,
            List.of(FrostMetalUpgradeTemplateItem.EMPTY_SLOT_PICKAXE, FrostMetalUpgradeTemplateItem.EMPTY_SLOT_HAMMER),
            List.of(FrostMetalUpgradeTemplateItem.EMPTY_SLOT_INGOT, FrostMetalUpgradeTemplateItem.EMPTY_SLOT_BLOCK),
            properties);
    }
}
