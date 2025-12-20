package dev.dubhe.anvilcraft.data.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;

public class LangHandler {
    public static void init(RegistrateLangProvider provider) {
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
        CategoryLang.init(provider);
    }
}
