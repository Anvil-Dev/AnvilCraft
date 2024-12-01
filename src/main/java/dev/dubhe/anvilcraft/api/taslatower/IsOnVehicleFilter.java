package dev.dubhe.anvilcraft.api.taslatower;

import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

public class IsOnVehicleFilter extends TeslaFilter{
    @Getter
    private final String id = "IsOnVehicleFilter";

    @Override
    public boolean match(LivingEntity entity, String arg) { return entity.getVehicle() != null;}

    @Override
    public Component title() {
        return Component.translatable("screen.anvilcraft.tesla_tower.filter.is_on_vehicle");
    }
}