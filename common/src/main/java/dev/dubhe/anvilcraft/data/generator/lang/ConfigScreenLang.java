package dev.dubhe.anvilcraft.data.generator.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;
import dev.dubhe.anvilcraft.AnvilCraft;


public class ConfigScreenLang {
    private static final String OPTION_STRING = "text.autoconfig.%s.option.%s";
    private static final String OPTION_TOOLTIP_STRING = "text.autoconfig.%s.option.%s.@Tooltip";

    public static void init(RegistrateLangProvider provider) {
        provider.add("text.autoconfig.anvilcraft.title", "AnvilCraft Config");

        provider.add(OPTION_STRING.formatted(AnvilCraft.MOD_ID, "anvilEfficiency"), "Anvil Efficiency");
        provider.add(OPTION_TOOLTIP_STRING.formatted(AnvilCraft.MOD_ID, "anvilEfficiency"), "Maximum number of items processed by the anvil at the same time");

        provider.add(OPTION_STRING.formatted(AnvilCraft.MOD_ID, "lightningStrikeDepth"), "Lightning Strike Depth");
        provider.add(OPTION_TOOLTIP_STRING.formatted(AnvilCraft.MOD_ID, "lightningStrikeDepth"), "Maximum depth a lightning strike can reach");

        provider.add(OPTION_STRING.formatted(AnvilCraft.MOD_ID, "magnetAttractsDistance"), "Magnet Attracts Distance");
        provider.add(OPTION_TOOLTIP_STRING.formatted(AnvilCraft.MOD_ID, "magnetAttractsDistance"), "Maximum radius a handheld magnet attracts");

        provider.add(OPTION_STRING.formatted(AnvilCraft.MOD_ID, "magnetItemAttractsRadius"), "Magnet Item Attracts Radius");
        provider.add(OPTION_TOOLTIP_STRING.formatted(AnvilCraft.MOD_ID, "magnetItemAttractsRadius"), "Maximum radius a handheld magnet attracts");

        provider.add(OPTION_STRING.formatted(AnvilCraft.MOD_ID, "redstoneEmpRadius"), "Redstone EMP Radius");
        provider.add(OPTION_TOOLTIP_STRING.formatted(AnvilCraft.MOD_ID, "redstoneEmpRadius"), "Redstone EMP distance generated per block dropped by the anvil");

        provider.add(OPTION_STRING.formatted(AnvilCraft.MOD_ID, "redstoneEmpMaxRadius"), "Redstone EMP Max Radius");
        provider.add(OPTION_TOOLTIP_STRING.formatted(AnvilCraft.MOD_ID, "redstoneEmpMaxRadius"), "Maximum distance of redstone EMP");

        provider.add(OPTION_STRING.formatted(AnvilCraft.MOD_ID, "chuteMaxCooldown"), "Chute Max Cooldown");
        provider.add(OPTION_TOOLTIP_STRING.formatted(AnvilCraft.MOD_ID, "chuteMaxCooldown"), "Maximum distance of chute");
    }
}
