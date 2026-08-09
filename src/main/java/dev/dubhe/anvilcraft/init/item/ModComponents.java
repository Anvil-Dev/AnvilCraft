package dev.dubhe.anvilcraft.init.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.item.property.component.BoxContents;
import dev.dubhe.anvilcraft.item.property.component.CanTakeOutAmmo;
import dev.dubhe.anvilcraft.item.property.component.DevourRange;
import dev.dubhe.anvilcraft.item.property.component.DiskData;
import dev.dubhe.anvilcraft.item.property.component.Eternal;
import dev.dubhe.anvilcraft.item.property.component.Ferocious;
import dev.dubhe.anvilcraft.item.property.component.FilterContent;
import dev.dubhe.anvilcraft.item.property.component.FlightTime;
import dev.dubhe.anvilcraft.item.property.component.HeliostatsData;
import dev.dubhe.anvilcraft.item.property.component.Merciless;
import dev.dubhe.anvilcraft.item.property.component.Multiphase;
import dev.dubhe.anvilcraft.item.property.component.OverLimitItemContainerContents;
import dev.dubhe.anvilcraft.item.property.component.PillBoxContents;
import dev.dubhe.anvilcraft.item.property.component.SavedEntity;
import dev.dubhe.anvilcraft.item.property.component.StorageRef;
import dev.dubhe.anvilcraft.item.property.component.StoredEnergy;
import dev.dubhe.anvilcraft.item.property.component.StoredFluids;
import dev.dubhe.anvilcraft.item.property.component.StoredItem;
import dev.dubhe.anvilcraft.item.property.component.StructureData;
import dev.dubhe.anvilcraft.item.property.component.StructureDiskData;
import dev.dubhe.anvilcraft.item.property.component.amulet.IAmulet;
import dev.dubhe.anvilcraft.item.tool.HeavyHalberdMode;
import dev.dubhe.anvilcraft.item.tool.MultitoolMode;
import dev.dubhe.anvilcraft.item.tool.ResonateMode;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Unit;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Consumer;

public class ModComponents {
    public static final DeferredRegister<DataComponentType<?>> DR = DeferredRegister.create(
        Registries.DATA_COMPONENT_TYPE, AnvilCraft.MOD_ID
    );

    public static final DataComponentType<DiskData> DISK_DATA = ModComponents.register(
        "disk_data",
        b -> b.persistent(DiskData.CODEC).networkSynchronized(DiskData.STREAM_CODEC)
    );

    public static final DataComponentType<SavedEntity> SAVED_ENTITY = ModComponents.register(
        "saved_entity",
        b -> b.persistent(SavedEntity.CODEC).networkSynchronized(SavedEntity.STREAM_CODEC)
    );

    public static final DataComponentType<HeliostatsData> HELIOSTATS_DATA = ModComponents.register(
        "heliostats_data",
        b -> b.persistent(HeliostatsData.CODEC).networkSynchronized(HeliostatsData.STREAM_CODEC)
    );

    public static final DataComponentType<StructureData> STRUCTURE_DATA = ModComponents.register(
        "structure_data",
        b -> b.persistent(StructureData.CODEC).networkSynchronized(StructureData.STREAM_CODEC)
    );

    public static final DataComponentType<StructureDiskData> STRUCTURE_DISK_DATA = ModComponents.register(
        "structure_disk_data",
        b -> b.persistent(StructureDiskData.CODEC).networkSynchronized(StructureDiskData.STREAM_CODEC)
    );

    public static final DataComponentType<StoredItem> DISPLAY_ITEM = ModComponents.register(
        "display_item",
        b -> b.persistent(StoredItem.CODEC).networkSynchronized(StoredItem.STREAM_CODEC)
    );

    public static final DataComponentType<FlightTime> FLIGHT_TIME = ModComponents.register(
        "flight_time",
        it -> it.persistent(FlightTime.CODEC.codec()).networkSynchronized(FlightTime.STREAM_CODEC)
    );

    public static final DataComponentType<StoredEnergy> STORED_ENERGY = ModComponents.register(
        "stored_energy",
        builder -> builder.persistent(StoredEnergy.CODEC.codec()).networkSynchronized(StoredEnergy.STREAM_CODEC)
    );

    public static final DataComponentType<ChargedProjectiles> RAILGUN_AMMO = ModComponents.register(
        "railgun_ammo",
        builder -> builder.persistent(ChargedProjectiles.CODEC).networkSynchronized(ChargedProjectiles.STREAM_CODEC)
    );

