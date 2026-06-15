package dev.dubhe.anvilcraft.api.taslatower;

import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;

public class IsHostileFilter extends TeslaFilter {
    @Getter
    private final String id = "IsHostileFilter";

    @Override
    public boolean match(LivingEntity entity, String arg) {
        return entity.getType().getCategory() == MobCategory.MONSTER;
    }

    @Override
    public Component title() {
        return Component.translatable("screen.anvilcraft.tesla_tower.filter.is_hostile");
    }
}