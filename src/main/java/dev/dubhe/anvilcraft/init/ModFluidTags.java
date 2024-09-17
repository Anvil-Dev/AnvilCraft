package dev.dubhe.anvilcraft.init;

import dev.dubhe.anvilcraft.AnvilCraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

import org.jetbrains.annotations.NotNull;

public class ModFluidTags {
    public static final TagKey<Fluid> MENGER_SPONGE_CAN_ABSORB = bind("menger_sponge_can_absorb");

    private static @NotNull TagKey<Fluid> bind(String id) {
        return TagKey.create(Registries.FLUID, AnvilCraft.of(id));
    }
}
