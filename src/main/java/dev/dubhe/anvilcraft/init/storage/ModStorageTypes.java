package dev.dubhe.anvilcraft.init.storage;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.registry.ModRegistryKeys;
import dev.dubhe.anvilcraft.saved.storage.BaseStorage;
import dev.dubhe.anvilcraft.saved.storage.CrateStorage;
import dev.dubhe.anvilcraft.saved.storage.HyperdimensionStorage;
import dev.dubhe.anvilcraft.saved.storage.IStorageType;
import dev.dubhe.anvilcraft.saved.storage.LargeCrateStorage;
import dev.dubhe.anvilcraft.saved.storage.ShulkerContainerStorage;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.UUID;
import java.util.function.Function;

public class ModStorageTypes {
    private static final DeferredRegister<IStorageType<?>> REGISTER = DeferredRegister.create(
        ModRegistryKeys.STORAGE_TYPE,
        AnvilCraft.MOD_ID
    );

    public static final DeferredHolder<IStorageType<?>, IStorageType<CrateStorage>> CRATE = ModStorageTypes.of(
        "crate",
        CrateStorage::new,
        CrateStorage.class
    );
    public static final DeferredHolder<IStorageType<?>, IStorageType<LargeCrateStorage>> LARGE_CRATE = ModStorageTypes.of(
        "large_crate",
        LargeCrateStorage::new,
        LargeCrateStorage.class
    );
    public static final DeferredHolder<IStorageType<?>, IStorageType<ShulkerContainerStorage>> SHULKER_CONTAINER = ModStorageTypes.of(
        "shulker_container",
        ShulkerContainerStorage::new,
        ShulkerContainerStorage.class
    );
    public static final DeferredHolder<IStorageType<?>, IStorageType<HyperdimensionStorage>> HYPERDIMENSION = ModStorageTypes.of(
        "hyperdimension",
        HyperdimensionStorage::new,
        HyperdimensionStorage.class
    );

    private static <T extends BaseStorage<?>> DeferredHolder<IStorageType<?>, IStorageType<T>> of(
        String id,
        Function<UUID, T> factory,
        Class<T> clazz
    ) {
        DeferredHolder<IStorageType<?>, IStorageType<T>> holder = ModStorageTypes.REGISTER.register(id, () -> new IStorageType<>() {
            @Override
            public T newInstance(UUID uuid) {
                return factory.apply(uuid);
            }

            @Override
            public Class<T> clazz() {
                return clazz;
            }
        }
        );
        IStorageType.CLASS_MAP.put(clazz, holder);
        return holder;
    }

    public static void register(IEventBus modEventBus) {
        ModStorageTypes.REGISTER.register(modEventBus);
    }
}
