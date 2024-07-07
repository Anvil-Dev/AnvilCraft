package dev.dubhe.anvilcraft.data.generator.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;
import dev.dubhe.anvilcraft.event.TooltipEventListener;
import org.jetbrains.annotations.NotNull;

public class ItemTooltipLang {
    /**
     * 初始化物品 tooltip 语言
     *
     * @param provider 提供器
     */
    public static void init(@NotNull RegistrateLangProvider provider) {
        TooltipEventListener.NEED_TOOLTIP_ITEM.forEach(
            (item, s) -> provider.add(TooltipEventListener.getTranslationKey(item), s)
        );
        provider.add("tooltip.anvilcraft.item.reinforced_concrete", "Creeper proof");
        provider.add("tooltip.anvilcraft.item.recipe.processing.chance", "%1$s%% Chance");
    }
}
