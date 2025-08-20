package dev.dubhe.anvilcraft.integration.ponder.data;

import com.tterrag.registrate.providers.RegistrateLangProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.createmod.ponder.foundation.PonderIndex;
import org.jetbrains.annotations.NotNull;

public class PonderLangHandler {
    public static void init(@NotNull RegistrateLangProvider provider) {
        PonderIndex.getLangAccess().provideLang(AnvilCraft.MOD_ID, provider::add);
    }
}
