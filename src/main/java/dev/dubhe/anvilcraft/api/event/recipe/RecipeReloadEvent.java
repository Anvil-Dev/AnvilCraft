package dev.dubhe.anvilcraft.api.event.recipe;

import lombok.Getter;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

@Getter
public class RecipeReloadEvent extends Event implements IModBusEvent {
    public RecipeReloadEvent() {}
}
