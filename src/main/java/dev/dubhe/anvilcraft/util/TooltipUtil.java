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

    /**
     * 添加一段可能含换行的翻译 tooltip，先按 {@code \n} 拆成多条再逐条加入。
     *
     * <p>整条 {@link Component} 直接放进 tooltip 时，换行符只在原版按宽度折行时才会被处理：
     * 文本放得下时 {@code Font} 会把 {@code \n} 当作普通字符丢弃，显式换行随之失效。
     * 中文等无空格文本一旦被丢弃换行就会连成长句，因此这里主动拆行。</p>
     *
     * @param tooltip 目标 tooltip
     * @param color   文本颜色
     * @param key     翻译键
     * @param args    翻译参数
     */
    public static void addTranslatedLines(List<Component> tooltip, ChatFormatting color, String key, Object... args) {
        for (String line : Component.translatable(key, args).getString().split("\n")) {
            tooltip.add(Component.literal(line).withStyle(color));
        }
    }

    private static String getModName(String modId) {
        return ModList.get()
            .getModContainerById(modId)
            .map(ModContainer::getModInfo)
            .map(IModInfo::getDisplayName)
            .orElseGet(() -> StringUtils.capitalise(modId));
    }
}
