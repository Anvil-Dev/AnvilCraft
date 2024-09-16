package dev.dubhe.anvilcraft.api.event.item;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

@Getter
public class UseMagnetEvent extends Event implements ICancellableEvent {
    private final Level level;
    @Setter
    private double attractRadius;
    private final Player player;

    public UseMagnetEvent(Level level, Player player, double attractRadius) {
        this.level = level;
        this.attractRadius = attractRadius;
        this.player = player;
    }
}
