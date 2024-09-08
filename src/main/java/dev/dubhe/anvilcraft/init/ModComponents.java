package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.item.DiskItem;
import dev.dubhe.anvilcraft.item.HasMobBlockItem;
import dev.dubhe.anvilcraft.item.HeliostatsItem;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Consumer;

public class ModComponents {

    public static final DeferredRegister<DataComponentType<?>> DR = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, AnvilCraft.MOD_ID);

    public static final DataComponentType<DiskItem.DiskData> DISK_DATA = register("disk_data", b -> b
            .persistent(DiskItem.DiskData.CODEC)
            .networkSynchronized(DiskItem.DiskData.STREAM_CODEC)
    );

    public static final DataComponentType<HasMobBlockItem.SavedEntity> SAVED_ENTITY = register("saved_entity", b -> b
            .persistent(HasMobBlockItem.SavedEntity.CODEC)
            .networkSynchronized(HasMobBlockItem.SavedEntity.STREAM_CODEC)
    );

    public static final DataComponentType<HeliostatsItem.HeliostatsData> HELIOSTATS_DATA = register("heliostats_data", b -> b
            .persistent(HeliostatsItem.HeliostatsData.CODEC)
            .networkSynchronized(HeliostatsItem.HeliostatsData.STREAM_CODEC)
    );

    private static <T> DataComponentType<T> register(
            String name, Consumer<DataComponentType.Builder<T>> customizer) {
        var builder = DataComponentType.<T>builder();
        customizer.accept(builder);
        var componentType = builder.build();
        DR.register(name, () -> componentType);
        return componentType;
    }

    public static void register(IEventBus bus) {
        DR.register(bus);
    }
}
