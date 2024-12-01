package dev.dubhe.anvilcraft.api.taslatower;

import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

public class IsEntityIdFilter extends TeslaFilter{
    @Getter
    private final String id = "IsEntityIdFilter";

    @Override
    public boolean match(LivingEntity entity, String arg) { return entity.getType().getDescriptionId().equals(arg); }

    @Override
    public boolean needArg() { return true; }

    @Override
    public Component title() {
        return Component.translatable("screen.anvilcraft.tesla_tower.filter.is_entity_id");
    }

    @Override
    public String tooltip(String arg) {
        return arg;
    }
}