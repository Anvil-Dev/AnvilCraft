package dev.dubhe.anvilcraft.saved.trading;

import net.minecraft.network.chat.Component;

import java.util.UUID;
import java.util.function.Function;

public interface ITradingStationMessage {
    UUID owner();

    Component getRealTimeMessage(Function<UUID, Component> getter);

    Component getOwnerMessage(Function<UUID, Component> getter);
}
