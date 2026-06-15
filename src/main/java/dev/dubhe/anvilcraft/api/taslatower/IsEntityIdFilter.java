package dev.dubhe.anvilcraft.api.taslatower;

import lombok.Getter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

public class IsEntityIdFilter extends TeslaFilter {
    @Getter
    private final String id = "IsEntityIdFilter";

    @Override
    public boolean match(LivingEntity entity, String arg) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString().equals(arg);
    }

    @Override
    public boolean needArg() {
        return true;
    }

    @Override
    public Component title() {
        return Component.translatable("screen.anvilcraft.tesla_tower.filter.is_entity_id");
    }

    @Override
    public Component getTitle(String arg) {
        EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.tryParse(arg));
        return Component.translatable(type.getDescriptionId());
    }

    @Override
    public String tooltip(String arg) {
        return Component.translatable("screen.anvilcraft.tesla_tower.filter.is_entity_id").getString();
    }
}