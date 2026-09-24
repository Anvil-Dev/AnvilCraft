package dev.dubhe.anvilcraft.data.lang;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;

public class MessageLang {
    public static void init(RegistrumLangProvider provider) {
        provider.add("book.anvilcraft.monolith.title", "Celestial Knowledge");
        provider.add("book.anvilcraft.monolith.page",
            "[%1$s]\nTime Anvils: %2$s\nSpace Anvils: %3$s\nMass Anvils: %4$s\nEnergy Anvils: %5$s\nSeed Item: %6$s");
        provider.add("hud.anvilcraft.weatherproof_chestplate_power", "%s%%");
        provider.add("message.anvilcraft.buffer_boots.charged", "Press %1$s to jump");

        provider.add("item.anvilcraft.hyperdimension_terminal.bound", "Bound to a Hyperdimension Storage Station");
        provider.add("item.anvilcraft.hyperdimension_terminal.unbound", "Right-click a Hyperdimension Storage Station to bind");
        provider.add("message.anvilcraft.hyperdimension_terminal.bound", "Terminal bound");
        provider.add("message.anvilcraft.hyperdimension_terminal.not_bound", "Terminal is not bound");
        provider.add("message.anvilcraft.hyperdimension_terminal.not_found", "Bound storage station not found");
        provider.add("message.anvilcraft.local_terminal.not_found", "No large crate within 32 blocks");
        provider.add("message.anvilcraft.shulker_terminal.not_found", "No shulker container found");
        provider.add("screen.anvilcraft.balance_mode.deposit", "Deposit only");
        provider.add("screen.anvilcraft.balance_mode.off", "Off");
        provider.add("screen.anvilcraft.balance_mode.restock", "Restock only");
        provider.add("screen.anvilcraft.balance_mode.smart", "Smart");
        provider.add(
            "message.anvilcraft.trading_station.break.player.title",
            "===|| Someone broke a trading station! ||==="
        );
        provider.add(
            "message.anvilcraft.trading_station.break.non_player.title",
            "===|| A trading station was broken! ||==="
        );
        provider.add("message.anvilcraft.trading_station.break.owner", "Owner: %s");
        provider.add("message.anvilcraft.trading_station.break.breaker", "Breaker: %s");
        provider.add(
            "message.anvilcraft.trading_station.break.pos",
            "Position: %1$d %2$d %3$d in %4$s"
        );
        provider.add("message.anvilcraft.trading_station.break.time", "Time: %s");
        provider.add("message.anvilcraft.trading_station.break.onliners", "Online Players: ");
        provider.add("message.anvilcraft.trading_station.break.closest", "Closest Player: %s");
    }
}
