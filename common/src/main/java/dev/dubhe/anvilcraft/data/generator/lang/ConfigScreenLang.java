package dev.dubhe.anvilcraft.data.generator.lang;

import com.tterrag.registrate.providers.RegistrateLangProvider;
import dev.dubhe.anvilcraft.AnvilCraft;

public class ConfigScreenLang {
    public static void init(RegistrateLangProvider provider) {
        provider.add("yacl3.config.%s.category.common".formatted(ID), "Common");
        provider.add("yacl3.config.%s.anvilEfficiency".formatted(ID), "Anvil Efficiency");
        provider.add("yacl3.config.%s.lightningStrikeDepth".formatted(ID), "Lightning Strike Depth");
        provider.add("yacl3.config.%s.magnetAttractsDistance".formatted(ID), "Magnet Attracts Distance");
        provider.add("yacl3.config.%s.magnetItemAttractsRadius".formatted(ID), "Magnet Item Attracts Radius");
        provider.add("yacl3.config.%s.redstoneEmpRadius".formatted(ID), "Redstone EMP Radius");
        provider.add("yacl3.config.%s.redstoneEmpMaxRadius".formatted(ID), "Redstone EMP Max Radius");
        provider.add("yacl3.config.%s.chuteMaxCooldown".formatted(ID), "Chute Max Cooldown");

        provider.add("config.anvilcraft.option.anvilEfficiency", "Maximum number of items processed by the anvil at the same time");
        provider.add("config.anvilcraft.option.lightningStrikeDepth", "Maximum depth a lightning strike can reach");
        provider.add("config.anvilcraft.option.magnetAttractsDistance", "Maximum distance a magnet attracts");
        provider.add("config.anvilcraft.option.magnetItemAttractsRadius", "Maximum radius a handheld magnet attracts");
        provider.add("config.anvilcraft.option.redstoneEmpRadius", "Redstone EMP distance generated per block dropped by the anvil");
        provider.add("config.anvilcraft.option.redstoneEmpMaxRadius", "Maximum distance of redstone EMP");
        provider.add("config.anvilcraft.option.chuteMaxCooldown", "Maximum cooldown time of chute (in ticks)");




    }

    private static final String ID = AnvilCraft.of("config").toString();
}
