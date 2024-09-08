package dev.dubhe.anvilcraft.data.generator.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;
import org.jetbrains.annotations.NotNull;

public class PatchouliLang {

    /**
     * 初始化 Patchouli 语言
     *
     * @param provider 提供器
     */
    public static void init(@NotNull RegistrateLangProvider provider) {
        provider.add("message.anvilcraft.need_patchouli_installed", "Patchouli needs to be installed");

        provider.add("patchouli.anvilcraft.landing_text", "Welcome to AnvilCraft");
    }
}
