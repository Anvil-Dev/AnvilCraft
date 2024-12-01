package dev.dubhe.anvilcraft.api.taslatower;

import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

public class HasCustomNameFilter extends TeslaFilter{
    @Getter
    private final String id = "HasCustomNameFilter";

    @Override
    public boolean match(LivingEntity entity, String arg) { return entity.getCustomName() != null;}

    @Override
    public Component title() {
        return Component.translatable("screen.anvilcraft.tesla_tower.filter.has_custom_name");
    }
}