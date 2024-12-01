package dev.dubhe.anvilcraft.api.taslatower;

import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;

public class IsPetFilter extends TeslaFilter{
    @Getter
    private final String id = "IsPetFilter";

    @Override
    public boolean match(LivingEntity entity, String arg) {
        return entity instanceof TamableAnimal tamableAnimal && tamableAnimal.getOwner() != null;
    }

    @Override
    public Component title() {
        return Component.translatable("screen.anvilcraft.tesla_tower.filter.is_pet");
    }
}