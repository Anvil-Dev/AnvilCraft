package dev.dubhe.anvilcraft.api.event;

import lombok.Getter;
import net.neoforged.bus.api.Event;

@Getter
public class CheckIntegrationLoadedEvent extends Event {
    private final String id;
    private boolean loaded = false;

    public CheckIntegrationLoadedEvent(String id) {
        this.id = id;
    }

    public void setLoaded() {
        this.loaded = true;
    }
}
