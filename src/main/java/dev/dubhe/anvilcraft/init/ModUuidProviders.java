package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.uuid.CreateOnFirstUuidProvider;
import dev.dubhe.anvilcraft.api.uuid.DirectUuidProvider;
import dev.dubhe.anvilcraft.api.uuid.IUuidProvider;
import dev.dubhe.anvilcraft.api.uuid.NoUuidProvider;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModUuidProviders {
    private static final DeferredRegister<IUuidProvider.Type<?>> REGISTER = DeferredRegister.create(
        ModRegistries.UUID_PROVIDER_TYPE_KEY,
        AnvilCraft.MOD_ID
    );

    public static final DeferredHolder<IUuidProvider.Type<?>, DirectUuidProvider.Type> DIRECT = REGISTER.register(
        "direct",
        DirectUuidProvider.Type::new
    );

    public static final DeferredHolder<IUuidProvider.Type<?>, CreateOnFirstUuidProvider.Type> CREATE_ON_FIRST = REGISTER.register(
        "create_on_first",
        CreateOnFirstUuidProvider.Type::new
    );

    public static final DeferredHolder<IUuidProvider.Type<?>, NoUuidProvider.Type> NO = REGISTER.register(
        "no",
        NoUuidProvider.Type::new
    );

    public static void register(IEventBus modEventBus) {
        REGISTER.register(modEventBus);
    }
}
