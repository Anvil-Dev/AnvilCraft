package dev.dubhe.anvilcraft.init.block;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

public class ModFluidTags {

    public static final TagKey<Fluid> OIL = bindC("oil");
    public static final TagKey<Fluid> CEMENT = bindC("cement");
    public static final TagKey<Fluid> EXPERIENCE = bindC("experience");
    public static final TagKey<Fluid> IGNITABLE = bind("ignitable");

    public static TagKey<Fluid> bindC(String id) {
        return TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("c", id));
    }

    private static TagKey<Fluid> bind(String id) {
        return TagKey.create(Registries.FLUID, AnvilCraft.of(id));
    }
}
