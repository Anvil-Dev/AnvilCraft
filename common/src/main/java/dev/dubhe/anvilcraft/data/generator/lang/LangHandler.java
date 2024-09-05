package dev.dubhe.anvilcraft.data.generator.lang;

import dev.anvilcraft.lib.data.provider.LanguageProvider;

public class LangHandler {
    /**
     * 语言文件初始化
     *
     * @param provider 提供器
     */
    public static void init(LanguageProvider provider) {
        AdvancementLang.init(provider);
        ConfigScreenLang.init(provider);
        ItemTooltipLang.init(provider);
        JadeLang.init(provider);
        WthitLang.init(provider);
        OtherLang.init(provider);
        PatchouliLang.init(provider);
        ScreenLang.init(provider);
        EmiLang.init(provider);
    }
}
