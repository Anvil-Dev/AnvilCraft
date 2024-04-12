package dev.dubhe.anvilcraft.data.generator.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.config.AnvilCraftConfig;
import dev.dubhe.anvilcraft.util.FormattingUtil;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;

import java.lang.reflect.Field;


public class ConfigScreenLang {
    private static final String OPTION_STRING = "text.autoconfig.%s.option.%s";
    private static final String OPTION_TOOLTIP_STRING = "text.autoconfig.%s.option.%s.@Tooltip";

    public static void init(RegistrateLangProvider provider) {
        provider.add("text.autoconfig.anvilcraft.title", "AnvilCraft Config");

        readConfigClass(AnvilCraftConfig.class, provider);
    }

    private static void readConfigClass(Class<? extends ConfigData> configClass, RegistrateLangProvider provider) {
        for (Field field : configClass.getDeclaredFields()) {
            String name = field.getName();
            provider.add(OPTION_STRING.formatted(AnvilCraft.MOD_ID, name), FormattingUtil.toEnglishName(FormattingUtil.toLowerCaseUnder(name)));
            if (field.isAnnotationPresent(Comment.class)) {
                Comment comment = field.getAnnotation(Comment.class);
                provider.add(OPTION_TOOLTIP_STRING.formatted(AnvilCraft.MOD_ID, name), comment.value());
            }
        }
    }
}
