package dev.dubhe.anvilcraft.init.reicpe;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.ModRegistries;
import dev.dubhe.anvilcraft.recipe.anvil.trigger.IRecipeTrigger;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipeTriggers {
    public static final DeferredRegister<IRecipeTrigger> TRIGGER = DeferredRegister
        .create(ModRegistries.TRIGGER_REGISTRY, AnvilCraft.MOD_ID);

    public static final DeferredHolder<IRecipeTrigger, IRecipeTrigger> ON_ANVIL_FALL_ON = TRIGGER.register(
        "on_anvil_fall_on",
        IRecipeTrigger.Impl::new
    );
    public static final DeferredHolder<IRecipeTrigger, IRecipeTrigger> ITEM_INTO_BLOCK = TRIGGER.register(
        "item_into_block",
        IRecipeTrigger.Impl::new
    );
    public static final DeferredHolder<IRecipeTrigger, IRecipeTrigger> ON_ENDER_PEARL_TICK = TRIGGER.register(
        "on_ender_pearl_tick",
        IRecipeTrigger.Impl::new
    );
}
