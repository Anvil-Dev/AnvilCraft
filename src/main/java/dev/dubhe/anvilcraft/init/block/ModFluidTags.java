package dev.dubhe.anvilcraft.init.block;

import dev.dubhe.anvilcraft.AnvilCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

public class ModFluidTags {

    public static final TagKey<Fluid> OIL = ModFluidTags.bindC("oil");
    public static final TagKey<Fluid> CEMENT = ModFluidTags.bindC("cement");
    public static final TagKey<Fluid> EXPERIENCE = ModFluidTags.bindC("experience");
    public static final TagKey<Fluid> IGNITABLE = ModFluidTags.bind("ignitable");

    /**
     * 列出无法进入无限状态的流体：即使储罐已增强并达到无限阈值，也不会转为无限供给。
     * 由整合包或附属模组把不希望无限的流体加入本标签。
     */
    public static final TagKey<Fluid> CANNOT_BE_INFINITE = ModFluidTags.bind("cannot_be_infinite");

    public static final TagKey<Fluid> MENGER_SPONGE_CAN_ABSORB = ModFluidTags.bind("menger_sponge_can_absorb");

    public static TagKey<Fluid> bindC(String id) {
        return TagKey.create(Registries.FLUID, Identifier.fromNamespaceAndPath("c", id));
    }

    private static TagKey<Fluid> bind(String id) {
        return TagKey.create(Registries.FLUID, AnvilCraft.of(id));
    }
}
