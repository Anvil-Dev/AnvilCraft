package dev.dubhe.anvilcraft.api.taslatower;

import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.LivingEntity;

public class IsBabyFriendlyFilter extends TeslaFilter {
    @Getter
    private final String id = "IsBabyFriendlyFilter";

    @Override
    public boolean match(LivingEntity entity, String arg) {
        return entity.getType().getCategory().isFriendly() && entity instanceof AgeableMob ageable && ageable.isBaby();
    }

    @Override
    public Component title() {
        return Component.translatable("screen.anvilcraft.tesla_tower.filter.is_baby_friendly");
    }
}