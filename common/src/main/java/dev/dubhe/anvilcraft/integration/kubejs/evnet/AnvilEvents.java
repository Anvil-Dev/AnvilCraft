package dev.dubhe.anvilcraft.integration.kubejs.evnet;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

/**
 * kubeJs的铁砧工艺事件
 */
public interface AnvilEvents {
    EventGroup GROUP = EventGroup.of("AnvilEvents");
    EventHandler LOAD_ANVIL_CRAFTING_RECIPES =
            GROUP.server("load_anvil_crafting_recipes", () -> LoadAnvilCraftingRecipeEvent.class);
}
