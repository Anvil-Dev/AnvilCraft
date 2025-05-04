package dev.dubhe.anvilcraft.item.template;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.SmithingTemplateItem;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class FrostMetalUpgradeTemplateItem extends SmithingTemplateItem {

    private static final ChatFormatting TITLE_FORMAT = ChatFormatting.GRAY;
    private static final ChatFormatting DESCRIPTION_FORMAT = ChatFormatting.BLUE;
    private static final Component APPLIES_TO = Component.translatable(
            "screen.anvilcraft.smithing_template.frost_metal_upgrade_smithing_template.applies_to")
        .withStyle(DESCRIPTION_FORMAT);
    private static final Component UPGRADE_INGREDIENTS =
        Component.translatable("screen.anvilcraft.smithing_template.frost_metal_upgrade_smithing_template"
                + ".upgrade_ingredients")
            .withStyle(DESCRIPTION_FORMAT);
    private static final Component UPGRADE = Component.translatable(
            "screen.anvilcraft.frost_metal_upgrade_smithing_template")
        .withStyle(TITLE_FORMAT);
    private static final Component UPGRADE_BASE_SLOT_DESCRIPTION = Component.translatable(
        "screen.anvilcraft.smithing_template.frost_metal_upgrade_smithing_template.base_slot_description");
    private static final Component UPGRADE_ADDITIONS_SLOT_DESCRIPTION = Component.translatable(
        "screen.anvilcraft.smithing_template.frost_metal_upgrade_smithing_template.additions_slot_description");
    private static final ResourceLocation EMPTY_SLOT_PICKAXE =
        ResourceLocation.withDefaultNamespace("item/empty_slot_pickaxe");
    private static final ResourceLocation EMPTY_SLOT_INGOT =
        ResourceLocation.withDefaultNamespace("item/empty_slot_ingot");

    /**
     * @param properties 物品属性
     */
    public FrostMetalUpgradeTemplateItem(@SuppressWarnings("unused") Properties properties) {
        super(
            APPLIES_TO,
            UPGRADE_INGREDIENTS,
            UPGRADE,
            UPGRADE_BASE_SLOT_DESCRIPTION,
            UPGRADE_ADDITIONS_SLOT_DESCRIPTION,
            List.of(EMPTY_SLOT_PICKAXE),
            List.of(EMPTY_SLOT_INGOT));
    }

    @Override
    public @NotNull String getDescriptionId() {
        return this.getOrCreateDescriptionId();
    }
}
