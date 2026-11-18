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
    /**
     * 列出无法进入无限状态的流体：即使储罐已增强并达到无限阈值，也不会转为无限供给。
     * 由整合包或附属模组把不希望无限的流体加入本标签。
     */
    public static final TagKey<Fluid> CANNOT_BE_INFINITE = bind("cannot_be_infinite");

    public static TagKey<Fluid> bindC(String id) {
        return TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("c", id));
    }

    private static TagKey<Fluid> bind(String id) {
        return TagKey.create(Registries.FLUID, AnvilCraft.of(id));
    }
}
