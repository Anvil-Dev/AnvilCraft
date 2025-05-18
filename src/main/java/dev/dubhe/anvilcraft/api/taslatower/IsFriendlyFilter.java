package dev.dubhe.anvilcraft.api.taslatower;

import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

@Getter
public class IsFriendlyFilter extends TeslaFilter {
    private final String id = "IsFriendlyFilter";

    @Override
    public boolean match(LivingEntity entity, String arg) {
        return entity.getType().getCategory().isFriendly();
    }

    @Override
    public Component title() {
        return Component.translatable("screen.anvilcraft.tesla_tower.filter.is_friendly");
    }
}