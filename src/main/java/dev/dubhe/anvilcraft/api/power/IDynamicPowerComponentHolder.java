package dev.dubhe.anvilcraft.api.power;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

public interface IDynamicPowerComponentHolder {
    AABB anvilcraft$getPowerSupplyingBoundingBox();

    void anvilcraft$gridTick();

    void anvilcraft$switchTo(@Nullable PowerGrid grid);

    DynamicPowerComponent anvilcraft$getPowerComponent();

    static IDynamicPowerComponentHolder of(ServerPlayer player) {
        return (IDynamicPowerComponentHolder) player;
    }
}
