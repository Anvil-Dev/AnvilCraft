package dev.dubhe.anvilcraft.init.recipe;

import dev.anvilcraft.lib.recipe.init.LibRegistries;
import dev.anvilcraft.lib.recipe.trigger.IRecipeTrigger;
import dev.dubhe.anvilcraft.AnvilCraft;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipeTriggers {
    public static final DeferredRegister<IRecipeTrigger> TRIGGER = DeferredRegister
        .create(LibRegistries.TRIGGER_REGISTRY, AnvilCraft.MOD_ID);

    public static final DeferredHolder<IRecipeTrigger, IRecipeTrigger> ON_ANVIL_FALL_ON = TRIGGER.register(
        "on_anvil_fall_on",
        IRecipeTrigger.Impl::new
    );
}
