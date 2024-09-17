package dev.dubhe.anvilcraft.api.event.recipe;

import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

import lombok.Getter;

@Getter
public class RecipeReloadEvent extends Event implements IModBusEvent {
    public RecipeReloadEvent() {}
}
