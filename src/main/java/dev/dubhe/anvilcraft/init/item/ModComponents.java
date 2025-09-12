package dev.dubhe.anvilcraft.init.item;

import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.item.property.component.BoxContents;
import dev.dubhe.anvilcraft.item.property.component.DiskData;
import dev.dubhe.anvilcraft.item.property.component.Eternal;
import dev.dubhe.anvilcraft.item.property.component.FilterContent;
import dev.dubhe.anvilcraft.item.property.component.HeliostatsData;
import dev.dubhe.anvilcraft.item.property.component.Merciless;
import dev.dubhe.anvilcraft.item.property.component.Multiphase;
import dev.dubhe.anvilcraft.item.property.component.Providence;
import dev.dubhe.anvilcraft.item.property.component.SavedEntity;
import dev.dubhe.anvilcraft.item.property.component.SignedPlayers;
import dev.dubhe.anvilcraft.item.property.component.StoredItem;
import dev.dubhe.anvilcraft.item.property.component.StructureData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class ModComponents {
    public static final DeferredRegister<DataComponentType<?>> DR = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, AnvilCraft.MOD_ID);

    public static final DataComponentType<DiskData> DISK_DATA = register(
        "disk_data",
        b -> b.persistent(DiskData.CODEC).networkSynchronized(DiskData.STREAM_CODEC)
    );

    public static final DataComponentType<SavedEntity> SAVED_ENTITY = register(
        "saved_entity",
        b -> b.persistent(SavedEntity.CODEC).networkSynchronized(SavedEntity.STREAM_CODEC)
    );

    public static final DataComponentType<HeliostatsData> HELIOSTATS_DATA = register(
        "heliostats_data",
        b -> b.persistent(HeliostatsData.CODEC).networkSynchronized(HeliostatsData.STREAM_CODEC)
    );

    public static final DataComponentType<StructureData> STRUCTURE_DATA = register(
        "structure_data",
        b -> b.persistent(StructureData.CODEC).networkSynchronized(StructureData.STREAM_CODEC)
    );

    public static final DataComponentType<StoredItem> DISPLAY_ITEM = register(
        "display_item",
        b -> b.persistent(StoredItem.CODEC).networkSynchronized(StoredItem.STREAM_CODEC)
    );

    public static final DataComponentType<SignedPlayers> SIGNED_PLAYERS = register(
        "signed_player",
        b -> b.persistent(SignedPlayers.CODEC).networkSynchronized(SignedPlayers.STREAM_CODEC)
    );

    public static final DataComponentType<Integer> FLIGHT_TIME = register(
        "flight_time",
        it -> it.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT)
    );

    public static final DataComponentType<Integer> STORED_ENERGY = register(
        "stored_energy",
        (builder) -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT)
    );

    public static final DataComponentType<Unit> FIRE_REFORGING = registerEmpty("reforging");

    public static final DataComponentType<Multiphase> MULTIPHASE = register(
        "multiphase",
        b -> b.persistent(Multiphase.CODEC).networkSynchronized(Multiphase.STREAM_CODEC)
    );

    public static final DataComponentType<Merciless> MERCILESS = register(
        "merciless",
        b -> b.persistent(Merciless.CODEC).networkSynchronized(Merciless.STREAM_CODEC)
    );

    public static final DataComponentType<Integer> DEVOUR_RANGE = register(
        "devour_range",
        b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT)
    );

    public static final DataComponentType<BoxContents> BOX_CONTENTS = register(
        "box_contents",
        b -> b.persistent(BoxContents.CODEC).networkSynchronized(BoxContents.STREAM_CODEC)
    );

    public static final DataComponentType<Eternal> ETERNAL = register(
        "eternal",
        b -> b.persistent(Eternal.CODEC).networkSynchronized(Eternal.STREAM_CODEC)
    );

    public static final DataComponentType<Providence> PROVIDENCE = register(
        "providence",
        b -> b.persistent(Providence.CODEC).networkSynchronized(Providence.STREAM_CODEC)
    );

    public static final DataComponentType<FilterContent> FILTER_CONTENT = register(
        "filter_contents",
        b -> b.persistent(FilterContent.CODEC.codec()).networkSynchronized(FilterContent.STREAM_CODEC)
    );

    private static <T> @NotNull DataComponentType<T> register(String name, @NotNull Consumer<DataComponentType.Builder<T>> customizer) {
        var builder = DataComponentType.<T>builder();
        customizer.accept(builder);
        var componentType = builder.build();
        DR.register(name, () -> componentType);
        return componentType;
    }

    public static void register(IEventBus bus) {
        DR.register(bus);
    }

    @SuppressWarnings("SameParameterValue")
    private static @NotNull DataComponentType<Unit> registerEmpty(String name) {
        return register(
            name,
            b -> b.persistent(Codec.EMPTY.codec()).networkSynchronized(StreamCodec.unit(Unit.INSTANCE))
        );
    }
}
