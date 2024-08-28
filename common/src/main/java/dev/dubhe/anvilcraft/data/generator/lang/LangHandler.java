package dev.dubhe.anvilcraft.data.generator.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;

public class LangHandler {
    /**
     * 语言文件初始化
     *
     * @param provider 提供器
     */
    public static void init(RegistrateLangProvider provider) {
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
