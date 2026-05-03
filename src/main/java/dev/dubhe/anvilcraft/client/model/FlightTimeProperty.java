package dev.dubhe.anvilcraft.client.model;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.dubhe.anvilcraft.AnvilCraft;
import dev.dubhe.anvilcraft.init.item.ModComponents;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record FlightTimeProperty() implements RangeSelectItemModelProperty {
    public static final MapCodec<FlightTimeProperty> CODEC = MapCodec.unit(new FlightTimeProperty());

    @Override
    public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        return (float) stack.get(ModComponents.FLIGHT_TIME) / AnvilCraft.CONFIG.ionoCraftBackpackMaxFlightTime;
    }

    @Override
    public MapCodec<? extends RangeSelectItemModelProperty> type() {
        return FlightTimeProperty.CODEC;
    }
}
