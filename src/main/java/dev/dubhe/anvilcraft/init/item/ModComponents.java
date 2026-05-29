package dev.dubhe.anvilcraft.init.item;

import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.item.property.component.BoxContents;
import dev.dubhe.anvilcraft.item.property.component.DiskData;
import dev.dubhe.anvilcraft.item.property.component.Eternal;
import dev.dubhe.anvilcraft.item.property.component.Ferocious;
import dev.dubhe.anvilcraft.item.property.component.FilterContent;
import dev.dubhe.anvilcraft.item.property.component.HeliostatsData;
import dev.dubhe.anvilcraft.item.property.component.Merciless;
import dev.dubhe.anvilcraft.item.property.component.MultiphaseRef;
import dev.dubhe.anvilcraft.item.property.component.OverLimitItemContainerContents;
import dev.dubhe.anvilcraft.item.property.component.PillBocContents;
import dev.dubhe.anvilcraft.item.property.component.Providence;
import dev.dubhe.anvilcraft.item.property.component.SavedEntity;
import dev.dubhe.anvilcraft.item.property.component.SignedPlayers;
import dev.dubhe.anvilcraft.item.property.component.StoredItem;
import dev.dubhe.anvilcraft.item.property.component.StructureData;
import dev.dubhe.anvilcraft.item.property.component.StructureDiskData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Consumer;

public class ModComponents {
    public static final DeferredRegister<DataComponentType<?>> DR = DeferredRegister.create(
        Registries.DATA_COMPONENT_TYPE, AnvilCraft.MOD_ID
    );

    public static final DataComponentType<DiskData> DISK_DATA = register(
        "disk_data",
        b -> b.persistent(DiskData.CODEC).networkSynchronized(DiskData.STREAM_CODEC)
    );

    public static final DataComponentType<StructureDiskData> STRUCTURE_DISK_DATA = register(
        "structure_disk_data",
        b -> b.persistent(StructureDiskData.CODEC).networkSynchronized(StructureDiskData.STREAM_CODEC)
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

    public static final DataComponentType<MultiphaseRef> MULTIPHASE = register(
        "multiphase",
        b -> b.persistent(MultiphaseRef.CODEC.codec()).networkSynchronized(MultiphaseRef.STREAM_CODEC)
    );

    public static final DataComponentType<Merciless> MERCILESS = register(
        "merciless",
        b -> b.persistent(Merciless.CODEC.codec()).networkSynchronized(Merciless.STREAM_CODEC)
    );

    public static final DataComponentType<Ferocious> FEROCIOUS = register(
        "ferocious",
        b -> b.persistent(Ferocious.CODEC.codec()).networkSynchronized(Ferocious.STREAM_CODEC)
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

    public static final DataComponentType<ItemEnchantments> MERCILESS_ENCHANTMENTS = register(
        "merciless_enchantments",
        b -> b.persistent(ItemEnchantments.CODEC).networkSynchronized(ItemEnchantments.STREAM_CODEC)
    );

    public static final DataComponentType<ItemEnchantments> DISABLED_ENCHANTMENTS = register(
        "disabled_enchantments",
        b -> b.persistent(ItemEnchantments.CODEC).networkSynchronized(ItemEnchantments.STREAM_CODEC)
    );

    public static final DataComponentType<Boolean> CAN_TAKE_OUT_AMMO = register(
        "can_take_out_ammo",
        it -> it.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL)
    );

    public static final DataComponentType<Boolean> WEAKENING = register(
        "weakening",
        b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL)
    );

    public static final DataComponentType<PillBocContents> PILL_BOC_CONTENTS = register(
        "pill_box_contents",
        (builder) -> builder.persistent(PillBocContents.CODEC).networkSynchronized(PillBocContents.STREAM_CODEC)
    );

    public static final DataComponentType<OverLimitItemContainerContents> OVER_LIMIT_CONTAINER = register(
        "over_limit_item_container_contents",
        b -> b.persistent(OverLimitItemContainerContents.CODEC).networkSynchronized(OverLimitItemContainerContents.STREAM_CODEC)
    );

    private static <T> DataComponentType<T> register(String name, Consumer<DataComponentType.Builder<T>> customizer) {
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
    private static DataComponentType<Unit> registerEmpty(String name) {
        return register(
            name,
            b -> b.persistent(Codec.EMPTY.codec()).networkSynchronized(StreamCodec.unit(Unit.INSTANCE))
        );
    }
}
