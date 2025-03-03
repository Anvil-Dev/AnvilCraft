package dev.dubhe.anvilcraft.api.power;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;

public interface IDynamicPowerComponentHolder {
    static IDynamicPowerComponentHolder of(ServerPlayer player) {
        return (IDynamicPowerComponentHolder) player;
    }

    AABB anvilCraft$getPowerSupplyingBoundingBox();

    void anvilCraft$gridTick();

    DynamicPowerComponent anvilCraft$getPowerComponent();
}