    public static final DataComponentType<Integer> RAILGUN_INFINITE_AMMO_MASK = ModComponents.register(
        "railgun_infinite_ammo_mask",
        builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.INT)
    );

    public static final DataComponentType<Unit> FIRE_REFORGING = ModComponents.registerEmpty("reforging");

    public static final DataComponentType<Multiphase> MULTIPHASE = ModComponents.register(
        "multiphase",
        b -> b.persistent(Multiphase.CODEC.codec()).networkSynchronized(Multiphase.STREAM_CODEC)
    );

    public static final DataComponentType<Merciless> MERCILESS = ModComponents.register(
        "merciless",
        b -> b.persistent(Merciless.CODEC.codec()).networkSynchronized(Merciless.STREAM_CODEC)
    );

    public static final DataComponentType<Ferocious> FEROCIOUS = ModComponents.register(
        "ferocious",
        b -> b.persistent(Ferocious.CODEC.codec()).networkSynchronized(Ferocious.STREAM_CODEC)
    );

    public static final DataComponentType<DevourRange> DEVOUR_RANGE = ModComponents.register(
        "devour_range",
        b -> b.persistent(DevourRange.CODEC).networkSynchronized(DevourRange.STREAM_CODEC)
    );

    public static final DataComponentType<BoxContents> BOX_CONTENTS = ModComponents.register(
        "box_contents",
        b -> b.persistent(BoxContents.CODEC).networkSynchronized(BoxContents.STREAM_CODEC)
    );

    public static final DataComponentType<Eternal> ETERNAL = ModComponents.register(
        "eternal",
        b -> b.persistent(Eternal.CODEC.codec()).networkSynchronized(Eternal.STREAM_CODEC)
    );

    public static final DataComponentType<Unit> PROVIDENCE = ModComponents.registerEmpty("providence");

    public static final DataComponentType<FilterContent> FILTER_CONTENT = ModComponents.register(
        "filter_contents",
        b -> b.persistent(FilterContent.CODEC.codec()).networkSynchronized(FilterContent.STREAM_CODEC)
    );

    public static final DataComponentType<ItemEnchantments> MERCILESS_ENCHANTMENTS = ModComponents.register(
        "merciless_enchantments",
        b -> b.persistent(ItemEnchantments.CODEC).networkSynchronized(ItemEnchantments.STREAM_CODEC)
    );

    public static final DataComponentType<ItemEnchantments> DISABLED_ENCHANTMENTS = ModComponents.register(
        "disabled_enchantments",
        b -> b.persistent(ItemEnchantments.CODEC).networkSynchronized(ItemEnchantments.STREAM_CODEC)
    );

    public static final DataComponentType<Holder<Enchantment>> LIQUID_ENCHANTMENT = ModComponents.register(
        "liquid_enchantment",
        b -> b.persistent(Enchantment.CODEC).networkSynchronized(Enchantment.STREAM_CODEC)
    );

    public static final DataComponentType<CanTakeOutAmmo> CAN_TAKE_OUT_AMMO = ModComponents.register(
        "can_take_out_ammo",
        it -> it.persistent(CanTakeOutAmmo.CODEC).networkSynchronized(CanTakeOutAmmo.STREAM_CODEC)
    );

    public static final DataComponentType<Boolean> WEAKENING = ModComponents.register(
        "weakening",
        b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL)
    );

    public static final DataComponentType<PillBoxContents> PILL_BOX_CONTENTS = ModComponents.register(
        "pill_box_contents",
        builder -> builder.persistent(PillBoxContents.CODEC).networkSynchronized(PillBoxContents.STREAM_CODEC)
    );

    public static final DataComponentType<OverLimitItemContainerContents> OVER_LIMIT_CONTAINER = ModComponents.register(
        "over_limit_item_container_contents",
        b -> b.persistent(OverLimitItemContainerContents.CODEC).networkSynchronized(OverLimitItemContainerContents.STREAM_CODEC)
    );

    public static final DataComponentType<ResonateMode> RESONATE_MODE = ModComponents.register(
        "resonate_mode",
        b -> b.persistent(ResonateMode.CODEC).networkSynchronized(ResonateMode.STREAM_CODEC)
    );

    public static final DataComponentType<MultitoolMode> MULTITOOL_MODE = ModComponents.register(
        "multitool_mode",
        b -> b.persistent(MultitoolMode.CODEC).networkSynchronized(MultitoolMode.STREAM_CODEC)
    );

    public static final DataComponentType<HeavyHalberdMode> HEAVY_HALBERD_MODE = ModComponents.register(
        "heavy_halberd_mode",
        b -> b.persistent(HeavyHalberdMode.CODEC).networkSynchronized(HeavyHalberdMode.STREAM_CODEC)
    );

    public static final DataComponentType<ItemContainerContents> BURNING_HEATER_CONTENTS = ModComponents.register(
        "burning_heater_contents",
        b -> b.persistent(ItemContainerContents.CODEC).networkSynchronized(ItemContainerContents.STREAM_CODEC)
    );

    public static final DataComponentType<StoredFluids> CREATIVE_TANK_FLUIDS = ModComponents.register(
        "creative_tank_fluids",
        b -> b.persistent(StoredFluids.CODEC).networkSynchronized(StoredFluids.STREAM_CODEC)
    );

    public static final DataComponentType<IAmulet> AMULET = ModComponents.register(
        "amulet",
        b -> b.persistent(IAmulet.CODEC).networkSynchronized(IAmulet.STREAM_CODEC)
    );

    public static final DataComponentType<StorageRef> STORAGE = ModComponents.register(
        "storage",
        b -> b.persistent(StorageRef.CODEC.codec()).networkSynchronized(StorageRef.STREAM_CODEC)
    );

    private static <T> DataComponentType<T> register(String name, Consumer<DataComponentType.Builder<T>> customizer) {
        var builder = DataComponentType.<T>builder();
        customizer.accept(builder);
        var componentType = builder.build();
        ModComponents.DR.register(name, () -> componentType);
        return componentType;
    }

    public static void register(IEventBus bus) {
        ModComponents.DR.register(bus);
    }

    @SuppressWarnings("SameParameterValue")
    private static DataComponentType<Unit> registerEmpty(String name) {
        return ModComponents.register(
            name,
            b -> b.persistent(MapCodec.unit(Unit.INSTANCE).codec()).networkSynchronized(StreamCodec.unit(Unit.INSTANCE))
        );
    }
}
