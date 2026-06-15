package dev.dubhe.anvilcraft.api.teslatower;

import lombok.Getter;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
        int colonIndex = arg.indexOf(':');
        if (colonIndex < 0 || colonIndex >= arg.length() - 1) {
            return Component.literal(arg);
        }
        Identifier identifier = Identifier.fromNamespaceAndPath(
            arg.substring(0, colonIndex),
            arg.substring(colonIndex + 1)
        );
        return BuiltInRegistries.ENTITY_TYPE.get(identifier)
            .map(ref -> Component.translatable(ref.value().getDescriptionId()))
            .orElse(Component.literal(arg));
    }

    @Override
    public String tooltip(String arg) {
        return Component.translatable("screen.anvilcraft.tesla_tower.filter.is_entity_id").getString();
    }
}