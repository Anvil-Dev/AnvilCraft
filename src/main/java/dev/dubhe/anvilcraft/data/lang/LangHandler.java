package dev.dubhe.anvilcraft.data.lang;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;

public class LangHandler {
    public static void init(RegistrumLangProvider provider) {
        AdvancementLang.init(provider);
        ConfigScreenLang.init(provider);
        ItemTooltipLang.init(provider);
        JadeLang.init(provider);
        WthitLang.init(provider);
        OtherLang.init(provider);
        PatchouliLang.init(provider);
        ScreenLang.init(provider);
        JeiLang.init(provider);
        EnchantmentDescriptionsLang.init(provider);
        CuriosLang.init(provider);
        ToolPropertyLang.init(provider);
        CommandLang.init(provider);
        KeyMappingLang.init(provider);
        FluidLang.init(provider);
        IntegrationScreenLang.init(provider);
        ScreenTooltipLang.init(provider);
    }
}
