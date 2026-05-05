package dev.dubhe.anvilcraft.api.tooltip.providers;

import net.minecraft.world.phys.AABB;

import org.jspecify.annotations.Nullable;

/**
 * 拥有作用范围的方块实体
 */
public interface IHasAffectRange {
    @Nullable
    AABB shape();
}
