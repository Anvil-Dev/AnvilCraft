package dev.dubhe.anvilcraft.item.template;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.SmithingTemplateItem;

import java.util.List;

public class EmberMetalUpgradeTemplateItem extends SmithingTemplateItem {
    private static final ChatFormatting TITLE_FORMAT = ChatFormatting.GRAY;
    private static final ChatFormatting DESCRIPTION_FORMAT = ChatFormatting.BLUE;
    private static final Component APPLIES_TO = Component.translatable(
        "screen.anvilcraft.smithing_template.ember_metal_upgrade_smithing_template.applies_to"
    ).withStyle(EmberMetalUpgradeTemplateItem.DESCRIPTION_FORMAT);
    private static final Component UPGRADE_INGREDIENTS =
        Component.translatable("screen.anvilcraft.smithing_template.ember_metal_upgrade_smithing_template"
                + ".upgrade_ingredients")
            .withStyle(EmberMetalUpgradeTemplateItem.DESCRIPTION_FORMAT);
    private static final Component UPGRADE = Component.translatable(
            "screen.anvilcraft.ember_metal_upgrade_smithing_template")
        .withStyle(EmberMetalUpgradeTemplateItem.TITLE_FORMAT);
    private static final Component UPGRADE_BASE_SLOT_DESCRIPTION = Component.translatable(
        "screen.anvilcraft.smithing_template.ember_metal_upgrade_smithing_template.base_slot_description");
    private static final Component UPGRADE_ADDITIONS_SLOT_DESCRIPTION = Component.translatable(
        "screen.anvilcraft.smithing_template.ember_metal_upgrade_smithing_template.additions_slot_description");
    private static final Identifier EMPTY_SLOT_PICKAXE =
        Identifier.withDefaultNamespace("container/slot/pickaxe");
    private static final Identifier EMPTY_SLOT_INGOT =
        Identifier.withDefaultNamespace("container/slot/ingot");
    private static final Identifier EMPTY_SLOT_HAMMER = AnvilCraft.of("item/empty_slot_hammer");
    private static final Identifier EMPTY_SLOT_BLOCK = AnvilCraft.of("item/empty_slot_block");

    public EmberMetalUpgradeTemplateItem(Properties properties) {
        super(
            EmberMetalUpgradeTemplateItem.APPLIES_TO,
            EmberMetalUpgradeTemplateItem.UPGRADE_INGREDIENTS,
            EmberMetalUpgradeTemplateItem.UPGRADE_BASE_SLOT_DESCRIPTION,
            EmberMetalUpgradeTemplateItem.UPGRADE_ADDITIONS_SLOT_DESCRIPTION,
            List.of(EmberMetalUpgradeTemplateItem.EMPTY_SLOT_PICKAXE, EmberMetalUpgradeTemplateItem.EMPTY_SLOT_HAMMER),
            List.of(EmberMetalUpgradeTemplateItem.EMPTY_SLOT_INGOT, EmberMetalUpgradeTemplateItem.EMPTY_SLOT_BLOCK),
            properties);
    }
}
