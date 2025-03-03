package dev.dubhe.anvilcraft.item;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.SmithingTemplateItem;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class EmberMetalUpgradeTemplateItem extends SmithingTemplateItem {

    private static final ChatFormatting TITLE_FORMAT = ChatFormatting.GRAY;
    private static final ChatFormatting DESCRIPTION_FORMAT = ChatFormatting.BLUE;
    private static final Component APPLIES_TO = Component.translatable(
            "screen.anvilcraft.smithing_template.ember_metal_upgrade_smithing_template.applies_to")
        .withStyle(DESCRIPTION_FORMAT);
    private static final Component UPGRADE_INGREDIENTS =
        Component.translatable("screen.anvilcraft.smithing_template.ember_metal_upgrade_smithing_template"
                + ".upgrade_ingredients")
            .withStyle(DESCRIPTION_FORMAT);
    private static final Component UPGRADE = Component.translatable(
            "screen.anvilcraft.ember_metal_upgrade_smithing_template")
        .withStyle(TITLE_FORMAT);
    private static final Component UPGRADE_BASE_SLOT_DESCRIPTION = Component.translatable(
        "screen.anvilcraft.smithing_template.ember_metal_upgrade_smithing_template.base_slot_description");
    private static final Component UPGRADE_ADDITIONS_SLOT_DESCRIPTION = Component.translatable(
        "screen.anvilcraft.smithing_template.ember_metal_upgrade_smithing_template.additions_slot_description");
    private static final ResourceLocation EMPTY_SLOT_PICKAXE =
        ResourceLocation.withDefaultNamespace("item/empty_slot_pickaxe");
    private static final ResourceLocation EMPTY_SLOT_INGOT =
        ResourceLocation.withDefaultNamespace("item/empty_slot_ingot");
    private static final ResourceLocation EMPTY_SLOT_HAMMER = AnvilCraft.of("item/empty_slot_hammer");
    private static final ResourceLocation EMPTY_SLOT_BLOCK = AnvilCraft.of("item/empty_slot_block");

    /**
     * @param properties 物品属性
     */
    public EmberMetalUpgradeTemplateItem(@SuppressWarnings("unused") Properties properties) {
        super(
            APPLIES_TO,
            UPGRADE_INGREDIENTS,
            UPGRADE,
            UPGRADE_BASE_SLOT_DESCRIPTION,
            UPGRADE_ADDITIONS_SLOT_DESCRIPTION,
            List.of(EMPTY_SLOT_PICKAXE, EMPTY_SLOT_HAMMER),
            List.of(EMPTY_SLOT_INGOT, EMPTY_SLOT_BLOCK));
    }

    @Override
    public @NotNull String getDescriptionId() {
        return this.getOrCreateDescriptionId();
    }
}
