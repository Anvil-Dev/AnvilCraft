package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.anvil.IRecipeTrigger;
import dev.dubhe.anvilcraft.recipe.anvil.trigger.OnAnvilFallOn;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipeTriggers {
    public static final DeferredRegister<IRecipeTrigger> TRIGGER = DeferredRegister
        .create(ModRegistries.TRIGGER_REGISTRY, AnvilCraft.MOD_ID);

    public static final DeferredHolder<IRecipeTrigger, OnAnvilFallOn> ON_ANVIL_FALL_ON = TRIGGER.register("on_anvil_fall_on", OnAnvilFallOn::new);
}
