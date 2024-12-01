package dev.dubhe.anvilcraft.api.taslatower;

import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;

public class IsBabyFriendlyFilter extends TeslaFilter{
    @Getter
    private final String id = "IsBabyFriendlyFilter";

    @Override
    public boolean match(LivingEntity entity, String arg) {
        return entity.getType().getCategory().isFriendly() && entity instanceof Animal animal && animal.isBaby();
    }

    @Override
    public Component title() {
        return Component.translatable("screen.anvilcraft.tesla_tower.filter.is_baby_friendly");
    }
}