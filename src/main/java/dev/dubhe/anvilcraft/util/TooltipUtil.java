package dev.dubhe.anvilcraft.util;

import dev.dubhe.anvilcraft.recipe.transform.TransformResult;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;
import org.checkerframework.checker.units.qual.C;
import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class TooltipUtil {
    public static List<Component> tooltip(Block block) {
        List<Component> tooltip = new ArrayList<>();
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
        tooltip.add(block.getName());
        tooltip.add(Component.literal(key.toString()).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(
            Component.literal(getModName(key.getNamespace())).withStyle(ChatFormatting.BLUE).withStyle(Style.EMPTY.withItalic(true))
        );
        return tooltip;
    }

    public static List<Component> recipeIDTooltip(Block block, ResourceLocation id) {
        List<Component> tooltip = new ArrayList<>();
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
        tooltip.add(block.getName());
        tooltip.add(Component.literal(key.toString()).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("jei.tooltip.recipe.id", id.toString()).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(
            Component.literal(getModName(key.getNamespace())).withStyle(ChatFormatting.BLUE).withStyle(Style.EMPTY.withItalic(true))
        );
        return tooltip;
    }

    public static List<Component> recipeIDTooltip(ResourceLocation id) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable("jei.tooltip.recipe.id", id.toString()).withStyle(ChatFormatting.DARK_GRAY));
        return tooltip;
    }

    public static List<Component> entityTypeTooltip(EntityType<?> entityType) {
        List<Component> tooltip = new ArrayList<>();
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
        tooltip.add(entityType.getDescription());
        tooltip.add(Component.literal(key.toString()).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(
            Component.literal(getModName(key.getNamespace())).withStyle(ChatFormatting.BLUE).withStyle(Style.EMPTY.withItalic(true))
        );
        return tooltip;
    }

    public static List<Component> transListTooltip(List<TransformResult> transList) {
        List<Component> tooltip = new ArrayList<>();
        for (TransformResult result : transList) {
            EntityType<?> entityType = result.resultEntityType();
            ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entityType);
            tooltip.add(entityType.getDescription());
            tooltip.add(Component.translatable("gui.anvilcraft.category.chance", result.probability() * 100).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal(key.toString()).withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(
                Component.literal(getModName(key.getNamespace())).withStyle(ChatFormatting.BLUE).withStyle(Style.EMPTY.withItalic(true))
            );
        }
        return tooltip;
    }

    private static String getModName(String modId) {
        return ModList.get()
            .getModContainerById(modId)
            .map(ModContainer::getModInfo)
            .map(IModInfo::getDisplayName)
            .orElseGet(() -> StringUtils.capitalise(modId));
    }
}
