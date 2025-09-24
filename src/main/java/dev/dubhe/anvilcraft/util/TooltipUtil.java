package dev.dubhe.anvilcraft.util;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;
import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class TooltipUtil {
    public static List<Component> tooltip(Block block) {
        List<Component> tooltip = new ArrayList<>();
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
        tooltip.add(block.getName());
        tooltip.add(Component.literal(key.toString()).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal(getModName(key.getNamespace())).withStyle(ChatFormatting.BLUE).withStyle(Style.EMPTY.withItalic(true)));
        return tooltip;
    }

    public static List<Component> recipeIDTooltip(Block block, ResourceLocation id) {
        List<Component> tooltip = new ArrayList<>();
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
        tooltip.add(block.getName());
        tooltip.add(Component.literal(key.toString()).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("jei.tooltip.recipe.id", id.toString()).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.literal(getModName(key.getNamespace())).withStyle(ChatFormatting.BLUE).withStyle(Style.EMPTY.withItalic(true)));
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
