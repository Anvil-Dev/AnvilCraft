package dev.dubhe.anvilcraft.data.lang;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;

public class MessageLang {
    @SuppressWarnings("checkstyle:LineLength")
    public static void init(RegistrumLangProvider provider) {
        provider.add("message.anvilcraft.trading_station.break.player.title", "===|| Someone broke a trading station! ||===");
        provider.add("message.anvilcraft.trading_station.break.non_player.title", "===|| A trading station was broken! ||===");
        provider.add("message.anvilcraft.trading_station.break.owner", "Owner: %s");
        provider.add("message.anvilcraft.trading_station.break.breaker", "Breaker: %s");
        provider.add("message.anvilcraft.trading_station.break.pos", "Position: %1$d %2$d %3$d in %4$s");
        provider.add("message.anvilcraft.trading_station.break.time", "Time: %s");
        provider.add("message.anvilcraft.trading_station.break.onliners", "Online Players: ");
        provider.add("message.anvilcraft.trading_station.break.closest", "Closest Player: %s");
        provider.add("message.anvilcraft.hyperdimension_terminal.bound", "Terminal bound");
        provider.add("message.anvilcraft.hyperdimension_terminal.not_bound", "Terminal is not bound");
        provider.add("message.anvilcraft.hyperdimension_terminal.not_found", "Bound storage station not found");
        provider.add("message.anvilcraft.local_terminal.not_found", "No large crate within 32 blocks");
        provider.add("message.anvilcraft.shulker_terminal.not_found", "No shulker container or shulker box found");
    }
}
