package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.recipe.multiple.result.modifier.ApplyData;
import dev.dubhe.anvilcraft.recipe.multiple.result.modifier.CopyData;
import dev.dubhe.anvilcraft.recipe.multiple.result.modifier.IResultModifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModResultModifierTypes {
    private static final DeferredRegister<IResultModifier.Type<?>> DF = DeferredRegister
        .create(ModRegistries.MODIFIER_TYPE_REGISTRY, AnvilCraft.MOD_ID);

    public static final DeferredHolder<IResultModifier.Type<?>, ApplyData.Type> APPLY_DATA = DF
        .register("apply_data", ApplyData.Type::new);

    public static final DeferredHolder<IResultModifier.Type<?>, CopyData.Type> COPY_DATA = DF
        .register("copy_data", CopyData.Type::new);

    public static void register(IEventBus bus) {
        DF.register(bus);
    }
}
