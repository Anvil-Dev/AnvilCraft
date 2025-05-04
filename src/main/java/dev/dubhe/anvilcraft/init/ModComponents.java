package dev.dubhe.anvilcraft.init;

import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.api.item.IExtraItemDisplay;
import dev.dubhe.anvilcraft.api.item.property.Multiphase;
import dev.dubhe.anvilcraft.item.DiskItem;
import dev.dubhe.anvilcraft.item.HasMobBlockItem;
import dev.dubhe.anvilcraft.item.HeliostatsItem;
import dev.dubhe.anvilcraft.item.StructureToolItem;
import dev.dubhe.anvilcraft.item.amulet.ComradeAmuletItem;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Consumer;

public class ModComponents {

    public static final DeferredRegister<DataComponentType<?>> DR =
        DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, AnvilCraft.MOD_ID);

    public static final DataComponentType<DiskItem.DiskData> DISK_DATA =
        register("disk_data", b -> b.persistent(DiskItem.DiskData.CODEC)
            .networkSynchronized(DiskItem.DiskData.STREAM_CODEC));

    public static final DataComponentType<HasMobBlockItem.SavedEntity> SAVED_ENTITY =
        register("saved_entity", b -> b.persistent(HasMobBlockItem.SavedEntity.CODEC)
            .networkSynchronized(HasMobBlockItem.SavedEntity.STREAM_CODEC));

    public static final DataComponentType<HeliostatsItem.HeliostatsData> HELIOSTATS_DATA =
        register("heliostats_data", b -> b.persistent(HeliostatsItem.HeliostatsData.CODEC)
            .networkSynchronized(HeliostatsItem.HeliostatsData.STREAM_CODEC));

    public static final DataComponentType<StructureToolItem.StructureData> STRUCTURE_DATA =
        register("structure_data", b -> b.persistent(StructureToolItem.StructureData.CODEC)
            .networkSynchronized(StructureToolItem.StructureData.STREAM_CODEC));

    public static final DataComponentType<IExtraItemDisplay.StoredItem> DISPLAY_ITEM =
        register("display_item",
            b -> b.persistent(IExtraItemDisplay.StoredItem.CODEC)
                .networkSynchronized(IExtraItemDisplay.StoredItem.STREAM_CODEC));

    public static final DataComponentType<ComradeAmuletItem.SignedPlayers> SIGNED_PLAYERS =
        register("signed_player", b -> b.persistent(ComradeAmuletItem.SignedPlayers.CODEC)
            .networkSynchronized(ComradeAmuletItem.SignedPlayers.STREAM_CODEC));
  
    public static final DataComponentType<Integer> FLIGHT_TIME = register(
        "flight_time",
        it -> it.persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.INT)
    );

    public static final DataComponentType<Integer> TOTEM_COUNT = register(
        "totem_count", b -> b.persistent(Codec.INT)
            .networkSynchronized(ByteBufCodecs.INT)
    );

    public static final DataComponentType<Unit> FIRE_REFORGING = registerEmpty("reforging");

    public static final DataComponentType<Multiphase> MULTIPHASE =
        register("multiphase", b -> b.persistent(Multiphase.CODEC)
            .networkSynchronized(Multiphase.STREAM_CODEC));

    public static final DataComponentType<Unit> MERCILESS = registerEmpty("merciless");

    private static <T> DataComponentType<T> register(String name, Consumer<DataComponentType.Builder<T>> customizer) {
        var builder = DataComponentType.<T>builder();
        customizer.accept(builder);
        var componentType = builder.build();
        DR.register(name, () -> componentType);
        return componentType;
    }

    private static DataComponentType<Unit> registerEmpty(String name) {
        return register(name, b -> b.persistent(Codec.EMPTY.codec())
            .networkSynchronized(StreamCodec.unit(Unit.INSTANCE)));
    }

    public static void register(IEventBus bus) {
        DR.register(bus);
    }
}
