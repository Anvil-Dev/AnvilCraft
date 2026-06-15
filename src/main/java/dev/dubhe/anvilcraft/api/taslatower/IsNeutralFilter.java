package dev.dubhe.anvilcraft.api.taslatower;

import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.NeutralMob;

public class IsNeutralFilter extends TeslaFilter {
    @Getter
    private final String id = "IsNeutralFilter";

    @Override
    public boolean match(LivingEntity entity, String arg) {
        return entity instanceof NeutralMob;
    }

    @Override
    public Component title() {
        return Component.translatable("screen.anvilcraft.tesla_tower.filter.is_neutral");
    }
}